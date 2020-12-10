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
package com.espertech.esper.common.internal.epl.resultset.rowperevent;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryForgeBase;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_SELECTEXPRPROCESSOR;

/**
 * Result set processor prototype for the case: aggregation functions used in the select clause, and no group-by,
 * and not all of the properties in the select clause are under an aggregation function.
 */
public class ResultSetProcessorRowPerEventForge extends ResultSetProcessorFactoryForgeBase {
    private final ExprForge optionalHavingNode;
    private final boolean isSelectRStream;
    private final boolean isUnidirectional;
    private final boolean isHistoricalOnly;
    private final OutputLimitSpec outputLimitSpec;
    private final boolean hasOrderBy;
    private StateMgmtSetting outputAllHelperSettings;
    private StateMgmtSetting outputLastHelperSettings;

    public ResultSetProcessorRowPerEventForge(EventType resultEventType,
                                              EventType[] typesPerStream,
                                              ExprForge optionalHavingNode,
                                              boolean isSelectRStream,
                                              boolean isUnidirectional,
                                              boolean isHistoricalOnly,
                                              OutputLimitSpec outputLimitSpec,
                                              boolean hasOrderBy) {
        super(resultEventType, typesPerStream);
        this.optionalHavingNode = optionalHavingNode;
        this.isSelectRStream = isSelectRStream;
        this.isUnidirectional = isUnidirectional;
        this.isHistoricalOnly = isHistoricalOnly;
        this.outputLimitSpec = outputLimitSpec;
        this.hasOrderBy = hasOrderBy;
    }

    public ExprForge getOptionalHavingNode() {
        return optionalHavingNode;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public boolean isUnidirectional() {
        return isUnidirectional;
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

    public boolean isSorting() {
        return hasOrderBy;
    }

    public EPTypeClass getInterfaceClass() {
        return ResultSetProcessorRowPerEvent.EPTYPE;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        instance.getMethods().addMethod(SelectExprProcessor.EPTYPE, "getSelectExprProcessor", Collections.emptyList(), ResultSetProcessorRowPerEvent.class, classScope, methodNode -> methodNode.getBlock().methodReturn(MEMBER_SELECTEXPRPROCESSOR));
        instance.getMethods().addMethod(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "hasHavingClause", Collections.emptyList(), ResultSetProcessorRowPerEvent.class, classScope, methodNode -> methodNode.getBlock().methodReturn(constant(optionalHavingNode != null)));
        ResultSetProcessorUtil.evaluateHavingClauseCodegen(optionalHavingNode, classScope, instance);
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.processViewResultCodegen(this, classScope, method, instance);
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.processJoinResultCodegen(this, classScope, method, instance);
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.getIteratorViewCodegen(this, classScope, method);
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.getIteratorJoinCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.processOutputLimitedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.processOutputLimitedJoinCodegen(this, classScope, method, instance);
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.applyViewResultCodegen(method);
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.applyJoinResultCodegen(method);
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.continueOutputLimitedLastAllNonBufferedViewCodegen(this, method);
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.continueOutputLimitedLastAllNonBufferedJoinCodegen(this, method);
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.processOutputLimitedLastAllNonBufferedViewCodegen(this, classScope, method, instance);
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.processOutputLimitedLastAllNonBufferedJoinCodegen(this, classScope, method, instance);
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.acceptHelperVisitorCodegen(method, instance);
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerEventImpl.stopCodegen(method, instance);
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethod method) {
        ResultSetProcessorRowPerEventImpl.clearMethodCodegen(method);
    }

    public String getInstrumentedQName() {
        return "ResultSetProcessUngroupedNonfullyAgg";
    }

    public StateMgmtSetting getOutputAllHelperSettings() {
        return outputAllHelperSettings;
    }

    public StateMgmtSetting getOutputLastHelperSettings() {
        return outputLastHelperSettings;
    }

    public void planStateSettings(FabricCharge fabricCharge, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        if (isOutputAll()) {
            this.outputAllHelperSettings = services.getStateMgmtSettingsProvider().resultSet().rowPerEventOutputAll(fabricCharge, statementRawInfo, this);
        } else if (isOutputLast()) {
            this.outputLastHelperSettings = services.getStateMgmtSettingsProvider().resultSet().rowPerEventOutputLast(fabricCharge, statementRawInfo, this);
        }
    }
}
