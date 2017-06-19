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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

public class ExceptionHandlingService {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingService.class);

    private final String engineURI;
    private final List<ExceptionHandler> exceptionHandlers;
    private final List<ConditionHandler> conditionHandlers;

    public ExceptionHandlingService(String engineURI, List<ExceptionHandler> exceptionHandlers, List<ConditionHandler> conditionHandlers) {
        this.engineURI = engineURI;
        this.exceptionHandlers = exceptionHandlers;
        this.conditionHandlers = conditionHandlers;
    }

    public void handleCondition(BaseCondition condition, EPStatementHandle handle) {
        if (conditionHandlers.isEmpty()) {
            log.info("Condition encountered processing statement '" + handle.getStatementName() + "' statement text '" + handle.getEPL() + "' : " + condition.toString());
            return;
        }

        ConditionHandlerContext context = new ConditionHandlerContext(engineURI, handle.getStatementName(), handle.getEPL(), condition);
        for (ConditionHandler handler : conditionHandlers) {
            handler.handle(context);
        }
    }

    public void handleException(RuntimeException ex, EPStatementAgentInstanceHandle handle, ExceptionHandlerExceptionType type, EventBean optionalCurrentEvent) {
        handleException(ex, handle.getStatementHandle().getStatementName(), handle.getStatementHandle().getEPL(), type, optionalCurrentEvent);
    }

    public String getEngineURI() {
        return engineURI;
    }

    public void handleException(RuntimeException ex, String statementName, String epl, ExceptionHandlerExceptionType type, EventBean optionalCurrentEvent) {
        if (exceptionHandlers.isEmpty()) {
            StringWriter writer = new StringWriter();
            if (type == ExceptionHandlerExceptionType.PROCESS) {
                writer.append("Exception encountered processing ");
            } else {
                writer.append("Exception encountered performing instance stop for ");
            }
            writer.append("statement '");
            writer.append(statementName);
            writer.append("' expression '");
            writer.append(epl);
            writer.append("' : ");
            writer.append(ex.getMessage());
            String message = writer.toString();

            if (type == ExceptionHandlerExceptionType.PROCESS) {
                log.error(message, ex);
            } else {
                log.warn(message, ex);
            }
            return;
        }

        ExceptionHandlerContext context = new ExceptionHandlerContext(engineURI, ex, statementName, epl, type, optionalCurrentEvent);
        for (ExceptionHandler handler : exceptionHandlers) {
            handler.handle(context);
        }
    }

    public void handleInboundPoolException(String engineURI, Throwable exception, Object event) {
        ExceptionHandlerContextUnassociated context = new ExceptionHandlerContextUnassociated(engineURI, exception, event);
        for (ExceptionHandler handler : exceptionHandlers) {
            if (handler instanceof ExceptionHandlerInboundPool) {
                ((ExceptionHandlerInboundPool) handler).handleInboundPoolUnassociated(context);
            }
        }
    }
}
