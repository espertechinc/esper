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
package com.espertech.esper.common.client.soda;

import java.io.StringWriter;

/**
 * The "new instance" operator instantiates a host language object.
 */
public class NewInstanceOperatorExpression extends ExpressionBase {

    private static final long serialVersionUID = 4725168176516142366L;
    private String className;
    private boolean array;

    /**
     * Ctor.
     */
    public NewInstanceOperatorExpression() {
    }

    /**
     * Ctor.
     * <p>
     *
     * @param className the class name
     */
    public NewInstanceOperatorExpression(String className) {
        this.className = className;
    }

    /**
     * Ctor.
     * <p>
     *
     * @param className the class name
     * @param array for array initialization, the child nodes providing the dimensions
     */
    public NewInstanceOperatorExpression(String className, boolean array) {
        this.className = className;
        this.array = array;
    }

    /**
     * Returns the class name.
     *
     * @return class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the class name.
     *
     * @param className class name to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns the array flag, with child nodes providing dimensions
     * @return flag
     */
    public boolean isArray() {
        return array;
    }

    /**
     * Set the array flag, with child nodes providing dimensions
     * @param array flag
     */
    public void setArray(boolean array) {
        this.array = array;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new ");
        writer.write(className);
        if (!array) {
            writer.write("(");
            ExpressionBase.toPrecedenceFreeEPL(this.getChildren(), writer);
            writer.write(")");
        } else {
            for (Expression expression : this.getChildren()) {
                writer.write("[");
                expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                writer.write("]");
            }
        }
    }
}
