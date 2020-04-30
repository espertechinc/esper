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
package com.espertech.esper.common.internal.epl.classprovided.compiletime;

import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathExtensionAggregationFunction;
import com.espertech.esper.common.internal.settings.ClasspathExtensionAggregationMultiFunction;
import com.espertech.esper.common.internal.settings.ClasspathExtensionClass;
import com.espertech.esper.common.internal.settings.ClasspathExtensionSingleRow;

import java.util.List;
import java.util.Map;

public interface ClassProvidedClasspathExtension extends ClasspathExtensionClass, ClasspathExtensionSingleRow,
    ClasspathExtensionAggregationFunction, ClasspathExtensionAggregationMultiFunction {
    void add(List<Class> classes, Map<String, byte[]> bytes) throws StatementSpecCompileException, ExprValidationException;
    Map<String, byte[]> getBytes();
    boolean isLocalInlinedClass(Class<?> declaringClass);
}
