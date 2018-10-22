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
package com.espertech.esper.common.internal.epl.agg.method.leaving;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationWFilterWInputType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public class AggregationPortableValidationLeaving extends AggregationPortableValidationWFilterWInputType {

    public AggregationPortableValidationLeaving() {
    }

    public AggregationPortableValidationLeaving(boolean distinct, boolean hasFilter, Class inputValueType) {
        super(distinct, hasFilter, inputValueType);
    }

    protected Class typeOf() {
        return AggregationPortableValidationLeaving.class;
    }

    protected void validateIntoTableWFilterWInputType(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
    }

    protected void codegenInlineSetWFilterWInputType(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
    }
}
