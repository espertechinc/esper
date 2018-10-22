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
package com.espertech.esper.common.internal.epl.agg.method.rate;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationWFilterWInputType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class AggregationPortableValidationRate extends AggregationPortableValidationWFilterWInputType {

    private long intervalTime;

    public AggregationPortableValidationRate(boolean distinct, boolean hasFilter, Class inputValueType, long intervalTime) {
        super(distinct, hasFilter, inputValueType);
        this.intervalTime = intervalTime;
    }

    public AggregationPortableValidationRate() {
    }

    protected Class typeOf() {
        return AggregationPortableValidationRate.class;
    }

    protected void codegenInlineSetWFilterWInputType(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(ref, "setIntervalTime", constant(intervalTime));
    }

    protected void validateIntoTableWFilterWInputType(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationPortableValidationRate that = (AggregationPortableValidationRate) intoTableAgg;
        if (intervalTime != that.intervalTime) {
            throw new ExprValidationException("The interval-time is " +
                    intervalTime +
                    " and provided is " +
                    that.intervalTime);
        }
    }

    public void setIntervalTime(long intervalTime) {
        this.intervalTime = intervalTime;
    }
}
