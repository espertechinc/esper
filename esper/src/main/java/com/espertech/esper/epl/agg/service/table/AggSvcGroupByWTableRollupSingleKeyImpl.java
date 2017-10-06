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
package com.espertech.esper.epl.agg.service.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationAgentCodegenSymbols;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGrouped;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.NAME_GROUPKEY;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.REF_GROUPKEY;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByWTableRollupSingleKeyImpl extends AggSvcGroupByWTableBase {
    public AggSvcGroupByWTableRollupSingleKeyImpl(TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, boolean join, TableStateInstanceGrouped tableStateInstance, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents) {
        super(tableMetadata, methodPairs, accessors, join, tableStateInstance, targetStates, accessStateExpr, agents);
    }

    public void applyEnterInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (Object groupByKey : groupKeyPerLevel) {
            applyEnterGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public void applyLeaveInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (Object groupByKey : groupKeyPerLevel) {
            applyLeaveGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public static CodegenMethodNode applyRollupCodegen(boolean enter, CodegenMethodNode parent, CodegenClassScope classScope, CodegenNamedMethods namedMethods, TableColumnMethodPair[] methodPairs, AggregationAgentForge[] agentForges, AggregationAgent[] agents, int[] targetStates, AggregationGroupByRollupDesc groupByRollupDesc, int numTableKeys) {
        AggregationAgentCodegenSymbols symbols = new AggregationAgentCodegenSymbols(true, enter);
        CodegenMethodNode method = parent.makeChildWithScope(void.class, AggSvcGroupByWTableImpl.class, symbols, classScope).addParam(EventBean[].class, NAME_EPS).addParam(Object.class, NAME_GROUPKEY).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        CodegenMethodNode applyKey = AggSvcGroupByWTableImpl.applyGroupKeyCodegen(enter, method, classScope, methodPairs, agentForges, agents, targetStates);

        method.getBlock().declareVar(Object[].class, "groupKeyPerLevel", cast(Object[].class, REF_GROUPKEY))
                .forEach(Object.class, "groupKey", ref("groupKeyPerLevel"))
                    .localMethod(applyKey, REF_EPS, ref("groupKey"), REF_EXPREVALCONTEXT);
        return method;
    }
}
