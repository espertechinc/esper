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
package com.espertech.esper.common.internal.epl.datetime.dtlocal;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

public class DTLocalBeanIntervalNoEndTSForge implements DTLocalForge {
    protected final EventPropertyGetterSPI getter;
    protected final Class getterResultType;
    protected final DTLocalForge inner;
    protected final Class returnType;

    public DTLocalBeanIntervalNoEndTSForge(EventPropertyGetterSPI getter, Class getterResultType, DTLocalForge inner, Class returnType) {
        this.getter = getter;
        this.getterResultType = getterResultType;
        this.inner = inner;
        this.returnType = returnType;
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalBeanIntervalNoEndTSEval(getter, inner.getDTEvaluator());
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return DTLocalBeanIntervalNoEndTSEval.codegen(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
    }
}
