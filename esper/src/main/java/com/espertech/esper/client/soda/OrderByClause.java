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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An order-by clause consists of expressions and flags indicating if ascending or descending.
 */
public class OrderByClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private List<OrderByElement> orderByExpressions;

    /**
     * Create an empty order-by clause.
     *
     * @return clause
     */
    public static OrderByClause create() {
        return new OrderByClause();
    }

    /**
     * Create an order-by clause.
     *
     * @param properties is the property names to order by
     * @return clause
     */
    public static OrderByClause create(String... properties) {
        return new OrderByClause(properties);
    }

    /**
     * Create an order-by clause.
     *
     * @param expressions is the expressios returning values to order by
     * @return clause
     */
    public static OrderByClause create(Expression... expressions) {
        return new OrderByClause(expressions);
    }

    /**
     * Adds a property and flag.
     *
     * @param property     is the name of the property to add
     * @param isDescending true for descending, false for ascending sort
     * @return clause
     */
    public OrderByClause add(String property, boolean isDescending) {
        orderByExpressions.add(new OrderByElement(Expressions.getPropExpr(property), isDescending));
        return this;
    }

    /**
     * Adds an expression and flag.
     *
     * @param expression   returns values to order by
     * @param isDescending true for descending, false for ascending sort
     * @return clause
     */
    public OrderByClause add(Expression expression, boolean isDescending) {
        orderByExpressions.add(new OrderByElement(expression, isDescending));
        return this;
    }

    /**
     * Ctor.
     */
    public OrderByClause() {
        orderByExpressions = new ArrayList<OrderByElement>();
    }

    /**
     * Ctor.
     *
     * @param properties property names
     */
    public OrderByClause(String... properties) {
        this();
        for (int i = 0; i < properties.length; i++) {
            orderByExpressions.add(new OrderByElement(Expressions.getPropExpr(properties[i]), false));
        }
    }

    /**
     * Ctor.
     *
     * @param expressions is the expressions
     */
    public OrderByClause(Expression... expressions) {
        this();
        for (int i = 0; i < expressions.length; i++) {
            orderByExpressions.add(new OrderByElement(expressions[i], false));
        }
    }

    /**
     * Returns a list of expressions and flags to order by.
     *
     * @return order-by elements
     */
    public List<OrderByElement> getOrderByExpressions() {
        return orderByExpressions;
    }

    /**
     * Sets a list of expressions and flags to order by.
     *
     * @param orderByExpressions is the expressions to order by
     */
    public void setOrderByExpressions(List<OrderByElement> orderByExpressions) {
        this.orderByExpressions = orderByExpressions;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        String delimiter = "";
        for (OrderByElement element : orderByExpressions) {
            writer.write(delimiter);
            element.toEPL(writer);
            delimiter = ", ";
        }
    }
}
