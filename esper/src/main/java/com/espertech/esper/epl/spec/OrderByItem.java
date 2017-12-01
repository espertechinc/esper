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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;
import java.util.Collection;

/**
 * Specification object to an element in the order-by expression.
 */
public class OrderByItem implements Serializable {
    public static final OrderByItem[] EMPTY_ORDERBY_ARRAY = new OrderByItem[0];

    private ExprNode exprNode;
    private boolean isDescending;
    private static final long serialVersionUID = 4147598689501964350L;

    /**
     * Ctor.
     *
     * @param exprNode  is the order-by expression node
     * @param ascending is true for ascending, or false for descending sort
     */
    public OrderByItem(ExprNode exprNode, boolean ascending) {
        this.exprNode = exprNode;
        isDescending = ascending;
    }

    /**
     * Returns the order-by expression node.
     *
     * @return expression node.
     */
    public ExprNode getExprNode() {
        return exprNode;
    }

    /**
     * Returns true for ascending, false for descending.
     *
     * @return indicator if ascending or descending
     */
    public boolean isDescending() {
        return isDescending;
    }

    public OrderByItem copy() {
        return new OrderByItem(exprNode, isDescending());
    }

    public static OrderByItem[] toArray(Collection<OrderByItem> expressions) {
        if (expressions.isEmpty()) {
            return EMPTY_ORDERBY_ARRAY;
        }
        return expressions.toArray(new OrderByItem[expressions.size()]);
    }
}
