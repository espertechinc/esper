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
package com.espertech.esper.common.internal.event.bean.instantiator;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class BeanInstantiatorForgeByReflection implements BeanInstantiatorForge, BeanInstantiator {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorForgeByReflection.class);

    private final Method method;

    public BeanInstantiatorForgeByReflection(Method method) {
        this.method = method;
    }

    public Object instantiate() {
        try {
            return method.invoke(null, null);
        } catch (InvocationTargetException e) {
            String message = "Unexpected exception encountered invoking factory method '" + method.getName() + "' on class '" + method.getDeclaringClass().getName() + "': " + e.getTargetException().getMessage();
            log.error(message, e);
            return null;
        } catch (IllegalAccessException ex) {
            String message = "Unexpected exception encountered invoking factory method '" + method.getName() + "' on class '" + method.getDeclaringClass().getName() + "': " + ex.getMessage();
            log.error(message, ex);
            return null;
        }
    }

    public BeanInstantiator getBeanInstantiator() {
        return this;
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        return staticMethod(method.getDeclaringClass(), method.getName());
    }
}
