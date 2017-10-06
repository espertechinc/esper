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
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPair;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationAgentCodegenSymbols;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.agg.service.common.AggregationRowPair;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGrouped;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.agg.service.table.AggSvcGroupByWTableCodegenUtil.REF_TABLESTATEINSTANCE;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByWTableRollupMultiKeyImpl extends AggSvcGroupByWTableBase {
    private final static String NAME_CURRENTGROUPKEY = "currentGroupKey";
    final static CodegenExpressionRef REF_CURRENTGROUPKEY = ref(NAME_CURRENTGROUPKEY);
    private final static String NAME_CURRENTAGGREGATORMETHODS = "currentAggregatorMethods";
    private final static CodegenExpressionRef REF_CURRENTAGGREGATORMETHODS = ref(NAME_CURRENTAGGREGATORMETHODS);
    private final static String NAME_CURRENTAGGREGATORSTATES = "currentAggregatorStates";
    private final static CodegenExpressionRef REF_CURRENTAGGREGATORSTATES = ref(NAME_CURRENTAGGREGATORSTATES);

    private final AggregationGroupByRollupDesc groupByRollupDesc;

    public AggSvcGroupByWTableRollupMultiKeyImpl(TableMetadata tableMetadata, TableColumnMethodPair[] methodPairs, AggregationAccessorSlotPair[] accessors, boolean join, TableStateInstanceGrouped tableStateInstance, int[] targetStates, ExprNode[] accessStateExpr, AggregationAgent[] agents, AggregationGroupByRollupDesc groupByRollupDesc) {
        super(tableMetadata, methodPairs, accessors, join, tableStateInstance, targetStates, accessStateExpr, agents);
        this.groupByRollupDesc = groupByRollupDesc;
    }

    public void applyEnterInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = groupByRollupDesc.getLevels()[i];
            Object groupByKey = level.computeMultiKey(groupKeyPerLevel[i], tableMetadata.getKeyTypes().length);
            applyEnterGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public void applyLeaveInternal(EventBean[] eventsPerStream, Object compositeGroupByKey, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] groupKeyPerLevel = (Object[]) compositeGroupByKey;
        for (int i = 0; i < groupKeyPerLevel.length; i++) {
            AggregationGroupByRollupLevel level = groupByRollupDesc.getLevels()[i];
            Object groupByKey = level.computeMultiKey(groupKeyPerLevel[i], tableMetadata.getKeyTypes().length);
            applyLeaveGroupKey(eventsPerStream, groupByKey, exprEvaluatorContext);
        }
    }

    public static CodegenMethodNode applyRollupCodegen(boolean enter, CodegenMethodNode parent, CodegenClassScope classScope, CodegenNamedMethods namedMethods, TableColumnMethodPair[] methodPairs, AggregationAgentForge[] agentForges, AggregationAgent[] agents, int[] targetStates, AggregationGroupByRollupDesc groupByRollupDesc, int numTableKeys) {
        CodegenMember levels = classScope.makeAddMember(AggregationGroupByRollupLevel[].class, groupByRollupDesc.getLevels());
        AggregationAgentCodegenSymbols symbols = new AggregationAgentCodegenSymbols(true, enter);
        CodegenMethodNode method = parent.makeChildWithScope(void.class, AggSvcGroupByWTableImpl.class, symbols, classScope).addParam(EventBean[].class, NAME_EPS).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        CodegenMethodNode applyKey = AggSvcGroupByWTableImpl.applyGroupKeyCodegen(enter, method, classScope, methodPairs, agentForges, agents, targetStates);

        method.getBlock().declareVar(Object[].class, "groupKeyPerLevel", cast(Object[].class, AggregationServiceCodegenNames.REF_GROUPKEY))
                .declareVarNoInit(Object.class, "groupByKey")
                .forLoopIntSimple("i", arrayLength(member(levels.getMemberId())))
                .declareVar(AggregationGroupByRollupLevel.class, "level", arrayAtIndex(member(levels.getMemberId()), ref("i")))
                .assignRef("groupByKey", exprDotMethod(ref("level"), "computeMultiKey", arrayAtIndex(ref("groupKeyPerLevel"), ref("i")), constant(numTableKeys)))
                .localMethod(applyKey, REF_EPS, ref("groupByKey"), REF_EXPREVALCONTEXT);
        return method;
    }

    @Override
    public void setCurrentAccess(Object groupByKey, int agentInstanceId, AggregationGroupByRollupLevel rollupLevel) {
        MultiKeyUntyped key = rollupLevel.computeMultiKey(groupByKey, tableMetadata.getKeyTypes().length);
        ObjectArrayBackedEventBean bean = tableStateInstance.getRowForGroupKey(key);

        if (bean != null) {
            AggregationRowPair row = (AggregationRowPair) bean.getProperties()[0];
            currentAggregatorMethods = row.getMethods();
            currentAggregatorStates = row.getStates();
        } else {
            currentAggregatorMethods = null;
        }

        this.currentGroupKey = key;
    }

    public static void setCurrentAccessRollupCodegen(CodegenMethodNode method, CodegenClassScope classScope, int numTableKeys) {
        method.getBlock().declareVar(MultiKeyUntyped.class, "key", exprDotMethod(AggregationServiceCodegenNames.REF_ROLLUPLEVEL, "computeMultiKey", AggregationServiceCodegenNames.REF_GROUPKEY, constant(numTableKeys)))
                .declareVar(ObjectArrayBackedEventBean.class, "bean", exprDotMethod(REF_TABLESTATEINSTANCE, "getRowForGroupKey", ref("key")))
                .ifRefNotNull("bean")
                .declareVar(AggregationRowPair.class, "row", cast(AggregationRowPair.class, arrayAtIndex(exprDotMethod(ref("bean"), "getProperties"), constant(0))))
                .assignRef(REF_CURRENTAGGREGATORMETHODS, exprDotMethod(ref("row"), "getMethods"))
                .assignRef(REF_CURRENTAGGREGATORSTATES, exprDotMethod(ref("row"), "getStates"))
                .ifElse()
                .assignRef(REF_CURRENTAGGREGATORMETHODS, constantNull())
                .blockEnd()
                .assignRef(REF_CURRENTGROUPKEY, ref("key"));
    }
}
