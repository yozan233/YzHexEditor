package com.peke.hex.editor.utils;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.peke.hex.editor.bean.ByteBean;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileDataEditor {

    private File srcFile = null;
    private RandomAccessFile srcFileReader = null;

    private final Context context;
    //数据库
    private BytesModifiedDbHelper bytesModifiedDbHelper;
    //历史记录当前指针
    private int mHistoryPointer = -1;
    //当前历史记录大小
    private List<DataHistory> mHistoryRangeStack = new ArrayList<>();

    private HexRangeHelper hexRangeHelper;

    public FileDataEditor(Context context) {
        this.context = context;
    }

    public FileDataEditor(Context context,File srcFile) {
        this(context);
        setSrcFile(srcFile);
    }

    public void setSrcFile(File file){
        destroy();
        try {
            srcFile = file;
            srcFileReader = new RandomAccessFile(file,"rw");
            bytesModifiedDbHelper = new BytesModifiedDbHelper(context,srcFile.getAbsolutePath());
            mHistoryPointer = -1;
            mHistoryRangeStack.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHexRangeHelper(HexRangeHelper hexRangeHelper) {
        this.hexRangeHelper = hexRangeHelper;
    }

    /**
     * 读取-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * 读取1字节
     */
    public ByteBean readByteBean(int index){
        return readByteBeans(index,1)[0];
    }

    /**
     * 读取bytes数据
     */
    public ByteBean[] readByteBeans(int index, int length){
        byte[] bytes = readOriginalBytes(index, length);
        ByteBean[] hexData = ByteBean.parseBytes(bytes);
        queryModifiedByteBeans(index, hexData);
        return hexData;
    }

    public byte readByte(int index){
        return readBytes(index,1)[0];
    }

    /**
     * 读取bytes数据
     */
    public byte[] readBytes(int index, int length){
        byte[] bytes = readOriginalBytes(index, length);
        queryModifiedBytes(index, bytes);
        return bytes;
    }

    private byte readOriginalByte(int index){
        byte b = 0;
        try {
            srcFileReader.seek(index);
            b = srcFileReader.readByte();
        } catch (Exception ignored) { }
        return b;
    }

    /**
     * 读取原始数据
     */
    private byte[] readOriginalBytes(int index,int length){
        if (length <= 0) {
            return new byte[0];
        }
        int fileLength = length();
        if (index + length > fileLength){
            length = fileLength - index;
        }
        if (length <= 0) {
            return new byte[0];
        }
        byte[] bytes = new  byte[length];
        try {
            srcFileReader.seek(index);
            srcFileReader.read(bytes);
        } catch (Exception e) {
            bytes = new byte[0];
        }
        return bytes;
    }

    public int findData(byte[] search, int startIndex){
        return findData(search, startIndex, -1);
    }

    public int findData(byte[] search, int startIndex, int endIndex) {
        SearchHelper searchHelper = new SearchHelper(this);
        return searchHelper.startSearch(search, startIndex, endIndex);
    }

    /**
     * 写入-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * 写入半字节
     * @param isFront 写入前半字节
     */
    public void writeHalfByte(int index,int b,boolean isFront){
        ByteBean bean = readByteBean(index);
        int[] halfValues = new int[2];
        if (isFront){
            halfValues[0] = b & 0xF;
            halfValues[1] = bean.value & 0xF;
        }
        else {
            halfValues[0] = bean.value >> 4;
            halfValues[1] = b & 0xF;
        }
        int value = (halfValues[0] << 4) | halfValues[1];
        writeByte(index, (byte) value);
    }

    /**
     * 写入1字节
     */
    public void writeByte(int index,byte b){
        writeBytes(index,new byte[]{b});
    }

    /**
     * 写入数据
     */
    public void writeBytes(int index, byte[] bytes){
        writeBytes(index,bytes,true);
    }

    private void writeBytes(int index,byte[] modifiedBytes,boolean history){
        byte[] orgBytes = readOriginalBytes(index, modifiedBytes.length);
        writeBytes(index,modifiedBytes,orgBytes,history);
    }

    /**
     * 写入数据
     * @param history 是否记录历史
     */
    private void writeBytes(int index,byte[] modifiedBytes,byte[] orgBytes,boolean history){

        if (history){
            if (mHistoryRangeStack.size() > mHistoryPointer+1){
                mHistoryRangeStack = mHistoryRangeStack.subList(0,mHistoryPointer+1);
            }
            mHistoryRangeStack.add(new DataHistory(
                    hexRangeHelper.getStartPosition()
                    ,hexRangeHelper.getStartOffset()
                    ,index
                    ,modifiedBytes.length)
            );
            mHistoryPointer = mHistoryRangeStack.size()-1;
            ByteBean[] lastData = ByteBean.parseBytes(orgBytes);
            queryModifiedByteBeans(index,lastData);
            insertHistory(mHistoryPointer,index,modifiedBytes,lastData);
        }

        insertBytesToDb(index,modifiedBytes,orgBytes);
        deleteUnmodifiedData();
//        logIndex(index);
//        logBytes(modifiedBytes);


    }

    private void logIndex(int i){
        Log.e("FileDataEditor","index="+Integer.toHexString(i).toUpperCase());
    }

    private void logBytes(byte[] bytes){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (byte b : bytes) {
            stringBuilder.append(',')
                    .append(Integer.toHexString(b & 0xFF).toUpperCase());
        }
        stringBuilder.append(']');
        Log.e("FileDataEditor","bytes="+stringBuilder);
    }

    /**
     * 数据库-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * 查询已修改的数据
     * @param orgData 原始数据
     */
    private void queryModifiedByteBeans(int index, ByteBean[] orgData){
        bytesModifiedDbHelper.queryModifiedByteBeans(index,orgData);
    }

    private void queryModifiedBytes(int index, byte[] orgData){
        bytesModifiedDbHelper.queryModifiedBytes(index,orgData);
    }

    /**
     * 写入修改的数据到数据库
     * @param index 数据位置
     * @param modifiedBytes 修改的数据
     * @param orgBytes 原始数据
     */
    private void insertBytesToDb(int index, byte[] modifiedBytes, byte[] orgBytes){
        bytesModifiedDbHelper.insertBytesToDb(index,modifiedBytes,orgBytes);
    }

    /**
     * 读取历史数据写入到修改数据的数据库
     * @param undo true:undo，false:redo
     */
    private void insertHistoryToDb(int index, List<Pair<Integer, Integer>> historyBytes,boolean undo){
        byte[] orgBytes = readOriginalBytes(index, historyBytes.size());
        bytesModifiedDbHelper.insertHistoryToDb(index,historyBytes,orgBytes,undo);
    }

    /**
     * 添加历史数据
     * @param pointer 指针位置
     * @param index 数据位置
     * @param bytes 写入的数据
     * @param lastBytes 最后修改的数据（当前页面数据）
     */
    private void insertHistory(int pointer,int index,byte[] bytes,ByteBean[] lastBytes) {
        bytesModifiedDbHelper.insertHistory(pointer,index,bytes,lastBytes);
    }

    /**
     * 删除未修改的byte数据
     */
    private void deleteUnmodifiedData(){
        bytesModifiedDbHelper.deleteUnmodifiedData();
    }

    /**
     * 查询修改历史
     * @return List Pair&lt;修改数据,上一次数据&gt;
     */
    private List<Pair<Integer,Integer>> queryModifiedHistory(int pointer){
        return bytesModifiedDbHelper.queryModifiedHistory(pointer);
    }

    /**
     * 撤销与重做-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    // 0  1  2
    // AA AA AA => p=0
    // BA AA AA => p=1,h={index=0,history=BA,last=AA}
    // BB AA AA => p=2,h={index=0,history=BB,last=BA}
    // BB AA BA => p=3,h={index=2,history=BA,last=AA}
    // BB AA BB => p=4,h={index=2,history=BB,last=BA}

    //undo1: ,

    /**
     * 撤销
     */
    public DataHistory undo(){
        if (!canUndo())
            return null;

        DataHistory history = mHistoryRangeStack.get(mHistoryPointer);

        List<Pair<Integer, Integer>> historyBytes = queryModifiedHistory(mHistoryPointer);
        insertHistoryToDb(history.getIndex(),historyBytes,true);

        mHistoryPointer--;

        deleteUnmodifiedData();
        return history;
    }

    /**
     * 重做
     */
    public DataHistory redo(){
        if (!canRedo())
            return null;

        mHistoryPointer++;

        DataHistory history = mHistoryRangeStack.get(mHistoryPointer);

        List<Pair<Integer, Integer>> historyBytes = queryModifiedHistory(mHistoryPointer);
        insertHistoryToDb(history.getIndex(),historyBytes,false);


        deleteUnmodifiedData();

        return history;
    }

    public boolean canRedo(){
        return mHistoryRangeStack.size()-1 > mHistoryPointer;
    }

    public boolean canUndo(){
        return mHistoryPointer >= 0;
    }


    /**
     * 其它-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    /**
     * 获取源文件
     */
    public File getSrcFile() {
        return srcFile;
    }

    /**
     * 文件另存为
     */
    public void saveToFile(File dstFile){
        if (dstFile == null){
            saveFile();
            return;
        }
        try {
            new FastSaveFileHelper(srcFile, dstFile, this).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存文件
     */
    public void saveFile(){
        try {
            IOUtils.closeQuietly(srcFileReader);
            File tempFile = new File(context.getFilesDir(), UUID.randomUUID().toString());
            FileUtils.copyFile(srcFile,tempFile);
            new FastSaveFileHelper(tempFile, srcFile, this).start();
            tempFile.delete();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 文件大小
     */
    public int length(){
        int len = 0;
        try {
            len = (int) srcFileReader.length();
        } catch (Exception ignored) { }
        return len;
    }

    public boolean isEmpty(){
        if (srcFileReader == null)
            return true;
        return length() == 0;
    }

    public boolean isNotEmpty(){
        return !isEmpty();
    }

    public void destroy(){
        IOUtils.closeQuietly(srcFileReader);
        srcFileReader = null;
        if (bytesModifiedDbHelper != null){
            bytesModifiedDbHelper.close();
        }
    }

    /**
     * 保存文件帮助类
     */
    private static class FastSaveFileHelper {
        private final File srcFile;
        private final File dstFile;
        private final FileDataEditor fileDataEditor;

        private static final int THREAD_COUNT = 5;
        private CountDownLatch mCountDownLatch;
        private ExecutorService mThreadPool;
        private final AtomicBoolean mIsCancel = new AtomicBoolean();

        public FastSaveFileHelper(File srcFile, File dstFile, FileDataEditor fileDataEditor) throws IOException {
            this.srcFile = srcFile;
            this.dstFile = dstFile;
            this.fileDataEditor = fileDataEditor;
            if (dstFile.exists()){
                boolean delete = dstFile.delete();
                if (!delete){
                    throw new IOException(dstFile.getAbsolutePath()+" delete fail !");
                }
            }
            boolean newFile = dstFile.createNewFile();
            if (!newFile){
                throw new IOException(dstFile.getAbsolutePath()+" create fail !");
            }
        }

        private void start(){
            cancel();

            if (mThreadPool != null && !mThreadPool.isShutdown())
                mThreadPool.shutdown();

            mThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);

            if (mThreadPool != null && !mThreadPool.isShutdown())
                mThreadPool.shutdown();

            if (mCountDownLatch != null && mCountDownLatch.getCount() > 0){
                try {
                    mCountDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            mThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);

            mIsCancel.set(false);

            mCountDownLatch = new CountDownLatch(THREAD_COUNT);
            int fileSize = (int) srcFile.length();
            int partCount = fileSize/THREAD_COUNT;
            int[] partCounts = new int[]{
                    partCount,partCount,partCount,partCount,
                    partCount+(fileSize%THREAD_COUNT)
            };
            partCount = 0;
            for (int i=0;i<THREAD_COUNT;i++){
                int nextPartCount = partCount + partCounts[i];
                mThreadPool.execute(new FastSaveFileRunnable(
                        partCount,nextPartCount,
                        mCountDownLatch,
                        srcFile,dstFile,
                        fileDataEditor,
                        mIsCancel));
                partCount = nextPartCount;
            }
            try {
                mCountDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThreadPool.shutdown();

        }

        private void cancel(){
            mIsCancel.set(true);
        }

    }
    private static class FastSaveFileRunnable implements Runnable {
        private final int start,end;
        private final File srcFile;
        private final File dstFile;

        private static final int BUFFER_SIZE = 1024 * 1024;
        private final CountDownLatch countDownLatch;
        private final AtomicBoolean isCancel;
        private final FileDataEditor fileDataEditor;

        FastSaveFileRunnable(int start,int end,CountDownLatch countDownLatch,
                             File srcFile,File dstFile,
                             FileDataEditor fileDataEditor,
                             AtomicBoolean isCancel){
            this.start = start;
            this.end = end;
            this.countDownLatch = countDownLatch;
            this.fileDataEditor = fileDataEditor;
            this.isCancel = isCancel;
            this.srcFile = srcFile;
            this.dstFile = dstFile;
        }

        @Override
        public void run() {
            if (start == end){
                finish();
                return;
            }
            int pos = start;
            int readLen;
            RandomAccessFile src = null,dst = null;
            try {
                src = new RandomAccessFile(srcFile, "r");
                dst = new RandomAccessFile(dstFile, "rw");
                byte[] buff = new byte[BUFFER_SIZE];

                src.seek(start);
                dst.seek(start);

                while (!isCancel.get() && !Thread.interrupted()){
                    readLen = (pos + BUFFER_SIZE > end) ? (end -pos) : BUFFER_SIZE;
                    if (readLen < 0)
                        break;
                    src.read(buff,0,readLen);

                    fileDataEditor.queryModifiedBytes(pos,buff);

                    dst.write(buff,0,readLen);
                    pos += BUFFER_SIZE;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                IOUtils.closeQuietly(src,dst);
            }
            finish();
        }

        private void finish(){
            countDownLatch.countDown();
        }

    }

    /**
     * 搜索帮助类
     */
    private static class SearchHelper {

        private static final int DefBufferSize = 10 * 1024 * 1024;

        private final FileDataEditor fileDataEditor;

        public SearchHelper(FileDataEditor fileDataEditor) {
            this.fileDataEditor = fileDataEditor;
        }

        int startSearch(byte[] searchBytes, int startIndex, int endIndex){
            int fileSize = fileDataEditor.length();
            if (fileDataEditor.isEmpty()
                    || searchBytes == null
                    || searchBytes.length == 0
                    || searchBytes.length > fileSize
                    || startIndex < 0
                    || startIndex == endIndex
            ) {
                return -1;
            }
            if (endIndex < 0 || endIndex >= fileSize) {
                endIndex = fileSize;
            }
            int needSrcLength;

            int count = 1;

            do {
                needSrcLength = calcBufferSize(startIndex,endIndex,searchBytes.length);
                if (needSrcLength <= 0)
                    break;
//                LOG.e("HPT","count="+count+",startIndex="+Integer.toHexString(startIndex));
                byte[] buffer = fileDataEditor.readBytes(startIndex,needSrcLength);
                Pair<Integer,Integer> find = findData(buffer, searchBytes);
                if (find.first != -1)
                    return find.first + startIndex;

                startIndex += (needSrcLength - find.second);
                count++;

            }while (startIndex < fileSize && startIndex < endIndex);

            return -1;
        }

        private int calcBufferSize(int startIndex, int endIndex,int searchLength){
            if (startIndex < 0 || startIndex+searchLength > fileDataEditor.length())
                return -1;
            int needSize = endIndex-startIndex;
            return Math.min(needSize, DefBufferSize);
        }

        /**
         * 查找数据
         * @param data 源数据
         * @param searchBytes 需要查找的数据
         * @return first查找到的地址，second最后匹配的byte数量
         */
        private Pair<Integer,Integer> findData(byte[] data,byte[] searchBytes) {
            int i, j = 0;
            for (i = 0; i < data.length; i++) {
                if (data[i] == searchBytes[0] && i + searchBytes.length < data.length) {
                    for (j = 1; j < searchBytes.length; j++) {
                        if (data[i + j] != searchBytes[j]) {
                            break;
                        }
                    }

                    if (j == searchBytes.length) {
                        return new Pair<>(i,j);
                    }
                }
            }
            return new Pair<>(-1,j);
        }


    }

    /**
     * 写入数据的历史记录
     */
    public static class DataHistory {
        public final int position;
        public final int offset;
        public final int length;
        private final int index;

        public DataHistory(int position,int offset,int index,int length) {
            this.position = position;
            this.offset = offset;
            this.index = index;
            this.length = length;
        }

        public int getIndex() {
            return index;
        }
    }

}