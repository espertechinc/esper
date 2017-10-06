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
package com.espertech.esper.epl.agg.service.groupby;

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.codegen.AggregationCodegenRowLevelDesc;
import com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames;
import com.espertech.esper.epl.agg.service.common.AggregationRowStateEvalDesc;
import com.espertech.esper.epl.agg.service.common.AggregationServiceCodegenUtil;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactory;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryForge;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.REF_AGGVISITOR;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.REF_GROUPKEY;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.NAME_AGENTINSTANCECONTEXT;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.REF_AGENTINSTANCECONTEXT;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByForge implements AggregationServiceFactoryForge {
    private final static CodegenExpressionRef REF_CURRENTROW = new CodegenExpressionRef("currentRow");
    private final static CodegenExpressionRef REF_CURRENTGROUPKEY = new CodegenExpressionRef("currentGroupKey");
    final static CodegenExpressionRef REF_AGGREGATORSPERGROUP = ref("aggregatorsPerGroup");
    private final static CodegenExpressionRef REF_REMOVEDKEYS = ref("removedKeys");

    protected final AggGroupByDesc aggGroupByDesc;
    protected final TimeAbacus timeAbacus;

    public AggSvcGroupByForge(AggGroupByDesc aggGroupByDesc, TimeAbacus timeAbacus) {
        this.aggGroupByDesc = aggGroupByDesc;
        this.timeAbacus = timeAbacus;
    }

    public AggregationServiceFactory getAggregationServiceFactory(StatementContext stmtContext, boolean isFireAndForget) {
        AggregationRowStateEvalDesc rowStateEvalDesc = aggGroupByDesc.getRowStateForgeDescs().toEval(stmtContext, isFireAndForget);
        if (aggGroupByDesc.isRefcounted()) {
            return new AggSvcGroupByRefcountedFactory(rowStateEvalDesc, aggGroupByDesc.isJoin());
        } else {
            if (!aggGroupByDesc.isReclaimAged()) {
                return new AggSvcGroupByNoReclaimFactory(rowStateEvalDesc, aggGroupByDesc.isJoin());
            }
            return new AggSvcGroupByReclaimAgedFactory(rowStateEvalDesc, aggGroupByDesc.isJoin(), aggGroupByDesc.getReclaimEvaluationFunctionMaxAge(), aggGroupByDesc.getReclaimEvaluationFunctionFrequency());
        }
    }

    public void rowCtorCodegen(CodegenClassScope classScope, CodegenCtor rowCtor, List<CodegenTypedParam> rowMembers, CodegenNamedMethods namedMethods) {
        AggregationServiceCodegenUtil.generateRefCount(hasRefCounting(), namedMethods, rowCtor, rowMembers, classScope);
        if (aggGroupByDesc.isReclaimAged()) {
            AggSvcGroupByReclaimAgedImpl.rowCtorCodegen(namedMethods, classScope, rowMembers);
        }
    }

    public void makeServiceCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONSERVICE, ref("o"), REF_AGENTINSTANCECONTEXT));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope) {
        ctor.getCtorParams().add(new CodegenTypedParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT));
        explicitMembers.add(new CodegenTypedParam(Map.class, REF_AGGREGATORSPERGROUP.getRef()));
        explicitMembers.add(new CodegenTypedParam(Object.class, REF_CURRENTGROUPKEY.getRef()));
        explicitMembers.add(new CodegenTypedParam(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, REF_CURRENTROW.getRef()));
        ctor.getBlock().assignRef(REF_AGGREGATORSPERGROUP, newInstance(HashMap.class));
        if (aggGroupByDesc.isReclaimAged()) {
            AggSvcGroupByReclaimAgedImpl.ctorCodegenReclaim(ctor, explicitMembers, classScope, aggGroupByDesc.getReclaimEvaluationFunctionMaxAge(), aggGroupByDesc.getReclaimEvaluationFunctionFrequency());
        }
        if (hasRefCounting()) {
            explicitMembers.add(new CodegenTypedParam(List.class, REF_REMOVEDKEYS.getRef()));
            ctor.getBlock().assignRef(REF_REMOVEDKEYS, newInstance(ArrayList.class, constant(4)));
        }
    }

    public void getValueCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getValue", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getEventBeanCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getEventBean", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionScalarCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getCollectionScalar", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionOfEventsCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getCollectionOfEvents", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void applyEnterCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (aggGroupByDesc.isReclaimAged()) {
            AggSvcGroupByReclaimAgedImpl.applyEnterCodegenSweep(method, classScope, timeAbacus);
        }

        if (hasRefCounting()) {
            method.getBlock().localMethod(handleRemovedKeysCodegen(method, classScope));
        }

        CodegenBlock block = method.getBlock().assignRef(REF_CURRENTROW, cast(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, exprDotMethod(REF_AGGREGATORSPERGROUP, "get", AggregationServiceCodegenNames.REF_GROUPKEY)));
        CodegenBlock ifNoRow = block.ifCondition(equalsNull(REF_CURRENTROW))
                .assignRef(REF_CURRENTROW, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")))
                .exprDotMethod(REF_AGGREGATORSPERGROUP, "put", AggregationServiceCodegenNames.REF_GROUPKEY, REF_CURRENTROW);
        if (hasRefCounting()) {
            ifNoRow.ifElse().exprDotMethod(REF_CURRENTROW, "increaseRefcount")
                    .blockEnd();
        }
        if (aggGroupByDesc.isReclaimAged()) {
            block.exprDotMethod(REF_CURRENTROW, "setLastUpdateTime", ref("currentTime"));
        }
        block.exprDotMethod(REF_CURRENTROW, "applyEnter", REF_EPS, REF_EXPREVALCONTEXT);
    }

    public void applyLeaveCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenBlock block = method.getBlock().assignRef(REF_CURRENTROW, cast(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, exprDotMethod(REF_AGGREGATORSPERGROUP, "get", AggregationServiceCodegenNames.REF_GROUPKEY)));
        block.ifCondition(equalsNull(REF_CURRENTROW))
                .assignRef(REF_CURRENTROW, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")))
                .exprDotMethod(REF_AGGREGATORSPERGROUP, "put", AggregationServiceCodegenNames.REF_GROUPKEY, REF_CURRENTROW);

        if (hasRefCounting()) {
            block.exprDotMethod(REF_CURRENTROW, "decreaseRefcount");
        }
        if (aggGroupByDesc.isReclaimAged()) {
            block.exprDotMethod(REF_CURRENTROW, "setLastUpdateTime", exprDotMethodChain(REF_EXPREVALCONTEXT).add("getTimeProvider").add("getTime"));
        }
        block.exprDotMethod(REF_CURRENTROW, "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);

        if (hasRefCounting()) {
            block.ifCondition(relational(exprDotMethod(REF_CURRENTROW, "getRefcount"), LE, constant(0)))
                    .exprDotMethod(REF_REMOVEDKEYS, "add", REF_GROUPKEY);
        }
    }

    public void stopMethodCodegen(AggregationServiceFactoryForge forge, CodegenMethodNode method) {
        // no code
    }

    public void setRemovedCallbackCodegen(CodegenMethodNode method) {
        if (aggGroupByDesc.isReclaimAged()) {
            method.getBlock().assignRef("removedCallback", AggregationServiceCodegenNames.REF_CALLBACK);
        }
    }

    public void setCurrentAccessCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().assignRef(REF_CURRENTGROUPKEY, AggregationServiceCodegenNames.REF_GROUPKEY)
                .assignRef(REF_CURRENTROW, cast(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, exprDotMethod(REF_AGGREGATORSPERGROUP, "get", AggregationServiceCodegenNames.REF_GROUPKEY)))
                .ifCondition(equalsNull(REF_CURRENTROW))
                .assignRef(REF_CURRENTROW, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")));
    }

    public void clearResultsCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGREGATORSPERGROUP, "clear");
    }

    public AggregationCodegenRowLevelDesc getRowLevelDesc() {
        return AggregationCodegenRowLevelDesc.fromTopOnly(aggGroupByDesc.getRowStateForgeDescs());
    }

    public void acceptCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitAggregations", exprDotMethod(REF_AGGREGATORSPERGROUP, "size"), REF_AGGREGATORSPERGROUP);
    }

    public void getGroupKeysCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        if (aggGroupByDesc.isRefcounted()) {
            method.getBlock().localMethod(handleRemovedKeysCodegen(method, classScope));
        }
        method.getBlock().methodReturn(exprDotMethod(REF_AGGREGATORSPERGROUP, "keySet"));
    }

    public void getGroupKeyCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(REF_CURRENTGROUPKEY);
    }

    public void acceptGroupDetailCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitGrouped", exprDotMethod(REF_AGGREGATORSPERGROUP, "size"))
                .forEach(Map.Entry.class, "entry", exprDotMethod(REF_AGGREGATORSPERGROUP, "entrySet"))
                .exprDotMethod(REF_AGGVISITOR, "visitGroup", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(ref("entry"), "getValue"));
    }

    public void isGroupedCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantTrue());
    }

    private boolean hasRefCounting() {
        return aggGroupByDesc.isRefcounted() || aggGroupByDesc.isReclaimAged();
    }

    private CodegenMethodNode handleRemovedKeysCodegen(CodegenMethodNode scope, CodegenClassScope classScope) {
        CodegenMethodNode method = scope.makeChild(void.class, AggSvcGroupByForge.class, classScope);
        method.getBlock().ifCondition(not(exprDotMethod(REF_REMOVEDKEYS, "isEmpty")))
                .forEach(Object.class, "removedKey", REF_REMOVEDKEYS)
                    .exprDotMethod(REF_AGGREGATORSPERGROUP, "remove", ref("removedKey"))
                .blockEnd()
                .exprDotMethod(REF_REMOVEDKEYS, "clear");
        return method;
    }
}
