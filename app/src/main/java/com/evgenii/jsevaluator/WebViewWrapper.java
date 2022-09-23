package com.evgenii.jsevaluator;

import android.content.Context;
import android.util.Base64;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.evgenii.jsevaluator.callback.CallJavaResultInterface;
import com.evgenii.jsevaluator.callback.WebViewWrapperInterface;
import java.io.UnsupportedEncodingException;

/* loaded from: classes6.dex */
public class WebViewWrapper implements WebViewWrapperInterface {
    public WebView webView;
    public WebViewWrapper(Context context, CallJavaResultInterface aVar) {
        WebView webView = new WebView(context);
        this.webView = webView;
        webView.setWillNotDraw(true);
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("utf-8");
        this.webView.addJavascriptInterface(new JavaScriptInterface(aVar), "app");
        this.webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                System.out.println(consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }
        });
    }

    @Override // com.evgenii.jsevaluator.callback.WebViewWrapperInterface
    public void loadJavaScript(String str) {
        try {
            String encodeToString = Base64.encodeToString(("<script>" + str + "</script>").getBytes("UTF-8"), 0);
            WebView webView = this.webView;
            webView.loadUrl("data:text/html;charset=utf-8;base64," + encodeToString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
