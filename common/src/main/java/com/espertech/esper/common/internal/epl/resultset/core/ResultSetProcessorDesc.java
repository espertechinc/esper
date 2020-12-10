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
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.List;

public class ResultSetProcessorDesc {
    private final ResultSetProcessorFactoryForge resultSetProcessorFactoryForge;
    private final ResultSetProcessorFlags flags;
    private final ResultSetProcessorType resultSetProcessorType;
    private final SelectExprProcessorForge[] selectExprProcessorForges;
    private final EventType resultEventType;
    private final boolean rollup;
    private final AggregationServiceForgeDesc aggregationServiceForgeDesc;
    private final OrderByProcessorFactoryForge orderByProcessorFactoryForge;
    private final SelectSubscriberDescriptor selectSubscriberDescriptor;
    private final List<StmtClassForgeableFactory> additionalForgeables;
    private final FabricCharge fabricCharge;

    public ResultSetProcessorDesc(ResultSetProcessorFactoryForge resultSetProcessorFactoryForge, ResultSetProcessorFlags flags, ResultSetProcessorType resultSetProcessorType, SelectExprProcessorForge[] selectExprProcessorForges, EventType resultEventType, boolean rollup, AggregationServiceForgeDesc aggregationServiceForgeDesc, OrderByProcessorFactoryForge orderByProcessorFactoryForge, SelectSubscriberDescriptor selectSubscriberDescriptor, List<StmtClassForgeableFactory> additionalForgeables, FabricCharge fabricCharge) {
        this.resultSetProcessorFactoryForge = resultSetProcessorFactoryForge;
        this.resultSetProcessorType = resultSetProcessorType;
        this.selectExprProcessorForges = selectExprProcessorForges;
        this.flags = flags;
        this.resultEventType = resultEventType;
        this.rollup = rollup;
        this.aggregationServiceForgeDesc = aggregationServiceForgeDesc;
        this.orderByProcessorFactoryForge = orderByProcessorFactoryForge;
        this.selectSubscriberDescriptor = selectSubscriberDescriptor;
        this.additionalForgeables = additionalForgeables;
        this.fabricCharge = fabricCharge;
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
        return flags.isJoin();
    }

    public boolean isHasOutputLimit() {
        return flags.isHasOutputLimit();
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return flags.getOutputConditionType();
    }

    public boolean isHasOutputLimitSnapshot() {
        return flags.isOutputLimitWSnapshot();
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

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
