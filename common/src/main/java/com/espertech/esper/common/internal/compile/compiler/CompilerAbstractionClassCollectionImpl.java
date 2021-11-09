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
package com.espertech.esper.common.internal.compile.compiler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompilerAbstractionClassCollectionImpl implements CompilerAbstractionClassCollection {
    private final ConcurrentHashMap<String, byte[]> classes = new ConcurrentHashMap<>();

    public Map<String, byte[]> getClasses() {
        return classes;
    }

    public void add(Map<String, byte[]> bytes) {
        classes.putAll(bytes);
    }

    public void remove(String name) {
        classes.remove(name);
    }
}
