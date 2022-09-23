package com.xlzhen.sharefileserver.server;

import android.content.res.AssetManager;
import com.xlzhen.sharefileserver.Application;
import com.xlzhen.sharefileserver.entity.ServerPackage;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes5.dex */
public class MiniJsServer extends NanoHTTPD {
    public ServerPackage serverPackage;

    public MiniJsServer(int port, ServerPackage serverPackage) {
        super(port);
        this.serverPackage = serverPackage;
    }

    @Override // fi.iki.elonen.NanoHTTPD
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
        NanoHTTPD.Method method = session.getMethod();
        Map<String, String> params = session.getParms();

        String substring = uri.substring(1);
        if (substring.length() == 0) {
            try {
                AssetManager assets = Application.getContext().getAssets();
                return newChunkedResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, assets.open(this.serverPackage.getServerPath() + "/index.html"));
            } catch (IOException e) {
                e.printStackTrace();
                return NanoHTTPD.newFixedLengthResponse("<html><body><div><h1>Please Load web</h1></div></body></html>");
            }
        } else if (substring.contains("storage/emulated/0/") || !substring.contains(".")) {
            if (method == NanoHTTPD.Method.POST) {
                try {
                    session.parseBody(new HashMap());
                } catch (NanoHTTPD.ResponseException e2) {
                    e2.printStackTrace();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            ServerJS instance = ServerJS.getInstance();
            String serverPath = this.serverPackage.getServerPath();
            String serverJs = this.serverPackage.getServerJs();
            boolean uploadFile=session.getHeaders().containsKey("content-type")&&session.getHeaders().get("content-type").contains("multipart/form-data");

            return instance.executeJSServerAPI(serverPath, serverJs, substring, uploadFile, params);
        } else {
            try {
                if (substring.contains("package.json") || substring.contains(this.serverPackage.getMain())) {
                    throw new IOException("Insufficient permissions");
                }
                String str = substring.endsWith(".css") ? "text/css" : "*/*";
                AssetManager assets2 = Application.getContext().getAssets();
                return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, str, assets2.open(this.serverPackage.getServerPath() + "/" + substring));
            } catch (IOException e4) {
                e4.printStackTrace();
                return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.NOT_FOUND, "*/*", null);
            }
        }
    }
}