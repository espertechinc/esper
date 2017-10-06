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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryDesc;
import com.espertech.esper.epl.core.orderby.OrderByProcessorFactory;

/**
 * Processor prototype for result sets for instances that apply the select-clause, group-by-clause and having-clauses
 * as supplied.
 */
public class ResultSetProcessorFactoryDesc {
    private final ResultSetProcessorFactory resultSetProcessorFactory;
    private final ResultSetProcessorType resultSetProcessorType;
    private final EventType resultEventType;
    private final OrderByProcessorFactory orderByProcessorFactory;
    private final AggregationServiceFactoryDesc aggregationServiceFactoryDesc;

    public ResultSetProcessorFactoryDesc(ResultSetProcessorFactory resultSetProcessorFactory, ResultSetProcessorType resultSetProcessorType, EventType resultEventType, OrderByProcessorFactory orderByProcessorFactory, AggregationServiceFactoryDesc aggregationServiceFactoryDesc) {
        this.resultSetProcessorFactory = resultSetProcessorFactory;
        this.resultSetProcessorType = resultSetProcessorType;
        this.resultEventType = resultEventType;
        this.orderByProcessorFactory = orderByProcessorFactory;
        this.aggregationServiceFactoryDesc = aggregationServiceFactoryDesc;
    }

    public ResultSetProcessorFactory getResultSetProcessorFactory() {
        return resultSetProcessorFactory;
    }

    public OrderByProcessorFactory getOrderByProcessorFactory() {
        return orderByProcessorFactory;
    }

    public AggregationServiceFactoryDesc getAggregationServiceFactoryDesc() {
        return aggregationServiceFactoryDesc;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return resultSetProcessorType;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
