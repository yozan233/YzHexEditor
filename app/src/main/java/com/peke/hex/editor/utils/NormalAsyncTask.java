package com.peke.hex.editor.utils;

import com.peke.hex.editor.interfaces.OnTaskExecute;

//普通的异步任务
public class NormalAsyncTask {
    private OnTaskExecute onTaskExecute = null;
    private SimpleAsyncTask task;
    private boolean mIsCancel = false;

    public NormalAsyncTask() {

    }

    public void executeTask(OnTaskExecute onTaskExecute) {
        this.onTaskExecute = onTaskExecute;
        execute();
    }

    private void execute(){
        mIsCancel = false;
        new Thread(()->{
            if (task != null && task.isRunning()){
                task.cancelAwait();
            }
            if (mIsCancel)
                return;
            task = new SimpleAsyncTask();
            task.setOnExecute(onTaskExecute);
            task.execute();
        }).start();
    }

    public void cancel(){
        mIsCancel = true;
        if (task != null){
            task.cancel();
        }
    }

    public boolean isCancelled(){
        if (task != null){
            return !task.isRunning();
        }
        return mIsCancel;
    }

    public boolean isRunning(){
        if (task != null){
            return task.isRunning();
        }
        return !mIsCancel;
    }

}