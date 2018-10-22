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
package com.espertech.esper.common.internal.context.aifactory.core;

import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.util.Map;

public class ModuleIncidentals {
    private final Map<String, NamedWindowMetaData> namedWindows;
    private final Map<String, ContextMetaData> contexts;
    private final Map<String, VariableMetaData> variables;
    private final Map<String, ExpressionDeclItem> expressions;
    private final Map<String, TableMetaData> tables;

    public ModuleIncidentals(Map<String, NamedWindowMetaData> namedWindows, Map<String, ContextMetaData> contexts, Map<String, VariableMetaData> variables, Map<String, ExpressionDeclItem> expressions, Map<String, TableMetaData> tables) {
        this.namedWindows = namedWindows;
        this.contexts = contexts;
        this.variables = variables;
        this.expressions = expressions;
        this.tables = tables;
    }

    public Map<String, NamedWindowMetaData> getNamedWindows() {
        return namedWindows;
    }

    public Map<String, ContextMetaData> getContexts() {
        return contexts;
    }

    public Map<String, VariableMetaData> getVariables() {
        return variables;
    }

    public Map<String, ExpressionDeclItem> getExpressions() {
        return expressions;
    }

    public Map<String, TableMetaData> getTables() {
        return tables;
    }
}
