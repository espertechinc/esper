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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateKey;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateLocalGroupByDesc;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AggregationMultiFunctionAnalysisHelper {
    // handle accessor aggregation (direct data window by-group access to properties)
    public static AggregationMultiFunctionAnalysisResult analyzeAccessAggregations(List<AggregationServiceAggExpressionDesc> aggregations, ClasspathImportServiceCompileTime classpathImportService, boolean isFireAndForget, String statementName, ExprNode[] groupByNodes) {
        int currentSlot = 0;
        Deque<AggregationMFIdentifier> accessProviderSlots = new ArrayDeque<AggregationMFIdentifier>();
        List<AggregationAccessorSlotPairForge> accessorPairsForges = new ArrayList<>();
        List<AggregationStateFactoryForge> stateFactoryForges = new ArrayList<>();

        for (AggregationServiceAggExpressionDesc aggregation : aggregations) {
            ExprAggregateNode aggregateNode = aggregation.getAggregationNode();
            if (!aggregateNode.getFactory().isAccessAggregation()) {
                continue;
            }

            AggregationMultiFunctionStateKey providerKey = aggregateNode.getFactory().getAggregationStateKey(false);
            AggregationMFIdentifier existing = findExisting(accessProviderSlots, providerKey, aggregateNode.getOptionalLocalGroupBy(), groupByNodes);

            int slot;
            if (existing == null) {
                accessProviderSlots.add(new AggregationMFIdentifier(providerKey, aggregateNode.getOptionalLocalGroupBy(), currentSlot));
                slot = currentSlot++;
                AggregationStateFactoryForge providerForge = aggregateNode.getFactory().getAggregationStateFactory(false);
                stateFactoryForges.add(providerForge);
            } else {
                slot = existing.getSlot();
            }

            AggregationAccessorForge accessorForge = aggregateNode.getFactory().getAccessorForge();
            accessorPairsForges.add(new AggregationAccessorSlotPairForge(slot, accessorForge));
        }

        AggregationAccessorSlotPairForge[] forges = accessorPairsForges.toArray(new AggregationAccessorSlotPairForge[accessorPairsForges.size()]);
        AggregationStateFactoryForge[] accessForges = stateFactoryForges.toArray(new AggregationStateFactoryForge[stateFactoryForges.size()]);
        return new AggregationMultiFunctionAnalysisResult(forges, accessForges);
    }

    private static AggregationMFIdentifier findExisting(Deque<AggregationMFIdentifier> accessProviderSlots, AggregationMultiFunctionStateKey providerKey, ExprAggregateLocalGroupByDesc optionalOver, ExprNode[] groupByNodes) {
        for (AggregationMFIdentifier ident : accessProviderSlots) {
            if (!providerKey.equals(ident.getAggregationStateKey())) {
                continue;
            }
            // if there is no local-group by, but there is group-by-clause, and the ident-over matches, use that
            if (optionalOver == null && groupByNodes.length > 0 && ident.optionalLocalGroupBy != null &&
                    ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder(groupByNodes, ident.optionalLocalGroupBy.getPartitionExpressions())) {
                return ident;
            }
            if (optionalOver == null && ident.optionalLocalGroupBy == null) {
                return ident;
            }
            if (optionalOver != null &&
                    ident.optionalLocalGroupBy != null &&
                    ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder(optionalOver.getPartitionExpressions(), ident.optionalLocalGroupBy.getPartitionExpressions())) {
                return ident;
            }
        }
        return null;
    }

    private static class AggregationMFIdentifier {
        private final AggregationMultiFunctionStateKey aggregationStateKey;
        private final ExprAggregateLocalGroupByDesc optionalLocalGroupBy;
        private final int slot;

        private AggregationMFIdentifier(AggregationMultiFunctionStateKey aggregationStateKey, ExprAggregateLocalGroupByDesc optionalLocalGroupBy, int slot) {
            this.aggregationStateKey = aggregationStateKey;
            this.optionalLocalGroupBy = optionalLocalGroupBy;
            this.slot = slot;
        }

        public AggregationMultiFunctionStateKey getAggregationStateKey() {
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
