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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.epl.agg.groupby.AggSvcGroupByReclaimAgedEvalFuncFactory;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggregationLocalGroupByColumn;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggregationLocalGroupByLevel;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;

public class AggregationServiceFactoryServiceImpl implements AggregationServiceFactoryService {

    public final static AggregationServiceFactoryServiceImpl INSTANCE = new AggregationServiceFactoryServiceImpl();

    private AggregationServiceFactoryServiceImpl() {
    }

    public AggregationServiceFactory groupAll(AggregationServiceFactory nonHAFactory, AggregationRowFactory rowFactory, AggregationUseFlags useFlags, DataInputOutputSerde<AggregationRow> serde) {
        return nonHAFactory;
    }

    public AggregationServiceFactory groupBy(AggregationServiceFactory nonHAFactory, AggregationRowFactory rowFactory, AggregationUseFlags useFlags, DataInputOutputSerde<AggregationRow> serde, Class[] groupByTypes, AggSvcGroupByReclaimAgedEvalFuncFactory reclaimMaxAge, AggSvcGroupByReclaimAgedEvalFuncFactory reclaimFreq, TimeAbacus timeAbacus, DataInputOutputSerde<Object> groupKeySerde) {
        return nonHAFactory;
    }

    public AggregationServiceFactory groupByRollup(AggregationServiceFactory nonHAFactory, AggregationGroupByRollupDesc groupByRollupDesc, AggregationRowFactory rowFactory, AggregationUseFlags useFlags, DataInputOutputSerde<AggregationRow> serde, Class[] groupByTypes) {
        return nonHAFactory;
    }

    public AggregationServiceFactory groupLocalGroupBy(AggregationServiceFactory nonHAFactory, AggregationUseFlags useFlags, boolean hasGroupBy, AggregationLocalGroupByLevel optionalTop, AggregationLocalGroupByLevel[] levels, AggregationLocalGroupByColumn[] columns) {
        return nonHAFactory;
    }
}
