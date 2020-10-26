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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
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

import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeInteger;

public class AggregationForgeFactorySum extends AggregationForgeFactoryBase {
    protected final ExprSumNode parent;
    protected final EPTypeClass resultType;
    protected final EPTypeClass inputValueType;
    protected final DataInputOutputSerdeForge distinctSerde;
    protected AggregatorMethod aggregator;

    public AggregationForgeFactorySum(ExprSumNode parent, EPTypeClass inputValueType, DataInputOutputSerdeForge distinctSerde) {
        this.parent = parent;
        this.inputValueType = inputValueType;
        this.distinctSerde = distinctSerde;
        this.resultType = getSumAggregatorType(inputValueType);

        EPTypeClass distinctValueType = !parent.isDistinct() ? null : inputValueType;
        if (resultType.getType() == BigInteger.class || resultType.getType() == BigDecimal.class) {
            aggregator = new AggregatorSumBig(distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), resultType);
        } else {
            aggregator = new AggregatorSumNonBig(distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), resultType);
        }
    }

    public EPType getResultType() {
        return resultType;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
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

    private EPTypeClass getSumAggregatorType(EPTypeClass type) {
        if (type.getType() == BigInteger.class) {
            return EPTypePremade.BIGINTEGER.getEPType();
        }
        if (type.getType() == BigDecimal.class) {
            return EPTypePremade.BIGDECIMAL.getEPType();
        }
        return JavaClassHelper.getBoxedType(getMemberType(type));
    }

    protected static SimpleNumberCoercer getCoercerNonBigIntDec(Class inputValueType) {
        SimpleNumberCoercer coercer;
        if (inputValueType == Long.class || inputValueType == long.class) {
            coercer = SimpleNumberCoercerFactory.SimpleNumberCoercerLong.INSTANCE;
        } else if (isTypeInteger(inputValueType)) {
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

    protected static EPTypeClass getMemberType(EPTypeClass type) {
        Class inputValueType = type.getType();
        if (inputValueType == Long.class || inputValueType == long.class) {
            return EPTypePremade.LONGPRIMITIVE.getEPType();
        } else if (isTypeInteger(inputValueType)) {
            return EPTypePremade.INTEGERPRIMITIVE.getEPType();
        } else if (inputValueType == Double.class || inputValueType == double.class) {
            return EPTypePremade.DOUBLEPRIMITIVE.getEPType();
        } else if (inputValueType == Float.class || inputValueType == float.class) {
            return EPTypePremade.FLOATPRIMITIVE.getEPType();
        } else {
            return EPTypePremade.INTEGERPRIMITIVE.getEPType();
        }
    }
}