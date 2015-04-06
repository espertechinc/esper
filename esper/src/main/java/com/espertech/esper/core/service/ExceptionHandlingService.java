/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.service;

import com.espertech.esper.client.hook.*;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class ExceptionHandlingService {

    private static final Log log = LogFactory.getLog(ExceptionHandlingService.class);

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

    public void handleException(RuntimeException ex, EPStatementAgentInstanceHandle handle) {
        if (exceptionHandlers.isEmpty()) {
            log.error("Exception encountered processing statement '" + handle.getStatementHandle().getStatementName() + "' statement text '" + handle.getStatementHandle().getEPL() + "' : " + ex.getMessage(), ex);
            return;
        }

        ExceptionHandlerContext context = new ExceptionHandlerContext(engineURI, ex, handle.getStatementHandle().getStatementName(), handle.getStatementHandle().getEPL());
        for (ExceptionHandler handler : exceptionHandlers) {
            handler.handle(context);
        }
    }

    public String getEngineURI() {
        return engineURI;
    }
}
