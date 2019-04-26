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
package com.espertech.esper.common.internal.epl.agg.method.median;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMedianNode;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class AggregationForgeFactoryMedian extends AggregationForgeFactoryBase {
    protected final ExprMedianNode parent;
    protected final Class aggregatedValueType;
    protected final DataInputOutputSerdeForge distinctSerde;
    private AggregatorMethod aggregator;

    public AggregationForgeFactoryMedian(ExprMedianNode parent, Class aggregatedValueType, DataInputOutputSerdeForge distinctSerde) {
        this.parent = parent;
        this.aggregatedValueType = aggregatedValueType;
        this.distinctSerde = distinctSerde;
    }

    public Class getResultType() {
        return Double.class;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        Class distinctType = !parent.isDistinct() ? null : aggregatedValueType;
        aggregator = new AggregatorMedian(this, col, rowCtor, membersColumnized, classScope, distinctType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter());
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationMedian(parent.isDistinct(), parent.isHasFilter(), aggregatedValueType);
    }
}