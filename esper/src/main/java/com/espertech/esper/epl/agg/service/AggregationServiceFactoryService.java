/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.agg.service;

import com.espertech.esper.client.annotation.Hint;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.IntoTableSpec;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.variable.VariableService;

public interface AggregationServiceFactoryService {
    public AggregationServiceFactory getNullAggregationService();
    public AggregationServiceFactory getNoGroupNoAccess(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getNoGroupAccessOnly(AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggSpecs, boolean join, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getNoGroupAccessMixed(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggregations, boolean join, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupedNoReclaimNoAccess(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, Object groupKeyBinding, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupNoReclaimAccessOnly(AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggSpecs, Object groupKeyBinding, boolean join, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupNoReclaimMixed(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggregations, boolean join, Object groupKeyBinding, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupReclaimAged(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, Hint reclaimGroupAged, Hint reclaimGroupFrequency, VariableService variableService, AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggregations, boolean join, Object groupKeyBinding, String optionalContextName, boolean isUnidirectional, boolean isFireAndForget) throws ExprValidationException;
    public AggregationServiceFactory getGroupReclaimNoAccess(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggregations, boolean join, Object groupKeyBinding, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupReclaimMixable(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggregations, boolean join, Object groupKeyBinding, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupReclaimMixableRollup(ExprEvaluator[] evaluatorsArr, AggregationMethodFactory[] aggregatorsArr, AggregationAccessorSlotPair[] pairs, AggregationStateFactory[] accessAggregations, boolean join, Object groupKeyBinding, AggregationGroupByRollupDesc groupByRollupDesc, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupWBinding(TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessorPairs, boolean join, IntoTableSpec bindings, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents, AggregationGroupByRollupDesc groupByRollupDesc);
    public AggregationServiceFactory getNoGroupWBinding(AggregationAccessorSlotPair[] accessors, boolean join, TableColumnMethodPair[] methodPairs, String tableName, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents);
    public AggregationServiceFactory getNoGroupLocalGroupBy(boolean join, AggregationLocalGroupByPlan localGroupByPlan, Object groupKeyBinding, boolean isUnidirectional, boolean isFireAndForget);
    public AggregationServiceFactory getGroupLocalGroupBy(boolean join, AggregationLocalGroupByPlan localGroupByPlan, Object groupKeyBinding, boolean isUnidirectional, boolean isFireAndForget);
}
