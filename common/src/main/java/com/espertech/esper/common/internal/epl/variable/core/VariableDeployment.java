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
package com.espertech.esper.common.internal.epl.variable.core;

import java.util.HashMap;
import java.util.Map;

public class VariableDeployment {
    private final Map<String, Variable> variables = new HashMap<>(4);

    public void addVariable(String variableName, Variable variable) {
        variables.put(variableName, variable);
    }

    public Variable getVariable(String variableName) {
        return variables.get(variableName);
    }

    public void remove(String variableName) {
        variables.remove(variableName);
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }
}
