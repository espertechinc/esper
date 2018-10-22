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
package com.espertech.esper.common.internal.epl.expression.declared.core;

public class ExprDeclaredCacheKeyGlobal {
    private final String deploymentIdExpr;
    private final String expressionName;

    public ExprDeclaredCacheKeyGlobal(String deploymentIdExpr, String expressionName) {
        this.deploymentIdExpr = deploymentIdExpr;
        this.expressionName = expressionName;
    }

    public String getDeploymentIdExpr() {
        return deploymentIdExpr;
    }

    public String getExpressionName() {
        return expressionName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprDeclaredCacheKeyGlobal that = (ExprDeclaredCacheKeyGlobal) o;

        if (!deploymentIdExpr.equals(that.deploymentIdExpr)) return false;
        return expressionName.equals(that.expressionName);
    }

    public int hashCode() {
        int result = deploymentIdExpr.hashCode();
        result = 31 * result + expressionName.hashCode();
        return result;
    }
}
