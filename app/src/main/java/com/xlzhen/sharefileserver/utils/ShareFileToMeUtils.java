package com.xlzhen.sharefileserver.utils;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import com.xlzhen.sharefileserver.Application;
import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes7.dex */
public class ShareFileToMeUtils {
    public static List<String> getShareFiles(Activity activity) {
        ArrayList<String> arrayList = new ArrayList<>();
        Intent intent = activity.getIntent();
        if ("android.intent.action.SEND".equals(intent.getAction())) {
            if (intent.getType().equals(NanoHTTPD.MIME_PLAINTEXT)) {
                Application.setClipDataNote(intent.getClipData().getItemAt(0).getText().toString());
                return new ArrayList();
            }
            arrayList.add(parseFileUri(activity, (Uri) intent.getParcelableExtra("android.intent.extra.STREAM")));
        } else if ("android.intent.action.SEND_MULTIPLE".equals(intent.getAction())) {
            Iterator it = intent.getParcelableArrayListExtra("android.intent.extra.STREAM").iterator();
            while (it.hasNext()) {
                Parcelable uri = (Parcelable) it.next();
                String b = parseFileUri(activity, (Uri) uri);
                if (b != null) {
                    arrayList.add(b);
                }
            }
        }
        return arrayList;
    }

    public static String parseFileUri(Activity activity, Uri uri) {
        String str = "";
        if (uri == null) {
            return str;
        }
        Log.v("Before Transformation", uri.getPath());
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        if (uri.getPath().startsWith("/root/data/app/") && uri.getPath().endsWith(".apk")) {
            return uri.getPath().replace("/root/data/app/", "data/app/");
        }
        String str2 = null;
        if (!"content".equalsIgnoreCase(uri.getScheme()) || !uri.getHost().equalsIgnoreCase("com.google.android.apps.photos.contentprovider")) {
            try {
                Cursor managedQuery = activity.managedQuery(uri, new String[]{"_data"}, null, null, null);
                int columnIndexOrThrow = managedQuery.getColumnIndexOrThrow("_data");
                managedQuery.moveToFirst();
                str2 = managedQuery.getString(columnIndexOrThrow);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (str2 == null) {
                str2 = ContentUriUtil.getPath(activity, uri);
            }
            if (str2 != null) {
                str = str2;
            }
            Log.v("After Transformation", str);
            return str;
        }
        try {
            Bitmap decodeStream = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(uri));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            decodeStream.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            File externalFilesDir = Application.getContext().getExternalFilesDir("Google Photo");
            if (!externalFilesDir.exists()) {
                externalFilesDir.mkdirs();
            }
            String absolutePath = externalFilesDir.getAbsolutePath();
            File file = new File(absolutePath, System.currentTimeMillis() + ".jpg");
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(byteArrayOutputStream.toByteArray());
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            return file.getAbsolutePath();
        } catch (FileNotFoundException e3) {
            e3.printStackTrace();
            return null;
        }
    }

    /*public static String m85a(Activity activity, Uri uri) {
        return parseFileUri(activity, uri);
    }*/
}
