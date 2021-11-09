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

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;

import java.util.Map;

public interface ClassProvidedCompileTimeResolver extends CompileTimeResolver {
    ClassProvided resolveClass(String name);

    Pair<Class, ClasspathImportSingleRowDesc> resolveSingleRow(String name);

    Class resolveAggregationFunction(String name);

    Pair<Class, String[]> resolveAggregationMultiFunction(String name);

    boolean isEmpty();

    void addTo(ClassProvidedClassesAdd additionalClasses);

    void addTo(Map<String, byte[]> bytes);

    void removeFrom(ClassProvidedClassRemove moduleBytes);
}
