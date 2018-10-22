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
package com.espertech.esper.common.internal.epl.historical.method.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.historical.method.poll.MethodPollingExecStrategyEnum;
import com.espertech.esper.common.internal.epl.script.core.ExprNodeScript;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.util.LinkedHashMap;
import java.util.Map;

public class MethodPollingViewableMeta {
    private final Class methodProviderClass;
    private final boolean isStaticMethod;
    private final Map<String, Object> optionalMapType;
    private final LinkedHashMap<String, Object> optionalOaType;
    private final MethodPollingExecStrategyEnum strategy;
    private final boolean isCollection;
    private final boolean isIterator;
    private final VariableMetaData variable;
    private final EventType eventTypeEventBeanArray;
    private final ExprNodeScript scriptExpression;

    public MethodPollingViewableMeta(Class methodProviderClass, boolean isStaticMethod, Map<String, Object> optionalMapType, LinkedHashMap<String, Object> optionalOaType, MethodPollingExecStrategyEnum strategy, boolean isCollection, boolean isIterator, VariableMetaData variable, EventType eventTypeEventBeanArray, ExprNodeScript scriptExpression) {
        this.methodProviderClass = methodProviderClass;
        this.isStaticMethod = isStaticMethod;
        this.optionalMapType = optionalMapType;
        this.optionalOaType = optionalOaType;
        this.strategy = strategy;
        this.isCollection = isCollection;
        this.isIterator = isIterator;
        this.variable = variable;
        this.eventTypeEventBeanArray = eventTypeEventBeanArray;
        this.scriptExpression = scriptExpression;
    }

    public Map<String, Object> getOptionalMapType() {
        return optionalMapType;
    }

    public LinkedHashMap<String, Object> getOptionalOaType() {
        return optionalOaType;
    }

    public MethodPollingExecStrategyEnum getStrategy() {
        return strategy;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public boolean isIterator() {
        return isIterator;
    }

    public VariableMetaData getVariable() {
        return variable;
    }

    public EventType getEventTypeEventBeanArray() {
        return eventTypeEventBeanArray;
    }

    public ExprNodeScript getScriptExpression() {
        return scriptExpression;
    }

    public Class getMethodProviderClass() {
        return methodProviderClass;
    }

    public boolean isStaticMethod() {
        return isStaticMethod;
    }
}
