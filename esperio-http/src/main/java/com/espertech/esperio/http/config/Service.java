package com.espertech.esperio.http.config;

public class Service {
    private int port;
    private boolean nio;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isNio() {
        return nio;
    }

    public void setNio(boolean nio) {
        this.nio = nio;
    }
}
