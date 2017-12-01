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

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactory;
import com.espertech.esper.epl.agg.service.common.AggregatorUtil;
import com.espertech.esper.epl.agg.service.groupbylocal.AggSvcLocalGroupByForge;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;

public class AggregationLocalGroupByPlanForge {

    private final int numMethods;
    private final int numAccess;
    private final AggregationLocalGroupByColumnForge[] columnsForges;
    private final AggregationLocalGroupByLevelForge optionalLevelTopForge;
    private final AggregationLocalGroupByLevelForge[] allLevelsForges;

    public AggregationLocalGroupByPlanForge(int numMethods, int numAccess, AggregationLocalGroupByColumnForge[] columns, AggregationLocalGroupByLevelForge optionalLevelTop, AggregationLocalGroupByLevelForge[] allLevels) {
        this.numMethods = numMethods;
        this.numAccess = numAccess;
        this.columnsForges = columns;
        this.optionalLevelTopForge = optionalLevelTop;
        this.allLevelsForges = allLevels;
    }

    public AggregationLocalGroupByColumnForge[] getColumnsForges() {
        return columnsForges;
    }

    public AggregationLocalGroupByLevelForge getOptionalLevelTopForge() {
        return optionalLevelTopForge;
    }

    public AggregationLocalGroupByLevelForge[] getAllLevelsForges() {
        return allLevelsForges;
    }

    public int getNumMethods() {
        return numMethods;
    }

    public int getNumAccess() {
        return numAccess;
    }

    public AggregationLocalGroupByPlan toEvaluators(StatementContext stmtContext, boolean isFireAndForget) {
        AggregationLocalGroupByColumn[] columns = new AggregationLocalGroupByColumn[columnsForges.length];
        for (int i = 0; i < columns.length; i++) {
            AggregationLocalGroupByColumnForge forge = columnsForges[i];
            ExprEvaluator[] evaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(forge.getPartitionForges(), stmtContext.getEngineImportService(), AggSvcLocalGroupByForge.class, isFireAndForget, stmtContext.getStatementName());
            AggregationAccessorSlotPair pair = forge.getPair() == null ? null : AggregatorUtil.getAccessorForForge(forge.getPair(), stmtContext.getEngineImportService(), isFireAndForget, stmtContext.getStatementName());
            columns[i] = new AggregationLocalGroupByColumn(forge.isDefaultGroupLevel(), evaluators, forge.getMethodOffset(), forge.isMethodAgg(), pair, forge.getLevelNum());
        }

        AggregationLocalGroupByLevel optionalLevelTop = optionalLevelTopForge == null ? null : makeLevel(optionalLevelTopForge, stmtContext, isFireAndForget);
        AggregationLocalGroupByLevel[] allLevels = new AggregationLocalGroupByLevel[allLevelsForges.length];
        for (int i = 0; i < allLevels.length; i++) {
            allLevels[i] = makeLevel(allLevelsForges[i], stmtContext, isFireAndForget);
        }

        return new AggregationLocalGroupByPlan(numMethods, numAccess, columns, optionalLevelTop, allLevels);
    }

    private AggregationLocalGroupByLevel makeLevel(AggregationLocalGroupByLevelForge forge, StatementContext stmtContext, boolean isFireAndForget) {
        ExprEvaluator[] methodEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompileWMultiValue(forge.getMethodForges(), stmtContext.getEngineImportService(), this.getClass(), isFireAndForget, stmtContext.getStatementName());
        AggregationStateFactory[] stateFactories = AggregatorUtil.getAccesssFactoriesFromForges(forge.getAccessStateForges(), stmtContext, isFireAndForget);
        ExprEvaluator[] partitionEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(forge.getPartitionForges(), stmtContext.getEngineImportService(), this.getClass(), isFireAndForget, stmtContext.getStatementName());
        Class[] partitionEvaluatorsTypes = ExprNodeUtilityCore.getExprResultTypes(forge.getPartitionForges());
        return new AggregationLocalGroupByLevel(methodEvaluators, forge.getMethodFactories(), stateFactories, partitionEvaluators, partitionEvaluatorsTypes, forge.isDefaultLevel());
    }
}
