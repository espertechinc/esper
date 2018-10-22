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
package com.espertech.esper.common.internal.epl.agg.method.nth;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationWFilterWInputType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class AggregationPortableValidationNth extends AggregationPortableValidationWFilterWInputType {

    private int size;

    public AggregationPortableValidationNth(boolean distinct, boolean hasFilter, Class inputValueType, int size) {
        super(distinct, hasFilter, inputValueType);
        this.size = size;
    }

    public AggregationPortableValidationNth() {
    }

    protected Class typeOf() {
        return AggregationPortableValidationNth.class;
    }

    protected void codegenInlineSetWFilterWInputType(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(ref, "setSize", constant(size));
    }

    protected void validateIntoTableWFilterWInputType(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationPortableValidationNth that = (AggregationPortableValidationNth) intoTableAgg;
        if (size != that.size) {
            throw new ExprValidationException("The size is " +
                    size +
                    " and provided is " +
                    that.size);
        }
    }

    public void setSize(int size) {
        this.size = size;
    }
}
