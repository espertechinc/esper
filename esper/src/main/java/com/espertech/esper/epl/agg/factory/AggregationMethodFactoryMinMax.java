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
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.epl.agg.access.AggregationAccessorForge;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.*;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.agg.service.common.AggregationValidationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.epl.expression.methodagg.ExprMinMaxAggrNode;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;

public class AggregationMethodFactoryMinMax implements AggregationMethodFactory {
    protected final ExprMinMaxAggrNode parent;
    protected final Class type;
    protected final boolean hasDataWindows;

    public AggregationMethodFactoryMinMax(ExprMinMaxAggrNode parent, Class type, boolean hasDataWindows) {
        this.parent = parent;
        this.type = type;
        this.hasDataWindows = hasDataWindows;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public AggregationStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public AggregationAccessorForge getAccessorForge() {
        throw new IllegalStateException("Not an access aggregation function");
    }

    public Class getResultType() {
        return type;
    }

    public AggregationMethod make() {
        AggregationMethod method = makeMinMaxAggregator(parent.getMinMaxTypeEnum(), type, hasDataWindows, parent.isHasFilter());
        if (!parent.isDistinct()) {
            return method;
        }
        return AggregationMethodFactoryUtil.makeDistinctAggregator(method, parent.isHasFilter());
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void validateIntoTableCompatible(AggregationMethodFactory intoTableAgg) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, intoTableAgg);
        AggregationMethodFactoryMinMax that = (AggregationMethodFactoryMinMax) intoTableAgg;
        AggregationValidationUtil.validateAggregationInputType(type, that.type);
        AggregationValidationUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
        if (parent.getMinMaxTypeEnum() != that.parent.getMinMaxTypeEnum()) {
            throw new ExprValidationException("The aggregation declares " +
                    parent.getMinMaxTypeEnum().getExpressionText() +
                    " and provided is " +
                    that.parent.getMinMaxTypeEnum().getExpressionText());
        }
        AggregationValidationUtil.validateAggregationUnbound(hasDataWindows, that.hasDataWindows);
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        return null;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public ExprMinMaxAggrNode getParent() {
        return parent;
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        if (!hasDataWindows) {
            AggregatorMinMaxEver.rowMemberCodegen(column, ctor, membersColumnized);
        } else {
            AggregatorMinMax.rowMemberCodegen(parent.isDistinct(), column, ctor, membersColumnized);
        }
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (!hasDataWindows) {
            AggregatorMinMaxEver.applyEnterCodegen(this, column, method, symbols, forges, classScope);
        } else {
            AggregatorMinMax.applyEnterCodegen(this, column, method, symbols, forges, classScope);
        }
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (!hasDataWindows) {
            // no code
        } else {
            AggregatorMinMax.applyLeaveCodegen(this, column, method, symbols, forges, classScope);
        }
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (!hasDataWindows) {
            AggregatorMinMaxEver.clearCodegen(column, method, classScope);
        } else {
            AggregatorMinMax.clearCodegen(parent.isDistinct(), column, method, classScope);
        }
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (!hasDataWindows) {
            AggregatorMinMaxEver.getValueCodegen(column, method);
        } else {
            AggregatorMinMax.getValueCodegen(this, column, method);
        }
    }

    private AggregationMethod makeMinMaxAggregator(MinMaxTypeEnum minMaxTypeEnum, Class targetType, boolean isHasDataWindows, boolean hasFilter) {
        if (!hasFilter) {
            if (!isHasDataWindows) {
                return new AggregatorMinMaxEver(minMaxTypeEnum);
            }
            return new AggregatorMinMax(minMaxTypeEnum);
        } else {
            if (!isHasDataWindows) {
                return new AggregatorMinMaxEverFilter(minMaxTypeEnum);
            }
            return new AggregatorMinMaxFilter(minMaxTypeEnum);
        }
    }
}
