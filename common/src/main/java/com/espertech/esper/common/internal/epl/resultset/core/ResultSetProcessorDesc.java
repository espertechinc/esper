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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceForgeDesc;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;

import java.util.List;

public class ResultSetProcessorDesc {
    private final ResultSetProcessorFactoryForge resultSetProcessorFactoryForge;
    private final ResultSetProcessorType resultSetProcessorType;
    private final SelectExprProcessorForge[] selectExprProcessorForges;
    private final boolean join;
    private final boolean hasOutputLimit;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final boolean hasOutputLimitSnapshot;
    private final EventType resultEventType;
    private final boolean rollup;
    private final AggregationServiceForgeDesc aggregationServiceForgeDesc;
    private final OrderByProcessorFactoryForge orderByProcessorFactoryForge;
    private final SelectSubscriberDescriptor selectSubscriberDescriptor;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public ResultSetProcessorDesc(ResultSetProcessorFactoryForge resultSetProcessorFactoryForge, ResultSetProcessorType resultSetProcessorType, SelectExprProcessorForge[] selectExprProcessorForges, boolean join, boolean hasOutputLimit, ResultSetProcessorOutputConditionType outputConditionType, boolean hasOutputLimitSnapshot, EventType resultEventType, boolean rollup, AggregationServiceForgeDesc aggregationServiceForgeDesc, OrderByProcessorFactoryForge orderByProcessorFactoryForge, SelectSubscriberDescriptor selectSubscriberDescriptor, List<StmtClassForgeableFactory> additionalForgeables) {
        this.resultSetProcessorFactoryForge = resultSetProcessorFactoryForge;
        this.resultSetProcessorType = resultSetProcessorType;
        this.selectExprProcessorForges = selectExprProcessorForges;
        this.join = join;
        this.hasOutputLimit = hasOutputLimit;
        this.outputConditionType = outputConditionType;
        this.hasOutputLimitSnapshot = hasOutputLimitSnapshot;
        this.resultEventType = resultEventType;
        this.rollup = rollup;
        this.aggregationServiceForgeDesc = aggregationServiceForgeDesc;
        this.orderByProcessorFactoryForge = orderByProcessorFactoryForge;
        this.selectSubscriberDescriptor = selectSubscriberDescriptor;
        this.additionalForgeables = additionalForgeables;
    }

    public ResultSetProcessorFactoryForge getResultSetProcessorFactoryForge() {
        return resultSetProcessorFactoryForge;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return resultSetProcessorType;
    }

    public SelectExprProcessorForge[] getSelectExprProcessorForges() {
        return selectExprProcessorForges;
    }

    public boolean isJoin() {
        return join;
    }

    public boolean isHasOutputLimit() {
        return hasOutputLimit;
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public boolean isHasOutputLimitSnapshot() {
        return hasOutputLimitSnapshot;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public boolean isRollup() {
        return rollup;
    }

    public AggregationServiceForgeDesc getAggregationServiceForgeDesc() {
        return aggregationServiceForgeDesc;
    }

    public OrderByProcessorFactoryForge getOrderByProcessorFactoryForge() {
        return orderByProcessorFactoryForge;
    }

    public SelectSubscriberDescriptor getSelectSubscriberDescriptor() {
        return selectSubscriberDescriptor;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
