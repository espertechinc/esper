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
 * <p>
 *     Set a array dimension value greater zero for new array.
 *     If the child node is a single {@link ArrayExpression}, the expression is "new array[] {...}".
 *     If the child node is not a single {@link ArrayExpression}, the expression is "new array[...][...]".
 *     For 2-dimensionnal array initialization, put {@link ArrayExpression} inside {@link ArrayExpression}, i.e. the expression is "new array[] {{...}, {...}}".
 * </p>
 */
public class NewInstanceOperatorExpression extends ExpressionBase {

    private static final long serialVersionUID = 4725168176516142366L;
    private String className;
    private int numArrayDimensions;

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
     * @param numArrayDimensions for array initialization set a dimension value greater zero
     */
    public NewInstanceOperatorExpression(String className, int numArrayDimensions) {
        this.className = className;
        this.numArrayDimensions = numArrayDimensions;
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
     * Returns the array dimension, with child nodes providing either dimensions or array initialization values
     * @return array dimensions, zero for not-an-array
     */
    public int getNumArrayDimensions() {
        return numArrayDimensions;
    }

    /**
     * Sets the array dimension, with child nodes providing either dimensions or array initialization values
     * @param numArrayDimensions array dimensions, zero for not-an-array
     */
    public void setNumArrayDimensions(int numArrayDimensions) {
        this.numArrayDimensions = numArrayDimensions;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("new ");
        writer.write(className);
        if (numArrayDimensions == 0) {
            writer.write("(");
            ExpressionBase.toPrecedenceFreeEPL(this.getChildren(), writer);
            writer.write(")");
        } else {
            if (this.getChildren().size() == 1 && this.getChildren().get(0) instanceof ArrayExpression) {
                for (int i = 0; i < numArrayDimensions; i++) {
                    writer.write("[]");
                }
                writer.write(" ");
                getChildren().get(0).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            } else {
                for (Expression expression : this.getChildren()) {
                    writer.write("[");
                    expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
                    writer.write("]");
                }
                if (numArrayDimensions > this.getChildren().size()) {
                    writer.write("[]");
                }
            }
        }
    }
}
