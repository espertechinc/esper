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

import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * - Each local-group-by gets its own access state factory, shared between same-local-group-by for compatible states
 */
public class AggregationGroupByLocalGroupByAnalyzer {

    public static AggregationLocalGroupByPlan analyze(ExprEvaluator[] evaluators, AggregationMethodFactory[] prototypes, AggregationStateFactory[] accessAggregations, AggregationGroupByLocalGroupDesc localGroupDesc, ExprNode[] groupByExpressions, AggregationAccessorSlotPair[] accessors) {

        if (groupByExpressions == null) {
            groupByExpressions = ExprNodeUtility.EMPTY_EXPR_ARRAY;
        }

        AggregationLocalGroupByColumn[] columns = new AggregationLocalGroupByColumn[localGroupDesc.getNumColumns()];
        List<AggregationLocalGroupByLevel> levelsList = new ArrayList<AggregationLocalGroupByLevel>();
        AggregationLocalGroupByLevel optionalTopLevel = null;

        // determine optional top level (level number is -1)
        for (int i = 0; i < localGroupDesc.getLevels().length; i++) {
            AggregationGroupByLocalGroupLevel levelDesc = localGroupDesc.getLevels()[i];
            if (levelDesc.getPartitionExpr().length == 0) {
                optionalTopLevel = getLevel(-1, levelDesc, evaluators, prototypes, accessAggregations, columns, groupByExpressions.length == 0, accessors);
            }
        }

        // determine default (same as group-by) level, if any, assign level number 0
        int levelNumber = 0;
        for (int i = 0; i < localGroupDesc.getLevels().length; i++) {
            AggregationGroupByLocalGroupLevel levelDesc = localGroupDesc.getLevels()[i];
            if (levelDesc.getPartitionExpr().length == 0) {
                continue;
            }
            boolean isDefaultLevel = groupByExpressions != null && ExprNodeUtility.deepEqualsIgnoreDupAndOrder(groupByExpressions, levelDesc.getPartitionExpr());
            if (isDefaultLevel) {
                AggregationLocalGroupByLevel level = getLevel(0, levelDesc, evaluators, prototypes, accessAggregations, columns, isDefaultLevel, accessors);
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
            boolean isDefaultLevel = groupByExpressions != null && ExprNodeUtility.deepEqualsIgnoreDupAndOrder(groupByExpressions, levelDesc.getPartitionExpr());
            if (isDefaultLevel) {
                continue;
            }
            AggregationLocalGroupByLevel level = getLevel(levelNumber, levelDesc, evaluators, prototypes, accessAggregations, columns, isDefaultLevel, accessors);
            levelsList.add(level);
            levelNumber++;
        }

        // totals
        int numMethods = 0;
        int numAccesses = 0;
        if (optionalTopLevel != null) {
            numMethods += optionalTopLevel.getMethodFactories().length;
            numAccesses += optionalTopLevel.getStateFactories().length;
        }
        for (AggregationLocalGroupByLevel level : levelsList) {
            numMethods += level.getMethodFactories().length;
            numAccesses += level.getStateFactories().length;
        }

        AggregationLocalGroupByLevel[] levels = levelsList.toArray(new AggregationLocalGroupByLevel[levelsList.size()]);
        return new AggregationLocalGroupByPlan(numMethods, numAccesses, columns, optionalTopLevel, levels);
    }

    // Obtain those method and state factories for each level
    private static AggregationLocalGroupByLevel getLevel(int levelNumber, AggregationGroupByLocalGroupLevel level, ExprEvaluator[] methodEvaluatorsAll, AggregationMethodFactory[] methodFactoriesAll, AggregationStateFactory[] stateFactoriesAll, AggregationLocalGroupByColumn[] columns, boolean defaultLevel, AggregationAccessorSlotPair[] accessors) {

        ExprNode[] partitionExpr = level.getPartitionExpr();
        ExprEvaluator[] partitionEvaluators = ExprNodeUtility.getEvaluators(partitionExpr);

        List<ExprEvaluator> methodEvaluators = new ArrayList<ExprEvaluator>();
        List<AggregationMethodFactory> methodFactories = new ArrayList<AggregationMethodFactory>();
        List<AggregationStateFactory> stateFactories = new ArrayList<AggregationStateFactory>();

        for (AggregationServiceAggExpressionDesc expr : level.getExpressions()) {
            int column = expr.getColumnNum();
            int methodOffset = -1;
            boolean methodAgg = true;
            AggregationAccessorSlotPair pair = null;

            if (column < methodEvaluatorsAll.length) {
                methodEvaluators.add(methodEvaluatorsAll[column]);
                methodFactories.add(methodFactoriesAll[column]);
                methodOffset = methodFactories.size() - 1;
            } else {
                // slot gives us the number of the state factory
                int absoluteSlot = accessors[column - methodEvaluatorsAll.length].getSlot();
                AggregationAccessor accessor = accessors[column - methodEvaluatorsAll.length].getAccessor();
                AggregationStateFactory factory = stateFactoriesAll[absoluteSlot];
                int relativeSlot = stateFactories.indexOf(factory);
                if (relativeSlot == -1) {
                    stateFactories.add(factory);
                    relativeSlot = stateFactories.size() - 1;
                }
                methodAgg = false;
                pair = new AggregationAccessorSlotPair(relativeSlot, accessor);
            }
            columns[column] = new AggregationLocalGroupByColumn(defaultLevel, partitionEvaluators, methodOffset, methodAgg, pair, levelNumber);
        }

        return new AggregationLocalGroupByLevel(methodEvaluators.toArray(new ExprEvaluator[methodEvaluators.size()]),
                methodFactories.toArray(new AggregationMethodFactory[methodFactories.size()]),
                stateFactories.toArray(new AggregationStateFactory[stateFactories.size()]), partitionEvaluators, defaultLevel);
    }
}
