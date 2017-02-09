/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esperio.amqp;

import java.util.Map;

public abstract class AMQPSettingsBase {
    private String host;
    private int port = -1;
    private String username;
    private String password;
    private String vhost;
    private String queueName;
    private String exchange;
    private String routingKey;
    private boolean logMessages;

    private long waitMSecNextMsg = 1L;
    private boolean declareDurable = false;
    private boolean declareExclusive = false;
    private boolean declareAutoDelete = true;
    private Map<String, Object> declareAdditionalArgs;

    public AMQPSettingsBase() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public boolean isLogMessages() {
        return logMessages;
    }

    public void setLogMessages(boolean logMessages) {
        this.logMessages = logMessages;
    }

    public long getWaitMSecNextMsg() {
        return waitMSecNextMsg;
    }

    public void setWaitMSecNextMsg(long waitMSecNextMsg) {
        this.waitMSecNextMsg = waitMSecNextMsg;
    }

    public boolean isDeclareDurable() {
        return declareDurable;
    }

    public void setDeclareDurable(boolean declareDurable) {
        this.declareDurable = declareDurable;
    }

    public boolean isDeclareExclusive() {
        return declareExclusive;
    }

    public void setDeclareExclusive(boolean declareExclusive) {
        this.declareExclusive = declareExclusive;
    }

    public boolean isDeclareAutoDelete() {
        return declareAutoDelete;
    }

    public void setDeclareAutoDelete(boolean declareAutoDelete) {
        this.declareAutoDelete = declareAutoDelete;
    }

    public Map<String, Object> getDeclareAdditionalArgs() {
        return declareAdditionalArgs;
    }

    public void setDeclareAdditionalArgs(Map<String, Object> declareAdditionalArgs) {
        this.declareAdditionalArgs = declareAdditionalArgs;
    }

    public String toString() {
        return "AMQPSettingsBase{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", vhost='" + vhost + '\'' +
                ", queueName='" + queueName + '\'' +
                ", exchange='" + exchange + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", logMessages=" + logMessages +
                ", waitMSecNextMsg=" + waitMSecNextMsg +
                ", declareDurable=" + declareDurable +
                ", declareExclusive=" + declareExclusive +
                ", declareAutoDelete=" + declareAutoDelete +
                ", declareAdditionalArgs=" + declareAdditionalArgs +
                '}';
    }
}
