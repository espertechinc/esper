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

import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvidedCollector;

import java.util.Map;

public class ClassProvidedCollectorCompileTime implements ClassProvidedCollector {
    private final Map<String, ClassProvided> moduleClassProvideds;
    private final ClassLoader parentClassLoader;

    public ClassProvidedCollectorCompileTime(Map<String, ClassProvided> moduleClassProvideds, ClassLoader parentClassLoader) {
        this.moduleClassProvideds = moduleClassProvideds;
        this.parentClassLoader = parentClassLoader;
    }

    public void registerClass(String className, ClassProvided meta) {
        moduleClassProvideds.put(className, meta);
        meta.loadClasses(parentClassLoader);
    }
}
