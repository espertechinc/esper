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
package com.espertech.esper.epl.agg.util;

import com.espertech.esper.epl.agg.access.AggregationAccessorForge;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPairForge;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.ArrayList;
import java.util.List;

/**
 * - Each local-group-by gets its own access state factory, shared between same-local-group-by for compatible states
 */
public class AggregationGroupByLocalGroupByAnalyzer {

    public static AggregationLocalGroupByPlanForge analyze(ExprForge[][] methodForges, AggregationMethodFactory[] methodFactories, AggregationStateFactoryForge[] accessAggregations, AggregationGroupByLocalGroupDesc localGroupDesc, ExprNode[] groupByExpressions, AggregationAccessorSlotPairForge[] accessors, EngineImportService engineImportService, boolean fireAndForget, String statementName) {

        if (groupByExpressions == null) {
            groupByExpressions = ExprNodeUtilityCore.EMPTY_EXPR_ARRAY;
        }

        AggregationLocalGroupByColumnForge[] columns = new AggregationLocalGroupByColumnForge[localGroupDesc.getNumColumns()];
        List<AggregationLocalGroupByLevelForge> levelsList = new ArrayList<>();
        AggregationLocalGroupByLevelForge optionalTopLevel = null;

        // determine optional top level (level number is -1)
        for (int i = 0; i < localGroupDesc.getLevels().length; i++) {
            AggregationGroupByLocalGroupLevel levelDesc = localGroupDesc.getLevels()[i];
            if (levelDesc.getPartitionExpr().length == 0) {
                optionalTopLevel = getLevel(-1, levelDesc, methodForges, methodFactories, accessAggregations, columns, groupByExpressions.length == 0, accessors, engineImportService, fireAndForget, statementName);
            }
        }

        // determine default (same as group-by) level, if any, assign level number 0
        int levelNumber = 0;
        for (int i = 0; i < localGroupDesc.getLevels().length; i++) {
            AggregationGroupByLocalGroupLevel levelDesc = localGroupDesc.getLevels()[i];
            if (levelDesc.getPartitionExpr().length == 0) {
                continue;
            }
            boolean isDefaultLevel = groupByExpressions != null && ExprNodeUtilityCore.deepEqualsIgnoreDupAndOrder(groupByExpressions, levelDesc.getPartitionExpr());
            if (isDefaultLevel) {
                AggregationLocalGroupByLevelForge level = getLevel(0, levelDesc, methodForges, methodFactories, accessAggregations, columns, isDefaultLevel, accessors, engineImportService, fireAndForget, statementName);
                levelsList.add(level);
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
            boolean isDefaultLevel = groupByExpressions != null && ExprNodeUtilityCore.deepEqualsIgnoreDupAndOrder(groupByExpressions, levelDesc.getPartitionExpr());
            if (isDefaultLevel) {
                continue;
            }
            AggregationLocalGroupByLevelForge level = getLevel(levelNumber, levelDesc, methodForges, methodFactories, accessAggregations, columns, isDefaultLevel, accessors, engineImportService, fireAndForget, statementName);
            levelsList.add(level);
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
        return new AggregationLocalGroupByPlanForge(numMethods, numAccesses, columns, optionalTopLevel, levels);
    }

    // Obtain those method and state factories for each level
    private static AggregationLocalGroupByLevelForge getLevel(int levelNumber, AggregationGroupByLocalGroupLevel level, ExprForge[][] methodForgesAll, AggregationMethodFactory[] methodFactoriesAll, AggregationStateFactoryForge[] accessForges, AggregationLocalGroupByColumnForge[] columns, boolean defaultLevel, AggregationAccessorSlotPairForge[] accessors, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {

        ExprNode[] partitionExpr = level.getPartitionExpr();
        ExprForge[] partitionForges = ExprNodeUtilityCore.getForges(partitionExpr);

        List<ExprForge[]> methodForges = new ArrayList<>();
        List<AggregationMethodFactory> methodFactories = new ArrayList<>();
        List<AggregationStateFactoryForge> stateFactories = new ArrayList<>();

        for (AggregationServiceAggExpressionDesc expr : level.getExpressions()) {
            int column = expr.getColumnNum();
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
            columns[column] = new AggregationLocalGroupByColumnForge(defaultLevel, partitionForges, methodOffset, methodAgg, pair, levelNumber);
        }

        return new AggregationLocalGroupByLevelForge(methodForges.toArray(new ExprForge[methodForges.size()][]),
                methodFactories.toArray(new AggregationMethodFactory[methodFactories.size()]),
                stateFactories.toArray(new AggregationStateFactoryForge[stateFactories.size()]), partitionForges, defaultLevel);
    }
}
