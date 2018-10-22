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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.client.util.ClassForNameProvider;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface ClasspathImportService {
    TimeAbacus getTimeAbacus();

    ClassForNameProvider getClassForNameProvider();

    ClassLoader getClassLoader();

    Class resolveClass(String className, boolean forAnnotation) throws ClasspathImportException;

    Constructor resolveCtor(Class clazz, Class[] paramTypes) throws ClasspathImportException;

    Method resolveMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType) throws ClasspathImportException;

    Method resolveMethodOverloadChecked(String className, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType) throws ClasspathImportException;

    Class resolveClassForBeanEventType(String fullyQualClassName) throws ClasspathImportException;
}
