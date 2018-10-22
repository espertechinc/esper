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
package com.espertech.esper.common.internal.epl.agg.method.firstlastever;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationWFilterWInputType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class AggregationPortableValidationFirstLastEver extends AggregationPortableValidationWFilterWInputType {
    private boolean isFirst;

    public AggregationPortableValidationFirstLastEver() {
    }

    public AggregationPortableValidationFirstLastEver(boolean distinct, boolean hasFilter, Class inputValueType, boolean isFirst) {
        super(distinct, hasFilter, inputValueType);
        this.isFirst = isFirst;
    }

    protected Class typeOf() {
        return AggregationPortableValidationFirstLastEver.class;
    }

    protected void codegenInlineSetWFilterWInputType(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(ref, "setFirst", constant(isFirst));
    }

    protected void validateIntoTableWFilterWInputType(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationPortableValidationFirstLastEver that = (AggregationPortableValidationFirstLastEver) intoTableAgg;
        if (isFirst != that.isFirst) {
            throw new ExprValidationException("The aggregation declares " +
                    (isFirst ? "firstever" : "lastever") +
                    " and provided is " +
                    (that.isFirst ? "firstever" : "lastever"));
        }
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }
}
