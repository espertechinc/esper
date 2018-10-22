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
package com.espertech.esper.common.internal.epl.dataflow.realize;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowExceptionContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowExceptionHandler;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EPDataFlowEmitterExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EPDataFlowEmitterExceptionHandler.class);

    private final AgentInstanceContext agentInstanceContext;
    private final String dataFlowName;
    private final String instanceId;
    private final String operatorName;
    private final int operatorNumber;
    private final String operatorPrettyPrint;
    private final EPDataFlowExceptionHandler optionalExceptionHandler;

    public EPDataFlowEmitterExceptionHandler(AgentInstanceContext agentInstanceContext, String dataFlowName, String instanceId, String operatorName, int operatorNumber, String operatorPrettyPrint, EPDataFlowExceptionHandler optionalExceptionHandler) {
        this.agentInstanceContext = agentInstanceContext;
        this.dataFlowName = dataFlowName;
        this.instanceId = instanceId;
        this.operatorName = operatorName;
        this.operatorNumber = operatorNumber;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.optionalExceptionHandler = optionalExceptionHandler;
    }

    public void handleException(Object targetObject, Method fastMethod, InvocationTargetException ex, Object[] parameters) {
        log.error("Exception encountered: " + ex.getTargetException().getMessage(), ex.getTargetException());

        if (optionalExceptionHandler != null) {
            optionalExceptionHandler.handle(new EPDataFlowExceptionContext(dataFlowName, operatorName, operatorNumber, operatorPrettyPrint, ex.getTargetException()));
        }
    }

    public void handleException(Object targetObject, Method fastMethod, IllegalAccessException ex, Object[] parameters) {
        log.error("Exception encountered: " + ex.getMessage(), ex);

        if (optionalExceptionHandler != null) {
            optionalExceptionHandler.handle(new EPDataFlowExceptionContext(dataFlowName, operatorName, operatorNumber, operatorPrettyPrint, ex));
        }
    }

    public String getRuntimeURI() {
        return agentInstanceContext.getRuntimeURI();
    }

    public String getStatementName() {
        return agentInstanceContext.getStatementName();
    }

    public String getDataFlowName() {
        return dataFlowName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public int getOperatorNumber() {
        return operatorNumber;
    }

    public String getOperatorPrettyPrint() {
        return operatorPrettyPrint;
    }

    public EPDataFlowExceptionHandler getOptionalExceptionHandler() {
        return optionalExceptionHandler;
    }

    public String getDeploymentId() {
        return agentInstanceContext.getDeploymentId();
    }

    public void handleAudit(Object targetObject, Object[] parameters) {
        agentInstanceContext.getAuditProvider().dataflowOp(dataFlowName, instanceId, operatorName, operatorNumber, parameters, agentInstanceContext);
    }
}
