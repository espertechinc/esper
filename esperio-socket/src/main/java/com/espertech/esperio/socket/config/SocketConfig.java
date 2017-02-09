package com.espertech.esperio.socket.config;

public class SocketConfig {
    private int port;
    private DataType dataType;
    private String hostname;
    private Integer backlog;
    private String propertyOrder;
    private String stream;
    private boolean unescape;

    public SocketConfig() {
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getBacklog() {
        return backlog;
    }

    public void setBacklog(Integer backlog) {
        this.backlog = backlog;
    }

    public String getPropertyOrder() {
        return propertyOrder;
    }

    public void setPropertyOrder(String propertyOrder) {
        this.propertyOrder = propertyOrder;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public boolean isUnescape() {
        return unescape;
    }

    public void setUnescape(boolean unescape) {
        this.unescape = unescape;
    }
}
