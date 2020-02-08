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
package com.espertech.esper.common.internal.epl.classprovided.core;

import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;

import java.util.Map;

public class ClassProvidedImportClassLoaderFactory {
    public static ClassLoader getClassLoader(Map<String, byte[]> classes, ClassLoader parentClassLoader, PathRegistry<String, ClassProvided> classProvidedPathRegistry) {
        if (classProvidedPathRegistry.isEmpty()) {
            return new ByteArrayProvidingClassLoader(classes, parentClassLoader);
        }
        return new ClassProvidedImportClassLoader(classes, parentClassLoader, classProvidedPathRegistry);
    }
}
