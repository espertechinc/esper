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
import com.espertech.esper.epl.expression.methodagg.ExprAvgNode;
import com.espertech.esper.epl.expression.methodagg.ExprMethodAggUtil;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class AggregationMethodFactoryAvg implements AggregationMethodFactory {
    protected final ExprAvgNode parent;
    protected final Class childType;
    protected final Class resultType;
    protected final MathContext optionalMathContext;

    public AggregationMethodFactoryAvg(ExprAvgNode parent, Class childType, MathContext optionalMathContext) {
        this.parent = parent;
        this.childType = childType;
        this.resultType = getAvgAggregatorType(childType);
        this.optionalMathContext = optionalMathContext;
    }

    public boolean isAccessAggregation() {
        return false;
    }

    public Class getResultType() {
        return resultType;
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

    public AggregationMethod make() {
        AggregationMethod method = makeAvgAggregator(childType, parent.isHasFilter(), optionalMathContext);
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
        AggregationMethodFactoryAvg that = (AggregationMethodFactoryAvg) intoTableAgg;
        AggregationValidationUtil.validateAggregationInputType(childType, that.childType);
        AggregationValidationUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        return null;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        if (childType == BigDecimal.class || childType == BigInteger.class) {
            AggregatorAvgBigDecimal.rowMemberCodegen(parent.isDistinct(), column, ctor, membersColumnized);
        } else {
            AggregatorCodegenUtil.rowMemberSumAndCnt(parent.isDistinct(), column, ctor, membersColumnized, double.class);
        }
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (childType == BigDecimal.class || childType == BigInteger.class) {
            AggregatorAvgBigDecimal.applyEnterCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope);
        } else {
            AggregatorCodegenUtil.sumAndCountApplyEnterCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope, SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.INSTANCE);
        }
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (childType == BigDecimal.class || childType == BigInteger.class) {
            AggregatorAvgBigDecimal.applyLeaveCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope);
        } else {
            AggregatorCodegenUtil.sumAndCountApplyLeaveCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope, SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.INSTANCE);
        }
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (childType == BigDecimal.class || childType == BigInteger.class) {
            AggregatorAvgBigDecimal.clearCodegen(parent.isDistinct(), column, method);
        } else {
            AggregatorCodegenUtil.sumAndCountClearCodegen(parent.isDistinct(), column, method);
        }
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (childType == BigDecimal.class || childType == BigInteger.class) {
            AggregatorAvgBigDecimal.getValueCodegen(this, column, method, classScope);
        } else {
            AggregatorAvg.getValueCodegen(column, method);
        }
    }

    public MathContext getOptionalMathContext() {
        return optionalMathContext;
    }

    private Class getAvgAggregatorType(Class type) {
        if ((type == BigDecimal.class) || (type == BigInteger.class)) {
            return BigDecimal.class;
        }
        return Double.class;
    }

    private AggregationMethod makeAvgAggregator(Class type, boolean hasFilter, MathContext optionalMathContext) {
        if (hasFilter) {
            if ((type == BigDecimal.class) || (type == BigInteger.class)) {
                return new AggregatorAvgBigDecimalFilter(optionalMathContext);
            }
            return new AggregatorAvgFilter();
        }
        if ((type == BigDecimal.class) || (type == BigInteger.class)) {
            return new AggregatorAvgBigDecimal(optionalMathContext);
        }
        return new AggregatorAvg();
    }
}