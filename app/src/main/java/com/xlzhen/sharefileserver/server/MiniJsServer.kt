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

        var substring = uri.substring(1)

        val dbHelper = com.xlzhen.sharefileserver.utils.DatabaseHelper(Application.getContext())
        val savedPassword = dbHelper.getSetting("password")
        val isPasswordProtected = !savedPassword.isNullOrEmpty() && serverPackage.serverPath == "web"

        if (isPasswordProtected) {
            val cookieHeader = session.headers["cookie"]
            var authPassword = ""
            if (cookieHeader != null) {
                val cookies = cookieHeader.split(";")
                for (cookie in cookies) {
                    val parts = cookie.trim().split("=")
                    if (parts.size >= 2 && parts[0] == "auth_password") {
                        authPassword = java.net.URLDecoder.decode(parts[1], "UTF-8")
                        break
                    }
                }
            }
            val isAuthenticated = authPassword == savedPassword

            if (!isAuthenticated) {
                if (substring == "login_verify" || substring == "login.html" || substring.endsWith(".css") || substring.endsWith(".js") || substring.endsWith(".png") || substring.endsWith(".svg")) {
                    // Let static login assets pass
                } else if (substring.contains("storage/emulated/0/") || (!substring.contains(".") && substring.isNotEmpty())) {
                    return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json; charset=utf-8", "{\"code\": 401, \"message\": \"Unauthorized\"}")
                } else {
                    return try {
                        val assets = Application.getContext().assets
                        val inputStream = assets.open("${serverPackage.serverPath}/login.html")
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        newFixedLengthResponse(Response.Status.OK, MIME_HTML, String(bytes, Charsets.UTF_8))
                    } catch (e: IOException) {
                        newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Unauthorized")
                    }
                }
            }
        }

        if (substring == "login_verify") {
            if (method == Method.POST) {
                try {
                    session.parseBody(HashMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val pwd = params["password"]
            if (pwd == savedPassword) {
                val response = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"code\":200}")
                val expires = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("GMT")
                }.format(java.util.Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                response.addHeader("Set-Cookie", "auth_password=${java.net.URLEncoder.encode(pwd ?: "", "UTF-8")}; Expires=$expires; Path=/")
                return response
            } else {
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", "{\"code\":401}")
            }
        }

        if (substring.isEmpty()) {
            substring = "index.html"
        }

        if (substring == "download_zip") {
            val filesStr = params["files"]
            if (filesStr.isNullOrEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No files specified")
            }
            val filePaths = filesStr.split(",")
            val pos = java.io.PipedOutputStream()
            val pis = java.io.PipedInputStream(pos)

            Thread {
                try {
                    val zos = java.util.zip.ZipOutputStream(pos)
                    for (path in filePaths) {
                        val file = java.io.File(path)
                        if (file.exists() && file.isFile) {
                            val entry = java.util.zip.ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            val fis = java.io.FileInputStream(file)
                            fis.copyTo(zos)
                            fis.close()
                            zos.closeEntry()
                        }
                    }
                    zos.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            val response = newChunkedResponse(Response.Status.OK, "application/zip", pis)
            response.addHeader("Content-Disposition", "attachment; filename=\"shared_files.zip\"")
            return response
        }

        if (substring == "index.html" || substring == "login.html" || substring == "sharednote.html") {
            return try {
                val assets = Application.getContext().assets
                val inputStream = assets.open("${serverPackage.serverPath}/$substring")
                val bytes = inputStream.readBytes()
                inputStream.close()
                newFixedLengthResponse(Response.Status.OK, MIME_HTML, String(bytes, Charsets.UTF_8))
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
                val inputStream = assets2.open("${serverPackage.serverPath}/$substring")
                val bytes = inputStream.readBytes()
                inputStream.close()
                newFixedLengthResponse(Response.Status.OK, str, java.io.ByteArrayInputStream(bytes), bytes.size.toLong())
            } catch (e4: IOException) {
                e4.printStackTrace()
                newFixedLengthResponse(Response.Status.NOT_FOUND, "*/*", "Not Found")
            }
        }
    }
}
