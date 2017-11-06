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
package com.espertech.esper.epl.core.orderby;

import com.espertech.esper.core.context.util.AgentInstanceContext;

import java.util.Comparator;

/**
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorFactoryImpl implements OrderByProcessorFactory {

    private final OrderByElementEval[] orderBy;
    private final boolean needsGroupByKeys;
    private final Comparator<Object> comparator;
    private final OrderByElementEval[][] orderByRollup;
    private final OrderByProcessorImpl orderByProcessor;

    public OrderByProcessorFactoryImpl(OrderByElementEval[] orderBy,
                                       boolean needsGroupByKeys,
                                       Comparator<Object> comparator,
                                       OrderByElementEval[][] orderByRollup) {
        this.orderBy = orderBy;
        this.needsGroupByKeys = needsGroupByKeys;
        this.comparator = comparator;
        this.orderByRollup = orderByRollup;
        this.orderByProcessor = new OrderByProcessorImpl(this);
    }

    public OrderByProcessor instantiate(AgentInstanceContext agentInstanceContext) {
        return orderByProcessor;
    }

    public OrderByElementEval[] getOrderBy() {
        return orderBy;
    }

    public boolean isNeedsGroupByKeys() {
        return needsGroupByKeys;
    }

    public Comparator<Object> getComparator() {
        return comparator;
    }

    public OrderByElementEval[][] getOrderByRollup() {
        return orderByRollup;
    }
}
