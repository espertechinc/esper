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
package com.espertech.esper.common.internal.view.util;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class ViewMultiKeyHelper {
    public static void assign(ExprNode[] criteriaExpressions, MultiKeyClassRef multiKeyClassNames, CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression criteriaEval = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(criteriaExpressions, null, multiKeyClassNames, method, classScope);
        method.getBlock()
            .exprDotMethod(factory, "setCriteriaEval", criteriaEval)
            .exprDotMethod(factory, "setCriteriaTypes", constant(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions)))
            .exprDotMethod(factory, "setKeySerde", multiKeyClassNames.getExprMKSerde(method, classScope));
    }
}
