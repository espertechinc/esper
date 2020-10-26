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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
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
    protected final EPTypeClass childType;
    protected final DataInputOutputSerdeForge distinctSerde;
    protected final EPTypeClass resultType;
    protected final MathContext optionalMathContext;
    private final AggregatorMethod aggregator;

    public AggregationForgeFactoryAvg(ExprAvgNode parent, EPTypeClass childType, DataInputOutputSerdeForge distinctSerde, MathContext optionalMathContext) {
        this.parent = parent;
        this.childType = childType;
        this.distinctSerde = distinctSerde;
        this.resultType = getAvgAggregatorType(childType);
        this.optionalMathContext = optionalMathContext;

        EPTypeClass distinctValueType = !parent.isDistinct() ? null : childType;
        if (resultType.getType() == BigInteger.class || resultType.getType() == BigDecimal.class) {
            aggregator = new AggregatorAvgBig(distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), this);
        } else {
            aggregator = new AggregatorAvgNonBig(distinctValueType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter(), JavaClassHelper.getBoxedType(childType));
        }
    }

    public EPType getResultType() {
        return resultType;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationAvg(parent.isDistinct(), parent.isHasFilter(), (EPTypeClass) parent.getChildNodes()[0].getForge().getEvaluationType());
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    private EPTypeClass getAvgAggregatorType(EPTypeClass type) {
        if ((type.getType() == BigDecimal.class) || (type.getType() == BigInteger.class)) {
            return EPTypePremade.BIGDECIMAL.getEPType();
        }
        return EPTypePremade.DOUBLEBOXED.getEPType();
    }
}