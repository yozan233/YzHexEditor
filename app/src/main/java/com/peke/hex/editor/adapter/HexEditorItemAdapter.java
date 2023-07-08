package com.peke.hex.editor.adapter;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peke.hex.editor.R;
import com.peke.hex.editor.bean.ByteBean;
import com.peke.hex.editor.utils.FileDataEditor;
import com.peke.hex.editor.utils.HexRangeHelper;
import com.peke.hex.editor.widget.HexDataView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class HexEditorItemAdapter extends RecyclerView.Adapter<HexEditorItemAdapter.ViewHolder> {
    private final Context mContext;
    private final HexRangeHelper hexRangeHelper;
    private final FileDataEditor mFileDataEditor;

    //选择模式为单选模式
    private SelectMode mSelectMode = SelectMode.NONE;

    private int mItemHeight = 0;

    private View.OnClickListener onDataClickListener;
    private View.OnClickListener onDataLongClickListener;
    private Runnable onClearSelectListener;

//    private OnSelectModeChangeListener onSelectModeChangeListener;

    public HexEditorItemAdapter(Context context, FileDataEditor fileDataEditor) {
        this.mFileDataEditor = fileDataEditor;
        hexRangeHelper = new HexRangeHelper();
        mFileDataEditor.setHexRangeHelper(hexRangeHelper);
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.listitem_hex_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HexDataView dataView = holder.dataView;

        if (mItemHeight == 0){
            mItemHeight = dataView.getMeasuredHeight();
        }

        int offset = position * 8;

        ByteBean[] data = mFileDataEditor.readByteBeans(offset,8);

        dataView.setData(data);
//        dataView.setChars(text);
        dataView.setOffset(offset);

        if (hexRangeHelper.containsPosition(position)){
            int selectStartPosition = hexRangeHelper.getStartPosition();
            int selectEndPosition = hexRangeHelper.getEndPosition();
            if(selectStartPosition == selectEndPosition){
                dataView.setDataSelectedRange(hexRangeHelper.getStartOffset(), hexRangeHelper.getEndOffset());
            }
            else {
                if (position == selectStartPosition){
                    dataView.setDataSelectedRange(hexRangeHelper.getStartOffset(),16);
                }
                else if (position == selectEndPosition){
                    dataView.setDataSelectedRange(0, hexRangeHelper.getEndOffset());
                }
                else {
                    dataView.setDataSelectedRange(0,16);
                }
            }
        }
        else {
            dataView.clearDataSelect();
        }

        dataView.setOnDataClickListener(v -> {
            setSelectRange(position,((HexDataView) v).getDataSelectedRange());
            setSingleSelectMode();
            if (onDataClickListener != null)
                onDataClickListener.onClick(v);
        });

        dataView.setOnDataLongClickListener(v -> {
            setSelectRange(position,((HexDataView) v).getDataSelectedRange());
            setMultipleSelectMode();
            if (onDataLongClickListener != null)
                onDataLongClickListener.onClick(v);
        });

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) dataView.getLayoutParams();
        if (position == getItemCount() - 1){
            int space = mItemHeight*4;
            if (layoutParams.bottomMargin != space){
                layoutParams.bottomMargin = space;
            }
        }
        else {
            if (layoutParams.bottomMargin != 0){
                layoutParams.bottomMargin = 0;
            }
        }

    }

    private void setSingleSelectMode(){
        if (mSelectMode == SelectMode.SingleSelect)
            return;
        mSelectMode = SelectMode.SingleSelect;

//        if (onSelectModeChangeListener != null)
//            onSelectModeChangeListener.onSelectModeChange(mSelectMode);
    }

    private void setMultipleSelectMode(){
        if (mSelectMode == SelectMode.MultipleSelect)
            return;
        mSelectMode = SelectMode.MultipleSelect;

//        if (onSelectModeChangeListener != null)
//            onSelectModeChangeListener.onSelectModeChange(mSelectMode);
    }

    public SelectMode getSelectMode() {
        return mSelectMode;
    }

    public void moveSelectedOffsetToFirst(){
        int startPosition = hexRangeHelper.getStartPosition();
        int endPosition = hexRangeHelper.getEndPosition();
        int startOffset = 0;
        int endOffset = 1;
        setSelectRange(startPosition,endPosition,startOffset,endOffset);
    }

    public void moveSelectedOffsetToLast(){
        int startPosition = hexRangeHelper.getStartPosition();
        int endPosition = hexRangeHelper.getEndPosition();
        int endOffset = HexRangeHelper.calcMaxOffset(startPosition, mFileDataEditor.length());
        int startOffset = endOffset-1;
        setSelectRange(startPosition,endPosition,startOffset,endOffset);
    }

    public void moveSelectedOffsetLeft(){
        int startPosition = hexRangeHelper.getStartPosition();
        int endPosition = hexRangeHelper.getEndPosition();
        int startOffset = hexRangeHelper.getStartOffset();
        int endOffset = hexRangeHelper.getEndOffset();
        startOffset--;
        endOffset--;
        if (startOffset < 0){
            startOffset = 0;
            endOffset = 1;
        }
        setSelectRange(startPosition,endPosition,startOffset,endOffset);
    }

    public void moveSelectedOffsetRight(){
        int startPosition = hexRangeHelper.getStartPosition();
        int endPosition = hexRangeHelper.getEndPosition();
        int startOffset = hexRangeHelper.getStartOffset();
        int endOffset = hexRangeHelper.getEndOffset();
        startOffset++;
        endOffset++;

        int maxOffset = HexRangeHelper.calcMaxOffset(startPosition, mFileDataEditor.length());
        if (endOffset > maxOffset){
            endOffset = maxOffset;
            startOffset = endOffset-1;
        }
        setSelectRange(startPosition,endPosition,startOffset,endOffset);
    }

    public boolean moveSelectedPositionUp(){
        int startPosition = hexRangeHelper.getStartPosition();
        int endPosition = hexRangeHelper.getEndPosition();
        int startOffset = hexRangeHelper.getStartOffset();
        int endOffset = hexRangeHelper.getEndOffset();
        if (startPosition > 0){
            startPosition--;
            endPosition--;
        }
        else
            return false;

        return setSelectRange(startPosition, endPosition, startOffset, endOffset);
    }

    public boolean moveSelectedPositionDown(){
        int startPosition = hexRangeHelper.getStartPosition();
        int endPosition = hexRangeHelper.getEndPosition();
        int startOffset = hexRangeHelper.getStartOffset();
        int endOffset = hexRangeHelper.getEndOffset();
        int itemCount = getItemCount();
        if (endPosition < itemCount-1){
            startPosition ++;
            endPosition ++;
        }
        else
            return false;
        return setSelectRange(startPosition, endPosition, startOffset, endOffset);
    }

    public void moveToNextOffset(){
        int startPosition = hexRangeHelper.getStartPosition();
        int endPosition = hexRangeHelper.getEndPosition();
        int startOffset = hexRangeHelper.getStartOffset();
        int endOffset = hexRangeHelper.getEndOffset();

        if (endOffset == 16){
            int itemCount = getItemCount();
            if (endPosition < itemCount){
                startPosition ++;
                endPosition ++;
                startOffset = 0;
                endOffset = 1;
            }
        }
        else {
            startOffset ++;
            endOffset ++;
        }

        int maxOffset = HexRangeHelper.calcMaxOffset(startPosition, mFileDataEditor.length());
        if (endOffset > maxOffset){
            endOffset = maxOffset;
            startOffset = endOffset-1;
        }

        setSelectRange(startPosition, endPosition, startOffset, endOffset,true);

    }

    private void setSelectRange(int position, Range<Integer> dataSelectedRange){
        int start = dataSelectedRange.getLower();
        int end = dataSelectedRange.getUpper();
        setSelectRange(position,position,start,end);
    }

    public void setSelectIndex(int index){
        Pair<Integer, Integer> pair = HexRangeHelper.calcIndexPosition(index);
        setSelectRange(pair.first,pair.first,pair.second,pair.second+1);
    }

    public void setSelectStartIndex(int startIndex){
        Pair<Integer, Integer> startPair = HexRangeHelper.calcIndexPosition(startIndex);
        setSelectRange(startPair.first,hexRangeHelper.getEndPosition(),startPair.second,hexRangeHelper.getEndOffset());
    }

    public void setSelectEndIndex(int endIndex){
        Pair<Integer, Integer> endPair = HexRangeHelper.calcIndexPosition(endIndex);
        if (endPair.second == 0){
            endPair = new Pair<>(endPair.first-1,16);
        }
        setSelectRange(hexRangeHelper.getStartPosition(),endPair.first,hexRangeHelper.getStartOffset(),endPair.second);
    }

    public void setSelectIndex(int startIndex,int endIndex){
        Pair<Integer, Integer> startPair = HexRangeHelper.calcIndexPosition(startIndex);
        Pair<Integer, Integer> endPair = HexRangeHelper.calcIndexPosition(endIndex);
        if (endPair.second == 0){
            endPair = new Pair<>(endPair.first-1,16);
        }

        setSelectRange(startPair.first,endPair.first,startPair.second,endPair.second);
    }

    public void setSelectRange(HexRangeHelper range){
        setSelectRange(range,false);
    }

    public void setSelectRange(HexRangeHelper range,boolean coerciveNotify){
        if (range == null)
            return;
        setSelectRange(range.getStartPosition(),range.getEndPosition(),range.getStartOffset(),range.getEndOffset(),coerciveNotify);
    }

    public boolean setSelectRange(int position, int offset){
        return setSelectRange(position,position,offset,offset+1);
    }

    public boolean setSelectRange(int startPosition, int endPosition, int startOffset,int endOffset){
        return setSelectRange(startPosition, endPosition, startOffset, endOffset,false);
    }

    /**
     * 设置选择范围
     * @param startPosition item开始位置
     * @param endPosition item结束位置
     * @param startOffset item开始偏移量
     * @param endOffset item结束偏移量
     * @param coerciveNotify 是否强制刷新
     */
    public boolean setSelectRange(int startPosition, int endPosition, int startOffset,int endOffset,boolean coerciveNotify){
        int oldStartPosition = hexRangeHelper.getStartPosition();
        int oldEndPosition = hexRangeHelper.getEndPosition();
        int oldStartOffset = hexRangeHelper.getStartOffset();
        int oldEndOffset = hexRangeHelper.getEndOffset();

        boolean notify;
        if (coerciveNotify) {
            notify = true;
        } else {
            notify = oldStartPosition != startPosition
                    || oldEndPosition != endPosition
                    || oldStartOffset != startOffset
                    || oldEndOffset != endOffset;
        }

        if (notify) {
            Log.e("HPT","startPosition="+startPosition+",endPosition="+endPosition+",startOffset="+startOffset+",endOffset="+endOffset);
            hexRangeHelper.setRange(startPosition,endPosition,startOffset,endOffset);
            notifyItemRange(oldStartPosition,oldEndPosition);
            notifyItemRange(startPosition,endPosition);
            return true;
        }

        return false;
    }

    public void notifyItemRange(int startPosition, int endPosition){
        if (startPosition != -1 && endPosition != -1){
            int count = endPosition-startPosition+1;
            notifyItemRangeChanged(startPosition,count);
        }
    }

    public void notifyItemRangeByDataIndex(int startIndex,int endIndex){
        Pair<Integer, Integer> startPair = HexRangeHelper.calcIndexPosition(startIndex);
        Pair<Integer, Integer> endPair = HexRangeHelper.calcIndexPosition(endIndex);
        if (endPair.second == 0){
            endPair = new Pair<>(endPair.first-1,16);
        }
        notifyItemRange(startPair.first,endPair.first);
    }

    public void clearSelectRange(){
        boolean notify = setSelectRange(-1, -1, -1, -1);
        if (notify){
            mSelectMode = SelectMode.NONE;
            if (onClearSelectListener != null)
                onClearSelectListener.run();

//            if (onSelectModeChangeListener != null)
//                onSelectModeChangeListener.onSelectModeChange(mSelectMode);
        }
    }

    public HexRangeHelper getRangeHelper() {
        return hexRangeHelper;
    }

    public void setOnDataClickListener(View.OnClickListener onDataClickListener) {
        this.onDataClickListener = onDataClickListener;
    }

    public void setOnDataLongClickListener(View.OnClickListener onDataLongClickListener) {
        this.onDataLongClickListener = onDataLongClickListener;
    }

    public void setOnClearSelectListener(Runnable onClearSelectListener) {
        this.onClearSelectListener = onClearSelectListener;
    }

    public boolean isSelected(){
        return mSelectMode != SelectMode.NONE;
    }

    public boolean isMultipleSelected(){
        return mSelectMode == SelectMode.MultipleSelect;
    }

    @Override
    public int getItemCount() {
        long fileSize = mFileDataEditor.length();
        double lineCount = fileSize / 8d;
        return BigDecimal.valueOf(lineCount).setScale(0, RoundingMode.UP).intValue();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        HexDataView dataView;

        public ViewHolder(View itemView) {
            super(itemView);
            dataView = (HexDataView) itemView;
        }

    }

    public enum SelectMode {
        NONE,SingleSelect,MultipleSelect
    }

}