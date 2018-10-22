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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.util.OnDemandSunReflectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.bean.instantiator.BeanInstantiatorFactory.SUN_JVM_OBJECT_CONSTRUCTOR;

public class BeanInstantiatorForgeByCtor implements BeanInstantiatorForge {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorForgeByCtor.class);

    private final Class underlyingType;

    public BeanInstantiatorForgeByCtor(Class underlyingType) {
        this.underlyingType = underlyingType;
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpressionField ctor = codegenClassScope.addFieldUnshared(true, Constructor.class, staticMethod(BeanInstantiatorForgeByCtor.class, "getSunJVMCtor", constant(underlyingType)));
        return staticMethod(BeanInstantiatorForgeByCtor.class, "instantiateSunJVMCtor", ctor);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param underlyingType underlying
     * @return ctor
     */
    public static Constructor getSunJVMCtor(Class underlyingType) {
        return OnDemandSunReflectionFactory.getConstructor(underlyingType, SUN_JVM_OBJECT_CONSTRUCTOR);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param ctor ctor
     * @return object
     */
    public static Object instantiateSunJVMCtor(Constructor ctor) {
        try {
            return ctor.newInstance();
        } catch (InvocationTargetException e) {
            String message = "Unexpected exception encountered invoking constructor '" + ctor.getName() + "' on class '" + ctor.getDeclaringClass().getName() + "': " + e.getTargetException().getMessage();
            log.error(message, e);
            return null;
        } catch (IllegalAccessException | InstantiationException ex) {
            return handle(ex, ctor);
        }
    }

    public BeanInstantiator getBeanInstantiator() {
        return new BeanInstantiatorByCtor(getSunJVMCtor(underlyingType));
    }

    private static Object handle(Exception e, Constructor ctor) {
        String message = "Unexpected exception encountered invoking newInstance on class '" + ctor.getDeclaringClass().getName() + "': " + e.getMessage();
        log.error(message, e);
        return null;
    }
}
