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

import com.espertech.esper.common.client.dataflow.util.DataFlowParameterValidation;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Map;

public abstract class AMQPSettingsForgeBase {
    private ExprNode host;
    private ExprNode port;
    private ExprNode username;
    private ExprNode password;
    private ExprNode vhost;
    private ExprNode queueName;
    private ExprNode exchange;
    private ExprNode routingKey;
    private ExprNode logMessages;
    private ExprNode waitMSecNextMsg;
    private ExprNode declareDurable;
    private ExprNode declareExclusive;
    private ExprNode declareAutoDelete;
    private Map<String, Object> declareAdditionalArgs;

    public AMQPSettingsForgeBase() {
    }

    protected void validate(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        host = DataFlowParameterValidation.validate("host", host, String.class, context);
        port = DataFlowParameterValidation.validate("port", port, int.class, context);
        username = DataFlowParameterValidation.validate("username", username, String.class, context);
        password = DataFlowParameterValidation.validate("password", password, String.class, context);
        vhost = DataFlowParameterValidation.validate("vhost", vhost, String.class, context);
        queueName = DataFlowParameterValidation.validate("queueName", queueName, String.class, context);
        exchange = DataFlowParameterValidation.validate("exchange", exchange, String.class, context);
        routingKey = DataFlowParameterValidation.validate("routingKey", routingKey, String.class, context);
        logMessages = DataFlowParameterValidation.validate("logMessages", logMessages, boolean.class, context);
        waitMSecNextMsg = DataFlowParameterValidation.validate("waitMSecNextMsg", waitMSecNextMsg, long.class, context);
        declareDurable = DataFlowParameterValidation.validate("declareDurable", declareDurable, boolean.class, context);
        declareExclusive = DataFlowParameterValidation.validate("declareExclusive", declareExclusive, boolean.class, context);
        declareAutoDelete = DataFlowParameterValidation.validate("declareAutoDelete", declareAutoDelete, boolean.class, context);
    }

    void make(SAIFFInitializeBuilder builder) {
        builder.exprnode("host", host)
            .exprnode("port", port)
            .exprnode("username", username)
            .exprnode("password", password)
            .exprnode("vhost", vhost)
            .exprnode("queueName", queueName)
            .exprnode("exchange", exchange)
            .exprnode("routingKey", routingKey)
            .exprnode("logMessages", logMessages)
            .exprnode("waitMSecNextMsg", waitMSecNextMsg)
            .exprnode("declareDurable", declareDurable)
            .exprnode("declareExclusive", declareExclusive)
            .exprnode("declareAutoDelete", declareAutoDelete)
            .map("declareAdditionalArgs", declareAdditionalArgs);
    }

    public ExprNode getHost() {
        return host;
    }

    public void setHost(ExprNode host) {
        this.host = host;
    }

    public ExprNode getPort() {
        return port;
    }

    public void setPort(ExprNode port) {
        this.port = port;
    }

    public ExprNode getUsername() {
        return username;
    }

    public void setUsername(ExprNode username) {
        this.username = username;
    }

    public ExprNode getPassword() {
        return password;
    }

    public void setPassword(ExprNode password) {
        this.password = password;
    }

    public ExprNode getVhost() {
        return vhost;
    }

    public void setVhost(ExprNode vhost) {
        this.vhost = vhost;
    }

    public ExprNode getQueueName() {
        return queueName;
    }

    public void setQueueName(ExprNode queueName) {
        this.queueName = queueName;
    }

    public ExprNode getExchange() {
        return exchange;
    }

    public void setExchange(ExprNode exchange) {
        this.exchange = exchange;
    }

    public ExprNode getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(ExprNode routingKey) {
        this.routingKey = routingKey;
    }

    public ExprNode getLogMessages() {
        return logMessages;
    }

    public void setLogMessages(ExprNode logMessages) {
        this.logMessages = logMessages;
    }

    public ExprNode getWaitMSecNextMsg() {
        return waitMSecNextMsg;
    }

    public void setWaitMSecNextMsg(ExprNode waitMSecNextMsg) {
        this.waitMSecNextMsg = waitMSecNextMsg;
    }

    public ExprNode getDeclareDurable() {
        return declareDurable;
    }

    public void setDeclareDurable(ExprNode declareDurable) {
        this.declareDurable = declareDurable;
    }

    public ExprNode getDeclareExclusive() {
        return declareExclusive;
    }

    public void setDeclareExclusive(ExprNode declareExclusive) {
        this.declareExclusive = declareExclusive;
    }

    public ExprNode getDeclareAutoDelete() {
        return declareAutoDelete;
    }

    public void setDeclareAutoDelete(ExprNode declareAutoDelete) {
        this.declareAutoDelete = declareAutoDelete;
    }

    public Map<String, Object> getDeclareAdditionalArgs() {
        return declareAdditionalArgs;
    }

    public void setDeclareAdditionalArgs(Map<String, Object> declareAdditionalArgs) {
        this.declareAdditionalArgs = declareAdditionalArgs;
    }
}