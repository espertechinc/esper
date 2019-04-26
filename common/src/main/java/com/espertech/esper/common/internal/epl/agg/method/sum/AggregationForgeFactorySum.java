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
package com.espertech.esper.common.internal.epl.agg.method.sum;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprSumNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AggregationForgeFactorySum extends AggregationForgeFactoryBase {
    protected final ExprSumNode parent;
    protected final Class resultType;
    protected final Class inputValueType;
    protected final DataInputOutputSerdeForge distinctSerde;
    protected AggregatorMethod aggregator;

    public AggregationForgeFactorySum(ExprSumNode parent, Class inputValueType, DataInputOutputSerdeForge distinctSerde) {
        this.parent = parent;
        this.inputValueType = inputValueType;
        this.distinctSerde = distinctSerde;
        this.resultType = getSumAggregatorType(inputValueType);
    }

    public Class getResultType() {
        return resultType;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        Class distinctValueType = !parent.isDistinct() ? null : inputValueType;
        if (resultType == BigInteger.class || resultType == BigDecimal.class) {
            aggregator = new AggregatorSumBig(this, col, rowCtor, membersColumnized, classScope, distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), resultType);
        } else {
            aggregator = new AggregatorSumNonBig(this, col, rowCtor, membersColumnized, classScope, distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), resultType);
        }
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationSum(parent.isDistinct(), parent.isHasFilter(), inputValueType);
    }

    private Class getSumAggregatorType(Class type) {
        if (type == BigInteger.class) {
            return BigInteger.class;
        }
        if (type == BigDecimal.class) {
            return BigDecimal.class;
        }
        return JavaClassHelper.getBoxedType(getMemberType(type));
    }

    protected static SimpleNumberCoercer getCoercerNonBigIntDec(Class inputValueType) {
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

    protected static Class getMemberType(Class inputValueType) {
        if (inputValueType == Long.class || inputValueType == long.class) {
            return long.class;
        } else if (inputValueType == Integer.class || inputValueType == int.class) {
            return int.class;
        } else if (inputValueType == Double.class || inputValueType == double.class) {
            return double.class;
        } else if (inputValueType == Float.class || inputValueType == float.class) {
            return float.class;
        } else {
            return int.class;
        }
    }
}