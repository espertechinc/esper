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

/**
 * Base junction for conjunction (and) and disjunction (or).
 */
public abstract class Junction extends ExpressionBase {
    private static final long serialVersionUID = -8660058437278023625L;

    /**
     * Expression to add to the conjunction (AND) or disjunction (OR).
     *
     * @param expression to add
     * @return expression
     */
    public Junction add(Expression expression) {
        this.getChildren().add(expression);
        return this;
    }

    /**
     * Property to add to the conjunction (AND) or disjunction (OR).
     *
     * @param propertyName is the name of the property
     * @return expression
     */
    public Junction add(String propertyName) {
        this.getChildren().add(new PropertyValueExpression(propertyName));
        return this;
    }
}
