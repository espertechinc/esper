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
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorRowLimitOnlyFactory implements OrderByProcessorFactory {

    private final RowLimitProcessorFactory rowLimitProcessorFactory;

    public OrderByProcessorRowLimitOnlyFactory(RowLimitProcessorFactory rowLimitProcessorFactory) {
        this.rowLimitProcessorFactory = rowLimitProcessorFactory;
    }

    public OrderByProcessor instantiate(AgentInstanceContext agentInstanceContext) {
        RowLimitProcessor rowLimitProcessor = rowLimitProcessorFactory.instantiate(agentInstanceContext);
        return new OrderByProcessorRowLimitOnly(rowLimitProcessor);
    }
}
