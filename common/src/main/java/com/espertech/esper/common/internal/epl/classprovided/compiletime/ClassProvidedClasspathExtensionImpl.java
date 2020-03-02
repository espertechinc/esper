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
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathExtensionSingleRowDesc;
import com.espertech.esper.common.internal.settings.ClasspathExtensionSingleRowHelper;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;

import java.util.*;

public class ClassProvidedClasspathExtensionImpl implements ClassProvidedClasspathExtension {
    private final ClassProvidedCompileTimeResolver resolver;
    private final List<Class> classes = new ArrayList<>(2);
    private final Map<String, byte[]> bytes = new LinkedHashMap<>();
    private final Map<String, ClasspathExtensionSingleRowDesc> singleRowFunctionExtensions = new HashMap<>(2);

    public ClassProvidedClasspathExtensionImpl(ClassProvidedCompileTimeResolver resolver) {
        this.resolver = resolver;
    }

    public void add(List<Class> classes, Map<String, byte[]> bytes) throws ExprValidationException {
        this.classes.addAll(classes);
        this.bytes.putAll(bytes); // duplicate class names checked at compile-time
        ClasspathExtensionSingleRowHelper.processAnnotations(classes, null, singleRowFunctionExtensions);
    }

    public Class findClassByName(String className) {
        // check inlined classes
        for (Class clazz : classes) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        // check same-module (create inlined_class) or path classes
        ClassProvided provided = resolver.resolveClass(className);
        if (provided != null) {
            for (Class clazz : provided.getClassesMayNull()) {
                if (clazz.getName().equals(className)) {
                    return clazz;
                }
            }
        }
        return null;
    }

    public Pair<Class, ClasspathImportSingleRowDesc> resolveSingleRow(String name) {
        // check local
        ClasspathExtensionSingleRowDesc desc = singleRowFunctionExtensions.get(name);
        if (desc != null) {
            return desc.getAsPair();
        }
        // check same-module (create inlined_class) or path classes
        return resolver.resolveSingleRow(name);
    }

    public Map<String, byte[]> getBytes() {
        return bytes;
    }
}
