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
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;

import java.util.Map;

public class ClassProvidedCompileTimeResolverEmpty implements ClassProvidedCompileTimeResolver {
    public final static ClassProvidedCompileTimeResolverEmpty INSTANCE = new ClassProvidedCompileTimeResolverEmpty();

    private ClassProvidedCompileTimeResolverEmpty() {
    }

    public ClassProvided resolveClass(String name) {
        return null;
    }

    public Pair<Class, ClasspathImportSingleRowDesc> resolveSingleRow(String name) {
        return null;
    }

    public Class resolveAggregationFunction(String name) {
        return null;
    }

    public boolean isEmpty() {
        return true;
    }

    public void addTo(Map<String, byte[]> additionalClasses) {
    }

    public void removeFrom(Map<String, byte[]> moduleBytes) {
    }
}
