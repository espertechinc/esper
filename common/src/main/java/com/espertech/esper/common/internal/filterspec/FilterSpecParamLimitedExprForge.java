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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertorForge;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;

public class FilterSpecParamLimitedExprForge extends FilterSpecParamForge {

    private final ExprNode value;
    private final MatchedEventConvertorForge convertor;
    private final SimpleNumberCoercer numberCoercer;

    public FilterSpecParamLimitedExprForge(ExprFilterSpecLookupableForge lookupable, FilterOperator filterOperator, ExprNode value, MatchedEventConvertorForge convertor, SimpleNumberCoercer numberCoercer) {
        super(lookupable, filterOperator);
        this.value = value;
        this.convertor = convertor;
        this.numberCoercer = numberCoercer;
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {
        CodegenMethod method = parent.makeChild(FilterSpecParam.class, this.getClass(), classScope);
        CodegenMethod rhsExpression = CodegenLegoMethodExpression.codegenExpression(value.getForge(), method, classScope);
        CodegenMethod matchEventConvertor = convertor.make(method, classScope);

        method.getBlock()
            .declareVar(ExprFilterSpecLookupable.class, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
            .declareVar(FilterOperator.class, "op", enumValue(FilterOperator.class, filterOperator.name()));

        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParam.class, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);

        CodegenExpression valueExpr = localMethod(rhsExpression, ref("eps"), constantTrue(), REF_EXPREVALCONTEXT);
        if (numberCoercer != null) {
            valueExpr = numberCoercer.coerceCodegenMayNullBoxed(valueExpr, value.getForge().getEvaluationType(), method, classScope);
        }
        getFilterValue.getBlock()
            .declareVar(EventBean[].class, "eps", localMethod(matchEventConvertor, FilterSpecParam.REF_MATCHEDEVENTMAP))
            .methodReturn(valueExpr);

        method.getBlock().methodReturn(param);
        return method;
    }

    public void valueExprToString(StringBuilder out, int i) {
        out.append("expression '").append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(value)).append("'");
    }
}
