package com.evgenii.jsevaluator;

import android.os.Handler;
import com.evgenii.jsevaluator.callback.HandlerWrapperInterface;

/* loaded from: classes6.dex */
public class HandlerWrapper implements HandlerWrapperInterface {
    public final Handler handler = new Handler();

    @Override // com.evgenii.jsevaluator.callback.HandlerWrapperInterface
    public void post(Runnable runnable) {
        this.handler.post(runnable);
    }
}
