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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggSvcGroupByUtil;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactory;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

public class TableStateRowFactory {

    private final ObjectArrayEventType objectArrayEventType;
    private final EngineImportService engineImportService;
    private final AggregationMethodFactory[] methodFactories;
    private final AggregationStateFactory[] stateFactories;
    private final int[] groupKeyIndexes;
    private final EventAdapterService eventAdapterService;

    public TableStateRowFactory(ObjectArrayEventType objectArrayEventType, EngineImportService engineImportService, AggregationMethodFactory[] methodFactories, AggregationStateFactory[] stateFactories, int[] groupKeyIndexes, EventAdapterService eventAdapterService) {
        this.objectArrayEventType = objectArrayEventType;
        this.engineImportService = engineImportService;
        this.methodFactories = methodFactories;
        this.stateFactories = stateFactories;
        this.groupKeyIndexes = groupKeyIndexes;
        this.eventAdapterService = eventAdapterService;
    }

    public ObjectArrayBackedEventBean makeOA(int agentInstanceId, Object groupByKey, Object groupKeyBinding, AggregationServicePassThru passThru) {
        AggregationRowPair row = makeAggs(agentInstanceId, groupByKey, groupKeyBinding, passThru);
        Object[] data = new Object[objectArrayEventType.getPropertyDescriptors().length];
        data[0] = row;

        if (groupKeyIndexes.length == 1) {
            data[groupKeyIndexes[0]] = groupByKey;
        } else {
            if (groupKeyIndexes.length > 1) {
                Object[] keys = ((MultiKeyUntyped) groupByKey).getKeys();
                for (int i = 0; i < groupKeyIndexes.length; i++) {
                    data[groupKeyIndexes[i]] = keys[i];
                }
            }
        }

        return (ObjectArrayBackedEventBean) eventAdapterService.adapterForType(data, objectArrayEventType);
    }

    public AggregationRowPair makeAggs(int agentInstanceId, Object groupByKey, Object groupKeyBinding, AggregationServicePassThru passThru) {
        AggregationMethod[] methods = AggSvcGroupByUtil.newAggregators(methodFactories);
        AggregationState[] states = AggSvcGroupByUtil.newAccesses(agentInstanceId, false, stateFactories, groupByKey, passThru);
        return new AggregationRowPair(methods, states);
    }
}
