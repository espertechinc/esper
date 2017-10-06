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
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AggregationMethodFactorySum implements AggregationMethodFactory {
    protected final ExprSumNode parent;
    protected final Class resultType;
    protected final Class inputValueType;

    public AggregationMethodFactorySum(ExprSumNode parent, Class inputValueType) {
        this.parent = parent;
        this.inputValueType = inputValueType;
        this.resultType = getSumAggregatorType(inputValueType);
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
        return resultType;
    }

    public AggregationMethod make() {
        AggregationMethod method = makeSumAggregator(inputValueType, parent.isHasFilter());
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
        AggregationMethodFactorySum that = (AggregationMethodFactorySum) intoTableAgg;
        AggregationValidationUtil.validateAggregationInputType(inputValueType, that.inputValueType);
        AggregationValidationUtil.validateAggregationFilter(parent.isHasFilter(), that.parent.isHasFilter());
    }

    public AggregationAgentForge getAggregationStateAgent(EngineImportService engineImportService, String statementName) {
        return null;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    private Class getSumAggregatorType(Class type) {
        if (type == BigInteger.class) {
            return BigInteger.class;
        }
        if (type == BigDecimal.class) {
            return BigDecimal.class;
        }
        if ((type == Long.class) || (type == long.class)) {
            return Long.class;
        }
        if ((type == Integer.class) || (type == int.class)) {
            return Integer.class;
        }
        if ((type == Double.class) || (type == double.class)) {
            return Double.class;
        }
        if ((type == Float.class) || (type == float.class)) {
            return Float.class;
        }
        return Integer.class;
    }

    public void rowMemberCodegen(int column, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, ExprForge[] forges, CodegenClassScope classScope) {
        if (inputValueType == BigInteger.class) {
            AggregatorSumBigInteger.rowMemberCodegen(parent.isDistinct(), column, ctor, membersColumnized);
            return;
        }
        if (inputValueType == BigDecimal.class) {
            AggregatorSumBigDecimal.rowMemberCodegen(parent.isDistinct(), column, ctor, membersColumnized);
            return;
        }
        Class type;
        if (inputValueType == Long.class || inputValueType == long.class) {
            type = long.class;
        } else if (inputValueType == Integer.class || inputValueType == int.class) {
            type = int.class;
        } else if (inputValueType == Double.class || inputValueType == double.class) {
            type = double.class;
        } else if (inputValueType == Float.class || inputValueType == float.class) {
            type = float.class;
        } else {
            type = int.class;
        }
        AggregatorCodegenUtil.rowMemberSumAndCnt(parent.isDistinct(), column, ctor, membersColumnized, type);
    }

    public void applyEnterCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (inputValueType == BigInteger.class) {
            AggregatorSumBigInteger.applyEnterCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope);
        } else if (inputValueType == BigDecimal.class) {
            AggregatorSumBigDecimal.applyEnterCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope);
        } else {
            SimpleNumberCoercer coercer = getCoercerNonBigIntDec(inputValueType);
            AggregatorCodegenUtil.sumAndCountApplyEnterCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope, coercer);
        }
    }

    public void applyLeaveCodegen(int column, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        if (inputValueType == BigInteger.class) {
            AggregatorSumBigInteger.applyLeaveCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope);
        } else if (inputValueType == BigDecimal.class) {
            AggregatorSumBigDecimal.applyLeaveCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope);
        } else {
            SimpleNumberCoercer coercer = getCoercerNonBigIntDec(inputValueType);
            AggregatorCodegenUtil.sumAndCountApplyLeaveCodegen(parent.isDistinct(), parent.isHasFilter(), column, method, symbols, forges, classScope, coercer);
        }
    }

    public void clearCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (inputValueType == BigInteger.class) {
            AggregatorSumBigInteger.clearCodegen(parent.isDistinct(), column, method);
        } else if (inputValueType == BigDecimal.class) {
            AggregatorSumBigDecimal.clearCodegen(parent.isDistinct(), column, method);
        } else {
            AggregatorCodegenUtil.sumAndCountClearCodegen(parent.isDistinct(), column, method);
        }
    }

    public void getValueCodegen(int column, CodegenMethodNode method, CodegenClassScope classScope) {
        if (inputValueType == BigInteger.class) {
            AggregatorSumBigInteger.getValueCodegen(column, method);
        } else if (inputValueType == BigDecimal.class) {
            AggregatorSumBigDecimal.getValueCodegen(column, method);
        } else {
            AggregatorCodegenUtil.getValueSum(column, method);
        }
    }

    private AggregationMethod makeSumAggregator(Class type, boolean hasFilter) {
        if (!hasFilter) {
            if (type == BigInteger.class) {
                return new AggregatorSumBigInteger();
            }
            if (type == BigDecimal.class) {
                return new AggregatorSumBigDecimal();
            }
            if ((type == Long.class) || (type == long.class)) {
                return new AggregatorSumLong();
            }
            if ((type == Integer.class) || (type == int.class)) {
                return new AggregatorSumInteger();
            }
            if ((type == Double.class) || (type == double.class)) {
                return new AggregatorSumDouble();
            }
            if ((type == Float.class) || (type == float.class)) {
                return new AggregatorSumFloat();
            }
            return new AggregatorSumNumInteger();
        } else {
            if (type == BigInteger.class) {
                return new AggregatorSumBigIntegerFilter();
            }
            if (type == BigDecimal.class) {
                return new AggregatorSumBigDecimalFilter();
            }
            if ((type == Long.class) || (type == long.class)) {
                return new AggregatorSumLongFilter();
            }
            if ((type == Integer.class) || (type == int.class)) {
                return new AggregatorSumIntegerFilter();
            }
            if ((type == Double.class) || (type == double.class)) {
                return new AggregatorSumDoubleFilter();
            }
            if ((type == Float.class) || (type == float.class)) {
                return new AggregatorSumFloatFilter();
            }
            return new AggregatorSumNumIntegerFilter();
        }
    }

    private SimpleNumberCoercer getCoercerNonBigIntDec(Class inputValueType) {
        SimpleNumberCoercer coercer;
        if (inputValueType == Long.class || inputValueType == long.class) {
            coercer = SimpleNumberCoercerFactory.SimpleNumberCoercerLong.INSTANCE;
        } else if (inputValueType == Integer.class || inputValueType == int.class) {
            coercer = SimpleNumberCoercerFactory.SimpleNumberCoercerInt.INSTANCE;
        } else if (inputValueType == Double.class || inputValueType == double.class) {
            coercer = SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.INSTANCE;
        } else if (inputValueType == Float.class || inputValueType == float.class) {
            coercer = SimpleNumberCoercerFactory.SimpleNumberCoercerFloat.INSTANCE;
        } else {
            coercer = SimpleNumberCoercerFactory.SimpleNumberCoercerInt.INSTANCE;
        }
        return coercer;
    }
}