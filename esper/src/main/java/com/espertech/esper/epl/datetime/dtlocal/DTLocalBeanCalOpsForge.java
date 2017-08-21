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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.event.EventPropertyGetterSPI;

public class DTLocalBeanCalOpsForge implements DTLocalForge {
    protected final EventPropertyGetterSPI getter;
    protected final Class getterReturnType;
    protected final DTLocalForge inner;
    protected final Class innerReturnType;

    public DTLocalBeanCalOpsForge(EventPropertyGetterSPI getter, Class getterReturnType, DTLocalForge inner, Class innerReturnType) {
        this.getter = getter;
        this.getterReturnType = getterReturnType;
        this.inner = inner;
        this.innerReturnType = innerReturnType;
    }

    public DTLocalEvaluator getDTEvaluator() {
        return new DTLocalBeanCalOpsEval(this, inner.getDTEvaluator());
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return DTLocalBeanCalOpsEval.codegen(this, inner, innerType, codegenMethodScope, exprSymbol, codegenClassScope);
    }
}
