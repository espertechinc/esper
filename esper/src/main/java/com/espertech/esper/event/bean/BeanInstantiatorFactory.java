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
package com.espertech.esper.event.bean;

import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.EventBeanManufactureException;
import com.espertech.esper.util.OnDemandSunReflectionFactory;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BeanInstantiatorFactory {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorFactory.class);

    private static final Constructor<Object> SUN_JVM_OBJECT_CONSTRUCTOR;

    static {
        Constructor<Object> c = null;
        Class<?> reflectionFactoryClass = null;
        try {
            c = Object.class.getConstructor((Class[]) null);
            reflectionFactoryClass = Thread.currentThread().getContextClassLoader()
                    .loadClass("sun.reflect.ReflectionFactory");
        } catch (Exception e) {
            // ignore
        }

        SUN_JVM_OBJECT_CONSTRUCTOR = c != null && reflectionFactoryClass != null ? c : null;
    }

    public static BeanInstantiator makeInstantiator(BeanEventType beanEventType, EngineImportService engineImportService) throws EventBeanManufactureException {

        // see if we use a factory method
        if (beanEventType.getFactoryMethodName() != null) {
            return resolveFactoryMethod(beanEventType, engineImportService);
        }

        // find public ctor
        EngineImportException ctorNotFoundEx;
        try {
            engineImportService.resolveCtor(beanEventType.getUnderlyingType(), new Class[0]);
            if (beanEventType.getFastClass() != null) {
                return new BeanInstantiatorByNewInstanceFastClass(beanEventType.getFastClass());
            } else {
                return new BeanInstantiatorByNewInstanceReflection(beanEventType.getUnderlyingType());
            }
        } catch (EngineImportException ex) {
            ctorNotFoundEx = ex;
        }

        // not found ctor, see if FastClass can handle
        if (beanEventType.getFastClass() != null) {
            FastClass fastClass = beanEventType.getFastClass();
            try {
                fastClass.newInstance();
                return new BeanInstantiatorByNewInstanceFastClass(beanEventType.getFastClass());
            } catch (InvocationTargetException e) {
                String message = "Failed to instantiate class '" + fastClass.getJavaClass().getName() + "', define a factory method if the class has no suitable constructors: " + e.getTargetException().getMessage();
                log.debug(message);
            } catch (IllegalArgumentException e) {
                String message = "Failed to instantiate class '" + fastClass.getJavaClass().getName() + "', define a factory method if the class has no suitable constructors";
                log.debug(message, e);
            }
        }

        // see if JVM ReflectionFactory (specific to JVM) may handle it
        if (SUN_JVM_OBJECT_CONSTRUCTOR != null) {
            Constructor ctor = OnDemandSunReflectionFactory.getConstructor(beanEventType.getUnderlyingType(), SUN_JVM_OBJECT_CONSTRUCTOR);
            return new BeanInstantiatorByCtor(ctor);
        }

        throw new EventBeanManufactureException("Failed to find no-arg constructor and no factory method has been configured and cannot use Sun-JVM reflection to instantiate object of type " + beanEventType.getUnderlyingType().getName(), ctorNotFoundEx);
    }

    private static BeanInstantiator resolveFactoryMethod(BeanEventType beanEventType, EngineImportService engineImportService)
            throws EventBeanManufactureException {
        String factoryMethodName = beanEventType.getFactoryMethodName();

        int lastDotIndex = factoryMethodName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            try {
                Method method = engineImportService.resolveMethod(beanEventType.getUnderlyingType(), factoryMethodName, new Class[0], new boolean[0], new boolean[0]);
                if (beanEventType.getFastClass() != null) {
                    return new BeanInstantiatorByFactoryFastClass(beanEventType.getFastClass().getMethod(method));
                } else {
                    return new BeanInstantiatorByFactoryReflection(method);
                }
            } catch (EngineImportException e) {
                String message = "Failed to resolve configured factory method '" + factoryMethodName +
                        "' expected to exist for class '" + beanEventType.getUnderlyingType() + "'";
                log.info(message, e);
                throw new EventBeanManufactureException(message, e);
            }
        }

        String className = factoryMethodName.substring(0, lastDotIndex);
        String methodName = factoryMethodName.substring(lastDotIndex + 1);
        try {
            Method method = engineImportService.resolveMethodOverloadChecked(className, methodName, new Class[0], new boolean[0], new boolean[0]);
            if (beanEventType.getFastClass() != null) {
                FastClass fastClassFactory = FastClass.create(engineImportService.getFastClassClassLoader(method.getDeclaringClass()), method.getDeclaringClass());
                return new BeanInstantiatorByFactoryFastClass(fastClassFactory.getMethod(method));
            } else {
                return new BeanInstantiatorByFactoryReflection(method);
            }
        } catch (EngineImportException e) {
            String message = "Failed to resolve configured factory method '" + methodName + "' expected to exist for class '" + className + "'";
            log.info(message, e);
            throw new EventBeanManufactureException(message, e);
        }
    }
}
