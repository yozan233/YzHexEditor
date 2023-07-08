package com.peke.hex.editor.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class FilePathUtils {

    /**
     * 文件Uri转绝对路径
     */
    public static String getPathFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            return null;
        }
        if (DocumentsContract.isDocumentUri(context, uri)) {
            final String authority = uri.getAuthority();
            // 判断Authority是否为本地端档案所使用的
            if ("com.android.externalstorage.documents".equals(authority)) {
                // 外部存储空间
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                if ("primary".equals(type)) {
                    return Environment.getExternalStorageDirectory().getAbsolutePath().concat("/").concat(divide[1]);
                } else {
                    return "/storage/".concat(type).concat("/").concat(divide[1]);
                }
            } else if ("com.android.providers.downloads.documents".equals(authority)) {
                // 下载目录
                final String docId = DocumentsContract.getDocumentId(uri);
                if (docId.startsWith("raw:")) {
                    return docId.replaceFirst("raw:", "");
                }
                final Uri downloadUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                String path = queryAbsolutePath(context, downloadUri);
                return processPathForN( context,uri,path);
            } else if ("com.android.providers.media.documents".equals(authority)) {
                // 图片、影音档案
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                Uri mediaUri = null;
                if ("image".equals(type)) {
                    mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                    return null;
                }
                mediaUri = ContentUris.withAppendedId(mediaUri, Long.parseLong(divide[1]));
                String path = queryAbsolutePath(context, mediaUri);
                return processPathForN( context,uri,path);
            }
        } else {
            // 如果是一般的URI
            final String scheme = uri.getScheme();
            String path = null;
            if ("content".equals(scheme)) {
                // 內容URI
                path = queryAbsolutePath(context, uri);
                if (path==null) {
                    path = uri.getPath();
                }
            } else if ("file".equals(scheme)) {
                // 档案URI
                path = uri.getPath();
            }
            return processPathForN( context,uri,path);
        }
        return null;
    }

    public static String queryAbsolutePath(final Context context, final Uri uri) {
        final String[] projection = {MediaStore.MediaColumns.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(index);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    private static String processPathForN(Context context, Uri uri,String path){
        if (Build.VERSION.SDK_INT >= 24 ){
            if (TextUtils.isEmpty(path)){
                path = getFilePathForN( context, uri, getFileName( context,uri) );
            }else{
                File tmpFile = new File(path);
                if (!tmpFile.exists() ){
                    path = getFilePathForN( context, uri ,getFileName( context,uri) );
                }else {
                    try {
                        ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY);
                        fileDescriptor.close();
                    }catch (Exception e){
                        path = getFilePathForN( context, uri ,tmpFile.getName());
                    }
                }
            }
        }
        return path;
    }


    /**
     * android7.0以上处理方法
     * 使用ContentResolver和openInputStream()在Uri标识的内容上获取InputStream。
     * 在控制的文件上使用InputStream和FileOutputStream复制内容，然后使用该文件。
     */
    private static String getFilePathForN(Context context, Uri uri,String fileName) {
        if ( !TextUtils.isEmpty(fileName)){
            File file = new File(context.getFilesDir(), fileName);
            if (file.exists()){
                file.delete();
            }
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri); FileOutputStream outputStream = new FileOutputStream(file)) {
                int read;
                int maxBufferSize = 1024 * 1024;
                int bytesAvailable = inputStream.available();
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                final byte[] buffers = new byte[bufferSize];
                while ((read = inputStream.read(buffers)) != -1) {
                    outputStream.write(buffers, 0, read);
                }
                return file.getPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获取文件名称
     */
    private static String getFileName(Context context, Uri uri){
        DocumentFile documentFile = DocumentFile.fromSingleUri(context,uri);
        String name = "";
        if(documentFile!=null){
            name = documentFile.getName();
        }

        if ( TextUtils.isEmpty(name)){
            try (Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null)) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                name = (returnCursor.getString(nameIndex));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return name;
    }

}
