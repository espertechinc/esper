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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

public class AggregatorUtil {
    public final static ExprEvaluator[] METHODAGG_EMPTYEVALUATORS = new ExprEvaluator[0];
    public final static AggregationMethodFactory[] METHODAGG_EMPTYFACTORIES = new AggregationMethodFactory[0];
    public final static AggregationAccessorSlotPair[] ACCESSAGG_EMPTY_ACCESSORS = new AggregationAccessorSlotPair[0];
    public final static AggregationStateFactory[] ACCESSAGG_EMPTY_STATEFACTORY = new AggregationStateFactory[0];

    public static boolean checkFilter(Object[] object) {
        Boolean pass = (Boolean) object[1];
        return pass != null && pass;
    }

    public static AggregationAccessorSlotPair[] getAccessorsForForges(AggregationAccessorSlotPairForge[] accessors, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        AggregationAccessorSlotPair[] pairs = new AggregationAccessorSlotPair[accessors.length];
        for (int i = 0; i < accessors.length; i++) {
            pairs[i] = getAccessorForForge(accessors[i], engineImportService, isFireAndForget, statementName);
        }
        return pairs;
    }

    public static AggregationStateFactory[] getAccesssFactoriesFromForges(AggregationStateFactoryForge[] accessFactories, StatementContext stmtContext, boolean isFireAndForget) {
        AggregationStateFactory[] array = new AggregationStateFactory[accessFactories.length];
        for (int i = 0; i < accessFactories.length; i++) {
            array[i] = accessFactories[i].makeFactory(stmtContext.getEngineImportService(), isFireAndForget, stmtContext.getStatementName());
        }
        return array;
    }

    public static AggregationAccessorSlotPair getAccessorForForge(AggregationAccessorSlotPairForge pair, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        AggregationAccessor accessor = pair.getAccessorForge() == null ? null : pair.getAccessorForge().getAccessor(engineImportService, isFireAndForget, statementName);
        return new AggregationAccessorSlotPair(pair.getSlot(), accessor);
    }

    public static AggregationAgent[] getAgentForges(AggregationAgentForge[] agentForges, EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        AggregationAgent[] agents = new AggregationAgent[agentForges.length];
        for (int i = 0; i < agents.length; i++) {
            agents[i] = agentForges[i].makeAgent(engineImportService, isFireAndForget, statementName);
        }
        return agents;
    }
}
