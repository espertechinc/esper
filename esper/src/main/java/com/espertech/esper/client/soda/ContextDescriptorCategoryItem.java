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
 * Context descriptor for categories.
 */
public class ContextDescriptorCategoryItem implements ContextDescriptor {

    private static final long serialVersionUID = -6914400028529675775L;
    private Expression expression;
    private String label;

    /**
     * Ctor.
     */
    public ContextDescriptorCategoryItem() {
    }

    /**
     * Ctor.
     *
     * @param expression category expression
     * @param label      category label
     */
    public ContextDescriptorCategoryItem(Expression expression, String label) {
        this.expression = expression;
        this.label = label;
    }

    /**
     * Returns the category expression.
     *
     * @return expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the category expression.
     *
     * @param expression to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * Returns the category label
     *
     * @return category label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the category label
     *
     * @param label category label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.append("group ");
        expression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        writer.append(" as ");
        writer.append(label);
    }
}
