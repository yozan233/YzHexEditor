package com.peke.hex.editor.utils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.peke.hex.editor.interfaces.OnTaskExecute;

import java.util.concurrent.CountDownLatch;

class SimpleAsyncTask extends AsyncTask<String, Integer, String> {
    private OnTaskExecute onTaskExecute = null;
    private boolean mIsRunning = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CountDownLatch cancelCountDownLatch;

    public SimpleAsyncTask() {

    }

    @Override
    protected void onPreExecute() {//task前
        mIsRunning = true;
        if (onTaskExecute != null){
            CountDownLatch countDownLatch = new CountDownLatch(1);
            runOnUiThread(()-> {
                onTaskExecute.before();
                countDownLatch.countDown();
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected String doInBackground(String... params) { //task主程序
        mIsRunning = true;
        if (onTaskExecute != null){
            onTaskExecute.main();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) { //task结束后
        mIsRunning = false;
        if (onTaskExecute != null){
            runOnUiThread(()->onTaskExecute.after());
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mIsRunning = false;
        if (cancelCountDownLatch != null)
            cancelCountDownLatch.countDown();
        if (onTaskExecute != null){
            runOnUiThread(()-> onTaskExecute.onCancelled());
        }
    }

    private void runOnUiThread(Runnable runnable){
        mainHandler.post(runnable);
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void setOnExecute(OnTaskExecute onTaskExecute) {
        this.onTaskExecute = onTaskExecute;
    }

    public void cancelAwait(){
        cancel();
        cancelCountDownLatch = new CountDownLatch(1);
        try {
            cancelCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cancel(){
        mIsRunning = false;
        mainHandler.removeCallbacksAndMessages(null);
        cancel(true);
    }

}