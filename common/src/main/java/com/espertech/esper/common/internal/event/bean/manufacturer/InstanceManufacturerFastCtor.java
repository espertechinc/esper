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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InstanceManufacturerFastCtor implements InstanceManufacturer {
    private final InstanceManufacturerFactoryFastCtor factory;
    private final ExprEvaluator[] evaluators;

    public InstanceManufacturerFastCtor(InstanceManufacturerFactoryFastCtor factory, ExprEvaluator[] evaluators) {
        this.factory = factory;
        this.evaluators = evaluators;
    }

    public Object make(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] row = new Object[evaluators.length];
        for (int i = 0; i < row.length; i++) {
            row[i] = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return makeUnderlyingFromFastCtor(row, factory.getCtor(), factory.getTargetClass());
    }

    public static Object makeUnderlyingFromFastCtor(Object[] properties, Constructor ctor, Class target) {
        try {
            return ctor.newInstance(properties);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw getTargetExceptionAsEPException(target.getName(), e);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param targetClassName name
     * @param thrown          ex
     * @return exception
     */
    public static EPException getTargetExceptionAsEPException(String targetClassName, Throwable thrown) {
        Throwable targetException = thrown instanceof InvocationTargetException ? ((InvocationTargetException) thrown).getTargetException() : thrown;
        return new EPException("InvocationTargetException received invoking constructor for type '" + targetClassName + "': " + targetException.getMessage(), targetException);
    }

    public static CodegenExpression codegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, Class targetClass, ExprForge[] forges) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(targetClass, InstanceManufacturerFastCtor.class, codegenClassScope);

        CodegenExpression[] params = new CodegenExpression[forges.length];
        for (int i = 0; i < forges.length; i++) {
            params[i] = forges[i].evaluateCodegen(forges[i].getEvaluationType(), methodNode, exprSymbol, codegenClassScope);
        }

        methodNode.getBlock()
                .tryCatch()
                .tryReturn(newInstance(targetClass, params))
                .addCatch(Throwable.class, "t")
                .blockThrow(staticMethod(InstanceManufacturerFastCtor.class, "getTargetExceptionAsEPException", constant(targetClass.getName()), ref("t")))
                .methodEnd();
        return localMethod(methodNode);
    }
}
