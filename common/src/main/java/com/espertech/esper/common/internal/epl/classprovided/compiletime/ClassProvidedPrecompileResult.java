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
    public final static ClassProvidedPrecompileResult EMPTY = new ClassProvidedPrecompileResult(Collections.emptyMap(), Collections.emptyList());

    private final Map<String, byte[]> bytes;
    private final List<Class> classes;

    public ClassProvidedPrecompileResult(Map<String, byte[]> bytes, List<Class> classes) {
        this.bytes = bytes;
        this.classes = classes;
    }

    public Map<String, byte[]> getBytes() {
        return bytes;
    }

    public List<Class> getClasses() {
        return classes;
    }
}
