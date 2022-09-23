package com.evgenii.jsevaluator;

import android.content.Context;
import com.evgenii.jsevaluator.callback.CallJavaResultInterface;
import com.evgenii.jsevaluator.callback.HandlerWrapperInterface;
import com.evgenii.jsevaluator.callback.JsCallback;
import com.evgenii.jsevaluator.callback.WebViewWrapperInterface;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/* loaded from: classes6.dex */
public class JsEvaluator implements CallJavaResultInterface {
    public WebViewWrapperInterface webViewWrapperInterface;
    public final Context context;
    public AtomicReference<JsCallback> callback = new AtomicReference<>(null);
    public HandlerWrapperInterface handler = new HandlerWrapper();



    public JsEvaluator(Context context) {
        this.context = context;
        if (this.webViewWrapperInterface == null) {
            this.webViewWrapperInterface = new WebViewWrapper(context, this);
        }
    }

    public static String escapeCarriageReturn(String str) {
        return str.replace("\r", "\\r");
    }

    public static String escapeClosingScript(String str) {
        return str.replace("</", "<\\/");
    }

    public static String escapeNewLines(String str) {
        return str.replace("\n", "\\n");
    }

    public static String escapeSingleQuotes(String str) {
        return str.replace("'", "\\'");
    }

    public static String escapeSlash(String str) {
        return str.replace("\\", "\\\\");
    }

    public static String getJsForEval(String str) {
        return String.format("%s.returnResultToJava(eval('try{%s}catch(e){\"%s\"+JSON.stringify({code:500,message:e.message})}'));", "app", escapeCarriageReturn(escapeNewLines(escapeClosingScript(escapeSingleQuotes(escapeSlash(str))))), "appJsEvaluatorException");
    }

    public void callFunction(String str, JsCallback cVar, String str2, Map<String, String> map) {
        evaluate(str + "; " + JsFunctionCallFormatter.toString(str2, map), cVar);
    }

    public void evaluate(String str, JsCallback cVar) {
        String js = getJsForEval(str);
        this.callback.set(cVar);
        this.handler.post(() -> JsEvaluator.this.m122a().loadJavaScript(js));
    }

    public WebViewWrapperInterface m122a() {
        return this.webViewWrapperInterface;
    }

    @Override // com.evgenii.jsevaluator.callback.CallJavaResultInterface
    public void jsCallFinished(String str) {
        JsCallback callbackAndSet = this.callback.getAndSet(null);
        if (callbackAndSet != null) {
            this.handler.post(new Runnable() {
                @Override
                public void run() {

                    if (str == null || !str.startsWith("appJsEvaluatorException")) {
                        callbackAndSet.onError(str);
                    } else {
                        callbackAndSet.onResult(str.substring(23));
                    }
                }
            });
        }
    }
}
