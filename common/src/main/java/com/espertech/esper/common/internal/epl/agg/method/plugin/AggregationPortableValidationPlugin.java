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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidationBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class AggregationPortableValidationPlugin extends AggregationPortableValidationBase {

    private String functionName;

    public AggregationPortableValidationPlugin(boolean distinct, String functionName) {
        super(distinct);
        this.functionName = functionName;
    }

    public AggregationPortableValidationPlugin() {
    }

    protected Class typeOf() {
        return AggregationPortableValidationPlugin.class;
    }

    protected void codegenInlineSet(CodegenExpressionRef ref, CodegenMethod method, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(ref, "setFunctionName", constant(functionName));
    }

    protected void validateIntoTable(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationPortableValidationPlugin that = (AggregationPortableValidationPlugin) intoTableAgg;
        if (!functionName.equals(that.functionName)) {
            throw new ExprValidationException("The aggregation declares '" + functionName + "' and provided is '" + that.functionName + "'");
        }
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
}
