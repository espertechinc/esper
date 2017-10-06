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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorForge;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.factory.AggregationFactoryFactory;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.agg.service.common.AggregationValidationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionAgentContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionHandler;

public class ExprPlugInAggMultiFunctionNodeFactory implements AggregationMethodFactory {
    private final ExprPlugInAggMultiFunctionNode parent;
    private final PlugInAggregationMultiFunctionHandler handlerPlugin;
    private final AggregationFactoryFactory aggregationFactoryFactory;
    private final StatementExtensionSvcContext statementExtensionSvcContext;
    private EPType returnType;

    public ExprPlugInAggMultiFunctionNodeFactory(ExprPlugInAggMultiFunctionNode parent, PlugInAggregationMultiFunctionHandler handlerPlugin, AggregationFactoryFactory aggregationFactoryFactory, StatementExtensionSvcContext statementExtensionSvcContext) {
        this.handlerPlugin = handlerPlugin;
        this.parent = parent;
        this.aggregationFactoryFactory = aggregationFactoryFactory;
        this.statementExtensionSvcContext = statementExtensionSvcContext;
    }

    public boolean isAccessAggregation() {
        return true;
    }

    public AggregationMethod make() {
        return null;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return handlerPlugin.getAggregationStateUniqueKey();
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        return aggregationFactoryFactory.makePlugInAccess(statementExtensionSvcContext, this);
    }

    public AggregationAccessorForge getAccessorForge() {
        return handlerPlugin.getAccessorForge();
    }

    public Class getResultType() {
        obtainReturnType();
        return EPTypeHelper.getNormalizedClass(returnType);
    }

    public PlugInAggregationMultiFunctionHandler getHandlerPlugin() {
        return handlerPlugin;
    }

    public Class getComponentTypeCollection() {
        obtainReturnType();
        return EPTypeHelper.getClassMultiValued(returnType);
    }

    public EventType getEventTypeSingle() {
        obtainReturnType();
        return EPTypeHelper.getEventTypeSingleValued(returnType);
    }

    public EventType getEventTypeCollection() {
        obtainReturnType();
        return EPTypeHelper.getEventTypeMultiValued(returnType);
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    private void obtainReturnType() {
        if (returnType == null) {
            returnType = handlerPlugin.getReturnType();
        }
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, intoTableAgg);
        ExprPlugInAggMultiFunctionNodeFactory that = (ExprPlugInAggMultiFunctionNodeFactory) intoTableAgg;
        if (!getAggregationStateKey(false).equals(that.getAggregationStateKey(false))) {
            throw new ExprValidationException("Mismatched state key");
        }
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        PlugInAggregationMultiFunctionAgentContext ctx = new PlugInAggregationMultiFunctionAgentContext(parent.getChildNodes(), parent.getOptionalFilter());
        return handlerPlugin.getAggregationAgent(ctx);
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return null;
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        // handled by AggregationMethodFactoryPlugIn
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
    }
}
