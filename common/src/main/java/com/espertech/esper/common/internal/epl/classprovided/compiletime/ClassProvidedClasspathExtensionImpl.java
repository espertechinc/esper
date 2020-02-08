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

import java.util.List;
import java.util.Map;

public class ClassProvidedClasspathExtensionImpl implements ClassProvidedClasspathExtension {
    private final List<Class> classes;
    private final Map<String, byte[]> bytes;
    private final ClassProvidedCompileTimeResolver resolver;

    public ClassProvidedClasspathExtensionImpl(ClassProvidedPrecompileResult classes, ClassProvidedCompileTimeResolver resolver) {
        this.classes = classes.getClasses();
        this.bytes = classes.getBytes();
        this.resolver = resolver;
    }

    public Class findClassByName(String className) {
        // Check inlined classes
        for (Class clazz : classes) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        // Check path classes
        ClassProvided provided = resolver.resolve(className);
        if (provided != null) {
            for (Class clazz : provided.getClassesMayNull()) {
                if (clazz.getName().equals(className)) {
                    return clazz;
                }
            }
        }
        return null;
    }

    public Map<String, byte[]> getBytes() {
        return bytes;
    }
}
