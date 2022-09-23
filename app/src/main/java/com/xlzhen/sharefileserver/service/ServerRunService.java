package com.xlzhen.sharefileserver.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.Html;
import com.evgenii.jsevaluator.JsEvaluator;
import com.xlzhen.sharefileserver.R;
import com.xlzhen.sharefileserver.entity.ServerPackage;
import com.xlzhen.sharefileserver.server.MiniJsServer;
import com.xlzhen.sharefileserver.server.ServerJS;
import com.xlzhen.sharefileserver.utils.NetWorkUtils;

import java.io.DataInputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes8.dex */
public class ServerRunService extends BaseService {
    public static ServerRunService f284c;
    public static String host;
    public int rootPort = 8080;
    public int userPort = 8090;



    public static void start(Context context) {
        if (f284c == null) {
            f284c = new ServerRunService();
            context.startService(new Intent(context, ServerRunService.class));
        }
    }

    public static String getHost() {
        return host;
    }

    public final void startManagementServer() {
        host = "http://" + NetWorkUtils.getDeviceIp() + ":" + this.rootPort;
        try {
            try {
                DataInputStream open = new DataInputStream(getAssets().open("management/package.json"));
                byte[] bArr = new byte[open.available()];
                open.readFully(bArr);
                JSONObject jSONObject = new JSONObject(new String(bArr));
                ServerPackage serverPackage = new ServerPackage();
                serverPackage.setLogo(jSONObject.getString("logo"));
                serverPackage.setName(jSONObject.getString("name"));
                serverPackage.setVersion(jSONObject.getString("version"));
                serverPackage.setAuthor(jSONObject.getString("author"));
                serverPackage.setDescription(jSONObject.getString("description"));
                serverPackage.setMain(jSONObject.getString("main"));
                serverPackage.setServerPath("management");
                open.close();
                ServerJS.getInstance().init(serverPackage, new JsEvaluator(this));
                serverPackage.setMiniJsServer(new MiniJsServer(this.rootPort, serverPackage));
                try {
                    serverPackage.getMiniJsServer().start();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                serverPackage.setPort(host);
                serverPackage.setPower(true);
            } catch (JSONException e4) {
                e4.printStackTrace();
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
    }

    public final void startUserServer() {
        String str = "http://" + NetWorkUtils.getDeviceIp() + ":" + this.userPort;
        try {
            try {
                DataInputStream open =new DataInputStream( getAssets().open("web/package.json"));
                byte[] bArr = new byte[open.available()];
                open.readFully(bArr);
                JSONObject jSONObject = new JSONObject(new String(bArr));
                ServerPackage serverPackage = new ServerPackage();
                serverPackage.setLogo(jSONObject.getString("logo"));
                serverPackage.setName(jSONObject.getString("name"));
                serverPackage.setVersion(jSONObject.getString("version"));
                serverPackage.setAuthor(jSONObject.getString("author"));
                serverPackage.setDescription(jSONObject.getString("description"));
                serverPackage.setMain(jSONObject.getString("main"));
                serverPackage.setServerPath("web");
                open.close();
                ServerJS.getInstance().init(serverPackage, new JsEvaluator(this));
                serverPackage.setMiniJsServer(new MiniJsServer(this.userPort, serverPackage));
                try {
                    serverPackage.getMiniJsServer().start();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                serverPackage.setPort(str);
                serverPackage.setPower(true);
            } catch (JSONException e3) {
                e3.printStackTrace();
            }
        } catch (IOException e4) {
            e4.printStackTrace();
        }
    }

    @Override // com.xlzhen.sharefileserver.service.BaseService, android.app.Service
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override // com.xlzhen.sharefileserver.service.BaseService, android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        startManagementServer();
        startUserServer();
        notification();
        return super.onStartCommand(intent, Service.START_FLAG_REDELIVERY, i2);
    }

    public void notification() {
        NotificationChannel notificationChannel = Build.VERSION.SDK_INT >= 26 ? new NotificationChannel(ServerRunService.class.getName(), ServerRunService.class.getSimpleName(), NotificationManager.IMPORTANCE_HIGH) : null;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setChannelId(ServerRunService.class.getName());
        }
        builder.setContentText(Html.fromHtml(getString(R.string.notification_content)));
        builder.setContentTitle(getString(R.string.notification_title));
        builder.setSmallIcon(R.mipmap.ic_launcher_notification);
        if (Build.VERSION.SDK_INT >= 23) {
            builder.setLargeIcon(Icon.createWithResource(getApplicationContext(), (int) R.mipmap.ic_launcher));
        }
        builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 1, getPackageManager().getLaunchIntentForPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE));
        Notification build = builder.build();
        build.flags = 2;
        build.flags = 34;
        build.flags = 34 | 64;
        startForeground(1, build);
    }
}