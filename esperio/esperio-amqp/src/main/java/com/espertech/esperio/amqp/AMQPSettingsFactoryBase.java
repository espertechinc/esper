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

import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.Map;

public abstract class AMQPSettingsFactoryBase {
    private ExprEvaluator host;
    private ExprEvaluator port;
    private ExprEvaluator username;
    private ExprEvaluator password;
    private ExprEvaluator vhost;
    private ExprEvaluator queueName;
    private ExprEvaluator exchange;
    private ExprEvaluator routingKey;
    private ExprEvaluator logMessages;
    private ExprEvaluator waitMSecNextMsg;
    private ExprEvaluator declareDurable;
    private ExprEvaluator declareExclusive;
    private ExprEvaluator declareAutoDelete;
    private Map<String, Object> declareAdditionalArgs;

    void evaluateAndSet(AMQPSettingsValuesBase values, DataFlowOpInitializeContext context) {
        values.setHost(DataFlowParameterResolution.resolveStringRequired("host", host, context));
        values.setPort(DataFlowParameterResolution.resolveWithDefault("port", port, -1, int.class, context));
        values.setUsername(DataFlowParameterResolution.resolveStringOptional("username", username, context));
        values.setPassword(DataFlowParameterResolution.resolveStringOptional("password", password, context));
        values.setVhost(DataFlowParameterResolution.resolveStringOptional("vhost", vhost, context));
        values.setQueueName(DataFlowParameterResolution.resolveStringRequired("queueName", queueName, context));
        values.setExchange(DataFlowParameterResolution.resolveStringOptional("exchange", exchange, context));
        values.setRoutingKey(DataFlowParameterResolution.resolveStringOptional("routingKey", routingKey, context));
        values.setLogMessages(DataFlowParameterResolution.resolveWithDefault("logMessages", logMessages, false, boolean.class, context));
        values.setWaitMSecNextMsg(DataFlowParameterResolution.resolveWithDefault("waitMSecNextMsg", waitMSecNextMsg, 1L, long.class, context));
        values.setDeclareDurable(DataFlowParameterResolution.resolveWithDefault("declareDurable", declareDurable, false, boolean.class, context));
        values.setDeclareExclusive(DataFlowParameterResolution.resolveWithDefault("declareExclusive", declareExclusive, false, boolean.class, context));
        values.setDeclareAutoDelete(DataFlowParameterResolution.resolveWithDefault("declareAutoDelete", declareAutoDelete, true, boolean.class, context));
        values.setDeclareAdditionalArgs(DataFlowParameterResolution.resolveMap("declareAutoDelete", declareAdditionalArgs, context));
    }

    public void setHost(ExprEvaluator host) {
        this.host = host;
    }

    public void setPort(ExprEvaluator port) {
        this.port = port;
    }

    public void setUsername(ExprEvaluator username) {
        this.username = username;
    }

    public void setPassword(ExprEvaluator password) {
        this.password = password;
    }

    public void setVhost(ExprEvaluator vhost) {
        this.vhost = vhost;
    }

    public void setQueueName(ExprEvaluator queueName) {
        this.queueName = queueName;
    }

    public void setExchange(ExprEvaluator exchange) {
        this.exchange = exchange;
    }

    public void setRoutingKey(ExprEvaluator routingKey) {
        this.routingKey = routingKey;
    }

    public void setLogMessages(ExprEvaluator logMessages) {
        this.logMessages = logMessages;
    }

    public void setWaitMSecNextMsg(ExprEvaluator waitMSecNextMsg) {
        this.waitMSecNextMsg = waitMSecNextMsg;
    }

    public void setDeclareDurable(ExprEvaluator declareDurable) {
        this.declareDurable = declareDurable;
    }

    public void setDeclareExclusive(ExprEvaluator declareExclusive) {
        this.declareExclusive = declareExclusive;
    }

    public void setDeclareAutoDelete(ExprEvaluator declareAutoDelete) {
        this.declareAutoDelete = declareAutoDelete;
    }

    public void setDeclareAdditionalArgs(Map<String, Object> declareAdditionalArgs) {
        this.declareAdditionalArgs = declareAdditionalArgs;
    }
}
