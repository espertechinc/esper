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
package com.espertech.esper.common.internal.epl.expression.declared.runtime;

import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.epl.expression.declared.core.ExprDeclaredCollector;

import java.util.Map;

public class ExprDeclaredCollectorRuntime implements ExprDeclaredCollector {
    private final Map<String, ExpressionDeclItem> expressions;

    public ExprDeclaredCollectorRuntime(Map<String, ExpressionDeclItem> expressions) {
        this.expressions = expressions;
    }

    public void registerExprDeclared(String expressionName, ExpressionDeclItem meta) {
        if (expressions.containsKey(expressionName)) {
            throw new IllegalStateException("Expression name already found '" + expressionName + "'");
        }
        expressions.put(expressionName, meta);
    }
}
