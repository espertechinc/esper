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
package com.espertech.esper.common.internal.epl.script.compiletime;

import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.script.core.ScriptCollector;

import java.util.Map;

public class ScriptCollectorCompileTime implements ScriptCollector {
    private final Map<NameAndParamNum, ExpressionScriptProvided> moduleScripts;

    public ScriptCollectorCompileTime(Map<NameAndParamNum, ExpressionScriptProvided> moduleScripts) {
        this.moduleScripts = moduleScripts;
    }

    public void registerScript(String scriptName, int numParams, ExpressionScriptProvided meta) {
        moduleScripts.put(new NameAndParamNum(scriptName, numParams), meta);
    }
}
