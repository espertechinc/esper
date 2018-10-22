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

import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.EventBeanManufactureException;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BeanInstantiatorFactory {
    private final static Logger log = LoggerFactory.getLogger(BeanInstantiatorFactory.class);

    public static final Constructor<Object> SUN_JVM_OBJECT_CONSTRUCTOR;

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

    public static BeanInstantiatorForge makeInstantiator(BeanEventType beanEventType, ClasspathImportService classpathImportService) throws EventBeanManufactureException {

        // see if we use a factory method
        if (beanEventType.getFactoryMethodName() != null) {
            return resolveFactoryMethod(beanEventType, classpathImportService);
        }

        // find public ctor
        ClasspathImportException ctorNotFoundEx;
        try {
            classpathImportService.resolveCtor(beanEventType.getUnderlyingType(), new Class[0]);
            return new BeanInstantiatorForgeByNewInstanceReflection(beanEventType.getUnderlyingType());
        } catch (ClasspathImportException ex) {
            ctorNotFoundEx = ex;
        }

        // see if JVM ReflectionFactory (specific to JVM) may handle it
        if (SUN_JVM_OBJECT_CONSTRUCTOR != null) {
            Class underlyingType = beanEventType.getUnderlyingType();
            return new BeanInstantiatorForgeByCtor(underlyingType);
        }

        throw new EventBeanManufactureException("Failed to find no-arg constructor and no factory method has been configured and cannot use Sun-JVM reflection to instantiate object of type " + beanEventType.getUnderlyingType().getName(), ctorNotFoundEx);
    }

    private static BeanInstantiatorForge resolveFactoryMethod(BeanEventType beanEventType, ClasspathImportService classpathImportService)
            throws EventBeanManufactureException {
        String factoryMethodName = beanEventType.getFactoryMethodName();

        int lastDotIndex = factoryMethodName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            try {
                Method method = classpathImportService.resolveMethod(beanEventType.getUnderlyingType(), factoryMethodName, new Class[0], new boolean[0], new boolean[0]);
                return new BeanInstantiatorForgeByReflection(method);
            } catch (ClasspathImportException e) {
                String message = "Failed to resolve configured factory method '" + factoryMethodName +
                        "' expected to exist for class '" + beanEventType.getUnderlyingType() + "'";
                log.info(message, e);
                throw new EventBeanManufactureException(message, e);
            }
        }

        String className = factoryMethodName.substring(0, lastDotIndex);
        String methodName = factoryMethodName.substring(lastDotIndex + 1);
        try {
            Method method = classpathImportService.resolveMethodOverloadChecked(className, methodName, new Class[0], new boolean[0], new boolean[0]);
            return new BeanInstantiatorForgeByReflection(method);
        } catch (ClasspathImportException e) {
            String message = "Failed to resolve configured factory method '" + methodName + "' expected to exist for class '" + className + "'";
            log.info(message, e);
            throw new EventBeanManufactureException(message, e);
        }
    }
}
