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
package com.espertech.esper.common.internal.epl.agg.method.plugin;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggfunc.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregationForgeFactoryBase;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodFactoryContext;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprMethodAggUtil;
import com.espertech.esper.common.internal.epl.expression.agg.method.ExprPlugInAggNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class AggregationForgeFactoryPlugin extends AggregationForgeFactoryBase {
    protected final ExprPlugInAggNode parent;
    protected final AggregationFunctionForge aggregationFunctionForge;
    private final AggregationFunctionMode mode;
    private final Class aggregatedValueType;
    private final DataInputOutputSerdeForge distinctSerde;
    private AggregatorMethod aggregator;

    public AggregationForgeFactoryPlugin(ExprPlugInAggNode parent, AggregationFunctionForge aggregationFunctionForge, AggregationFunctionMode mode, Class aggregatedValueType, DataInputOutputSerdeForge distinctSerde) {
        this.parent = parent;
        this.aggregationFunctionForge = aggregationFunctionForge;
        this.mode = mode;
        this.aggregatedValueType = aggregatedValueType;
        this.distinctSerde = distinctSerde;
    }

    public Class getResultType() {
        return aggregationFunctionForge.getValueType();
    }

    public ExprForge[] getMethodAggregationForge(boolean join, EventType[] typesPerStream) throws ExprValidationException {
        return ExprMethodAggUtil.getDefaultForges(parent.getPositionalParams(), join, typesPerStream);
    }

    public void initMethodForge(int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {

        if (mode instanceof AggregationFunctionModeManaged) {
            AggregationFunctionModeManaged singleValue = (AggregationFunctionModeManaged) mode;
            if (parent.getPositionalParams().length == 0) {
                throw new IllegalArgumentException(AggregationFunctionModeManaged.class.getSimpleName() + " requires at least one positional parameter");
            }
            Class distinctType = !parent.isDistinct() ? null : aggregatedValueType;
            aggregator = new AggregatorPlugInManaged(this, col, rowCtor, membersColumnized, classScope, distinctType, distinctSerde, parent.getChildNodes().length > 1, parent.getOptionalFilter(), singleValue);
        } else if (mode instanceof AggregationFunctionModeMultiParam) {
            AggregationFunctionModeMultiParam multiParam = (AggregationFunctionModeMultiParam) mode;
            aggregator = new AggregatorPlugInMultiParam(this, col, rowCtor, membersColumnized, classScope, multiParam);
        } else if (mode instanceof AggregationFunctionModeCodeGenerated) {
            AggregationFunctionModeCodeGenerated codeGenerated = (AggregationFunctionModeCodeGenerated) mode;
            aggregator = codeGenerated.getAggregatorMethodFactory().getAggregatorMethod(new AggregatorMethodFactoryContext(col, rowCtor, membersColumnized, classScope));
        } else {
            throw new IllegalStateException("Received an unrecognized value for mode, the value is " + mode);
        }
    }

    public AggregatorMethod getAggregator() {
        return aggregator;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationPlugin(parent.isDistinct(), parent.getAggregationFunctionName());
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationFunctionForge getAggregationFunctionForge() {
        return aggregationFunctionForge;
    }
}
