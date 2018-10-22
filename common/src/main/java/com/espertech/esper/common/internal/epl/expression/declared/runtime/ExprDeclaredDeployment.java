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

import java.util.HashSet;
import java.util.Set;

public class ExprDeclaredDeployment {
    private final Set<String> expressions = new HashSet<>(4);

    public void addExpression(String expressionName) {
        expressions.add(expressionName);
    }

    public void remove(String expressionsName) {
        expressions.remove(expressionsName);
    }

    public Set<String> getExpressions() {
        return expressions;
    }
}
