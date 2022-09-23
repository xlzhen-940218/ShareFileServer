package com.xlzhen.sharefileserver;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.xlzhen.sharefileserver.service.ServerRunService;
import com.xlzhen.sharefileserver.utils.ShareFileToMeUtils;

import java.util.List;

/* loaded from: classes3.dex */
public class MainActivity extends AppCompatActivity {
    public WebView webView;
    public List<String> shareFiles;

    @Override // android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && requestCode == 1000) {
            String[] shareFiles = null;
            Uri uri = data.getData();
            if ( uri  != null) {
                shareFiles = new String[]{ShareFileToMeUtils.parseFileUri(this,  uri )};
            } else {
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    shareFiles = new String[clipData.getItemCount()];
                    for (int i3 = 0; i3 < clipData.getItemCount(); i3++) {
                        shareFiles[i3] = ShareFileToMeUtils.parseFileUri(this, clipData.getItemAt(i3).getUri());
                    }
                }
            }
            if (shareFiles != null && shareFiles.length > 0) {
                Application.setShareFiles(shareFiles);
                webView.loadUrl(ServerRunService.getHost() + "/" + Application.getRegisterShareFile());
            }
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.webView.canGoBack()) {
            this.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != 0 || ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != 0) {
            Toast.makeText(this, (int) R.string.get_wifiname_toast, Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.ACCESS_FINE_LOCATION"}, 1);
        } else if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent();
            intent.setAction("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 10001);
        }
        ServerRunService.start(this);
        WebView webView = (WebView) findViewById(R.id.webView);
        this.webView = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        this.webView.setWebViewClient(new WebViewClient() { // from class: com.xlzhen.sharefileserver.MainActivity.1
            @Override // android.webkit.WebViewClient
            public boolean shouldOverrideUrlLoading(WebView webView2, WebResourceRequest webResourceRequest) {
                webView2.loadUrl(webResourceRequest.getUrl().toString());
                return true;
            }
        });
        this.webView.setWebChromeClient(new WebChromeClient() {
            @Override // android.webkit.WebChromeClient
            public boolean onJsAlert(WebView webView, String str, String str2, JsResult jsResult) {
                return super.onJsAlert(webView, str, str2, jsResult);
            }
        });
        this.shareFiles  = ShareFileToMeUtils.getShareFiles(this);

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (shareFiles.size() > 0) {
                        Application.setShareFiles((String[]) MainActivity.this.shareFiles.toArray(new String[MainActivity.this.shareFiles.size()]));
                        WebView webView1 = MainActivity.this.webView;
                        webView1.loadUrl(ServerRunService.getHost() + "/" + Application.getRegisterShareFile());
                    } else if (Application.getClipDataNote().length() > 0) {
                        WebView webView1 = MainActivity.this.webView;
                        webView1.loadUrl(ServerRunService.getHost() + "/sharednote.html");
                    } else {
                        MainActivity.this.webView.loadUrl(ServerRunService.getHost());
                    }
                }
            });
        }).start();

    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() != R.id.add_file) {
            return super.onOptionsItemSelected(menuItem);
        }
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.please_select_files)), 1000);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, (int) R.string.please_install_file_explorer, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2000 && grantResults.length > 0 && grantResults[0] == 0) {
            Toast.makeText(this, "Permission Get Success", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                Intent intent = new Intent();
                intent.setAction("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 10001);
            }
        }
    }
}
