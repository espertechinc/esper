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

import java.util.*;

public class ClassProvidedPrecompileResult {
    private final Map<String, byte[]> bytes;
    private final List<Class> classes;

    public ClassProvidedPrecompileResult() {
        this.bytes = Collections.emptyMap();
        this.classes = Collections.emptyList();
    }

    public ClassProvidedPrecompileResult(Map<String, byte[]> bytes, List<Class> classes) {
        this.bytes = bytes;
        this.classes = classes;
    }

    public static ClassProvidedPrecompileResult merge(ClassProvidedPrecompileResult first, ClassProvidedPrecompileResult second) {
        Map<String, byte[]> bytes = new HashMap<>(first.getBytes());
        List<Class> classes = new ArrayList<>(first.classes);
        bytes.putAll(second.getBytes());
        classes.addAll(second.getClasses());
        return new ClassProvidedPrecompileResult(bytes, classes);
    }

    public Map<String, byte[]> getBytes() {
        return bytes;
    }

    public List<Class> getClasses() {
        return classes;
    }
}
