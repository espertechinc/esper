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
package com.espertech.esper.common.internal.fabric;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchSpecForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public interface FabricTypeCollector {
    void builtin(Class... types);
    void refCountedSetOfDouble();
    void serde(DataInputOutputSerdeForge serde);
    void sortedDoubleVector();
    void sortedRefCountedSet(DataInputOutputSerdeForge serde);
    void serdeObjectArrayMayNullNull(DataInputOutputSerdeForge[] criteriaSerdes);
    void serdeNullableEvent(EventType eventType);
    void treeMapEventsMayDeque(DataInputOutputSerdeForge[] criteriaSerdes, EventType eventType);
    void refCountedSetAtomicInteger(EventType eventType);
    void linkedHashMapEventsAndInt(EventType eventType);
    void listEvents(EventType eventType);
    void refCountedSet(DataInputOutputSerdeForge serde);
    void aggregatorNth(short serdeVersion, int sizeOfBuf, DataInputOutputSerdeForge serde);
    void aggregatorRateEver(short serdeVersion);
    void bigDecimal();
    void bigInteger();
    void countMinSketch(CountMinSketchSpecForge spec);
    void plugInAggregation(Class serde);

    static void collect(AggregationForgeFactory[] methodFactories, AggregationStateFactoryForge[] accessFactories, FabricTypeCollector collector) {
        // collect serde information
        if (methodFactories != null) {
            for (int i = 0; i < methodFactories.length; i++) {
                methodFactories[i].getAggregator().collectFabricType(collector);
            }
        }
        if (accessFactories != null) {
            for (int i = 0; i < accessFactories.length; i++) {
                accessFactories[i].getAggregator().collectFabricType(collector);
            }
        }
    }
}
