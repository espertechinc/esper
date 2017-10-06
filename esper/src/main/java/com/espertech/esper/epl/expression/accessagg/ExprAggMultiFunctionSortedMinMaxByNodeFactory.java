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
import com.espertech.esper.epl.agg.access.AggregationAccessorForge;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.agg.service.common.AggregationValidationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;

public class ExprAggMultiFunctionSortedMinMaxByNodeFactory implements AggregationMethodFactory {
    private final ExprAggMultiFunctionSortedMinMaxByNode parent;
    private final AggregationAccessorForge accessor;
    private final Class accessorResultType;
    private final EventType containedEventType;

    private final AggregationStateKey optionalStateKey;
    private final SortedAggregationStateFactoryFactory optionalStateFactory;
    private final AggregationAgentForge optionalAgent;

    public ExprAggMultiFunctionSortedMinMaxByNodeFactory(ExprAggMultiFunctionSortedMinMaxByNode parent, AggregationAccessorForge accessor, Class accessorResultType, EventType containedEventType, AggregationStateKey optionalStateKey, SortedAggregationStateFactoryFactory optionalStateFactory, AggregationAgentForge optionalAgent) {
        this.parent = parent;
        this.accessor = accessor;
        this.accessorResultType = accessorResultType;
        this.containedEventType = containedEventType;
        this.optionalStateKey = optionalStateKey;
        this.optionalStateFactory = optionalStateFactory;
        this.optionalAgent = optionalAgent;
    }

    public boolean isAccessAggregation() {
        return true;
    }

    public AggregationMethod make() {
        throw new UnsupportedOperationException();
    }

    public Class getResultType() {
        return accessorResultType;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return optionalStateKey;
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        if (isMatchRecognize || optionalStateFactory == null) {
            return null;
        }
        return optionalStateFactory.makeForge();
    }

    public AggregationAccessorForge getAccessorForge() {
        return accessor;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, intoTableAgg);
        ExprAggMultiFunctionSortedMinMaxByNodeFactory other = (ExprAggMultiFunctionSortedMinMaxByNodeFactory) intoTableAgg;
        AggregationValidationUtil.validateEventType(this.containedEventType, other.getContainedEventType());
        AggregationValidationUtil.validateAggFuncName(parent.getAggregationFunctionName(), other.getParent().getAggregationFunctionName());
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        return optionalAgent;
    }

    public EventType getContainedEventType() {
        return containedEventType;
    }

    public ExprAggMultiFunctionSortedMinMaxByNode getParent() {
        return parent;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return null;
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        throw new UnsupportedOperationException();
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        throw new UnsupportedOperationException();
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        throw new UnsupportedOperationException();
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        throw new UnsupportedOperationException();
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        throw new UnsupportedOperationException();
    }
}