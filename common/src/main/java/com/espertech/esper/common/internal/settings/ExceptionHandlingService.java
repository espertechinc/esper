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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.condition.BaseCondition;
import com.espertech.esper.common.client.hook.condition.ConditionHandler;
import com.espertech.esper.common.client.hook.condition.ConditionHandlerContext;
import com.espertech.esper.common.client.hook.exception.*;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

public class ExceptionHandlingService {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingService.class);

    private final String runtimeURI;
    private final List<ExceptionHandler> exceptionHandlers;
    private final List<ConditionHandler> conditionHandlers;

    public ExceptionHandlingService(String runtimeURI, List<ExceptionHandler> exceptionHandlers, List<ConditionHandler> conditionHandlers) {
        this.runtimeURI = runtimeURI;
        this.exceptionHandlers = exceptionHandlers;
        this.conditionHandlers = conditionHandlers;
    }

    public void handleCondition(BaseCondition condition, StatementContext statement) {
        String optionalEPL = (String) statement.getStatementInformationals().getProperties().get(StatementProperty.EPL);
        handleCondition(condition, statement.getDeploymentId(), statement.getStatementName(), optionalEPL);
    }

    public void handleCondition(BaseCondition condition, String deplomentId, String statementName, String optionalEPL) {
        if (conditionHandlers.isEmpty()) {
            String message = "Condition encountered processing deployment id '" + deplomentId + "' statement '" + statementName + "'";
            if (optionalEPL != null) {
                message += " statement text '" + optionalEPL + "'";
            }
            message += " :" + condition.toString();
            log.info(message);
            return;
        }

        ConditionHandlerContext context = new ConditionHandlerContext(runtimeURI, statementName, deplomentId, condition);
        for (ConditionHandler handler : conditionHandlers) {
            handler.handle(context);
        }
    }

    public void handleException(RuntimeException ex, EPStatementAgentInstanceHandle handle, ExceptionHandlerExceptionType type, EventBean optionalCurrentEvent) {
        handleException(ex, handle.getStatementHandle().getDeploymentId(), handle.getStatementHandle().getStatementName(), handle.getStatementHandle().getOptionalStatementEPL(), type, optionalCurrentEvent);
    }

    public String getRuntimeURI() {
        return runtimeURI;
    }

    public void handleException(Throwable t, String deploymentId, String statementName, String optionalEPL, ExceptionHandlerExceptionType type, EventBean optionalCurrentEvent) {
        if (exceptionHandlers.isEmpty()) {
            StringWriter writer = new StringWriter();
            if (type == ExceptionHandlerExceptionType.PROCESS) {
                writer.append("Exception encountered processing ");
            } else {
                writer.append("Exception encountered performing instance stop for ");
            }
            writer.append("deployment-id '");
            writer.append(deploymentId);
            writer.append("' ");
            writer.append("statement '");
            writer.append(statementName);
            writer.append("'");
            if (optionalEPL != null) {
                writer.append(" expression '");
                writer.append(optionalEPL);
                writer.append("'");
            }
            writer.append(" : ");
            writer.append(t.getMessage());
            String message = writer.toString();

            if (type == ExceptionHandlerExceptionType.PROCESS) {
                log.error(message, t);
            } else {
                log.warn(message, t);
            }
            return;
        }

        ExceptionHandlerContext context = new ExceptionHandlerContext(runtimeURI, t, deploymentId, statementName, optionalEPL, type, optionalCurrentEvent);
        for (ExceptionHandler handler : exceptionHandlers) {
            handler.handle(context);
        }
    }

    public void handleInboundPoolException(String runtimeURI, Throwable exception, Object event) {
        ExceptionHandlerContextUnassociated context = new ExceptionHandlerContextUnassociated(runtimeURI, exception, event);
        for (ExceptionHandler handler : exceptionHandlers) {
            if (handler instanceof ExceptionHandlerInboundPool) {
                ((ExceptionHandlerInboundPool) handler).handleInboundPoolUnassociated(context);
            }
        }
    }
}
