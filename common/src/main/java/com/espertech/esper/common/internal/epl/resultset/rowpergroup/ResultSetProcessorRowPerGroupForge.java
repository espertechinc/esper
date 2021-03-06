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
package com.espertech.esper.common.internal.epl.resultset.rowpergroup;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactoryForge;
import com.espertech.esper.common.internal.epl.resultset.core.*;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAll;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.generateGroupKeyArrayJoinCodegen;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.generateGroupKeyArrayViewCodegen;

/**
 * Result set processor prototype for the fully-grouped case:
 * there is a group-by and all non-aggregation event properties in the select clause are listed in the group by,
 * and there are aggregation functions.
 */
public class ResultSetProcessorRowPerGroupForge extends ResultSetProcessorFactoryForgeBase {
    private final static String NAME_GROUPREPS = "groupReps";

    private final ExprNode[] groupKeyNodeExpressions;
    private final ExprForge optionalHavingNode;
    private final boolean isSorting;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean unboundedProcessor;
    private final boolean isHistoricalOnly;
    private final ResultSetProcessorOutputConditionType outputConditionType;
    private final OutputConditionPolledFactoryForge optionalOutputFirstConditionFactory;
    private final EPType[] groupKeyTypes;
    private final MultiKeyClassRef multiKeyClassRef;
    private StateMgmtSetting unboundGroupRepSettings;
    private StateMgmtSetting outputFirstHelperSettings;
    private StateMgmtSetting outputAllHelperSettings;
    private StateMgmtSetting outputAllOptHelperSettings;
    private StateMgmtSetting outputLastOptHelperSettings;

    private CodegenMethod generateGroupKeySingle;
    private CodegenMethod generateGroupKeyArrayView;
    private CodegenMethod generateGroupKeyArrayJoin;

    public ResultSetProcessorRowPerGroupForge(EventType resultEventType,
                                              EventType[] typesPerStream,
                                              ExprNode[] groupKeyNodeExpressions,
                                              ExprForge optionalHavingNode,
                                              boolean isSelectRStream,
                                              boolean isUnidirectional,
                                              OutputLimitSpec outputLimitSpec,
                                              boolean isSorting,
                                              boolean isHistoricalOnly,
                                              ResultSetProcessorOutputConditionType outputConditionType,
                                              OutputConditionPolledFactoryForge optionalOutputFirstConditionFactory,
                                              MultiKeyClassRef multiKeyClassRef,
                                              boolean unboundedProcessor) {
        super(resultEventType, typesPerStream);
        this.groupKeyNodeExpressions = groupKeyNodeExpressions;
        this.optionalHavingNode = optionalHavingNode;
        this.isSorting = isSorting;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.outputLimitSpec = outputLimitSpec;
        this.isHistoricalOnly = isHistoricalOnly;
        this.outputConditionType = outputConditionType;
        this.optionalOutputFirstConditionFactory = optionalOutputFirstConditionFactory;
        this.groupKeyTypes = ExprNodeUtilityQuery.getExprResultTypes(groupKeyNodeExpressions);
        this.multiKeyClassRef = multiKeyClassRef;
        this.unboundedProcessor = unboundedProcessor;
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
        return isLimitSpec(OutputLimitLimitType.LAST);
    }

    public boolean isOutputAll() {
        return isLimitSpec(OutputLimitLimitType.ALL);
    }

    public boolean isOutputFirst() {
        return isLimitSpec(OutputLimitLimitType.FIRST);
    }

    public OutputConditionPolledFactoryForge getOptionalOutputFirstConditionFactory() {
        return optionalOutputFirstConditionFactory;
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public int getNumStreams() {
        return typesPerStream.length;
    }

    public EPTypeClass getInterfaceClass() {
        return ResultSetProcessorRowPerGroup.EPTYPE;
    }

    public MultiKeyClassRef getMultiKeyClassRef() {
        return multiKeyClassRef;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        instance.getMethods().addMethod(SelectExprProcessor.EPTYPE, "getSelectExprProcessor", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_SELECTEXPRPROCESSOR));
        instance.getMethods().addMethod(AggregationService.EPTYPE, "getAggregationService", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_AGGREGATIONSVC));
        instance.getMethods().addMethod(ExprEvaluatorContext.EPTYPE, "getExprEvaluatorContext", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_EXPREVALCONTEXT));
        instance.getMethods().addMethod(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "hasHavingClause", Collections.emptyList(), this.getClass(), classScope, methodNode -> methodNode.getBlock().methodReturn(constant(optionalHavingNode != null)));
        instance.getMethods().addMethod(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "isSelectRStream", Collections.emptyList(), ResultSetProcessorRowForAll.class, classScope, methodNode -> methodNode.getBlock().methodReturn(constant(isSelectRStream)));
        ResultSetProcessorUtil.evaluateHavingClauseCodegen(optionalHavingNode, classScope, instance);
        generateGroupKeySingle = ResultSetProcessorGroupedUtil.generateGroupKeySingleCodegen(getGroupKeyNodeExpressions(), multiKeyClassRef, classScope, instance);
        generateGroupKeyArrayView = generateGroupKeyArrayViewCodegen(generateGroupKeySingle, classScope, instance);
        generateGroupKeyArrayJoin = generateGroupKeyArrayJoinCodegen(generateGroupKeySingle, classScope, instance);
        ResultSetProcessorRowPerGroupImpl.generateOutputBatchedNoSortWMapCodegen(this, classScope, instance);
        ResultSetProcessorRowPerGroupImpl.generateOutputBatchedArrFromIteratorCodegen(this, classScope, instance);
        ResultSetProcessorRowPerGroupImpl.removedAggregationGroupKeyCodegen(classScope, instance);

        if (unboundedProcessor) {
            CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
            instance.addMember(NAME_GROUPREPS, ResultSetProcessorRowPerGroupUnboundHelper.EPTYPE);
            CodegenExpression groupKeySerde = getMultiKeyClassRef().getExprMKSerde(classScope.getPackageScope().getInitMethod(), classScope);
            CodegenExpressionField eventType = classScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(typesPerStream[0], EPStatementInitServices.REF));
            instance.getServiceCtor().getBlock().assignRef(NAME_GROUPREPS, exprDotMethod(factory, "makeRSRowPerGroupUnboundGroupRep",
                constant(groupKeyTypes), groupKeySerde, eventType, unboundGroupRepSettings.toExpression(), MEMBER_EXPREVALCONTEXT))
                .exprDotMethod(MEMBER_AGGREGATIONSVC, "setRemovedCallback", member(NAME_GROUPREPS));
        } else {
            instance.getServiceCtor().getBlock().exprDotMethod(MEMBER_AGGREGATIONSVC, "setRemovedCallback", ref("this"));
        }
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.processViewResultUnboundCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.processViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processJoinResultCodegen(this, classScope, method, instance);
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.getIteratorViewUnboundedCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.getIteratorViewCodegen(this, classScope, method, instance);
        }
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.getIteratorJoinCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedJoinCodegen(this, classScope, method, instance);
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.applyViewResultCodegen(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.applyViewResultCodegen(this, classScope, method, instance);
        }
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.applyJoinResultCodegen(this, classScope, method, instance);
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.continueOutputLimitedLastAllNonBufferedViewCodegen(this, method);
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.continueOutputLimitedLastAllNonBufferedJoinCodegen(this, method);
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedLastAllNonBufferedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.processOutputLimitedLastAllNonBufferedJoinCodegen(this, classScope, method, instance);
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupImpl.acceptHelperVisitorCodegen(method, instance);
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (unboundedProcessor) {
            ResultSetProcessorRowPerGroupUnbound.stopMethodCodegenUnbound(this, classScope, method, instance);
        } else {
            ResultSetProcessorRowPerGroupImpl.stopMethodCodegenBound(method, instance);
        }
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethod method) {
        ResultSetProcessorRowPerGroupImpl.clearMethodCodegen(method);
    }

    public EPType[] getGroupKeyTypes() {
        return groupKeyTypes;
    }

    public String getInstrumentedQName() {
        return "ResultSetProcessGroupedRowPerGroup";
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

    public StateMgmtSetting getOutputFirstHelperSettings() {
        return outputFirstHelperSettings;
    }

    public StateMgmtSetting getOutputAllHelperSettings() {
        return outputAllHelperSettings;
    }

    public StateMgmtSetting getOutputAllOptHelperSettings() {
        return outputAllOptHelperSettings;
    }

    public StateMgmtSetting getOutputLastOptHelperSettings() {
        return outputLastOptHelperSettings;
    }

    public void planStateSettings(FabricCharge fabricCharge, StatementRawInfo statementRawInfo, ResultSetProcessorFlags flags, StatementCompileTimeServices services) {
        if (isOutputFirst()) {
            this.outputFirstHelperSettings = services.getStateMgmtSettingsProvider().resultSet().rowPerGroupOutputFirst(fabricCharge, statementRawInfo, this);
        } else if (isOutputAll()) {
            if (flags.getOutputConditionType() == POLICY_LASTALL_UNORDERED) {
                this.outputAllOptHelperSettings = services.getStateMgmtSettingsProvider().resultSet().rowPerGroupOutputAllOpt(fabricCharge, statementRawInfo, this);
            } else {
                this.outputAllHelperSettings = services.getStateMgmtSettingsProvider().resultSet().rowPerGroupOutputAll(fabricCharge, statementRawInfo, this);
            }
        } else if (isOutputLast()) {
            if (flags.getOutputConditionType() == POLICY_LASTALL_UNORDERED) {
                this.outputLastOptHelperSettings = services.getStateMgmtSettingsProvider().resultSet().rowPerGroupOutputLast(fabricCharge, statementRawInfo, this);
            }
        }

        if (unboundedProcessor) {
            this.unboundGroupRepSettings = services.getStateMgmtSettingsProvider().resultSet().rowPerGroupUnbound(fabricCharge, statementRawInfo, this);
        }
    }

    private boolean isLimitSpec(OutputLimitLimitType expected) {
        return outputLimitSpec != null && outputLimitSpec.getDisplayLimit() == expected;
    }
}
