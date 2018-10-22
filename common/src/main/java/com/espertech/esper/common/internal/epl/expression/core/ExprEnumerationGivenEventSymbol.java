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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProvider;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ExprEnumerationGivenEventSymbol implements CodegenSymbolProvider {
    private CodegenExpressionRef optionalExprEvalCtxRef;
    private CodegenExpressionRef optionalEventRef;

    public CodegenExpressionRef getAddExprEvalCtx(CodegenMethodScope scope) {
        if (optionalExprEvalCtxRef == null) {
            optionalExprEvalCtxRef = ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
        }
        scope.addSymbol(optionalExprEvalCtxRef);
        return optionalExprEvalCtxRef;
    }

    public CodegenExpressionRef getAddEvent(CodegenMethodScope scope) {
        if (optionalEventRef == null) {
            optionalEventRef = ref("event");
        }
        scope.addSymbol(optionalEventRef);
        return optionalEventRef;
    }

    public void provide(Map<String, Class> symbols) {
        if (optionalExprEvalCtxRef != null) {
            symbols.put(optionalExprEvalCtxRef.getRef(), ExprEvaluatorContext.class);
        }
        if (optionalEventRef != null) {
            symbols.put(optionalEventRef.getRef(), EventBean.class);
        }
    }
}
