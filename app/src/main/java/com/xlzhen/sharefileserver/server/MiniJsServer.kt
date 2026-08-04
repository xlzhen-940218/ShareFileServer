package com.xlzhen.sharefileserver.server

import android.content.res.AssetManager
import com.xlzhen.sharefileserver.Application
import com.xlzhen.sharefileserver.entity.ServerPackage
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class MiniJsServer(port: Int, var serverPackage: ServerPackage) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        val params = session.parms

        val substring = uri.substring(1)
        if (substring.isEmpty()) {
            return try {
                val assets = Application.getContext().assets
                newChunkedResponse(
                    Response.Status.OK,
                    MIME_HTML,
                    assets.open("${serverPackage.serverPath}/index.html")
                )
            } catch (e: IOException) {
                e.printStackTrace()
                newFixedLengthResponse("<html><body><div><h1>Please Load web</h1></div></body></html>")
            }
        } else if (substring.contains("storage/emulated/0/") || !substring.contains(".")) {
            if (method == Method.POST) {
                try {
                    session.parseBody(HashMap())
                } catch (e2: ResponseException) {
                    e2.printStackTrace()
                } catch (e3: IOException) {
                    e3.printStackTrace()
                }
            }
            val instance = ServerJS.instance!!
            val serverPath = serverPackage.serverPath
            val serverJs = serverPackage.serverJs
            val uploadFile = session.headers.containsKey("content-type") &&
                             session.headers["content-type"]!!.contains("multipart/form-data")

            return instance!!.executeJSServerAPI(serverPath, serverJs, substring, uploadFile, params)
        } else {
            return try {
                if (substring.contains("package.json") || substring.contains(serverPackage.main ?: "")) {
                    throw IOException("Insufficient permissions")
                }
                val str = if (substring.endsWith(".css")) "text/css" else "*/*"
                val assets2 = Application.getContext().assets
                newChunkedResponse(
                    Response.Status.OK,
                    str,
                    assets2.open("${serverPackage.serverPath}/$substring")
                )
            } catch (e4: IOException) {
                e4.printStackTrace()
                newChunkedResponse(Response.Status.NOT_FOUND, "*/*", null)
            }
        }
    }
}
