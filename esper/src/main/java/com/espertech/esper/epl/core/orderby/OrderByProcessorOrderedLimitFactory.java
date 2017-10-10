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

/**
 * Sorter and row limiter in one: sorts using a sorter and row limits
 */
public class OrderByProcessorOrderedLimitFactory implements OrderByProcessorFactory {
    private final OrderByProcessorFactoryImpl orderByProcessorFactory;
    private final RowLimitProcessorFactory rowLimitProcessorFactory;

    public OrderByProcessorOrderedLimitFactory(OrderByProcessorFactoryImpl orderByProcessorFactory, RowLimitProcessorFactory rowLimitProcessorFactory) {
        this.orderByProcessorFactory = orderByProcessorFactory;
        this.rowLimitProcessorFactory = rowLimitProcessorFactory;
    }

    public OrderByProcessor instantiate(AgentInstanceContext agentInstanceContext) {
        OrderByProcessorImpl orderByProcessor = (OrderByProcessorImpl) orderByProcessorFactory.instantiate(agentInstanceContext);
        RowLimitProcessor rowLimitProcessor = rowLimitProcessorFactory.instantiate(agentInstanceContext);
        return new OrderByProcessorOrderedLimit(orderByProcessor, rowLimitProcessor);
    }
}
