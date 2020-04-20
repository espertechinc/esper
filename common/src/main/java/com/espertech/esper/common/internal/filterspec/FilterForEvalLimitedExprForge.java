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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_FP;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.GET_FILTER_VALUE_REFS;

public class FilterForEvalLimitedExprForge implements FilterSpecParamInValueForge {

    private final ExprNode value;
    private final MatchedEventConvertorForge convertor;
    private final SimpleNumberCoercer numberCoercer;

    public FilterForEvalLimitedExprForge(ExprNode value, MatchedEventConvertorForge convertor, SimpleNumberCoercer numberCoercer) {
        this.value = value;
        this.convertor = convertor;
        this.numberCoercer = numberCoercer;
    }

    public CodegenExpression makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(GET_FILTER_VALUE_FP);
        CodegenMethod rhsExpression = CodegenLegoMethodExpression.codegenExpression(value.getForge(), method, classScope);
        CodegenMethod matchEventConvertor = convertor.make(method, classScope);

        CodegenExpression valueExpr = localMethod(rhsExpression, ref("eps"), constantTrue(), REF_EXPREVALCONTEXT);
        if (numberCoercer != null) {
            valueExpr = numberCoercer.coerceCodegenMayNullBoxed(valueExpr, value.getForge().getEvaluationType(), method, classScope);
        }
        method.getBlock()
            .declareVar(EventBean[].class, "eps", localMethod(matchEventConvertor, FilterSpecParam.REF_MATCHEDEVENTMAP))
            .methodReturn(valueExpr);

        return localMethod(method, GET_FILTER_VALUE_REFS);
    }

    public Class getReturnType() {
        return value.getForge().getEvaluationType();
    }

    public boolean isConstant() {
        return false;
    }

    public Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        return null;
    }

    public void valueToString(StringBuilder out) {
        out.append("expression '").append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(value)).append("'");
    }
}
