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
package com.espertech.esper.common.internal.epl.resultset.agggrouped;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAll;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.generateGroupKeyArrayViewCodegen;

/**
 * Result-set processor prototype for the aggregate-grouped case:
 * there is a group-by and one or more non-aggregation event properties in the select clause are not listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorAggregateGroupedForge implements ResultSetProcessorFactoryForge {
    private final EventType resultEventType;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprForge optionalHavingNode;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean isHistoricalOnly;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final EventType[] eventTypes;
    private final OutputConditionPolledFactoryForge optionalOutputFirstConditionFactory;
    private final Class[] groupKeyTypes;
    private final MultiKeyClassRef multiKeyClassRef;

    private CodegenMethod generateGroupKeySingle;
    private CodegenMethod generateGroupKeyArrayView;
    private CodegenMethod generateGroupKeyArrayJoin;

    public ResultSetProcessorAggregateGroupedForge(EventType resultEventType,
                                                   ExprNode[] groupKeyNodeExpressions,
                                                   ExprForge optionalHavingNode,
                                                   boolean isSelectRStream,
                                                   boolean isUnidirectional,
                                                   OutputLimitSpec outputLimitSpec,
                                                   boolean isSorting,
                                                   boolean isHistoricalOnly,
                                                   ResultSetProcessorOutputConditionType outputConditionType,
                                                   OutputConditionPolledFactoryForge optionalOutputFirstConditionFactory,
                                                   EventType[] eventTypes,
                                                   MultiKeyClassRef multiKeyClassRef) {
        this.resultEventType = resultEventType;
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.optionalHavingNode = optionalHavingNode;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
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

    public ExprForge getOptionalHavingNode() {
        return optionalHavingNode;
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

    public boolean isHistoricalOnly() {
        return isHistoricalOnly;
    }

    public OutputConditionPolledFactoryForge getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public boolean isOutputLast() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.LAST;
    }

    public boolean isOutputAll() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.ALL;
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

    public Class[] getGroupKeyTypes() {
        return groupKeyTypes;
    }

    public Class getInterfaceClass() {
        return ResultSetProcessorAggregateGrouped.class;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        instance.getMethods().addMethod(SelectExprProcessor.class, "getSelectExprProcessor", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_SELECTEXPRPROCESSOR));
        instance.getMethods().addMethod(AggregationService.class, "getAggregationService", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_AGGREGATIONSVC));
        instance.getMethods().addMethod(AgentInstanceContext.class, "getAgentInstanceContext", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_AGENTINSTANCECONTEXT));
        instance.getMethods().addMethod(boolean.class, "hasHavingClause", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(constant(optionalHavingNode != null)));
        instance.getMethods().addMethod(boolean.class, "isSelectRStream", Collections.emptyList(), ResultSetProcessorRowForAll.class, classScope, methodNode -> methodNode.getBlock().methodReturn(constant(isSelectRStream)));
        ResultSetProcessorUtil.evaluateHavingClauseCodegen(optionalHavingNode, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.removedAggregationGroupKeyCodegen(classScope, instance);

        generateGroupKeySingle = ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(groupKeyNodeExpressions, multiKeyClassRef, classScope, instance);
        generateGroupKeyArrayView = generateGroupKeyArrayViewCodegen(generateGroupKeySingle, classScope, instance);
        generateGroupKeyArrayJoin = ResultSetProcessorGroupedUtil.generateGroupKeyArrayJoinCodegen(generateGroupKeySingle, classScope, instance);

        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedSingleCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedViewUnkeyedCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedJoinUnkeyedCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedJoinPerKeyCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedViewPerKeyCodegen(this, classScope, instance);
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processViewResultCodegen(this, classScope, method, instance);
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processJoinResultCodegen(this, classScope, method, instance);
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.getIteratorViewCodegen(this, classScope, method, instance);
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.getIteratorJoinCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedJoinCodegen(this, classScope, method, instance);
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.applyViewResultCodegen(this, classScope, method, instance);
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.applyJoinResultCodegen(this, classScope, method, instance);
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.continueOutputLimitedLastAllNonBufferedViewCodegen(this, method);
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.continueOutputLimitedLastAllNonBufferedJoinCodegen(this, method);
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedLastAllNonBufferedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedLastAllNonBufferedJoinCodegen(this, classScope, method, instance);
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.acceptHelperVisitorCodegen(method, instance);
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.stopMethodCodegen(method, instance);
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethod method) {
        ResultSetProcessorAggregateGroupedImpl.clearMethodCodegen(method);
    }

    public String getInstrumentedQName() {
        return "ResultSetProcessGroupedRowPerEvent";
    }

    public CodegenMethod getGenerateGroupKeySingle() {
        return generateGroupKeySingle;
    }

    public CodegenMethod getGenerateGroupKeyArrayView() {
        return generateGroupKeyArrayView;
    }

    public CodegenMethod getGenerateGroupKeyArrayJoin() {
        return generateGroupKeyArrayJoin;
    }

    public MultiKeyClassRef getMultiKeyClassRef() {
        return multiKeyClassRef;
    }
}
