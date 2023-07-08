package com.peke.hex.editor.bean;

public class ByteBean {

    public int offset;
    public byte value;
    public boolean modified;

    public ByteBean(int offset, byte value) {
        this(offset,value,false);
    }

    public ByteBean(byte value) {
        this(0,value,false);
    }

    public ByteBean(int offset, byte value, boolean modified) {
        this.offset = offset;
        this.value = value;
        this.modified = modified;
    }

    public static ByteBean[] parseBytes(byte[] bytes){
        ByteBean[] hexData = new ByteBean[bytes.length];
        for (int i=0;i<bytes.length;i++)
            hexData[i] = new ByteBean(bytes[i]);
        return hexData;
    }

}
