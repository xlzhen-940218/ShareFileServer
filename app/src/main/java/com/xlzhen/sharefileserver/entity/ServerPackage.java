package com.xlzhen.sharefileserver.entity;

import com.xlzhen.sharefileserver.server.MiniJsServer;

/* loaded from: classes8.dex */
public class ServerPackage {
    public String author;
    public String description;
    public String logo;
    public String main;
    public MiniJsServer miniJsServer;
    public String name;
    public String port;
    public boolean power;
    public String serverJs;
    public String serverPath;
    public String version;

    public String getAuthor() {
        return this.author;
    }

    public String getDescription() {
        return this.description;
    }

    public String getLogo() {
        return this.logo;
    }

    public String getMain() {
        return this.main;
    }

    public MiniJsServer getMiniJsServer() {
        return this.miniJsServer;
    }

    public String getName() {
        return this.name;
    }

    public String getPort() {
        return this.port;
    }

    public String getServerJs() {
        return this.serverJs;
    }

    public String getServerPath() {
        return this.serverPath;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isPower() {
        return this.power;
    }

    public void setAuthor(String str) {
        this.author = str;
    }

    public void setDescription(String str) {
        this.description = str;
    }

    public void setLogo(String str) {
        this.logo = str;
    }

    public void setMain(String str) {
        this.main = str;
    }

    public void setMiniJsServer(MiniJsServer aVar) {
        this.miniJsServer = aVar;
    }

    public void setName(String str) {
        this.name = str;
    }

    public void setPort(String str) {
        this.port = str;
    }

    public void setPower(boolean z) {
        this.power = z;
    }

    public void setServerJs(String str) {
        this.serverJs = str;
    }

    public void setServerPath(String str) {
        this.serverPath = str;
    }

    public void setVersion(String str) {
        this.version = str;
    }
}
