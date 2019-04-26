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
package com.espertech.esper.common.internal.epl.agg.method.avg;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprAvgNode;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class AggregationForgeFactoryAvg extends AggregationForgeFactoryBase {
    protected final ExprAvgNode parent;
    protected final Class childType;
    protected final DataInputOutputSerdeForge distinctSerde;
    protected final Class resultType;
    protected final MathContext optionalMathContext;
    private AggregatorMethod aggregator;

    public AggregationForgeFactoryAvg(ExprAvgNode parent, Class childType, DataInputOutputSerdeForge distinctSerde, MathContext optionalMathContext) {
        this.parent = parent;
        this.childType = childType;
        this.distinctSerde = distinctSerde;
        this.resultType = getAvgAggregatorType(childType);
        this.optionalMathContext = optionalMathContext;
    }

    public Class getResultType() {
        return resultType;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationAvg(parent.isDistinct(), parent.isHasFilter(), parent.getChildNodes()[0].getForge().getEvaluationType());
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        Class distinctValueType = !parent.isDistinct() ? null : childType;
        if (resultType == BigInteger.class || resultType == BigDecimal.class) {
            aggregator = new AggregatorAvgBig(this, col, rowCtor, membersColumnized, classScope, distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter());
        } else {
            aggregator = new AggregatorAvgNonBig(this, col, rowCtor, membersColumnized, classScope, distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), JavaClassHelper.getBoxedType(childType));
        }
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    private Class getAvgAggregatorType(Class type) {
        if ((type == BigDecimal.class) || (type == BigInteger.class)) {
            return BigDecimal.class;
        }
        return Double.class;
    }
}