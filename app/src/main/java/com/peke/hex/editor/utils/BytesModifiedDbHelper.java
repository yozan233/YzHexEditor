package com.peke.hex.editor.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.peke.hex.editor.bean.ByteBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * bytes修改数据库帮助类
 * 用于存储修改bytes的历史记录
 */
public class BytesModifiedDbHelper {
    /**
     * 已修改的数据
     */
    private static final String TABLE_NAME_MODIFIED_BYTES = "modified_bytes";
    /**
     * 修改历史
     */
    private static final String TABLE_NAME_MODIFIED_HISTORY = "modified_history_bytes";

    private final File dbFileDir;

    private File bytesModifiedDbFile;
    private SQLiteDatabase db;

    private BytesModifiedDbHelper(Context context){
        dbFileDir = new File(context.getFilesDir(),"FileDataWriter");
    }

    public BytesModifiedDbHelper(Context context,String name) {
        this(context);
        String dbFileName = "bytesModified"+ Md5Utils.getMD5(name.getBytes()) + ".db";
        bytesModifiedDbFile = new File(dbFileDir,dbFileName);
        if (bytesModifiedDbFile.exists()){
            bytesModifiedDbFile.delete();
        }
        try {
            File parentFile = bytesModifiedDbFile.getParentFile();
            if (!parentFile.exists())
                parentFile.mkdirs();
            bytesModifiedDbFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        db = SQLiteDatabase.openDatabase(bytesModifiedDbFile.getAbsolutePath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        createDataTable();
        createHistoryTable();

    }

    /**
     * 获取已保存的实例
     * @param name 名称
     */
    public static BytesModifiedDbHelper ofSaveInstance(Context context,String name){
        String dbFileName = "bytesModified"+ Md5Utils.getMD5(name.getBytes()) + ".db";
        File bytesModifiedDbFile = new File(new File(context.getFilesDir(),"FileDataWriter"),dbFileName);
        if (bytesModifiedDbFile.exists()){
            BytesModifiedDbHelper dbHelper = new BytesModifiedDbHelper(context);
            dbHelper.bytesModifiedDbFile = bytesModifiedDbFile;

            dbHelper.db = SQLiteDatabase.openDatabase(bytesModifiedDbFile.getAbsolutePath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            dbHelper.createDataTable();
            dbHelper.createHistoryTable();

            return dbHelper;
        }
        else {
            return new BytesModifiedDbHelper(context,name);
        }
    }

    /**
     * 判断是否有存档
     */
    public boolean hasSaveInstance(String name){
        String dbFileName = "bytesModified"+ Md5Utils.getMD5(name.getBytes()) + ".db";
        File bytesModifiedDbFile = new File(dbFileDir,dbFileName);
        return bytesModifiedDbFile.exists();
    }

    /**
     * 删除数据库
     */
    private void dropDatabase(){
        db.close();
        if (bytesModifiedDbFile.exists()){
            //删除数据库文件
            bytesModifiedDbFile.delete();
            //删除数据库日志文件
            File journalFile = new File(bytesModifiedDbFile.getAbsolutePath()+"-journal");
            if (journalFile.exists())
                journalFile.delete();
        }
    }

    /**
     * 创建历史记录表
     */
    private void createHistoryTable(){
        String create_sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MODIFIED_HISTORY + "("
                + "_pointer INTEGER,"
                + "_index INTEGER,"
                + "_last_byte INTEGER,"
                + "_history_byte INTEGER"
                + ");";
        db.execSQL(create_sql);
    }

    /**
     * 创建数据表
     */
    private void createDataTable(){
        String create_sql = "CREATE TABLE IF NOT EXISTS " + BytesModifiedDbHelper.TABLE_NAME_MODIFIED_BYTES + "("
                + "_index INTEGER UNIQUE,"
                + "_byte INTEGER DEFAULT (256)"
                + ");";
        db.execSQL(create_sql);
    }

    /**
     * 查询修改历史
     * @return List Pair&lt;修改数据,上一次数据&gt;
     */
    public List<Pair<Integer,Integer>> queryModifiedHistory(int pointer){
        List<Pair<Integer,Integer>> bytes = new ArrayList<>();
        String sql = "SELECT * FROM "+TABLE_NAME_MODIFIED_HISTORY + " WHERE _pointer=? ORDER BY _index ASC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(pointer)});
        while (cursor.moveToNext()){
            int _history_byte = cursor.getInt(cursor.getColumnIndex("_history_byte"));
            int _last_byte = cursor.getInt(cursor.getColumnIndex("_last_byte"));
            bytes.add(new Pair<>(_history_byte,_last_byte));
        }
        IOUtils.closeQuietly(cursor);
        return bytes;
    }

    /**
     * 删除修改的数据
     */
    public void deleteModifiedData(int startIndex,int length){
        String whereClause = "_index>=? AND _index<?";
        String[] whereArgs = new String[]{String.valueOf(startIndex),String.valueOf(startIndex+length)};
        db.delete(TABLE_NAME_MODIFIED_BYTES,whereClause,whereArgs);
    }

    /**
     * 删除未修改的byte数据
     */
    public void deleteUnmodifiedData(){
        //删除大于等于当前指针的历史数据
        String whereClause = "_byte>=?";
        String[] whereArgs = new String[]{String.valueOf(256)};
        db.delete(TABLE_NAME_MODIFIED_BYTES,whereClause,whereArgs);
    }

    /**
     * 添加历史数据
     * @param pointer 指针位置
     * @param index 数据位置
     * @param bytes 写入的数据
     * @param lastBytes 最后修改的数据（当前页面数据）
     */
    public void insertHistory(int pointer, int index, byte[] bytes, ByteBean[] lastBytes) {
        //删除大于等于当前指针的历史数据
        String whereClause = "_pointer>=?";
        String[] whereArgs = new String[]{String.valueOf(pointer)};
        db.delete(TABLE_NAME_MODIFIED_HISTORY,whereClause,whereArgs);
        //插入数据
        String sql = "INSERT INTO "+TABLE_NAME_MODIFIED_HISTORY+" (_pointer,_index,_history_byte,_last_byte)" +
                " VALUES ";
        StringBuilder contentValuesBuilder = new StringBuilder();
        for (int i=index,j=0;j<bytes.length;i++,j++){
            if (j != 0)
                contentValuesBuilder.append(',');
            contentValuesBuilder.append('(')
                    .append(pointer).append(',')
                    .append(i).append(',')
                    .append(bytes[j]).append(',')
                    .append(lastBytes[j].value)
                    .append(')');
        }
        sql += contentValuesBuilder.toString();
        db.execSQL(sql);
    }

    /**
     * 写入修改的数据到数据库
     * @param index 数据位置
     * @param modifiedBytes 修改的数据
     * @param orgBytes 原始数据
     */
    public void insertBytesToDb(int index, byte[] modifiedBytes, byte[] orgBytes){
        String sql = "INSERT OR REPLACE INTO "+ TABLE_NAME_MODIFIED_BYTES +" (_index,_byte)" +
                " VALUES ";
        StringBuilder contentValuesBuilder = new StringBuilder();
        for (int i=index,j=0;j<modifiedBytes.length;i++,j++){
            int value = modifiedBytes[j] == orgBytes[j] ? 256 : modifiedBytes[j];
            if (j != 0)
                contentValuesBuilder.append(',');
            contentValuesBuilder.append('(')
                    .append(i).append(',')
                    .append(value)
                    .append(')');
        }
        sql += contentValuesBuilder.toString();
        db.execSQL(sql);
    }

    /**
     * 读取历史数据写入到修改数据的数据库
     * @param undo true:undo，false:redo
     */
    public void insertHistoryToDb(int index, List<Pair<Integer, Integer>> historyBytes,byte[] orgBytes,boolean undo){
        String sql = "INSERT OR REPLACE INTO "+ TABLE_NAME_MODIFIED_BYTES +" (_index,_byte)" +
                " VALUES ";
        StringBuilder contentValuesBuilder = new StringBuilder();
        for (int i=index,j=0;j<historyBytes.size();i++,j++){
            Pair<Integer, Integer> pair = historyBytes.get(j);
            int value = undo ? pair.second : pair.first;
            if (value == orgBytes[j])
                value = 256;
            if (j != 0)
                contentValuesBuilder.append(',');
            contentValuesBuilder.append('(')
                    .append(i).append(',')
                    .append(value)
                    .append(')');
        }
        sql += contentValuesBuilder.toString();
        db.execSQL(sql);
    }

    /**
     * 查询已修改的数据
     * @param orgData 原始数据
     */
    public void queryModifiedByteBeans(int index, ByteBean[] orgData){
        String sql = "SELECT * FROM "+TABLE_NAME_MODIFIED_BYTES + " WHERE _index>=? AND _index<?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(index), String.valueOf(index + orgData.length)});
        while (cursor.moveToNext()){
            int _index = cursor.getInt(cursor.getColumnIndex("_index"));
            int _byte = cursor.getInt(cursor.getColumnIndex("_byte"));
            if (_byte >= 256)
                continue;
            ByteBean bean = orgData[_index - index];
            bean.modified = bean.value != _byte;
            if (bean.modified)
                bean.value = (byte) _byte;
        }
        IOUtils.closeQuietly(cursor);
    }

    public void queryModifiedBytes(int index, byte[] orgData){
        String sql = "SELECT * FROM "+TABLE_NAME_MODIFIED_BYTES + " WHERE _index>=? AND _index<?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(index), String.valueOf(index + orgData.length)});
        while (cursor.moveToNext()){
            int _index = cursor.getInt(cursor.getColumnIndex("_index"));
            int _byte = cursor.getInt(cursor.getColumnIndex("_byte"));
            if (_byte >= 256)
                continue;
            orgData[_index - index] = (byte) _byte;
        }
        IOUtils.closeQuietly(cursor);
    }

    public void close(){
        dropDatabase();
    }

}