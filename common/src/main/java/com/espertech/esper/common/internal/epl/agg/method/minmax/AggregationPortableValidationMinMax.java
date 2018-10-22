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
package com.espertech.esper.common.internal.epl.agg.method.minmax;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationWFilterWInputType;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.core.MinMaxTypeEnum;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class AggregationPortableValidationMinMax extends AggregationPortableValidationWFilterWInputType {
    private MinMaxTypeEnum minMax;
    private boolean unbound;

    public AggregationPortableValidationMinMax(boolean distinct, boolean hasFilter, Class inputValueType, MinMaxTypeEnum minMax, boolean unbound) {
        super(distinct, hasFilter, inputValueType);
        this.minMax = minMax;
        this.unbound = unbound;
    }

    public AggregationPortableValidationMinMax() {
    }

    protected Class typeOf() {
        return AggregationPortableValidationMinMax.class;
    }

    protected void codegenInlineSetWFilterWInputType(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref, "setUnbound", constant(unbound))
                .exprDotMethod(ref, "setMinMax", constant(minMax));
    }

    protected void validateIntoTableWFilterWInputType(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationPortableValidationMinMax that = (AggregationPortableValidationMinMax) intoTableAgg;
        if (minMax != that.minMax) {
            throw new ExprValidationException("The aggregation declares " +
                    minMax.getExpressionText() +
                    " and provided is " +
                    that.minMax.getExpressionText());
        }
        AggregationValidationUtil.validateAggregationUnbound(unbound, that.unbound);
    }

    public void setUnbound(boolean unbound) {
        this.unbound = unbound;
    }

    public void setMinMax(MinMaxTypeEnum minMax) {
        this.minMax = minMax;
    }
}
