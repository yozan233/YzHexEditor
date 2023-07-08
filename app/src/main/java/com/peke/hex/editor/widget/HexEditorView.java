package com.peke.hex.editor.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.peke.hex.editor.R;
import com.peke.hex.editor.adapter.HexEditorItemAdapter;
import com.peke.hex.editor.interfaces.OnTaskExecute;
import com.peke.hex.editor.utils.AnimationUtils;
import com.peke.hex.editor.utils.FileDataEditor;
import com.peke.hex.editor.utils.FileUtils;
import com.peke.hex.editor.utils.HexRangeHelper;
import com.peke.hex.editor.utils.NormalAsyncTask;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HexEditorView extends LinearLayout {

    private Context mContext;

    private HexEditorItemAdapter hexEditorItemAdapter;
    private LinearLayoutManager rvLayoutManager;
    private HexRangeHelper hexRangeHelper;

    private RecyclerView rvHexEditor;
    private View vScrollBar;

    private View layoutHeadBar;
    private View layoutDataMain;
    private View layoutLoading;
    private TextView tvLoadingTips;
    private HexDataHeadView hexDataHeadView;

    private View vCursor1, vCursor2;
    private boolean isCursorSwap = false; //是否交换前后光标

    //用倒计时监听滑动结束
    private CountDownTimer scrollFinishCountDownTimer = null;

    private float rvScrollBarDownY = 0;
    private boolean rvScrollBarIsTouch = false;
    private float rvVisibleItemCount = 15;

    private FileDataEditor fileDataEditor;

    private boolean mIsLoading = false;
    private boolean mIsSearching = false;

    private Listener mListener;

    private File srcFile = null;
    private File tempFile = null;

    private NormalAsyncTask mAsyncTask = new NormalAsyncTask();

    public HexEditorView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public HexEditorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HexEditorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public HexEditorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    /**
     * 是否已选中数据
     */
    public boolean isDataSelected(){
        if (hexEditorItemAdapter == null)
            return false;
        if (hexRangeHelper == null)
            return false;
        return hexRangeHelper.isSelected();
    }

    /**
     * 是否已选中数据范围
     */
    public boolean isDataRangeSelected(){
        return hexRangeHelper.isSelectedBytes();
    }

    /**
     * 设置选中数据范围
     * @param position 行位置
     * @param offset 列位置
     */
    public boolean setDataSelectRange(int position, int offset){
        return hexEditorItemAdapter.setSelectRange(position, offset);
    }

    /**
     * 设置选中数据范围
     * @param startPosition 开始行
     * @param endPosition 结束行
     * @param startOffset 开始列
     * @param endOffset 结束列
     * @return
     */
    public boolean setDataSelectRange(int startPosition, int endPosition, int startOffset,int endOffset){
        return hexEditorItemAdapter.setSelectRange(startPosition, endPosition, startOffset, endOffset);
    }

    /**
     * 设置选中数据范围
     * @param startIndex 开始index
     * @param endIndex 结束index
     */
    public void updateItemRangeByDataIndex(int startIndex,int endIndex){
        hexEditorItemAdapter.notifyItemRangeByDataIndex(startIndex, endIndex);
    }

    public void setSelectIndex(int index){
        clearSelectRange();
        hexEditorItemAdapter.setSelectIndex(index);
        scrollToPosition(index/8);
    }

    public void setSelectStartIndex(int startIndex){
        hexEditorItemAdapter.setSelectStartIndex(startIndex);
        ensureSelectionVisible(startIndex/8);
        postUpdateCursors();
    }

    public void setSelectEndIndex(int endIndex){
        hexEditorItemAdapter.setSelectEndIndex(endIndex);
        ensureSelectionVisible(endIndex/8);
        postUpdateCursors();
    }

    public void setSelectIndex(int startIndex,int endIndex){
        hexEditorItemAdapter.setSelectIndex(startIndex, endIndex);
    }

    public int getSelectedIndex(){
        if (isDataSelected()){
            return hexRangeHelper.getStartIndex();
        }
        else {
            return rvLayoutManager.findFirstVisibleItemPosition()*8;
        }
    }

    public void moveSelectedOffsetToFirst(){
        hexEditorItemAdapter.moveSelectedOffsetToFirst();
    }

    public void moveSelectedOffsetToLast(){
        hexEditorItemAdapter.moveSelectedOffsetToLast();
    }

    public void moveSelectedOffsetLeft(){
        hexEditorItemAdapter.moveSelectedOffsetLeft();
    }

    public void moveSelectedOffsetRight(){
        hexEditorItemAdapter.moveSelectedOffsetRight();
    }

    public void moveSelectedPositionUp(){
        boolean notify = hexEditorItemAdapter.moveSelectedPositionUp();
        if (notify){
            ensureSelectionVisible(hexRangeHelper.getStartPosition());
        }
    }

    public void moveSelectedPositionDown(){
        boolean notify = hexEditorItemAdapter.moveSelectedPositionDown();
        if (notify){
            ensureSelectionVisible(hexRangeHelper.getStartPosition());
        }
    }

    /**
     * 撤回
     */
    public boolean undo(){
        if (hasTaskRunning())
            return false;
        FileDataEditor.DataHistory history = fileDataEditor.undo();
        if (history != null){
            hexEditorItemAdapter.setSelectRange(history.position,history.offset);
            hexEditorItemAdapter.notifyItemRangeByDataIndex(history.getIndex(),history.getIndex()+history.length);
            ensureSelectionVisible(history.position);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 重新做
     */
    public boolean redo(){
        if (hasTaskRunning())
            return false;
        FileDataEditor.DataHistory history = fileDataEditor.redo();
        if (history != null){
            hexEditorItemAdapter.setSelectRange(history.position,history.offset);
            hexEditorItemAdapter.notifyItemRangeByDataIndex(history.getIndex(),history.getIndex()+history.length);
            ensureSelectionVisible(history.position);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean canUndo(){
        return fileDataEditor.canUndo();
    }

    public boolean canRedo(){
        return fileDataEditor.canRedo();
    }

    public void searchData(byte[] searchBytes){
        searchData(searchBytes,null);
    }

    /**
     * 搜索数据
     * @param searchBytes 要搜索的数据
     * @param callback 回调
     */
    public void searchData(byte[] searchBytes,OnSearchCallback callback){
        if (hasTaskRunning()) {
            throw new RuntimeException("有任务正在执行，请先停止其它任务");
        }
        mIsSearching = true;
        final AtomicInteger dataIndex = new AtomicInteger(-1);
        mAsyncTask.executeTask(new OnTaskExecute() {
            @Override
            public void before() {
                layoutLoading.setVisibility(VISIBLE);
                tvLoadingTips.setText(R.string.searching);
            }

            @Override
            public void main() {

                long timeMillis = System.currentTimeMillis();
                int startIndex = getSelectedIndex();

                dataIndex.set(fileDataEditor.findData(searchBytes, startIndex, -1));

                if (startIndex != 0 && dataIndex.get() == -1){
                    Log.e("HPT","从头开始查找");
                    dataIndex.set(fileDataEditor.findData(searchBytes, 0, startIndex+searchBytes.length));
                }
                Log.e("HPT","搜索耗时:"+((System.currentTimeMillis()-timeMillis)/1000)+"秒");

            }

            @Override
            public void after() {
                layoutLoading.setVisibility(GONE);

                int mDataIndex = dataIndex.get();
                if (mDataIndex == -1){
                    Toast.makeText(mContext,R.string.no_match_data, Toast.LENGTH_LONG).show();
                }
                else {
                    setSelectIndex(mDataIndex, mDataIndex +searchBytes.length);
                    scrollToPosition(mDataIndex/8);
                }

                mIsSearching = false;

                if (callback != null){
                    callback.onSearchResult(mDataIndex);
                }

            }
        });
    }

    public byte[] readBytes(int index, int length){
        return fileDataEditor.readBytes(index,length);
    }

    public void writeBytes(int index, byte[] bytes){
        fileDataEditor.writeBytes(index,bytes);
        hexEditorItemAdapter.notifyItemRangeByDataIndex(index,index+bytes.length);
    }

    public void writeHalfByte(int value){
        int startIndex = hexRangeHelper.getStartIndex();
        int startOffset = hexRangeHelper.getStartOffset();
        fileDataEditor.writeHalfByte(startIndex,value,startOffset % 2 == 0);
        hexEditorItemAdapter.moveToNextOffset();
    }

    public int getDataSize(){
        return fileDataEditor.length();
    }

    private void init(Context context){
        this.mContext = context;
        View root = LayoutInflater.from(context).inflate(R.layout.layout_hex_editor_view, this, true);
        vScrollBar = root.findViewById(R.id.vScrollBar);
        rvHexEditor = root.findViewById(R.id.rv_hex_editor);
        layoutHeadBar = root.findViewById(R.id.layout_head_bar);
        hexDataHeadView = findViewById(R.id.hexDataHeadView);
        layoutDataMain = root.findViewById(R.id.layout_data_main);
        layoutLoading = root.findViewById(R.id.layout_loading);
        tvLoadingTips = root.findViewById(R.id.tv_loading_tips);
        vCursor1 = root.findViewById(R.id.v_cursor_start);
        vCursor2 = root.findViewById(R.id.v_cursor_end);

        layoutLoading.setOnClickListener(v -> {});

        fileDataEditor = new FileDataEditor(context);

        rvLayoutManager = new LinearLayoutManager(context){
            @Override
            public boolean canScrollVertically() {
                if (hexEditorItemAdapter == null)
                    return false;

                return !hexEditorItemAdapter.isSelected();
            }
        };
        rvHexEditor.setLayoutManager(rvLayoutManager);
        rvHexEditor.setItemAnimator(null);
        rvHexEditor.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull @NotNull RecyclerView rv, @NonNull @NotNull MotionEvent e) {
                if (hexEditorItemAdapter == null)
                    return false;

                return hexEditorItemAdapter.isMultipleSelected();
            }
            @Override
            public void onTouchEvent(@NonNull @NotNull RecyclerView rv, @NonNull @NotNull MotionEvent e) { }
            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
        });

        initRvScrollBar();
        initRvScrollListener();
        initHexEditorItemAdapter();

        initSelectCursor();

    }

    public void setSrcFile(@NonNull File file){
        this.srcFile = file;
        startLoadFile();
    }

    public File getSrcFile() {
        return srcFile;
    }

    private void startLoadFile(){
        if (hasTaskRunning()) {
            throw new RuntimeException("有任务正在执行，请先停止其它任务");
        }
        mIsLoading = true;
        AtomicBoolean isLoadSuccess = new AtomicBoolean(false);
        mAsyncTask.executeTask(new OnTaskExecute() {
            @Override
            public void before() {
                layoutLoading.setVisibility(VISIBLE);
                tvLoadingTips.setText(R.string.reading_file);
            }

            @Override
            public void main() {
                File file = srcFile;
                if (file == null){
                    tempFile = new File(mContext.getFilesDir(), UUID.randomUUID().toString());
                    file = tempFile;
                    FileUtils.copyFile(srcFile,tempFile);
                }
                fileDataEditor.setSrcFile(file);

                if (fileDataEditor.isEmpty()){
                    return;
                }

                isLoadSuccess.set(true);

            }

            @Override
            public void after() {
                mIsLoading = false;
                layoutLoading.setVisibility(GONE);

                if(isLoadSuccess.get()){
                    initHexEditorItemAdapter();

                    if (mListener != null){
                        mListener.onFileLoadFinish(true);
                    }
                }
                else {
                    Toast.makeText(mContext,R.string.read_file_failed, Toast.LENGTH_LONG).show();
                    if (mListener != null){
                        mListener.onFileLoadFinish(false);
                    }
                }
            }
        });

    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public boolean isIsSearching() {
        return mIsSearching;
    }

    public boolean hasTaskRunning(){
        return mIsLoading || mIsSearching;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initSelectCursor(){
        OnTouchListener onTouchListener = (v, event) -> {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    v.setTag(new Pair<>(event.getX(),event.getY()));
                    break;
                case MotionEvent.ACTION_MOVE:{
                    int itemHeight = getRvItemHeight();
                    float wordWidth3 = hexDataHeadView.getWordWith(3);
                    Pair<Float,Float> coordinate = (Pair<Float,Float>) v.getTag();
                    float cursorDownX = coordinate.first;
                    float cursorDownY = coordinate.second;

                    final float xDistance = event.getX() - cursorDownX;
                    final float yDistance = event.getY() - cursorDownY;

                    int moveXItemCount = (int) Math.ceil(xDistance / wordWidth3);
                    int moveYItemCount = (int) Math.ceil(yDistance / itemHeight);

                    if (moveXItemCount == 0 && moveYItemCount == 0)
                        break;

                    moveXItemCount = (moveXItemCount > 0 ? 1 : -1) * Math.min(2,Math.abs(moveXItemCount));
                    moveYItemCount = (moveYItemCount > 0 ? 1 : -1) * Math.min(2,Math.abs(moveYItemCount));

                    final int startPosition = hexRangeHelper.getStartPosition();
                    final int endPosition = hexRangeHelper.getEndPosition();
                    final int startOffset = hexRangeHelper.getStartOffset();
                    final int endOffset = hexRangeHelper.getEndOffset();

                    int adapterItemCount = hexEditorItemAdapter.getItemCount();

                    int movePosition = 2;

                    if (isStartCursor(v)){
                        int dstStartPosition = HexRangeHelper.calcDstStartPosition(startPosition,moveYItemCount);
                        int dstStartOffset = HexRangeHelper.calcDstStartOffset(dstStartPosition,startOffset,moveXItemCount*2, fileDataEditor.length());
                        int startIndex = HexRangeHelper.calcDataIndex(dstStartPosition, dstStartOffset);
                        int endIndex = HexRangeHelper.calcDataIndex(endPosition, endOffset);

                        if (startIndex >= endIndex)
                            break;

                        setEditIndexRange(startIndex,endIndex-1);

                        boolean notify = hexEditorItemAdapter.setSelectRange(dstStartPosition, endPosition, dstStartOffset, endOffset);

                        if (notify){
                            if (moveYItemCount != 0){
                                int firstVisibleItemPosition = rvLayoutManager.findFirstCompletelyVisibleItemPosition();
                                int lastVisibleItemPosition = rvLayoutManager.findLastCompletelyVisibleItemPosition();
                                if (dstStartPosition < firstVisibleItemPosition+2){
                                    if (firstVisibleItemPosition != 0){
                                        scrollToPosition(firstVisibleItemPosition-movePosition);
                                    }
                                }else if (dstStartPosition > lastVisibleItemPosition-2){
                                    if (lastVisibleItemPosition != adapterItemCount+3){
                                        scrollToPosition(firstVisibleItemPosition+movePosition);
                                    }
                                }
                            }

                            postUpdateCursors();
                        }

                    }
                    else {
                        int dstEndPosition = HexRangeHelper.calcDstEndPosition(endPosition,moveYItemCount, adapterItemCount);
                        int dstEndOffset = HexRangeHelper.calcDstEndOffset(dstEndPosition,endOffset,moveXItemCount*2, fileDataEditor.length());
                        int startIndex = HexRangeHelper.calcDataIndex(startPosition, startOffset);
                        int endIndex = HexRangeHelper.calcDataIndex(dstEndPosition, dstEndOffset);

                        if (startIndex >= endIndex)
                            break;

                        setEditIndexRange(startIndex,endIndex-1);

                        boolean notify = hexEditorItemAdapter.setSelectRange(startPosition, dstEndPosition, startOffset, dstEndOffset);

                        if (notify){
                            if (moveYItemCount != 0){
                                int firstVisibleItemPosition = rvLayoutManager.findFirstCompletelyVisibleItemPosition();
                                int lastVisibleItemPosition = rvLayoutManager.findLastCompletelyVisibleItemPosition();
                                if (dstEndPosition < firstVisibleItemPosition+2){
                                    if (firstVisibleItemPosition != 0){
                                        scrollToPosition(firstVisibleItemPosition-movePosition);
                                    }
                                }else if (dstEndPosition > lastVisibleItemPosition-2){
                                    if (lastVisibleItemPosition != adapterItemCount+3){
                                        scrollToPosition(firstVisibleItemPosition+movePosition);
                                    }
                                }
                            }

                            postUpdateCursors();
                        }

                    }
                }

            }

            return true;
        };

        vCursor1.setOnTouchListener(onTouchListener);
        vCursor2.setOnTouchListener(onTouchListener);

    }

    private boolean isStartCursor(View v){
        boolean isCursor1 = v == vCursor1;
        if (isCursor1){
            return !isCursorSwap;
        }
        else {
            return isCursorSwap;
        }
    }

    /**
     * 清除选中范围
     */
    public void clearSelectRange(){
        if (isDataSelected()){
            hexEditorItemAdapter.clearSelectRange();
        }
    }

    /**
     * 更新光标位置
     */
    private void postUpdateCursors(){
        rvHexEditor.post(()->{
            setStartCursor(hexRangeHelper.getStartPosition(), hexRangeHelper.getStartOffset());
            setEndCursor(hexRangeHelper.getEndPosition(), hexRangeHelper.getEndOffset());
        });
    }

    /**
     * 设置开始光标位置
     * @param startPosition 列表item位置
     * @param startOffset item内部偏移量
     */
    private void setStartCursor(int startPosition,int startOffset){
        View vStartCursor = !isCursorSwap ? vCursor1 : vCursor2;
        int cursorWidth = vStartCursor.getMeasuredWidth();
        float wordWith = hexDataHeadView.getWordWith();
        float baseDataTextPosX = hexDataHeadView.getWordWith(9.5f);

        int startX = (int) (HexDataView.dataIndex2TextIndex(startOffset) * wordWith + baseDataTextPosX);

        int startY = (startPosition+1) * getRvItemHeight() - getRvScrolledY();

        if (startY < 0){
            vStartCursor.setVisibility(View.GONE);
            return;
        }
        else {
            vStartCursor.setVisibility(View.VISIBLE);
        }

        vStartCursor.setX(startX - cursorWidth);
        vStartCursor.setY(startY);
    }

    private void setEditIndexRange(int startIndex,int endIndex){
        if (mListener != null){
            mListener.onIndexRangeSelected(startIndex,endIndex);
        }
    }

    /**
     * 设置结束光标位置
     * @param endPosition 列表item位置
     * @param endOffset item内部偏移量
     */
    private void setEndCursor(int endPosition,int endOffset){
        View vEndCursor = isCursorSwap ? vCursor1 : vCursor2;
        int bottom = getRvLayoutHeight();
        float wordWith = hexDataHeadView.getWordWith();
        float baseDataTextPosX = hexDataHeadView.getWordWith(9.5f);

        int textEndIndex = HexDataView.dataIndex2TextIndex(endOffset);
        int endX = (int) (textEndIndex * wordWith + baseDataTextPosX);
        if (HexDataView.isSpaceIndex(textEndIndex))
            endX -= wordWith;

        int endY = (endPosition+1) * getRvItemHeight() - getRvScrolledY();

        if (endY > bottom){
            vEndCursor.setVisibility(View.GONE);
            return;
        }
        else {
            vEndCursor.setVisibility(View.VISIBLE);
        }

        vEndCursor.setX(endX);
        vEndCursor.setY(endY);
    }

    private HexEditorItemAdapter.SelectMode mSelectMode = HexEditorItemAdapter.SelectMode.NONE;

    /**
     * 隐藏光标
     */
    public void hideCursor(){
        showCursorBySelectMode(HexEditorItemAdapter.SelectMode.NONE);
    }

    public void recoveryCursorState(){
        if (hexEditorItemAdapter == null)
            return;
        showCursorBySelectMode(hexEditorItemAdapter.getSelectMode());
    }

    private void showCursorBySelectMode(HexEditorItemAdapter.SelectMode selectMode){
        if (mSelectMode == selectMode)
            return;
        mSelectMode = selectMode;
        switch (selectMode){
            case NONE:
            case SingleSelect:
                vCursor1.setVisibility(View.GONE);
                vCursor2.setVisibility(View.GONE);
                break;
            case MultipleSelect:
                vCursor1.setVisibility(View.VISIBLE);
                vCursor2.setVisibility(View.VISIBLE);
                break;
        }

        if (mListener != null)
            mListener.onSelectModeChange(selectMode);
    }

    /**
     * 监听数据点击事件
     */
    private final OnClickListener _onHexDataClickListener = v -> {
        showCursorBySelectMode(HexEditorItemAdapter.SelectMode.SingleSelect);
        post(()-> ensureSelectionVisible(hexRangeHelper.getStartPosition()));
    };

    /**
     * 监听数据长按事件
     */
    private final OnClickListener onHexDataLongClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int startPosition = hexRangeHelper.getStartPosition();
            int endPosition = hexRangeHelper.getEndPosition();
            int startOffset = hexRangeHelper.getStartOffset();
            int endOffset = hexRangeHelper.getEndOffset();

            setStartCursor(startPosition,startOffset);
            setEndCursor(endPosition,endOffset);

            setEditIndexRange(hexRangeHelper.getStartIndex(), hexRangeHelper.getEndIndex());

            showCursorBySelectMode(HexEditorItemAdapter.SelectMode.MultipleSelect);
        }
    };

    /**
     * 监听取消选中事件
     */
    private final Runnable onClearHexDataSelectListener = this::hideCursor;

    private void initHexEditorItemAdapter(){
        if (fileDataEditor == null || fileDataEditor.length() == 0){
            showEmptyFileLayout(true);
            return;
        }
        hexEditorItemAdapter = null;
        hexEditorItemAdapter = new HexEditorItemAdapter(mContext, fileDataEditor);
        hexEditorItemAdapter.setOnDataClickListener(_onHexDataClickListener);
        hexEditorItemAdapter.setOnDataLongClickListener(onHexDataLongClickListener);
        hexEditorItemAdapter.setOnClearSelectListener(onClearHexDataSelectListener);
        rvHexEditor.setAdapter(hexEditorItemAdapter);
        hexRangeHelper = hexEditorItemAdapter.getRangeHelper();
        showEmptyFileLayout(false);
    }

    private void showEmptyFileLayout(boolean empty){
        if (mListener != null){
            mListener.onDataChange(empty ? 0 : fileDataEditor.length());
        }
    }


    private void startScrollFinishCountDownTimer(){
        if (scrollFinishCountDownTimer != null){
            scrollFinishCountDownTimer.cancel();
            scrollFinishCountDownTimer = null;
        }
        scrollFinishCountDownTimer = new CountDownTimer(300,10) {
            @Override
            public void onTick(long millisUntilFinished) { }
            @Override
            public void onFinish() {
                rvHexEditor.postDelayed(()-> hideRvScrollBar(true),600);
            }
        };
        scrollFinishCountDownTimer.start();
    }

    /**
     * 初始化列表滑动监听
     */
    private void initRvScrollListener(){
        rvHexEditor.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull @NotNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                startScrollFinishCountDownTimer();

                if (rvScrollBarIsTouch)
                    return;


                int firstPosition = rvLayoutManager.findFirstVisibleItemPosition();
                int lastPosition = rvLayoutManager.findLastVisibleItemPosition();


                int itemCount = hexEditorItemAdapter.getItemCount()+4;

                float progress;
                if (lastPosition == itemCount - 1){
                    progress = 1;
                }
                else {
                    float rvHeight = getRvLayoutHeight();
                    float visibleItemCount = rvHeight / getRvItemHeight();
                    rvVisibleItemCount = itemCount-visibleItemCount;

                    progress = firstPosition/rvVisibleItemCount;
                }

                updateRvScrollBar(progress);

            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initRvScrollBar(){
        vScrollBar.setOnTouchListener((v, event) -> {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    rvScrollBarDownY = event.getY();
                    rvScrollBarIsTouch = true;
                    vScrollBar.setSelected(true);
                    rvHexEditor.stopScroll();
                    break;
                case MotionEvent.ACTION_MOVE:{
                    final float yDistance = event.getY() - rvScrollBarDownY;
                    float posY = vScrollBar.getY() + yDistance;

                    int rvHeight = getRvLayoutHeight();
                    int barHeight = vScrollBar.getMeasuredHeight();
                    int layoutHeight = rvHeight - barHeight;

                    if (posY < 0){
                        posY = 0;
                    }
                    if (posY > layoutHeight){
                        posY = layoutHeight;
                    }

                    vScrollBar.setY(posY);

                    float progress = posY / layoutHeight;

                    updateRvProgress(progress);


                }break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    rvScrollBarIsTouch = false;
                    vScrollBar.setSelected(false);
                    break;
            }

            return true;
        });
    }

    /**
     * 更新滚动条
     */
    private void updateRvScrollBar(float progress){
        if (progress < 0){
            hideRvScrollBar(true);
        }

        int rvHeight = getRvLayoutHeight();
        int barHeight = vScrollBar.getMeasuredHeight();
        int layoutHeight = rvHeight - barHeight;
        float posY = layoutHeight * progress;

        hideRvScrollBar(false);
        vScrollBar.setY(posY);
    }

    /**
     * 更新列表进度
     */
    private void updateRvProgress(float progress){
        int firstPosition = (int) Math.ceil(progress * rvVisibleItemCount);
        scrollToPosition(firstPosition);
    }

    /**
     * 隐藏滚动条
     */
    private void hideRvScrollBar(boolean hide){
        int visibility = vScrollBar.getVisibility();
        if (hide){
            if (!rvScrollBarIsTouch && visibility != View.GONE){
                ObjectAnimator animator = AnimationUtils.alpha(vScrollBar, 300, 1, 0, true);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        vScrollBar.setVisibility(View.GONE);
                        vScrollBar.setAlpha(1);
                    }
                });
            }
        }
        else {
            if (visibility != View.VISIBLE){
                vScrollBar.setVisibility(View.VISIBLE);
            }
        }
    }

    public void scrollToStartCursor(){
        scrollToPosition(hexRangeHelper.getStartPosition());
    }

    public void scrollToEndCursor(){
        scrollToPosition(hexRangeHelper.getEndPosition());
    }

    /**
     * 滑动到指定位置
     */
    public void scrollToPosition(int position){
        int itemCount = hexEditorItemAdapter.getItemCount();
        if (position >= itemCount)
            position = itemCount - 1;
        if (position < 0)
            position = 0;
        int firstCompletelyVisibleItemPosition = rvLayoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstCompletelyVisibleItemPosition == position)
            return;
        rvLayoutManager.scrollToPositionWithOffset(position, 0);
    }

    /**
     * 获取一行数据高度
     */
    private int getRvItemHeight(){
        return hexDataHeadView.getMeasuredHeight();
    }

    /**
     * 获取列表布局高度
     */
    private int getRvLayoutHeight(){
        return layoutDataMain.getMeasuredHeight();
    }

    /**
     * 获取列表滑动距离
     */
    private int getRvScrolledY(){
        int firstVisibleItemPosition = rvLayoutManager.findFirstVisibleItemPosition();
        int rvItemHeight = getRvItemHeight();
        int scrolledY = firstVisibleItemPosition * rvItemHeight;
        View child = rvLayoutManager.getChildAt(0);
        if (child != null){
            int top = child.getTop();
            scrolledY -= top;
        }
        return scrolledY;
    }

    /**
     * 确保选中的数据可以显示
     */
    public void ensureSelectionVisible(int position) {
        int firstVisibleItemPosition = rvLayoutManager.findFirstVisibleItemPosition()+1;
        int lastVisibleItemPosition = rvLayoutManager.findLastVisibleItemPosition()-1;
        if (firstVisibleItemPosition < position && position < lastVisibleItemPosition){

        }
        else {
            int visibleItemCount = lastVisibleItemPosition-firstVisibleItemPosition;
            int displayPosition = position - visibleItemCount/2;
            if (displayPosition < 0)
                displayPosition = 0;

            scrollToPosition(displayPosition);
        }
    }

    /**
     * 保存文件
     * @param callback 保存成功的回调
     */
    public void saveFile(OnSaveFileFinishListener callback){
        saveToFile(null,callback);
    }

    /**
     * 保存到指定文件
     * @param dstFile 目标文件 如果为空则覆盖原文件
     * @param callback 保存成功的回调
     */
    public void saveToFile(File dstFile,OnSaveFileFinishListener callback){
        if (hasTaskRunning()) {
            throw new RuntimeException("有任务正在执行，请先停止其它任务");
        }

        mAsyncTask.executeTask(new OnTaskExecute() {
            @Override
            public void before() {
                layoutLoading.setVisibility(View.VISIBLE);
                tvLoadingTips.setText(R.string.saving);
            }

            @Override
            public void main() {
                fileDataEditor.saveToFile(dstFile);

                File srcFile = dstFile == null ? fileDataEditor.getSrcFile() : dstFile;

                fileDataEditor.setSrcFile(srcFile);

            }

            @Override
            public void after() {
                layoutLoading.setVisibility(View.GONE);

                Toast.makeText(mContext, R.string.save_success, Toast.LENGTH_SHORT).show();
                hexEditorItemAdapter.notifyDataSetChanged();
                if (callback != null){
                    callback.onSaveFileFinished();
                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAsyncTask.isRunning()){
            mAsyncTask.cancel();
        }
        if (fileDataEditor.isNotEmpty()){
            fileDataEditor.destroy();
        }
        if (tempFile != null){
            tempFile.delete();
        }

    }

    public interface Listener {
        void onSelectModeChange(HexEditorItemAdapter.SelectMode selectMode);
        void onIndexRangeSelected(int startIndex,int endIndex);
        void onDataChange(int size);
        void onFileLoadFinish(boolean isSuccess);
    }

    public interface OnSearchCallback {
        void onSearchResult(int dataIndex);
    }

    public interface OnSaveFileFinishListener {
        void onSaveFileFinished();
    }

}
