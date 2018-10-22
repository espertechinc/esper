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
package com.espertech.esper.common.internal.epl.agg.method.count;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationBase;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class AggregationPortableValidationCount extends AggregationPortableValidationBase {
    boolean ever;
    boolean hasFilter;
    Class countedValueType;
    boolean ignoreNulls;

    public AggregationPortableValidationCount() {
    }

    public AggregationPortableValidationCount(boolean distinct, boolean ever, boolean hasFilter, Class countedValueType, boolean ignoreNulls) {
        super(distinct);
        this.ever = ever;
        this.hasFilter = hasFilter;
        this.countedValueType = countedValueType;
        this.ignoreNulls = ignoreNulls;
    }

    public void setEver(boolean ever) {
        this.ever = ever;
    }

    public void setHasFilter(boolean hasFilter) {
        this.hasFilter = hasFilter;
    }

    public void setCountedValueType(Class countedValueType) {
        this.countedValueType = countedValueType;
    }

    public void setIgnoreNulls(boolean ignoreNulls) {
        this.ignoreNulls = ignoreNulls;
    }

    protected void validateIntoTable(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationPortableValidationCount that = (AggregationPortableValidationCount) intoTableAgg;
        AggregationValidationUtil.validateAggregationFilter(hasFilter, that.hasFilter);
        if (distinct) {
            AggregationValidationUtil.validateAggregationInputType(countedValueType, that.countedValueType);
        }
        if (ignoreNulls != that.ignoreNulls) {
            throw new ExprValidationException("The aggregation declares" +
                    (ignoreNulls ? "" : " no") +
                    " ignore nulls and provided is" +
                    (that.ignoreNulls ? "" : " no") +
                    " ignore nulls");
        }
    }

    protected Class typeOf() {
        return AggregationPortableValidationCount.class;
    }

    protected void codegenInlineSet(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(ref, "setEver", constant(ever))
                .exprDotMethod(ref, "setHasFilter", constant(hasFilter))
                .exprDotMethod(ref, "setCountedValueType", constant(countedValueType))
                .exprDotMethod(ref, "setIgnoreNulls", constant(ignoreNulls));
    }
}
