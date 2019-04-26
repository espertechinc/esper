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
package com.espertech.esper.common.internal.epl.agg.method.avedev;


import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprAvedevNode;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class AggregationForgeFactoryAvedev extends AggregationForgeFactoryBase {
    protected final ExprAvedevNode parent;
    protected final Class aggregatedValueType;
    protected final DataInputOutputSerdeForge distinctSerde;
    protected final ExprNode[] positionalParameters;
    private AggregatorMethod aggregator;

    public AggregationForgeFactoryAvedev(ExprAvedevNode parent, Class aggregatedValueType, DataInputOutputSerdeForge distinctSerde, ExprNode[] positionalParameters) {
        this.parent = parent;
        this.aggregatedValueType = aggregatedValueType;
        this.distinctSerde = distinctSerde;
        this.positionalParameters = positionalParameters;
    }

    public Class getResultType() {
        return Double.class;
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        Class distinctType = !parent.isDistinct() ? null : aggregatedValueType;
        aggregator = new AggregatorAvedev(this, col, rowCtor, membersColumnized, classScope, distinctType, distinctSerde, parent.isHasFilter(), parent.getOptionalFilter());
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationAvedev(parent.isDistinct(), parent.isHasFilter(), aggregatedValueType);
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }
}