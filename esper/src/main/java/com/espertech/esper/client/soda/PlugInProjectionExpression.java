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
package com.espertech.esper.client.soda;

import java.io.StringWriter;

/**
 * Represents a plug-in aggregation function.
 */
public class PlugInProjectionExpression extends ExpressionBase {
    private String functionName;
    private boolean isDistinct;
    private static final long serialVersionUID = -4474825377733541468L;

    /**
     * Ctor.
     */
    public PlugInProjectionExpression() {
    }

    /**
     * Ctor.
     *
     * @param functionName the name of the function
     * @param isDistinct   true for distinct
     */
    public PlugInProjectionExpression(String functionName, boolean isDistinct) {
        this.functionName = functionName;
        this.isDistinct = isDistinct;
    }

    /**
     * Ctor.
     *
     * @param functionName    the name of the function
     * @param isDistinct      true for distinct
     * @param moreExpressions provides aggregated values
     */
    public PlugInProjectionExpression(String functionName, boolean isDistinct, Expression... moreExpressions) {
        this.functionName = functionName;
        this.isDistinct = isDistinct;
        for (int i = 0; i < moreExpressions.length; i++) {
            this.getChildren().add(moreExpressions[i]);
        }
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExpressionBase.renderAggregation(writer, functionName, isDistinct, this.getChildren());
    }

    /**
     * Returns the function name.
     *
     * @return name of function
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Sets the function name.
     *
     * @param functionName name of function
     */
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    /**
     * Returns true for distinct.
     *
     * @return boolean indicating distinct or not
     */
    public boolean isDistinct() {
        return isDistinct;
    }

    /**
     * Set to true for distinct.
     *
     * @param distinct indicating distinct or not
     */
    public void setDistinct(boolean distinct) {
        isDistinct = distinct;
    }
}
