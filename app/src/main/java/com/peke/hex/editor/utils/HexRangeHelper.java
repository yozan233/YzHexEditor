package com.peke.hex.editor.utils;

import android.util.Pair;

public class HexRangeHelper {
    private int startPosition = -1;
    private int endPosition = -1;
    private int startOffset = -1;
    private int endOffset = -1;

    public HexRangeHelper() { }

    public HexRangeHelper(int startPosition, int endPosition, int startOffset, int endOffset) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public boolean containsPosition(int position){
        if (startPosition == -1)
            return false;
        return position >= startPosition && position <= endPosition;
    }

    public void setRange(int startPosition, int endPosition, int startOffset,int endOffset){
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public boolean isSelected(){
        return startPosition != -1;
    }

    public boolean isSelectedBytes(){
        if (startOffset == -1)
            return false;
        if (startPosition != endPosition)
            return true;
        return endOffset - startOffset > 1;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getStartIndex(){
        return calcDataIndex(startPosition,startOffset);
    }

    public int getEndIndex(){
        return calcDataIndex(endPosition,endOffset)-1;
    }

    public int getIndexLength(){
        int startIndex = getStartIndex();
        int endIndex = getEndIndex();
        int len = endIndex - startIndex;
        return Math.max(1,len);
    }

    public HexRangeHelper copy() {
        return new HexRangeHelper(startPosition,endPosition,startOffset,endOffset);
    }


    public static int calcDstStartOffset(int position,int offset,int move,int dataSize){
        int dstStartOffset = offset + move;
        if (dstStartOffset < 0)
            dstStartOffset = 0;

        int maxOffset = calcMaxOffset(position, dataSize);

        if (dstStartOffset > maxOffset-2)
            dstStartOffset = maxOffset-2;

        return dstStartOffset;
    }

    public static int calcDstEndOffset(int position,int offset,int move,int dataSize){
        int dstEndOffset = offset + move;

        int maxOffset = calcMaxOffset(position, dataSize);

        if (dstEndOffset > maxOffset)
            dstEndOffset = maxOffset;

        if (dstEndOffset < 2)
            dstEndOffset = 2;

        return dstEndOffset;
    }

    public static int calcMaxOffset(int position,int dataSize){
        if ((position+1)*8 <= dataSize)
            return 16;
        return (dataSize % 8) * 2;
    }

    public static int calcDstStartPosition(int position,int move){
        int dstStartPosition = position+move;
        if (dstStartPosition < 0)
            dstStartPosition = 0;

        return dstStartPosition;
    }

    public static int calcDstEndPosition(int position,int move,int itemCount){
        int dstEndPosition = position+move;
        if (dstEndPosition > itemCount-1)
            dstEndPosition = itemCount-1;
        return dstEndPosition;
    }

    public static Pair<Integer,Integer> calcIndexPosition(int index){
        int position = index/8;
        int offset = (index % 8)*2;
        return new Pair<>(position, offset);
    }

    public static int calcDataIndex(int position,int offset){
        return position * 8 + (offset/2);
    }

}