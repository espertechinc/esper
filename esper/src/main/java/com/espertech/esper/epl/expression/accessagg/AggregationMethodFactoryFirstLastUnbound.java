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
import com.espertech.esper.epl.agg.access.AggregationStateType;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.aggregator.AggregatorFirstEver;
import com.espertech.esper.epl.agg.aggregator.AggregatorLastEver;
import com.espertech.esper.epl.agg.factory.AggregationMethodFactoryUtil;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.agg.service.common.AggregationValidationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;

public class AggregationMethodFactoryFirstLastUnbound implements AggregationMethodFactory {
    protected final ExprAggMultiFunctionLinearAccessNode parent;
    private final EventType collectionEventType;
    private final Class resultType;
    private final int streamNum;
    protected final boolean hasFilter;

    public AggregationMethodFactoryFirstLastUnbound(ExprAggMultiFunctionLinearAccessNode parent, EventType collectionEventType, Class resultType, int streamNum, boolean hasFilter) {
        this.parent = parent;
        this.collectionEventType = collectionEventType;
        this.resultType = resultType;
        this.streamNum = streamNum;
        this.hasFilter = hasFilter;
    }

    public Class getResultType() {
        return resultType;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new UnsupportedOperationException();
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        throw new UnsupportedOperationException();
    }

    public AggregationAccessorForge getAccessorForge() {
        throw new UnsupportedOperationException();
    }

    public AggregationMethod make() {
        if (parent.getStateType() == AggregationStateType.FIRST) {
            return AggregationMethodFactoryUtil.makeFirstEver(hasFilter);
        } else if (parent.getStateType() == AggregationStateType.LAST) {
            return AggregationMethodFactoryUtil.makeLastEver(hasFilter);
        }
        throw new RuntimeException("Window aggregation function is not available");
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryFirstLastUnbound that = (AggregationMethodFactoryFirstLastUnbound) intoTableAgg;
        AggregationValidationUtil.validateStreamNumZero(that.streamNum);
        if (collectionEventType != null) {
            AggregationValidationUtil.validateEventType(collectionEventType, that.collectionEventType);
        } else {
            AggregationValidationUtil.validateAggregationInputType(resultType, that.resultType);
        }
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        throw new UnsupportedOperationException();
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        if (parent.getStateType() == AggregationStateType.FIRST) {
            AggregatorFirstEver.rowMemberCodegen(column, ctor, membersColumnized);
        } else if (parent.getStateType() == AggregationStateType.LAST) {
            AggregatorLastEver.rowMemberCodegen(column, ctor, membersColumnized);
        }
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (parent.getStateType() == AggregationStateType.FIRST) {
            AggregatorFirstEver.applyEnterCodegen(parent.getOptionalFilter() != null, column, method, symbols, forges, classScope);
        } else if (parent.getStateType() == AggregationStateType.LAST) {
            AggregatorLastEver.applyEnterCodegen(parent.getOptionalFilter() != null, column, method, symbols, forges, classScope);
        }
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        // no code
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (parent.getStateType() == AggregationStateType.FIRST) {
            AggregatorFirstEver.clearCodegen(column, method);
        } else if (parent.getStateType() == AggregationStateType.LAST) {
            AggregatorLastEver.clearCodegen(column, method);
        }
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (parent.getStateType() == AggregationStateType.FIRST) {
            AggregatorFirstEver.getValueCodegen(column, method);
        } else if (parent.getStateType() == AggregationStateType.LAST) {
            AggregatorLastEver.getValueCodegen(column, method);
        }
    }
}