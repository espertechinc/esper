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
import com.espertech.esper.common.internal.epl.util.CompileTimeRegistry;

import java.util.HashMap;
import java.util.Map;

public class ScriptCompileTimeRegistry implements CompileTimeRegistry {
    private final Map<NameAndParamNum, ExpressionScriptProvided> expressions = new HashMap<>();

    public void newScript(ExpressionScriptProvided detail) {
        if (!detail.getVisibility().isModuleProvidedAccessModifier()) {
            throw new IllegalStateException("Invalid visibility for contexts");
        }
        NameAndParamNum key = new NameAndParamNum(detail.getName(), detail.getParameterNames().length);
        ExpressionScriptProvided existing = expressions.get(key);
        if (existing != null) {
            throw new IllegalStateException("Duplicate script has been encountered for name '" + key + "'");
        }
        expressions.put(key, detail);
    }

    public Map<NameAndParamNum, ExpressionScriptProvided> getScripts() {
        return expressions;
    }
}
