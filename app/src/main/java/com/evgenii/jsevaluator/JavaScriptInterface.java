package com.evgenii.jsevaluator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.wifi.WifiManager;
import android.webkit.JavascriptInterface;
import com.evgenii.jsevaluator.callback.CallJavaResultInterface;
import com.evgenii.jsevaluator.utils.LanguageUtils;
import com.evgenii.jsevaluator.utils.MimeTypeConvert;
import com.evgenii.jsevaluator.utils.SQLUtils;
import com.xlzhen.sharefileserver.Application;
import com.xlzhen.sharefileserver.utils.NetWorkUtils;
import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes6.dex */
public class JavaScriptInterface {
    public final CallJavaResultInterface mCallJavaResultInterface;
    private final Object lock = new Object();
    private String ssid = "未知wifi";

    public JavaScriptInterface(CallJavaResultInterface aVar) {
        this.mCallJavaResultInterface = aVar;
    }

    @JavascriptInterface
    public boolean createDataBase(String str, String[] strArr) {
        return SQLUtils.createDb(str, strArr);
    }

    @JavascriptInterface
    public boolean deleteDataBase(String str, String str2, String[] strArr, String[] strArr2) {
        return SQLUtils.deleteDb(str, str2, strArr, strArr2);
    }

    @JavascriptInterface
    public String getDir(String str) {
        return Application.getContext().getExternalFilesDir(str).getAbsolutePath() + "/";
    }

    @JavascriptInterface
    public InputStream getFile(String str) {
        try {
            return new FileInputStream(str);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @JavascriptInterface
    public long getFileCreateTime(String str) {
        return new File(str).lastModified();
    }

    @JavascriptInterface
    public String getFileName(String str) {
        String name = new File(str).getName();
        return (!str.startsWith("data/app/") || !str.endsWith(".apk")) ? name : str.split("/")[2];
    }

    @JavascriptInterface
    public long getFileSize(String str) {
        return new File(str).length();
    }

    @JavascriptInterface
    public int getFileType(String str) {
        String a = MimeTypeConvert.getSuffix(str.substring(str.lastIndexOf(".") + 1));
        if (a.startsWith("video")) {
            return 1;
        }
        if (a.startsWith("audio")) {
            return 3;
        }
        if (a.startsWith("image")) {
            return 0;
        }
        if (a.equalsIgnoreCase(NanoHTTPD.MIME_PLAINTEXT) || a.endsWith("pdf") || a.endsWith("word") || a.endsWith("document") || a.endsWith("powerpoint") || a.contains("officedocument") || a.endsWith("excel") || a.endsWith("chm")) {
            return 2;
        }
        if (a.endsWith("vnd.android.package-archive")) {
            return 4;
        }
        return (a.contains("zip") || a.contains("rar") || a.startsWith("7z")) ? 5 : 6;
    }

    @JavascriptInterface
    public String getFilesForDir(String str) {
        String[] list;
        File file = new File(str);
        if (!file.isDirectory()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[");
        for (String str2 : file.list()) {
            sb.append("\"");
            sb.append(str2);
            sb.append("\",");
        }
        return sb.substring(0, sb.length() - 1) + "]";
    }

    @JavascriptInterface
    public String getLanguage() {
        return LanguageUtils.getLanguage();
    }

    @JavascriptInterface
    public String getShareFiles() {
        String[] m8c;
        if (Application.getShareFiles() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[");
        for (String str : Application.getShareFiles()) {
            if (str != null && str.length() > 0) {
                sb.append("\"");
                sb.append(str);
                sb.append("\",");
            }
        }
        return sb.substring(0, sb.length() - 1) + "]";
    }

    @JavascriptInterface
    public String getShareNote() {
        String clipDataNote = Application.getClipDataNote();
        if (clipDataNote.length() <= 0) {
            return clipDataNote;
        }
        Application.setClipDataNote("");
        return "\r\n\n" + clipDataNote;
    }

    @JavascriptInterface
    public String getThumbnail(String filePath) {
        if(!new File(filePath).exists())
            return "./imgs/unknown.png";
        String suffix = MimeTypeConvert.getSuffix(filePath.substring(filePath.lastIndexOf(".") + 1));
        if (suffix.equalsIgnoreCase(NanoHTTPD.MIME_PLAINTEXT) || suffix.endsWith("chm")) {
            return "./imgs/txt.png";
        }
        if (suffix.endsWith("pdf")) {
            return "./imgs/pdf.png";
        }
        if (suffix.endsWith("word") || suffix.endsWith("document")) {
            return "./imgs/word.png";
        }
        if (suffix.endsWith("powerpoint") || suffix.contains("officedocument")) {
            return "./imgs/ppt.png";
        }
        if (suffix.endsWith("excel")) {
            return "./imgs/excel.png";
        }
        if (suffix.startsWith("audio") || suffix.startsWith("video")) {
            File externalFilesDir = Application.getContext().getExternalFilesDir("thumbnail");
            if (!externalFilesDir.exists()) {
                externalFilesDir.mkdirs();
            }
            String absolutePath = externalFilesDir.getAbsolutePath();
            File file = new File(absolutePath, getFileName(filePath) + ".jpg");
            if (file.exists()) {
                return file.getAbsolutePath();
            }
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(filePath);
            Bitmap frameAtTime = mediaMetadataRetriever.getFrameAtTime();
            if (frameAtTime == null) {
                try {
                    byte[] embeddedPicture = mediaMetadataRetriever.getEmbeddedPicture();
                    frameAtTime = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (frameAtTime == null) {
                return "";
            }
            Bitmap createScaledBitmap = Bitmap.createScaledBitmap(frameAtTime, 100, 100, true);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            createScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(byteArrayOutputStream.toByteArray());
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            return file.getAbsolutePath();
        } else if (suffix.startsWith("image")) {
            File externalFilesDir2 = Application.getContext().getExternalFilesDir("thumbnail");
            if (!externalFilesDir2.exists()) {
                externalFilesDir2.mkdirs();
            }
            File file2 = new File(externalFilesDir2.getAbsolutePath(), getFileName(filePath));
            if (file2.exists()) {
                return file2.getAbsolutePath();
            }
            try {
                Bitmap createScaledBitmap2 = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(new FileInputStream(filePath)), 100, 100, true);
                ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
                createScaledBitmap2.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream2);
                try {
                    FileOutputStream fileOutputStream2 = new FileOutputStream(file2);
                    fileOutputStream2.write(byteArrayOutputStream2.toByteArray());
                    fileOutputStream2.flush();
                    fileOutputStream2.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
                return file2.getAbsolutePath();
            } catch (FileNotFoundException e4) {
                e4.printStackTrace();
                return filePath;
            }catch (NullPointerException ex){
                ex.printStackTrace();
                return "./imgs/unknown.png";
            }
        } else if (!suffix.equalsIgnoreCase("application/vnd.android.package-archive")) {
            return "";
        } else {
            File externalFilesDir3 = Application.getContext().getExternalFilesDir("thumbnail");
            if (!externalFilesDir3.exists()) {
                externalFilesDir3.mkdirs();
            }
            String absolutePath2 = externalFilesDir3.getAbsolutePath();
            File file3 = new File(absolutePath2, getFileName(filePath) + ".png");
            if (file3.exists()) {
                return file3.getAbsolutePath();
            }
            PackageManager packageManager = Application.getContext().getPackageManager();
            Drawable applicationIcon = packageManager.getApplicationIcon(packageManager.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES).applicationInfo);
            int intrinsicWidth = applicationIcon.getIntrinsicWidth();
            int intrinsicHeight = applicationIcon.getIntrinsicHeight();
            Bitmap createBitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(createBitmap);
            applicationIcon.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
            applicationIcon.draw(canvas);
            Bitmap createScaledBitmap3 = Bitmap.createScaledBitmap(createBitmap, 100, 100, true);
            ByteArrayOutputStream byteArrayOutputStream3 = new ByteArrayOutputStream();
            createScaledBitmap3.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream3);
            try {
                FileOutputStream fileOutputStream3 = new FileOutputStream(file3);
                fileOutputStream3.write(byteArrayOutputStream3.toByteArray());
                fileOutputStream3.flush();
                fileOutputStream3.close();
            } catch (IOException e5) {
                e5.printStackTrace();
            }
            return file3.getAbsolutePath();
        }
    }

    @JavascriptInterface
    public String getWebIp() {
        return NetWorkUtils.getDeviceIp();
    }

    @JavascriptInterface
    public String getWifiName() {
        String ssid = ((WifiManager) Application.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();
        return (ssid == null || ssid.length() == 0) ? "未知WIFI名称" : ssid;
    }

    @JavascriptInterface
    public boolean insertDataBase(String str, String str2, String[] strArr, String[] strArr2) {
        return SQLUtils.insertDb(str, str2, strArr, strArr2);
    }

    @JavascriptInterface
    public String queryDataBase(String str, String str2, String[] strArr, String[] strArr2) {
        return SQLUtils.queryDb(str, str2, strArr, strArr2);
    }

    @JavascriptInterface
    public boolean registerShareFile(String str) {
        Application.registerShareFile(true, str);
        return true;
    }

    @JavascriptInterface
    public void returnResultToJava(String str) {
        this.mCallJavaResultInterface.jsCallFinished(str);
    }

    @JavascriptInterface
    public String[] saveFile(InputStream inputStream, String str, String str2) {
        File file = new File(str2 + str);
        String[] strArr = new String[3];
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] bArr = new byte[8192];
            while (true) {
                int read = inputStream.read(bArr);
                if (read == -1) {
                    break;
                }
                fileOutputStream.write(bArr, 0, read);
            }
            fileOutputStream.close();
            strArr[0] = "200";
            strArr[1] = "OK";
            strArr[2] = file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            strArr[1] = e.getMessage();
            strArr[0] = "404";
        } catch (IOException e2) {
            e2.printStackTrace();
            strArr[1] = e2.getMessage();
            strArr[0] = "500";
        }
        return strArr;
    }

    @JavascriptInterface
    public boolean unregisterShareFile() {
        Application.registerShareFile(false, "");
        return true;
    }

    @JavascriptInterface
    public boolean updateDataBase(String str, String str2, String[] strArr, String[] strArr2, String[] strArr3, String[] strArr4) {
        return SQLUtils.updateDb(str, str2, strArr, strArr2, strArr3, strArr4);
    }
}
