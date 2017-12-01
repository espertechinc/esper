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
package com.espertech.esper.epl.core.resultset.agggrouped;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.resultset.core.*;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.epl.core.resultset.rowforall.ResultSetProcessorRowForAll;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorCompiler;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.OutputLimitSpec;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;

/**
 * Result-set processor prototype for the aggregate-grouped case:
 * there is a group-by and one or more non-aggregation event properties in the select clause are not listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorAggregateGroupedForge implements ResultSetProcessorFactoryForge {
    private final EventType resultEventType;
    private final SelectExprProcessorForge selectExprProcessorForge;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprForge optionalHavingNode;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean isHistoricalOnly;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final OutputConditionPolledFactory optionalOutputFirstConditionFactory;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final int numStreams;
    private final Class[] groupKeyTypes;

    public ResultSetProcessorAggregateGroupedForge(EventType resultEventType,
                                                   SelectExprProcessorForge selectExprProcessorForge,
                                                   ExprNode[] groupKeyNodeExpressions,
                                                   ExprForge optionalHavingNode,
                                                   boolean isSelectRStream,
                                                   boolean isUnidirectional,
                                                   OutputLimitSpec outputLimitSpec,
                                                   boolean isSorting,
                                                   boolean isHistoricalOnly,
                                                   ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                                   OutputConditionPolledFactory optionalOutputFirstConditionFactory,
                                                   ResultSetProcessorOutputConditionType outputConditionType,
                                                   int numStreams) {
        this.resultEventType = resultEventType;
        this.selectExprProcessorForge = selectExprProcessorForge;
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.optionalHavingNode = optionalHavingNode;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        this.isHistoricalOnly = isHistoricalOnly;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
        this.outputConditionType = outputConditionType;
        this.numStreams = numStreams;
        this.groupKeyTypes = ExprNodeUtilityCore.getExprResultTypes(groupKeyNodeExpressions);
    }

    public ResultSetProcessorFactory getResultSetProcessorFactory(StatementContext stmtContext, boolean isFireAndForget) {
        SelectExprProcessor selectExprProcessor = SelectExprProcessorCompiler.allocateSelectExprEvaluator(stmtContext.getEventAdapterService(), selectExprProcessorForge, stmtContext.getEngineImportService(), ResultSetProcessorFactoryFactory.class, isFireAndForget, stmtContext.getStatementName());

        ExprEvaluator groupKeyNode;
        ExprEvaluator[] groupKeyNodes;
        if (groupKeyNodeExpressions.length == 1) {
            groupKeyNode = ExprNodeCompiler.allocateEvaluator(groupKeyNodeExpressions[0].getForge(), stmtContext.getEngineImportService(), this.getClass(), isFireAndForget, stmtContext.getStatementName());
            groupKeyNodes = null;
        } else {
            groupKeyNode = null;
            groupKeyNodes = ExprNodeUtilityRich.getEvaluatorsMayCompile(groupKeyNodeExpressions, stmtContext.getEngineImportService(), this.getClass(), isFireAndForget, stmtContext.getStatementName());
        }

        ExprEvaluator optionalHavingEval = optionalHavingNode == null ? null : ExprNodeCompiler.allocateEvaluator(optionalHavingNode, stmtContext.getEngineImportService(), this.getClass(), isFireAndForget, stmtContext.getStatementName());

        return new ResultSetProcessorAggregateGroupedFactory(resultEventType, selectExprProcessor, groupKeyNodeExpressions, groupKeyNode, groupKeyNodes, optionalHavingEval, isSelectRStream, isUnidirectional, outputLimitSpec,
                isSorting, isHistoricalOnly, resultSetProcessorHelperFactory, optionalOutputFirstConditionFactory, outputConditionType, numStreams);
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

    public boolean isOutputLast() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.LAST;
    }

    public boolean isOutputAll() {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.ALL;
    }

    public OutputConditionPolledFactory getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public Class[] getGroupKeyTypes() {
        return groupKeyTypes;
    }

    public Class getInterfaceClass() {
        return ResultSetProcessorAggregateGrouped.class;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        instance.getMethods().addMethod(SelectExprProcessor.class, "getSelectExprProcessor", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_SELECTEXPRPROCESSOR));
        instance.getMethods().addMethod(AggregationService.class, "getAggregationService", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_AGGREGATIONSVC));
        instance.getMethods().addMethod(AgentInstanceContext.class, "getAgentInstanceContext", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_AGENTINSTANCECONTEXT));
        instance.getMethods().addMethod(boolean.class, "hasHavingClause", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(constant(optionalHavingNode != null)));
        instance.getMethods().addMethod(boolean.class, "isSelectRStream", Collections.emptyList(), ResultSetProcessorRowForAll.class, classScope, methodNode -> methodNode.getBlock().methodReturn(constant(isSelectRStream)));
        ResultSetProcessorUtil.evaluateHavingClauseCodegen(optionalHavingNode, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.removedAggregationGroupKeyCodegen(classScope, instance);

        ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(groupKeyNodeExpressions, classScope, instance);
        ResultSetProcessorGroupedUtil.generateGroupKeyArrayViewCodegen(groupKeyNodeExpressions, classScope, instance);
        ResultSetProcessorGroupedUtil.generateGroupKeyArrayJoinCodegen(groupKeyNodeExpressions, classScope, instance);

        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedSingleCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedViewUnkeyedCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedJoinUnkeyedCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedJoinPerKeyCodegen(this, classScope, instance);
        ResultSetProcessorAggregateGroupedImpl.generateOutputBatchedViewPerKeyCodegen(this, classScope, instance);
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processViewResultCodegen(this, classScope, method, instance);
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processJoinResultCodegen(this, classScope, method, instance);
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.getIteratorViewCodegen(this, classScope, method, instance);
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.getIteratorJoinCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedJoinCodegen(this, classScope, method, instance);
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.applyViewResultCodegen(this, classScope, method, instance);
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.applyJoinResultCodegen(this, classScope, method, instance);
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.continueOutputLimitedLastAllNonBufferedViewCodegen(this, method);
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.continueOutputLimitedLastAllNonBufferedJoinCodegen(this, method);
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedLastAllNonBufferedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.processOutputLimitedLastAllNonBufferedJoinCodegen(this, classScope, method, instance);
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.acceptHelperVisitorCodegen(method, instance);
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorAggregateGroupedImpl.stopMethodCodegen(method, instance);
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method) {
        ResultSetProcessorAggregateGroupedImpl.clearMethodCodegen(method);

    }
}
