package com.peke.hex.editor.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.view.View
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

class ClipboardUtils(private val activity: Activity) {

    private val mClipboardManager:ClipboardManager = activity.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val myClipboardManager:MyClipboardManager = MyClipboardManager(activity)
    private val rootView: View = activity.window.decorView

    /**
     * 写入文本到系统剪贴板
     * @param text 文本
     */
    fun writeSysClipText(text: CharSequence?,block:((Boolean)->Unit)? = null) {
        if (text.isNullOrEmpty()){
            block?.invoke(false)
            return
        }
        rootView.post {
            var success = false
            val cm: ClipboardManager = mClipboardManager
            // 创建普通字符型ClipData
            val mClipData = ClipData.newPlainText("PokeTools", text)
            // 将ClipData内容放到系统剪贴板里。
            try {
                cm.setPrimaryClip(mClipData)
                success = true
            } catch (ignored: java.lang.Exception) {
            }
            block?.invoke(success)
        }
    }

    /**
     * 读取系统剪贴板的文本
     * 返回剪贴板的文本
     */
    fun readSysClipText(block: (String?) -> Unit) {
        rootView.post {
            val clipboard: ClipboardManager = mClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                var text: String? = null
                try {
                    text = clip.getItemAt(0).coerceToText(activity).toString()
                } catch (ignored: java.lang.Exception) {
                }
                if (!TextUtils.isEmpty(text)) {
                    block.invoke(text)
                }
            }
            block.invoke(null)
        }
    }

    fun readMyClipboardBytes(): ByteArray {
        return myClipboardManager.readBytes()
    }

    fun writeMyClipboardBytes(bytes: ByteArray) {
        myClipboardManager.writeBytes(bytes)
    }

    fun clearMyClipboardData() {
        myClipboardManager.deleteMyClipboardFile()
    }

    internal class MyClipboardManager(activity: Activity) {
        private val myClipboardFilePath: String = File(activity.filesDir.absoluteFile, "MyClipboard.bin").absolutePath

        fun deleteMyClipboardFile(){
            val file = File(myClipboardFilePath)
            if (file.exists()){
                file.delete()
            }
        }

        fun writeBytes(bytes: ByteArray){
            deleteMyClipboardFile()
            var raf: RandomAccessFile? = null
            try {
                raf = RandomAccessFile(myClipboardFilePath, "rw")
                raf.write(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                IOUtils.closeQuietly(raf)
            }
        }

        private fun writeText(text: String) {
            writeBytes(text.toByteArray(StandardCharsets.UTF_8))
        }

        fun readBytes(): ByteArray {
            var raf: RandomAccessFile? = null
            var bytes = byteArrayOf()
            try {
                raf = RandomAccessFile(myClipboardFilePath, "rw")
                bytes = ByteArray(raf.length().toInt())
                raf.read(bytes)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                IOUtils.closeQuietly(raf)
            }
            return bytes
        }

        private fun readText(): String {
            return String(readBytes(), StandardCharsets.UTF_8)
        }

    }

}