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

import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.util.Map;

public class VariableCollectorImpl implements VariableCollector {
    private final Map<String, VariableMetaData> moduleVariables;

    public VariableCollectorImpl(Map<String, VariableMetaData> moduleVariables) {
        this.moduleVariables = moduleVariables;
    }

    public void registerVariable(String variableName, VariableMetaData variableMetaData) {
        moduleVariables.put(variableName, variableMetaData);
    }
}
