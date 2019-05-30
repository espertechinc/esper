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
package com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDesc;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDescForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupPerLevelForge;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAll;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen;

/**
 * Result set processor prototype for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorRowPerGroupRollupForge implements ResultSetProcessorFactoryForge {
    private final EventType resultEventType;
    private final GroupByRollupPerLevelForge perLevelForges;
    private final ExprNode[] groupKeyNodeExpressions;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final AggregationGroupByRollupDescForge groupByRollupDesc;
    private final boolean isJoin;
    private final boolean isHistoricalOnly;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final OutputConditionPolledFactoryForge optionalOutputFirstConditionFactory;
    private final EventType[] eventTypes;
    private final Class[] groupKeyTypes;
    private final boolean unbounded;
    private final MultiKeyClassRef multiKeyClassRef;

    private CodegenMethod generateGroupKeySingle;

    public ResultSetProcessorRowPerGroupRollupForge(EventType resultEventType,
                                                    GroupByRollupPerLevelForge perLevelForges,
                                                    ExprNode[] groupKeyNodeExpressions,
                                                    boolean isSelectRStream,
                                                    boolean isUnidirectional,
                                                    OutputLimitSpec outputLimitSpec,
                                                    boolean isSorting,
                                                    boolean noDataWindowSingleStream,
                                                    AggregationGroupByRollupDescForge groupByRollupDesc,
                                                    boolean isJoin,
                                                    boolean isHistoricalOnly,
                                                    boolean iterateUnbounded,
                                                    ResultSetProcessorOutputConditionType outputConditionType,
                                                    OutputConditionPolledFactoryForge optionalOutputFirstConditionFactory,
                                                    EventType[] eventTypes,
                                                    MultiKeyClassRef multiKeyClassRef) {
        this.resultEventType = resultEventType;
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.perLevelForges = perLevelForges;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        boolean noDataWindowSingleSnapshot = iterateUnbounded || (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT && noDataWindowSingleStream);
        this.unbounded = noDataWindowSingleSnapshot && !isHistoricalOnly;
        this.groupByRollupDesc = groupByRollupDesc;
        this.isJoin = isJoin;
        this.isHistoricalOnly = isHistoricalOnly;
        this.outputConditionType = outputConditionType;
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
        this.eventTypes = eventTypes;
        this.groupKeyTypes = ExprNodeUtilityQuery.getExprResultTypes(groupKeyNodeExpressions);
        this.multiKeyClassRef = multiKeyClassRef;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public boolean isSorting() {
        return isSorting;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public boolean isUnidirectional() {
        return isUnidirectional;
    }

    public OutputLimitSpec getOutputLimitSpec() {
        return outputLimitSpec;
    }

    public ExprNode[] getGroupKeyNodeExpressions() {
        return groupKeyNodeExpressions;
    }

    public AggregationGroupByRollupDescForge getGroupByRollupDesc() {
        return groupByRollupDesc;
    }

    public GroupByRollupPerLevelForge getPerLevelForges() {
        return perLevelForges;
    }

    public boolean isJoin() {
        return isJoin;
    }

    public boolean isHistoricalOnly() {
        return isHistoricalOnly;
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public int getNumStreams() {
        return eventTypes.length;
    }

    public EventType[] getEventTypes() {
        return eventTypes;
    }

    public Class getInterfaceClass() {
        return ResultSetProcessorRowPerGroupRollup.class;
    }

    public OutputConditionPolledFactoryForge getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        instance.getMethods().addMethod(AggregationService.class, "getAggregationService", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_AGGREGATIONSVC));
        instance.getMethods().addMethod(AgentInstanceContext.class, "getAgentInstanceContext", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_AGENTINSTANCECONTEXT));
        instance.getMethods().addMethod(boolean.class, "isSelectRStream", Collections.emptyList(), ResultSetProcessorRowForAll.class, classScope, methodNode -> methodNode.getBlock().methodReturn(constant(isSelectRStream)));

        CodegenExpressionField rollupDesc = classScope.addFieldUnshared(true, AggregationGroupByRollupDesc.class, groupByRollupDesc.codegen(classScope.getPackageScope().getInitMethod(), classScope));
        instance.getMethods().addMethod(AggregationGroupByRollupDesc.class, "getGroupByRollupDesc", Collections.emptyList(), ResultSetProcessorRowPerGroupRollup.class, classScope, methodNode -> methodNode.getBlock().methodReturn(rollupDesc));

        generateGroupKeySingle = generateGroupKeySingleCodegen(getGroupKeyNodeExpressions(), multiKeyClassRef, classScope, instance);
        ResultSetProcessorRowPerGroupRollupImpl.removedAggregationGroupKeyCodegen(classScope, instance);
        ResultSetProcessorRowPerGroupRollupImpl.generateOutputBatchedMapUnsortedCodegen(this, instance, classScope);
        ResultSetProcessorRowPerGroupRollupImpl.generateOutputBatchedCodegen(this, instance, classScope);

        // generate having clauses
        ExprForge[] havingForges = perLevelForges.getOptionalHavingForges();
        if (havingForges != null) {
            factoryMembers.add(new CodegenTypedParam(HavingClauseEvaluator[].class, NAME_HAVINGEVALUATOR_ARRAYNONMEMBER));
            factoryCtor.getBlock().assignRef(NAME_HAVINGEVALUATOR_ARRAYNONMEMBER, newArrayByLength(HavingClauseEvaluator.class, constant(havingForges.length)));
            for (int i = 0; i < havingForges.length; i++) {
                CodegenExpressionNewAnonymousClass impl = newAnonymousClass(factoryCtor.getBlock(), HavingClauseEvaluator.class);
                CodegenMethod evaluateHaving = CodegenMethod.makeParentNode(boolean.class, this.getClass(), classScope).addParam(ExprForgeCodegenNames.PARAMS);
                impl.addMethod("evaluateHaving", evaluateHaving);
                evaluateHaving.getBlock().methodReturn(CodegenLegoMethodExpression.codegenBooleanExpressionReturnTrueFalse(havingForges[i], classScope, factoryCtor, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                factoryCtor.getBlock().assignArrayElement(NAME_HAVINGEVALUATOR_ARRAYNONMEMBER, constant(i), impl);
            }
        }
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.processViewResultUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.processViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processJoinResultCodegen(this, classScope, method, instance);
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.getIteratorViewUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.getIteratorViewCodegen(this, classScope, method, instance);
        }
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.getIteratorJoinCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedJoinCodegen(this, classScope, method, instance);
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.applyViewResultUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.applyViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.applyJoinResultCodegen(this, classScope, method, instance);
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.continueOutputLimitedLastAllNonBufferedViewCodegen(this, method);
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.continueOutputLimitedLastAllNonBufferedJoinCodegen(this, method);
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedLastAllNonBufferedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedLastAllNonBufferedJoinCodegen(this, classScope, method, instance);
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.acceptHelperVisitorCodegen(method, instance);
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.stopMethodUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.stopMethodCodegenBound(method, instance);
        }
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethod method) {
        ResultSetProcessorRowPerGroupRollupImpl.clearMethodCodegen(method);
    }

    public Class[] getGroupKeyTypes() {
        return groupKeyTypes;
    }

    public String getInstrumentedQName() {
        return "ResultSetProcessGroupedRowPerGroup";
    }

    public CodegenMethod getGenerateGroupKeySingle() {
        return generateGroupKeySingle;
    }
}
