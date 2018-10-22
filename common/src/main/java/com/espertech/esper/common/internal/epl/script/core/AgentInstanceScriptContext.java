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
package com.espertech.esper.common.internal.epl.script.core;

import com.espertech.esper.common.client.hook.expr.EPLScriptContext;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.context.util.StatementContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Context-partition local script context.
 */
public class AgentInstanceScriptContext implements EPLScriptContext {

    private StatementContext statementContext;
    private Map<String, Object> scriptProperties;

    public AgentInstanceScriptContext(StatementContext statementContext) {
        this.statementContext = statementContext;
    }

    public EventBeanService getEventBeanService() {
        return statementContext.getEventBeanService();
    }

    public void setScriptAttribute(String attribute, Object value) {
        allocateScriptProperties();
        scriptProperties.put(attribute, value);
    }

    public Object getScriptAttribute(String attribute) {
        allocateScriptProperties();
        return scriptProperties.get(attribute);
    }

    private void allocateScriptProperties() {
        if (scriptProperties == null) {
            scriptProperties = new HashMap<>();
        }
    }

    public static AgentInstanceScriptContext from(StatementContext statementContext) {
        return new AgentInstanceScriptContext(statementContext);
    }
}
