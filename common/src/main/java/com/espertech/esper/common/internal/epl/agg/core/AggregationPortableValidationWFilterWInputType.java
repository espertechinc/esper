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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public abstract class AggregationPortableValidationWFilterWInputType extends AggregationPortableValidationBase {
    protected boolean hasFilter;
    protected Class inputValueType;

    public AggregationPortableValidationWFilterWInputType() {
    }

    protected abstract void codegenInlineSetWFilterWInputType(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope);

    protected abstract void validateIntoTableWFilterWInputType(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException;

    public AggregationPortableValidationWFilterWInputType(boolean distinct, boolean hasFilter, Class inputValueType) {
        super(distinct);
        this.hasFilter = hasFilter;
        this.inputValueType = inputValueType;
    }

    protected final void codegenInlineSet(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref("v"), "setInputValueType", constant(inputValueType))
                .exprDotMethod(ref("v"), "setHasFilter", constant(hasFilter));
        codegenInlineSetWFilterWInputType(ref, method, symbols, classScope);
    }

    protected final void validateIntoTable(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationPortableValidationWFilterWInputType that = (AggregationPortableValidationWFilterWInputType) intoTableAgg;
        AggregationValidationUtil.validateAggregationInputType(inputValueType, that.inputValueType);
        AggregationValidationUtil.validateAggregationFilter(hasFilter, that.hasFilter);
        validateIntoTableWFilterWInputType(tableExpression, intoTableAgg, intoExpression, factory);
    }

    public void setHasFilter(boolean hasFilter) {
        this.hasFilter = hasFilter;
    }

    public void setInputValueType(Class inputValueType) {
        this.inputValueType = inputValueType;
    }
}
