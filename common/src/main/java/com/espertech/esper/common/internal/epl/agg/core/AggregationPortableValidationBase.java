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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionMethodDesc;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class AggregationPortableValidationBase implements AggregationPortableValidation {
    protected boolean distinct;

    protected abstract Class typeOf();

    protected abstract void codegenInlineSet(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope);

    protected abstract void validateIntoTable(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException;

    public AggregationPortableValidationBase() {
    }

    public AggregationPortableValidationBase(boolean distinct) {
        this.distinct = distinct;
    }

    public final void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, tableExpression, intoTableAgg, intoExpression);
        AggregationPortableValidationBase that = (AggregationPortableValidationBase) intoTableAgg;
        AggregationValidationUtil.validateDistinct(distinct, that.distinct);
        validateIntoTable(tableExpression, intoTableAgg, intoExpression, factory);
    }

    public final CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(typeOf(), this.getClass(), classScope);
        method.getBlock()
                .declareVar(typeOf(), "v", newInstance(typeOf()))
                .exprDotMethod(ref("v"), "setDistinct", constant(distinct));
        codegenInlineSet(ref("v"), method, symbols, classScope);
        method.getBlock().methodReturn(ref("v"));
        return localMethod(method);
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isAggregationMethod(String name, ExprNode[] parameters, ExprValidationContext validationContext) {
        return false;
    }

    public AggregationMultiFunctionMethodDesc validateAggregationMethod(ExprValidationContext validationContext, String aggMethodName, ExprNode[] params) throws ExprValidationException {
        throw new ExprValidationException("Aggregation-method not supported for this type of aggregation");
    }
}
