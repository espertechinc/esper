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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateLocalGroupByDesc;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AggregationMultiFunctionAnalysisHelper {
    // handle accessor aggregation (direct data window by-group access to properties)
    public static AggregationMultiFunctionAnalysisResult analyzeAccessAggregations(List<AggregationServiceAggExpressionDesc> aggregations) {
        int currentSlot = 0;
        Deque<AggregationMFIdentifier> accessProviderSlots = new ArrayDeque<AggregationMFIdentifier>();
        List<AggregationAccessorSlotPair> accessorPairs = new ArrayList<AggregationAccessorSlotPair>();
        List<AggregationStateFactory> stateFactories = new ArrayList<AggregationStateFactory>();

        for (AggregationServiceAggExpressionDesc aggregation : aggregations) {
            ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
            if (!aggregateNode.getFactory().isAccessAggregation()) {
                continue;
            }

            AggregationStateKey providerKey = aggregateNode.getFactory().getAggregationStateKey(false);
            AggregationMFIdentifier existing = findExisting(accessProviderSlots, providerKey, aggregateNode.getOptionalLocalGroupBy());

            int slot;
            if (existing == null) {
                accessProviderSlots.add(new AggregationMFIdentifier(providerKey, aggregateNode.getOptionalLocalGroupBy(), currentSlot));
                slot = currentSlot++;
                AggregationStateFactory providerFactory = aggregateNode.getFactory().getAggregationStateFactory(false);
                stateFactories.add(providerFactory);
            } else {
                slot = existing.getSlot();
            }

            AggregationAccessor accessor = aggregateNode.getFactory().getAccessor();
            accessorPairs.add(new AggregationAccessorSlotPair(slot, accessor));
        }

        AggregationAccessorSlotPair[] pairs = accessorPairs.toArray(new AggregationAccessorSlotPair[accessorPairs.size()]);
        AggregationStateFactory[] accessAggregations = stateFactories.toArray(new AggregationStateFactory[stateFactories.size()]);
        return new AggregationMultiFunctionAnalysisResult(pairs, accessAggregations);
    }

    private static AggregationMFIdentifier findExisting(Deque<AggregationMFIdentifier> accessProviderSlots, AggregationStateKey providerKey, ExprAggregateLocalGroupByDesc optionalOver) {
        for (AggregationMFIdentifier ident : accessProviderSlots) {
            if (!providerKey.equals(ident.getAggregationStateKey())) {
                continue;
            }
            if (optionalOver == null && ident.optionalLocalGroupBy == null) {
                return ident;
            }
            if (optionalOver != null &&
                    ident.optionalLocalGroupBy != null &&
                    ExprNodeUtility.deepEqualsIgnoreDupAndOrder(optionalOver.getPartitionExpressions(), ident.optionalLocalGroupBy.getPartitionExpressions())) {
                return ident;
            }
        }
        return null;
    }

    private static class AggregationMFIdentifier {
        private final AggregationStateKey aggregationStateKey;
        private final ExprAggregateLocalGroupByDesc optionalLocalGroupBy;
        private final int slot;

        private AggregationMFIdentifier(AggregationStateKey aggregationStateKey, ExprAggregateLocalGroupByDesc optionalLocalGroupBy, int slot) {
            this.aggregationStateKey = aggregationStateKey;
            this.optionalLocalGroupBy = optionalLocalGroupBy;
            this.slot = slot;
        }

        public AggregationStateKey getAggregationStateKey() {
            return aggregationStateKey;
        }

        public ExprAggregateLocalGroupByDesc getOptionalLocalGroupBy() {
            return optionalLocalGroupBy;
        }

        public int getSlot() {
            return slot;
        }
    }
}
