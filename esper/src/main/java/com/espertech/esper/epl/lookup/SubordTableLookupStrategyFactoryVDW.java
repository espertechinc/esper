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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.join.plan.CoercionDesc;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Strategy for looking up, in some sort of table or index, or a set of events, potentially based on the
 * events properties, and returning a set of matched events.
 */
public class SubordTableLookupStrategyFactoryVDW implements SubordTableLookupStrategyFactory {
    private final String statementName;
    private final int statementId;
    private final Annotation[] annotations;
    private final EventType[] outerStreams;
    private final List<SubordPropHashKey> hashKeys;
    private final CoercionDesc hashKeyCoercionTypes;
    private final List<SubordPropRangeKey> rangeKeys;
    private final CoercionDesc rangeKeyCoercionTypes;
    private final boolean nwOnTrigger;
    private final SubordPropPlan joinDesc;
    private final boolean forceTableScan;
    private final SubordinateQueryPlannerIndexPropListPair hashAndRanges;

    public SubordTableLookupStrategyFactoryVDW(String statementName, int statementId, Annotation[] annotations, EventType[] outerStreams, List<SubordPropHashKey> hashKeys, CoercionDesc hashKeyCoercionTypes, List<SubordPropRangeKey> rangeKeys, CoercionDesc rangeKeyCoercionTypes, boolean nwOnTrigger, SubordPropPlan joinDesc, boolean forceTableScan, SubordinateQueryPlannerIndexPropListPair hashAndRanges) {
        this.statementName = statementName;
        this.statementId = statementId;
        this.annotations = annotations;
        this.outerStreams = outerStreams;
        this.hashKeys = hashKeys;
        this.hashKeyCoercionTypes = hashKeyCoercionTypes;
        this.rangeKeys = rangeKeys;
        this.rangeKeyCoercionTypes = rangeKeyCoercionTypes;
        this.nwOnTrigger = nwOnTrigger;
        this.joinDesc = joinDesc;
        this.forceTableScan = forceTableScan;
        this.hashAndRanges = hashAndRanges;
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        Pair<IndexMultiKey, EventTable> tableVW = vdw.getSubordinateQueryDesc(false, hashAndRanges.getHashedProps(), hashAndRanges.getBtreeProps());
        return vdw.getSubordinateLookupStrategy(statementName,
                statementId, annotations,
                outerStreams, hashKeys, hashKeyCoercionTypes, rangeKeys, rangeKeyCoercionTypes, nwOnTrigger,
                tableVW.getSecond(), joinDesc, forceTableScan);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
