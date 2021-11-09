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
import com.espertech.esper.common.internal.epl.util.CompileTimeRegistry;

import java.util.HashMap;
import java.util.Map;

public class ClassProvidedCompileTimeRegistry implements CompileTimeRegistry {
    private final Map<String, ClassProvided> classes = new HashMap<>();

    public void newClass(ClassProvided detail) {
        if (!detail.getVisibility().isModuleProvidedAccessModifier()) {
            throw new IllegalStateException("Invalid visibility for contexts");
        }
        String key = detail.getClassName();
        ClassProvided existing = classes.get(key);
        if (existing != null) {
            throw new IllegalStateException("Duplicate class-provided-by-application has been encountered for name '" + key + "'");
        }
        classes.put(key, detail);
    }

    public Map<String, ClassProvided> getClasses() {
        return classes;
    }

    public void addTo(ClassProvidedClassesAdd add) {
        for (Map.Entry<String, ClassProvided> entry : classes.entrySet()) {
            add.add(entry.getValue().getBytes());
        }
    }

    public void addTo(Map<String, byte[]> bytes) {
        for (Map.Entry<String, ClassProvided> entry : classes.entrySet()) {
            bytes.putAll(entry.getValue().getBytes());
        }
    }
}
