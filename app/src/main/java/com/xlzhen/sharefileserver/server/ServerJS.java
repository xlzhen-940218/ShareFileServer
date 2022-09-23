package com.xlzhen.sharefileserver.server;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.evgenii.jsevaluator.JsEvaluator;
import com.evgenii.jsevaluator.callback.JsCallback;
import com.evgenii.jsevaluator.utils.MimeTypeConvert;
import com.xlzhen.sharefileserver.Application;
import com.xlzhen.sharefileserver.entity.ServerPackage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

/* loaded from: classes5.dex */
public class ServerJS {
    public static volatile ServerJS serverJS;
    public JsEvaluator jsEvaluator;
    public String jsResult;

    public final void initServerJS(ServerPackage serverPackage, JsEvaluator bVar) {
        try {
            AssetManager assets = Application.getContext().getAssets();
            DataInputStream open = new DataInputStream(assets.open(serverPackage.getServerPath() + "/" + serverPackage.getMain()));
            byte[] bArr = new byte[open.available()];
            open.readFully(bArr);
            serverPackage.setServerJs(new String(bArr));
            open.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadModule(serverPackage, 0);
        bVar.callFunction(serverPackage.getServerJs(), new JsCallback() {
            @Override
            public void onResult(String var1) {
                System.out.println(var1);
            }

            @Override
            public void onError(String var1) {

            }
        }, "main", new HashMap<>());
    }

    public static ServerJS getInstance() {
        if (serverJS == null) {
            synchronized (ServerJS.class) {
                if (serverJS == null) {
                    serverJS = new ServerJS();
                }
            }
        }
        return serverJS;
    }

    public synchronized NanoHTTPD.Response executeJSServerAPI(String str, String str2, String str3, boolean z, Map<String, String> map) {
        JSONException e4;
        FileNotFoundException e2;
        IOException e3;
        String string;
        String string2;
        Matcher matcher = Pattern.compile("function.*?" + str3 + ".*?\\((.*?)\\)").matcher(str2);
        if (!matcher.find()) {
            InputStream inputStream = null;
            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.NOT_FOUND, "application/json; charset=utf-8", null);
        }
        String[] split = matcher.group(1).split(",");
        if (split.length > map.size()) {
            for (String str4 : split) {
                if (!map.containsKey(str4.trim()) && str4.trim().length() > 0) {
                    map.put(str4.trim(), null);
                }
            }
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        this.jsEvaluator.callFunction(str2, new JsCallback() {
            @Override
            public void onResult(String str) {
                jsResult= str;//拿到结果
                countDownLatch.countDown();//释放锁
            }

            @Override
            public void onError(String str) {
                jsResult= str;//拿到结果
                countDownLatch.countDown();//释放锁
            }
        }, str3, map);
        try {
            countDownLatch.await(z ? 3600L : 10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String str5 = this.jsResult;
        if (str5 == null) {
            InputStream inputStream2 = null;
            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json; charset=utf-8", null);
        }
        if (str5.contains("requestStorageURL")) {
            try {
                JSONObject jSONObject = new JSONObject(this.jsResult);
                try {
                    if (jSONObject.getInt("code") == 200) {
                        string = jSONObject.getString("requestStorageURL");
                        string2 = jSONObject.getString("filename");
                        try {
                            if (!string.contains("storage/") && !string.contains("data/app")) {
                                return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, string.endsWith(".css") ? "text/css" : "*/*", Application.getContext().getAssets().open(str + "/" + string));
                            }
                            String a = MimeTypeConvert.m107a(string.substring(string.lastIndexOf(".") + 1));
                            Log.i("Play MimeType", a);
                            if (a.startsWith("image")) {
                                return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, a, new FileInputStream(string));
                            }
                            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, a, new FileInputStream(string));
                        } catch (FileNotFoundException e7) {
                            e2 = e7;
                            e2.printStackTrace();
                            String str6 = null;
                            InputStream inputStream3 = null;
                            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.NOT_FOUND, "*/*", null);
                        } catch (IOException e8) {
                            e3 = e8;
                            e3.printStackTrace();
                            if (this.jsResult.contains("requestToast")) {
                            }
                            return NanoHTTPD.newFixedLengthResponse(this.jsResult);
                        }
                    }
                } catch (JSONException e9) {
                    e4 = e9;
                    e4.printStackTrace();
                    if (this.jsResult.contains("requestToast")) {
                    }
                    return NanoHTTPD.newFixedLengthResponse(this.jsResult);
                }
            } catch (JSONException e10) {
                e4 = e10;
            }
        }
        if (this.jsResult.contains("requestToast")) {
            try {
                final JSONObject jSONObject2 = new JSONObject(this.jsResult);
                if (jSONObject2.getInt("code") == 200) {
                    new Handler(Application.getContext().getMainLooper()).post(new Runnable() { // from class: com.xlzhen.sharefileserver.server.ServerJS.1
                        @Override // java.lang.Runnable
                        public void run() {
                            try {
                                Toast.makeText(Application.getContext(), jSONObject2.getString("message"), Toast.LENGTH_SHORT).show();
                            } catch (JSONException e11) {
                                e11.printStackTrace();
                            }
                        }
                    });
                }
            } catch (JSONException e52) {
                e52.printStackTrace();
            }
        }
        return NanoHTTPD.newFixedLengthResponse(this.jsResult);
    }



    public void init(ServerPackage serverPackage, JsEvaluator bVar) {
        this.jsEvaluator = bVar;
        initServerJS(serverPackage, bVar);
    }

    public final void loadModule(ServerPackage serverPackage, int i) {
        Matcher matcher = Pattern.compile("require\\((.*?)\\)").matcher(serverPackage.getServerJs());
        int i2 = 0;
        while (matcher.find()) {
            i2++;
            if (i2 > i) {
                String group = matcher.group(1);
                Pattern compile = Pattern.compile("require\\(" + group + "\\)");
                String replace = group.replace("\"", "").replace("'", "");
                try {
                    AssetManager assets = Application.getContext().getAssets();
                    DataInputStream open =new DataInputStream( assets.open(serverPackage.getServerPath() + "/" + replace));
                    byte[] bArr = new byte[open.available()];
                    open.readFully(bArr);
                    serverPackage.setServerJs(compile.matcher(serverPackage.getServerJs()).replaceFirst(new String(bArr)));
                    open.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    i++;
                }
                loadModule(serverPackage, i);
                return;
            }
        }
    }
}