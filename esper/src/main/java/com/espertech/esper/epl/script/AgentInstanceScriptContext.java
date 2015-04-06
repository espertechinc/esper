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

package com.espertech.esper.epl.script;

import com.espertech.esper.client.hook.EPLScriptContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Context-partition local script context.
 */
public class AgentInstanceScriptContext implements EPLScriptContext {

    private final Map<String, Object> scriptProperties = new HashMap<String, Object>();

    public AgentInstanceScriptContext() {
    }

    public void setScriptAttribute(String attribute, Object value) {
        scriptProperties.put(attribute, value);
    }

    public Object getScriptAttribute(String attribute) {
        return scriptProperties.get(attribute);
    }
}
