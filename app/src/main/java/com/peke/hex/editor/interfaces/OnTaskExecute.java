package com.peke.hex.editor.interfaces;

public interface OnTaskExecute {
    default void before() { }
    void main();
    default void after() { }
    default void onCancelled() {}
}