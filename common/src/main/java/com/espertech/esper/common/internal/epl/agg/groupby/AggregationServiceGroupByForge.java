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
package com.espertech.esper.common.internal.epl.agg.groupby;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.*;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.context.module.EPStatementInitServices.GETAGGREGATIONSERVICEFACTORYSERVICE;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.REF_AGGVISITOR;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.REF_GROUPKEY;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.NAME_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggregationServiceGroupByForge implements AggregationServiceFactoryForgeWMethodGen {
    private final static CodegenExpressionMember MEMBER_CURRENTROW = member("currentRow");
    private final static CodegenExpressionMember MEMBER_CURRENTGROUPKEY = member("currentGroupKey");
    final static CodegenExpressionMember MEMBER_AGGREGATORSPERGROUP = member("aggregatorsPerGroup");
    private final static CodegenExpressionMember MEMBER_REMOVEDKEYS = member("removedKeys");

    protected final AggGroupByDesc aggGroupByDesc;
    protected final TimeAbacus timeAbacus;

    protected CodegenExpression reclaimAge;
    protected CodegenExpression reclaimFreq;

    public AggregationServiceGroupByForge(AggGroupByDesc aggGroupByDesc, TimeAbacus timeAbacus) {
        this.aggGroupByDesc = aggGroupByDesc;
        this.timeAbacus = timeAbacus;
    }

    public void providerCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        Class[] groupByTypes = ExprNodeUtilityQuery.getExprResultTypes(aggGroupByDesc.getGroupByNodes());

        if (aggGroupByDesc.isReclaimAged()) {
            reclaimAge = aggGroupByDesc.getReclaimEvaluationFunctionMaxAge().make(classScope);
            reclaimFreq = aggGroupByDesc.getReclaimEvaluationFunctionFrequency().make(classScope);
        } else {
            reclaimAge = constantNull();
            reclaimFreq = constantNull();
        }

        CodegenExpressionField timeAbacus = classScope.addOrGetFieldSharable(TimeAbacusField.INSTANCE);

        method.getBlock()
            .declareVar(AggregationRowFactory.class, "rowFactory", CodegenExpressionBuilder.newInstance(classNames.getRowFactoryTop(), ref("this")))
            .declareVar(DataInputOutputSerde.class, "rowSerde", CodegenExpressionBuilder.newInstance(classNames.getRowSerdeTop(), ref("this")))
            .declareVar(AggregationServiceFactory.class, "svcFactory", CodegenExpressionBuilder.newInstance(classNames.getServiceFactory(), ref("this")))
            .declareVar(DataInputOutputSerde.class, "serde", aggGroupByDesc.getGroupByMultiKey().getExprMKSerde(method, classScope))
            .methodReturn(exprDotMethodChain(EPStatementInitServices.REF).add(GETAGGREGATIONSERVICEFACTORYSERVICE).add(
                "groupBy", ref("svcFactory"), ref("rowFactory"), aggGroupByDesc.getRowStateForgeDescs().getUseFlags().toExpression(),
                ref("rowSerde"), constant(groupByTypes), reclaimAge, reclaimFreq, timeAbacus, ref("serde")));
    }

    public void rowCtorCodegen(AggregationRowCtorDesc rowCtorDesc) {
        AggregationServiceCodegenUtil.generateIncidentals(hasRefCounting(), aggGroupByDesc.isReclaimAged(), rowCtorDesc);
    }

    public void rowWriteMethodCodegen(CodegenMethod method, int level) {
        if (hasRefCounting()) {
            method.getBlock().exprDotMethod(ref("output"), "writeInt", ref("row.refcount"));
        }
        if (aggGroupByDesc.isReclaimAged()) {
            method.getBlock().exprDotMethod(ref("output"), "writeLong", ref("row.lastUpd"));
        }
    }

    public void rowReadMethodCodegen(CodegenMethod method, int level) {
        if (hasRefCounting()) {
            method.getBlock().assignRef("row.refcount", exprDotMethod(ref("input"), "readInt"));
        }
        if (aggGroupByDesc.isReclaimAged()) {
            method.getBlock().assignRef("row.lastUpd", exprDotMethod(ref("input"), "readLong"));
        }
    }

    public void makeServiceCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().methodReturn(CodegenExpressionBuilder.newInstance(classNames.getService(), ref("o"), MEMBER_AGENTINSTANCECONTEXT));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope, AggregationClassNames classNames) {
        ctor.getCtorParams().add(new CodegenTypedParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT));
        explicitMembers.add(new CodegenTypedParam(Map.class, MEMBER_AGGREGATORSPERGROUP.getRef()));
        explicitMembers.add(new CodegenTypedParam(Object.class, MEMBER_CURRENTGROUPKEY.getRef()));
        explicitMembers.add(new CodegenTypedParam(classNames.getRowTop(), MEMBER_CURRENTROW.getRef()));
        ctor.getBlock().assignRef(MEMBER_AGGREGATORSPERGROUP, newInstance(HashMap.class));
        if (aggGroupByDesc.isReclaimAged()) {
            AggSvcGroupByReclaimAgedImpl.ctorCodegenReclaim(ctor, explicitMembers, classScope, reclaimAge, reclaimFreq);
        }
        if (hasRefCounting()) {
            explicitMembers.add(new CodegenTypedParam(List.class, MEMBER_REMOVEDKEYS.getRef()));
            ctor.getBlock().assignRef(MEMBER_REMOVEDKEYS, newInstance(ArrayList.class, constant(4)));
        }
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(MEMBER_CURRENTROW, "getValue", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getEventBeanCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(MEMBER_CURRENTROW, "getEventBean", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionScalarCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(MEMBER_CURRENTROW, "getCollectionScalar", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionOfEventsCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(MEMBER_CURRENTROW, "getCollectionOfEvents", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void applyEnterCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
        method.getBlock()
            .apply(instblock(classScope, "qAggregationGroupedApplyEnterLeave", constantTrue(), constant(aggGroupByDesc.getNumMethods()), constant(aggGroupByDesc.getNumAccess()), REF_GROUPKEY));

        if (aggGroupByDesc.isReclaimAged()) {
            AggSvcGroupByReclaimAgedImpl.applyEnterCodegenSweep(method, classScope, classNames);
        }

        if (hasRefCounting()) {
            method.getBlock().localMethod(handleRemovedKeysCodegen(method, classScope));
        }

        CodegenBlock block = method.getBlock().assignRef(MEMBER_CURRENTROW, cast(classNames.getRowTop(), exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "get", REF_GROUPKEY)));
        block.ifCondition(equalsNull(MEMBER_CURRENTROW))
            .assignRef(MEMBER_CURRENTROW, CodegenExpressionBuilder.newInstance(classNames.getRowTop()))
            .exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "put", REF_GROUPKEY, MEMBER_CURRENTROW);

        if (hasRefCounting()) {
            block.exprDotMethod(MEMBER_CURRENTROW, "increaseRefcount");
        }
        if (aggGroupByDesc.isReclaimAged()) {
            block.exprDotMethod(MEMBER_CURRENTROW, "setLastUpdateTime", ref("currentTime"));
        }

        block.exprDotMethod(MEMBER_CURRENTROW, "applyEnter", REF_EPS, REF_EXPREVALCONTEXT)
            .apply(instblock(classScope, "aAggregationGroupedApplyEnterLeave", constantTrue()));
    }

    public void applyLeaveCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
        method.getBlock()
            .apply(instblock(classScope, "qAggregationGroupedApplyEnterLeave", constantFalse(), constant(aggGroupByDesc.getNumMethods()), constant(aggGroupByDesc.getNumAccess()), REF_GROUPKEY))
            .assignRef(MEMBER_CURRENTROW, cast(classNames.getRowTop(), exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "get", REF_GROUPKEY)))
            .ifCondition(equalsNull(MEMBER_CURRENTROW))
            .assignRef(MEMBER_CURRENTROW, CodegenExpressionBuilder.newInstance(classNames.getRowTop()))
            .exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "put", REF_GROUPKEY, MEMBER_CURRENTROW);

        if (hasRefCounting()) {
            method.getBlock().exprDotMethod(MEMBER_CURRENTROW, "decreaseRefcount");
        }
        if (aggGroupByDesc.isReclaimAged()) {
            method.getBlock().exprDotMethod(MEMBER_CURRENTROW, "setLastUpdateTime", exprDotMethodChain(REF_EXPREVALCONTEXT).add("getTimeProvider").add("getTime"));
        }
        method.getBlock().exprDotMethod(MEMBER_CURRENTROW, "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);

        if (hasRefCounting()) {
            method.getBlock().ifCondition(relational(exprDotMethod(MEMBER_CURRENTROW, "getRefcount"), LE, constant(0)))
                .exprDotMethod(MEMBER_REMOVEDKEYS, "add", REF_GROUPKEY);
        }

        method.getBlock().apply(instblock(classScope, "aAggregationGroupedApplyEnterLeave", constantFalse()));
    }

    public void stopMethodCodegen(AggregationServiceFactoryForgeWMethodGen forge, CodegenMethod method) {
        // no code
    }

    public void setRemovedCallbackCodegen(CodegenMethod method) {
        if (aggGroupByDesc.isReclaimAged()) {
            method.getBlock().assignRef("removedCallback", AggregationServiceCodegenNames.REF_CALLBACK);
        }
    }

    public void setCurrentAccessCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().assignRef(MEMBER_CURRENTGROUPKEY, REF_GROUPKEY)
            .assignRef(MEMBER_CURRENTROW, cast(classNames.getRowTop(), exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "get", REF_GROUPKEY)))
            .ifCondition(equalsNull(MEMBER_CURRENTROW))
            .assignRef(MEMBER_CURRENTROW, CodegenExpressionBuilder.newInstance(classNames.getRowTop()));
    }

    public void clearResultsCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "clear");
    }

    public AggregationCodegenRowLevelDesc getRowLevelDesc() {
        return AggregationCodegenRowLevelDesc.fromTopOnly(aggGroupByDesc.getRowStateForgeDescs());
    }

    public void acceptCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitAggregations", exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "size"), MEMBER_AGGREGATORSPERGROUP);
    }

    public void getGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope) {
        if (aggGroupByDesc.isRefcounted()) {
            method.getBlock().localMethod(handleRemovedKeysCodegen(method, classScope));
        }
        method.getBlock().methodReturn(exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "keySet"));
    }

    public void getGroupKeyCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(MEMBER_CURRENTGROUPKEY);
    }

    public void acceptGroupDetailCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitGrouped", exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "size"))
            .forEach(Map.Entry.class, "entry", exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "entrySet"))
            .exprDotMethod(REF_AGGVISITOR, "visitGroup", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(ref("entry"), "getValue"));
    }

    public void isGroupedCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantTrue());
    }

    public void getRowCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(MEMBER_CURRENTROW);
    }

    private boolean hasRefCounting() {
        return aggGroupByDesc.isRefcounted() || aggGroupByDesc.isReclaimAged();
    }

    private CodegenMethod handleRemovedKeysCodegen(CodegenMethod scope, CodegenClassScope classScope) {
        CodegenMethod method = scope.makeChild(void.class, AggregationServiceGroupByForge.class, classScope);
        method.getBlock().ifCondition(not(exprDotMethod(MEMBER_REMOVEDKEYS, "isEmpty")))
            .forEach(Object.class, "removedKey", MEMBER_REMOVEDKEYS)
            .exprDotMethod(MEMBER_AGGREGATORSPERGROUP, "remove", ref("removedKey"))
            .blockEnd()
            .exprDotMethod(MEMBER_REMOVEDKEYS, "clear");
        return method;
    }
}
