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
package com.espertech.esper.common.internal.epl.variable.compiletime;

import com.espertech.esper.common.internal.epl.util.CompileTimeRegistry;

import java.util.HashMap;
import java.util.Map;

public class VariableCompileTimeRegistry implements CompileTimeRegistry {
    private final Map<String, VariableMetaData> variables = new HashMap<>();

    public void newVariable(VariableMetaData metaData) {
        if (!metaData.getVariableVisibility().isModuleProvidedAccessModifier()) {
            throw new IllegalStateException("Invalid visibility for variables");
        }
        VariableMetaData existing = variables.get(metaData.getVariableName());
        if (existing != null) {
            throw new IllegalStateException("Duplicate variable definition for name '" + metaData.getVariableName() + "'");
        }
        variables.put(metaData.getVariableName(), metaData);
    }

    public Map<String, VariableMetaData> getVariables() {
        return variables;
    }

    public VariableMetaData getVariable(String variableName) {
        return variables.get(variableName);
    }
}
