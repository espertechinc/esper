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
package com.espertech.esper.epl.core.resultset.rowpergrouprollup;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPerLevelExpression;
import com.espertech.esper.epl.agg.rollup.GroupByRollupPerLevelForge;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.core.resultset.core.*;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.epl.core.resultset.rowforall.ResultSetProcessorRowForAll;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorCompiler;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames;
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

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames.REF_EPS;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;

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
    private final AggregationGroupByRollupDesc groupByRollupDesc;
    private final boolean isJoin;
    private final boolean isHistoricalOnly;
    private final OutputConditionPolledFactory optionalOutputFirstConditionFactory;
    private final ResultSetProcessorHelperFactory resultSetProcessorHelperFactory;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final int numStreams;
    private final Class[] groupKeyTypes;
    private final boolean unbounded;

    public ResultSetProcessorRowPerGroupRollupForge(EventType resultEventType,
                                                    GroupByRollupPerLevelForge perLevelForges,
                                                    ExprNode[] groupKeyNodeExpressions,
                                                    boolean isSelectRStream,
                                                    boolean isUnidirectional,
                                                    OutputLimitSpec outputLimitSpec,
                                                    boolean isSorting,
                                                    boolean noDataWindowSingleStream,
                                                    AggregationGroupByRollupDesc groupByRollupDesc,
                                                    boolean isJoin,
                                                    boolean isHistoricalOnly,
                                                    boolean iterateUnbounded,
                                                    OutputConditionPolledFactory optionalOutputFirstConditionFactory,
                                                    ResultSetProcessorHelperFactory resultSetProcessorHelperFactory,
                                                    ResultSetProcessorOutputConditionType outputConditionType,
                                                    int numStreams) {
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
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
        this.resultSetProcessorHelperFactory = resultSetProcessorHelperFactory;
        this.outputConditionType = outputConditionType;
        this.numStreams = numStreams;
        this.groupKeyTypes = ExprNodeUtilityCore.getExprResultTypes(groupKeyNodeExpressions);
    }

    public ResultSetProcessorFactory getResultSetProcessorFactory(StatementContext stmtContext, boolean isFireAndForget) {
        ExprForge[] havingForges = perLevelForges.getOptionalHavingForges();
        ExprEvaluator[] havingEvals = null;
        if (havingForges != null) {
            havingEvals = new ExprEvaluator[havingForges.length];
            for (int i = 0; i < havingForges.length; i++) {
                havingEvals[i] = havingForges[i] == null ? null : ExprNodeCompiler.allocateEvaluator(havingForges[i], stmtContext.getEngineImportService(), this.getClass(), isFireAndForget, stmtContext.getStatementName());
            }
        }

        SelectExprProcessorForge[] selectForges = perLevelForges.getSelectExprProcessorForges();
        SelectExprProcessor[] selectExprProcessors = new SelectExprProcessor[selectForges.length];
        for (int i = 0; i < selectForges.length; i++) {
            selectExprProcessors[i] = SelectExprProcessorCompiler.allocateSelectExprEvaluator(stmtContext.getEventAdapterService(), selectForges[i], stmtContext.getEngineImportService(), ResultSetProcessorFactoryFactory.class, isFireAndForget, stmtContext.getStatementName());
        }

        GroupByRollupPerLevelExpression perLevelEvals = new GroupByRollupPerLevelExpression(selectExprProcessors, havingEvals);
        ExprEvaluator[] groupKeyEvals = ExprNodeUtilityRich.getEvaluatorsMayCompile(groupKeyNodeExpressions, stmtContext.getEngineImportService(), this.getClass(), isFireAndForget, stmtContext.getStatementName());

        return new ResultSetProcessorRowPerGroupRollupFactory(resultEventType, perLevelEvals, groupKeyNodeExpressions, groupKeyEvals, isSelectRStream, isUnidirectional, outputLimitSpec, isSorting, groupByRollupDesc, isJoin, isHistoricalOnly,
                optionalOutputFirstConditionFactory, resultSetProcessorHelperFactory, outputConditionType, numStreams, unbounded);
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

    public AggregationGroupByRollupDesc getGroupByRollupDesc() {
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

    public OutputConditionPolledFactory getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public ResultSetProcessorHelperFactory getResultSetProcessorHelperFactory() {
        return resultSetProcessorHelperFactory;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public Class getInterfaceClass() {
        return ResultSetProcessorRowPerGroupRollup.class;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        instance.getMethods().addMethod(AggregationService.class, "getAggregationService", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_AGGREGATIONSVC));
        instance.getMethods().addMethod(AgentInstanceContext.class, "getAgentInstanceContext", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(REF_AGENTINSTANCECONTEXT));
        instance.getMethods().addMethod(boolean.class, "isSelectRStream", Collections.emptyList(), ResultSetProcessorRowForAll.class, classScope, methodNode -> methodNode.getBlock().methodReturn(constant(isSelectRStream)));

        CodegenMember rollupDesc = classScope.makeAddMember(AggregationGroupByRollupDesc.class, groupByRollupDesc);
        instance.getMethods().addMethod(AggregationGroupByRollupDesc.class, "getGroupByRollupDesc", Collections.emptyList(), ResultSetProcessorRowPerGroupRollup.class, classScope, methodNode -> methodNode.getBlock().methodReturn(member(rollupDesc.getMemberId())));

        ResultSetProcessorRowPerGroupRollupImpl.removedAggregationGroupKeyCodegen(classScope, instance);
        ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(getGroupKeyNodeExpressions(), classScope, instance);
        ResultSetProcessorRowPerGroupRollupImpl.generateOutputBatchedMapUnsortedCodegen(this, instance, classScope);
        ResultSetProcessorRowPerGroupRollupImpl.generateOutputBatchedCodegen(this, instance, classScope);

        // generate having clauses
        ExprForge[] havingForges = perLevelForges.getOptionalHavingForges();
        if (havingForges != null) {
            factoryMembers.add(new CodegenTypedParam(HavingClauseEvaluator[].class, NAME_HAVINGEVALUATOR_ARRAYNONMEMBER));
            factoryCtor.getBlock().assignRef(NAME_HAVINGEVALUATOR_ARRAYNONMEMBER, newArrayByLength(HavingClauseEvaluator.class, constant(havingForges.length)));
            for (int i = 0; i < havingForges.length; i++) {
                CodegenExpressionNewAnonymousClass impl = newAnonymousClass(factoryCtor.getBlock(), HavingClauseEvaluator.class, boolean.class, "evaluateHaving", ExprForgeCodegenNames.PARAMS);
                impl.getBlock().blockReturn(CodegenLegoMethodExpression.codegenBooleanExpressionReturnTrueFalse(havingForges[i], classScope, factoryCtor, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                factoryCtor.getBlock().assignArrayElement(NAME_HAVINGEVALUATOR_ARRAYNONMEMBER, constant(i), impl);
            }
        }
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.processViewResultUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.processViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processJoinResultCodegen(this, classScope, method, instance);
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.getIteratorViewUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.getIteratorViewCodegen(this, classScope, method, instance);
        }
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.getIteratorJoinCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedJoinCodegen(this, classScope, method, instance);
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.applyViewResultUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.applyViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.applyJoinResultCodegen(this, classScope, method, instance);
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.continueOutputLimitedLastAllNonBufferedViewCodegen(this, method);
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.continueOutputLimitedLastAllNonBufferedJoinCodegen(this, method);
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedLastAllNonBufferedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.processOutputLimitedLastAllNonBufferedJoinCodegen(this, classScope, method, instance);
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.acceptHelperVisitorCodegen(method, instance);
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method, CodegenInstanceAux instance) {
        if (unbounded) {
            ResultSetProcessorRowPerGroupRollupUnbound.stopMethodUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupRollupImpl.stopMethodCodegenBound(method, instance);
        }
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method) {
        ResultSetProcessorRowPerGroupRollupImpl.clearMethodCodegen(method);
    }

    public Class[] getGroupKeyTypes() {
        return groupKeyTypes;
    }
}
