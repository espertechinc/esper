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
package com.espertech.esper.common.internal.epl.virtualdw;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropHashKeyForge;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropPlan;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropRangeKeyForge;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryPlannerIndexPropListPair;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Strategy for looking up, in some sort of table or index, or a set of events, potentially based on the
 * events properties, and returning a set of matched events.
 */
public class SubordTableLookupStrategyFactoryForgeVDW implements SubordTableLookupStrategyFactoryForge {
    private final String statementName;
    private final Annotation[] annotations;
    private final EventType[] outerStreams;
    private final List<SubordPropHashKeyForge> hashKeys;
    private final CoercionDesc hashKeyCoercionTypes;
    private final List<SubordPropRangeKeyForge> rangeKeys;
    private final CoercionDesc rangeKeyCoercionTypes;
    private final boolean nwOnTrigger;
    private final SubordPropPlan joinDesc;
    private final boolean forceTableScan;
    private final SubordinateQueryPlannerIndexPropListPair hashAndRanges;

    public SubordTableLookupStrategyFactoryForgeVDW(String statementName, Annotation[] annotations, EventType[] outerStreams, List<SubordPropHashKeyForge> hashKeys, CoercionDesc hashKeyCoercionTypes, List<SubordPropRangeKeyForge> rangeKeys, CoercionDesc rangeKeyCoercionTypes, boolean nwOnTrigger, SubordPropPlan joinDesc, boolean forceTableScan, SubordinateQueryPlannerIndexPropListPair hashAndRanges) {
        this.statementName = statementName;
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

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        ExprNode[] hashes = new ExprNode[hashKeys.size()];
        Class[] hashTypes = new Class[hashKeys.size()];
        for (int i = 0; i < hashKeys.size(); i++) {
            hashes[i] = hashKeys.get(i).getHashKey().getKeyExpr();
            hashTypes[i] = hashKeyCoercionTypes.getCoercionTypes()[i];
        }

        QueryGraphValueEntryRangeForge[] ranges = new QueryGraphValueEntryRangeForge[rangeKeys.size()];
        Class[] rangesTypes = new Class[rangeKeys.size()];
        for (int i = 0; i < rangeKeys.size(); i++) {
            ranges[i] = rangeKeys.get(i).getRangeInfo();
            rangesTypes[i] = rangeKeyCoercionTypes.getCoercionTypes()[i];
        }

        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(SubordTableLookupStrategyFactoryVDW.class, this.getClass(), "lookup", parent, symbols, classScope);
        builder
                .expression("indexHashedProps", IndexedPropDesc.makeArray(hashAndRanges.getHashedProps()))
                .expression("indexBtreeProps", IndexedPropDesc.makeArray(hashAndRanges.getBtreeProps()))
                .constant("nwOnTrigger", nwOnTrigger)
                .constant("numOuterStreams", outerStreams.length)
                .expression("hashEvals", ExprNodeUtilityCodegen.codegenEvaluators(hashes, builder.getMethod(), this.getClass(), classScope))
                .constant("hashCoercionTypes", hashTypes)
                .expression("rangeEvals", QueryGraphValueEntryRangeForge.makeArray(ranges, builder.getMethod(), symbols, classScope))
                .constant("rangeCoercionTypes", rangesTypes);
        return builder.build();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
