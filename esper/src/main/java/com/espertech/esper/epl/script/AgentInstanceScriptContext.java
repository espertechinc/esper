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
package com.espertech.esper.epl.script;

import com.espertech.esper.client.hook.EPLScriptContext;
import com.espertech.esper.client.hook.EventBeanService;
import com.espertech.esper.event.EventAdapterService;

import java.util.HashMap;
import java.util.Map;

/**
 * Context-partition local script context.
 */
public class AgentInstanceScriptContext implements EPLScriptContext {

    private Map<String, Object> scriptProperties;

    private final EventBeanService eventBeanService;

    private AgentInstanceScriptContext(EventBeanService eventBeanService) {
        this.eventBeanService = eventBeanService;
    }
    
    public EventBeanService getEventBeanService() {
        return eventBeanService;
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

    public static AgentInstanceScriptContext from(EventAdapterService eventAdapterService) {
        return new AgentInstanceScriptContext(eventAdapterService);
    }
}
