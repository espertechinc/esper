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
package com.espertech.esper.common.internal.epl.agg.method.stddev;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprStddevNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class AggregationForgeFactoryStddev extends AggregationForgeFactoryBase {
    protected final ExprStddevNode parent;
    protected final EPTypeClass aggregatedValueType;
    protected final DataInputOutputSerdeForge distinctSerde;
    private final AggregatorMethod aggregator;

    public AggregationForgeFactoryStddev(ExprStddevNode parent, EPTypeClass aggregatedValueType, DataInputOutputSerdeForge distinctSerde) {
        this.parent = parent;
        this.aggregatedValueType = aggregatedValueType;
        this.distinctSerde = distinctSerde;

        EPTypeClass distinctType = !parent.isDistinct() ? null : aggregatedValueType;
        aggregator = new AggregatorStddev(distinctType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter());
    }

    public EPType getResultType() {
        return EPTypePremade.DOUBLEBOXED.getEPType();
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationStddev(parent.isDistinct(), parent.isHasFilter(), aggregatedValueType);
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }
}