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
package com.espertech.esper.common.internal.epl.agg.groupbylocal;

import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * - Each local-group-by gets its own access state factory, shared between same-local-group-by for compatible states
 */
public class AggregationGroupByLocalGroupByAnalyzer {

    public static AggregationLocalGroupByPlanDesc analyze(ExprForge[][] methodForges, AggregationForgeFactory[] methodFactories, AggregationStateFactoryForge[] accessAggregations, AggregationGroupByLocalGroupDesc localGroupDesc, ExprNode[] groupByExpressions, MultiKeyClassRef groupByMultiKey, AggregationAccessorSlotPairForge[] accessors, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {

        if (groupByExpressions == null) {
            groupByExpressions = ExprNodeUtilityQuery.EMPTY_EXPR_ARRAY;
        }

        AggregationLocalGroupByColumnForge[] columns = new AggregationLocalGroupByColumnForge[localGroupDesc.getNumColumns()];
        List<AggregationLocalGroupByLevelForge> levelsList = new ArrayList<>();
        AggregationLocalGroupByLevelForge optionalTopLevel = null;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // determine optional top level (level number is -1)
        for (int i = 0; i < localGroupDesc.getLevels().length; i++) {
            AggregationGroupByLocalGroupLevel levelDesc = localGroupDesc.getLevels()[i];
            if (levelDesc.getPartitionExpr().length == 0) {
                AggregationGroupByLocalGroupLevelDesc top = getLevel(-1, levelDesc, methodForges, methodFactories, accessAggregations, columns, groupByExpressions.length == 0, accessors, groupByExpressions, groupByMultiKey, raw, serdeResolver);
                optionalTopLevel = top.getForge();
                additionalForgeables.addAll(top.getAdditionalForgeables());
            }
        }

        // determine default (same as group-by) level, if any, assign level number 0
        int levelNumber = 0;
        for (int i = 0; i < localGroupDesc.getLevels().length; i++) {
            AggregationGroupByLocalGroupLevel levelDesc = localGroupDesc.getLevels()[i];
            if (levelDesc.getPartitionExpr().length == 0) {
                continue;
            }
            boolean isDefaultLevel = groupByExpressions != null && ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder(groupByExpressions, levelDesc.getPartitionExpr());
            if (isDefaultLevel) {
                AggregationGroupByLocalGroupLevelDesc level = getLevel(0, levelDesc, methodForges, methodFactories, accessAggregations, columns, isDefaultLevel, accessors, groupByExpressions, groupByMultiKey, raw, serdeResolver);
                additionalForgeables.addAll(level.getAdditionalForgeables());
                levelsList.add(level.getForge());
                levelNumber++;
                break;
            }
        }

        // determine all other levels, assign level numbers
        for (int i = 0; i < localGroupDesc.getLevels().length; i++) {
            AggregationGroupByLocalGroupLevel levelDesc = localGroupDesc.getLevels()[i];
            if (levelDesc.getPartitionExpr().length == 0) {
                continue;
            }
            boolean isDefaultLevel = groupByExpressions != null && ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder(groupByExpressions, levelDesc.getPartitionExpr());
            if (isDefaultLevel) {
                continue;
            }
            AggregationGroupByLocalGroupLevelDesc level = getLevel(levelNumber, levelDesc, methodForges, methodFactories, accessAggregations, columns, isDefaultLevel, accessors, groupByExpressions, groupByMultiKey, raw, serdeResolver);
            levelsList.add(level.getForge());
            additionalForgeables.addAll(level.getAdditionalForgeables());
            levelNumber++;
        }

        // totals
        int numMethods = 0;
        int numAccesses = 0;
        if (optionalTopLevel != null) {
            numMethods += optionalTopLevel.getMethodFactories().length;
            numAccesses += optionalTopLevel.getAccessStateForges().length;
        }
        for (AggregationLocalGroupByLevelForge level : levelsList) {
            numMethods += level.getMethodFactories().length;
            numAccesses += level.getAccessStateForges().length;
        }

        AggregationLocalGroupByLevelForge[] levels = levelsList.toArray(new AggregationLocalGroupByLevelForge[levelsList.size()]);
        AggregationLocalGroupByPlanForge forge = new AggregationLocalGroupByPlanForge(numMethods, numAccesses, columns, optionalTopLevel, levels);
        return new AggregationLocalGroupByPlanDesc(forge, additionalForgeables);
    }

    // Obtain those method and state factories for each level
    private static AggregationGroupByLocalGroupLevelDesc getLevel(int levelNumber, AggregationGroupByLocalGroupLevel level, ExprForge[][] methodForgesAll, AggregationForgeFactory[] methodFactoriesAll, AggregationStateFactoryForge[] accessForges, AggregationLocalGroupByColumnForge[] columns, boolean defaultLevel, AggregationAccessorSlotPairForge[] accessors, ExprNode[] groupByExpressions, MultiKeyClassRef optionalGroupByMultiKey, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {

        ExprNode[] partitionExpr = level.getPartitionExpr();
        MultiKeyPlan multiKeyPlan;
        if (defaultLevel && optionalGroupByMultiKey != null) { // use default multi-key that is already generated
            multiKeyPlan = new MultiKeyPlan(Collections.EMPTY_LIST, optionalGroupByMultiKey);
            partitionExpr = groupByExpressions;
        } else {
            multiKeyPlan = MultiKeyPlanner.planMultiKey(partitionExpr, false, raw, serdeResolver);
        }

        List<ExprForge[]> methodForges = new ArrayList<>();
        List<AggregationForgeFactory> methodFactories = new ArrayList<>();
        List<AggregationStateFactoryForge> stateFactories = new ArrayList<>();

        for (AggregationServiceAggExpressionDesc expr : level.getExpressions()) {
            int column = expr.getAggregationNode().getColumn();
            int methodOffset = -1;
            boolean methodAgg = true;
            AggregationAccessorSlotPairForge pair = null;

            if (column < methodForgesAll.length) {
                methodForges.add(methodForgesAll[column]);
                methodFactories.add(methodFactoriesAll[column]);
                methodOffset = methodFactories.size() - 1;
            } else {
                // slot gives us the number of the state factory
                int absoluteSlot = accessors[column - methodForgesAll.length].getSlot();
                AggregationAccessorForge accessor = accessors[column - methodForgesAll.length].getAccessorForge();
                AggregationStateFactoryForge factory = accessForges[absoluteSlot];
                int relativeSlot = stateFactories.indexOf(factory);
                if (relativeSlot == -1) {
                    stateFactories.add(factory);
                    relativeSlot = stateFactories.size() - 1;
                }
                methodAgg = false;
                pair = new AggregationAccessorSlotPairForge(relativeSlot, accessor);
            }
            columns[column] = new AggregationLocalGroupByColumnForge(defaultLevel, partitionExpr, methodOffset, methodAgg, pair, levelNumber);
        }

        AggregationLocalGroupByLevelForge forge = new AggregationLocalGroupByLevelForge(methodForges.toArray(new ExprForge[methodForges.size()][]),
            methodFactories.toArray(new AggregationForgeFactory[methodFactories.size()]),
            stateFactories.toArray(new AggregationStateFactoryForge[stateFactories.size()]), partitionExpr, multiKeyPlan.getClassRef(), defaultLevel);
        return new AggregationGroupByLocalGroupLevelDesc(forge, multiKeyPlan == null ? Collections.emptyList() : multiKeyPlan.getMultiKeyForgeables());
    }
}
