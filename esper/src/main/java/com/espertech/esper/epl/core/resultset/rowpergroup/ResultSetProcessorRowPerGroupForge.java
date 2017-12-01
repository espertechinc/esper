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
package com.espertech.esper.epl.core.resultset.rowpergroup;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.codegen.core.CodegenCtor;
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
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.spec.OutputLimitSpec;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;

/**
 * Result set processor prototype for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorRowPerGroupForge implements ResultSetProcessorFactoryForge {
    private final static String NAME_GROUPREPS = "groupReps";

    private final EventType resultEventType;
    private final SelectExprProcessorForge selectExprProcessorForge;
    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprForge optionalHavingNode;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean unboundedProcessor;
    private final boolean isHistoricalOnly;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final int numStreams;
    private final OutputConditionPolledFactory optionalOutputFirstConditionFactory;
    private final Class[] groupKeyTypes;

    public ResultSetProcessorRowPerGroupForge(EventType resultEventType,
                                              SelectExprProcessorForge selectExprProcessorForge,
                                              ExprNode[] groupKeyNodeExpressions,
                                              ExprForge optionalHavingNode,
                                              boolean isSelectRStream,
                                              boolean isUnidirectional,
                                              OutputLimitSpec outputLimitSpec,
                                              boolean isSorting,
                                              boolean noDataWindowSingleStream,
                                              boolean isHistoricalOnly,
                                              boolean iterateUnbounded,
                                              ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                              ResultSetProcessorOutputConditionType outputConditionType,
                                              int numStreams,
                                              OutputConditionPolledFactory optionalOutputFirstConditionFactory) {
        this.resultEventType = resultEventType;
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.selectExprProcessorForge = selectExprProcessorForge;
        this.optionalHavingNode = optionalHavingNode;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        boolean noDataWindowSingleSnapshot = iterateUnbounded || (outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT && noDataWindowSingleStream);
        this.unboundedProcessor = noDataWindowSingleSnapshot && !isHistoricalOnly;
        this.isHistoricalOnly = isHistoricalOnly;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.outputConditionType = outputConditionType;
        this.numStreams = numStreams;
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
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

        return new ResultSetProcessorRowPerGroupFactory(resultEventType, selectExprProcessor, groupKeyNodeExpressions, groupKeyNode, groupKeyNodes, optionalHavingEval, isSelectRStream, isUnidirectional, outputLimitSpec, isSorting, isHistoricalOnly, resultSetProcessorHelperFactory, outputConditionType, numStreams, optionalOutputFirstConditionFactory, unboundedProcessor, groupKeyTypes);
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

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public OutputConditionPolledFactory getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public Class getInterfaceClass() {
        return ResultSetProcessorRowPerGroup.class;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        instance.getMethods().addMethod(SelectExprProcessor.class, "getSelectExprProcessor", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_SELECTEXPRPROCESSOR));
        instance.getMethods().addMethod(AggregationService.class, "getAggregationService", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_AGGREGATIONSVC));
        instance.getMethods().addMethod(AgentInstanceContext.class, "getAgentInstanceContext", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_AGENTINSTANCECONTEXT));
        instance.getMethods().addMethod(boolean.class, "hasHavingClause", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(constant(optionalHavingNode != null)));
        instance.getMethods().addMethod(boolean.class, "isSelectRStream", Collections.emptyList(), ResultSetProcessorRowForAll.class, classScope, methodNode -> methodNode.getBlock().methodReturn(constant(isSelectRStream)));
        ResultSetProcessorUtil.evaluateHavingClauseCodegen(optionalHavingNode, classScope, instance);
        ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(getGroupKeyNodeExpressions(), classScope, instance);
        ResultSetProcessorRowPerGroupImpl.generateOutputBatchedNoSortWMapCodegen(this, classScope, instance);
        ResultSetProcessorRowPerGroupImpl.generateOutputBatchedArrFromIteratorCodegen(this, classScope, instance);
        ResultSetProcessorRowPerGroupImpl.removedAggregationGroupKeyCodegen(classScope, instance);

        if (unboundedProcessor) {
            CodegenMember factory = classScope.makeAddMember(ResultSetProcessorHelperFactory.class, resultSetProcessorHelperFactory);
            CodegenMember groupKeyTypesMember = classScope.makeAddMember(Class[].class, groupKeyTypes);
            instance.addMember(NAME_GROUPREPS, ResultSetProcessorRowPerGroupUnboundHelper.class);
            instance.getServiceCtor().getBlock().assignRef(NAME_GROUPREPS, exprDotMethod(member(factory.getMemberId()), "makeRSRowPerGroupUnboundGroupRep", REF_AGENTINSTANCECONTEXT, member(groupKeyTypesMember.getMemberId())))
                .exprDotMethod(REF_AGGREGATIONSVC, "setRemovedCallback", ref(NAME_GROUPREPS));
        } else {
            instance.getServiceCtor().getBlock().exprDotMethod(REF_AGGREGATIONSVC, "setRemovedCallback", ref("this"));
        }
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.processViewResultUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.processViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processJoinResultCodegen(this, classScope, method, instance);
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.getIteratorViewUnboundedCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.getIteratorViewCodegen(this, classScope, method, instance);
        }
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.getIteratorJoinCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedJoinCodegen(this, classScope, method, instance);
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.applyViewResultCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.applyViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.applyJoinResultCodegen(this, classScope, method, instance);
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.continueOutputLimitedLastAllNonBufferedViewCodegen(this, method);
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.continueOutputLimitedLastAllNonBufferedJoinCodegen(this, method);
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedLastAllNonBufferedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedLastAllNonBufferedJoinCodegen(this, classScope, method, instance);
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.acceptHelperVisitorCodegen(method, instance);
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.stopMethodCodegenUnbound(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.stopMethodCodegenBound(method, instance);
        }
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method) {
        ResultSetProcessorRowPerGroupImpl.clearMethodCodegen(method);
    }

    public Class[] getGroupKeyTypes() {
        return groupKeyTypes;
    }
}
