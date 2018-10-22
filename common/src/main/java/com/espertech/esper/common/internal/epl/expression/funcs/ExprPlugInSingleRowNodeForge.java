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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeInstrumentable;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeRenderable;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;

import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public abstract class ExprPlugInSingleRowNodeForge implements ExprForgeInstrumentable, EventPropertyValueGetterForge {

    private final ExprPlugInSingleRowNode parent;
    private final boolean isReturnsConstantResult;

    public abstract Method getMethod();

    public boolean isHasMethodInvocationContextParam() {
        for (Class param : getMethod().getParameterTypes()) {
            if (param == EPLMethodInvocationContext.class) {
                return true;
            }
        }
        return false;
    }

    public ExprPlugInSingleRowNodeForge(ExprPlugInSingleRowNode parent, boolean isReturnsConstantResult) {
        this.parent = parent;
        this.isReturnsConstantResult = isReturnsConstantResult;
    }

    public boolean isReturnsConstantResult() {
        return isReturnsConstantResult;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return parent;
    }

    protected CodegenExpression[] getMethodAsParams() {
        Method method = getMethod();
        String[] parameterTypes = new String[method.getParameterTypes().length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = method.getParameterTypes()[i].getName();
        }
        return new CodegenExpression[]{constant(method.getDeclaringClass().getName()),
                constant(method.getName()), constant(method.getReturnType().getSimpleName()), constant(parameterTypes)};
    }

}
