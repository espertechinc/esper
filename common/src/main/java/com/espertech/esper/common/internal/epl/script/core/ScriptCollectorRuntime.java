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

import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;

import java.util.Map;

public class ScriptCollectorRuntime implements ScriptCollector {
    private final Map<NameAndParamNum, ExpressionScriptProvided> scripts;

    public ScriptCollectorRuntime(Map<NameAndParamNum, ExpressionScriptProvided> scripts) {
        this.scripts = scripts;
    }

    public void registerScript(String scriptName, int numParameters, ExpressionScriptProvided meta) {
        NameAndParamNum key = new NameAndParamNum(scriptName, numParameters);
        if (scripts.containsKey(key)) {
            throw new IllegalStateException("Script already found '" + key + "'");
        }
        scripts.put(key, meta);
    }
}
