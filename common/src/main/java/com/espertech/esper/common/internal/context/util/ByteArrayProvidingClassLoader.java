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
package com.espertech.esper.common.internal.context.util;

import java.util.Map;

public class ByteArrayProvidingClassLoader extends ClassLoader {
    private final Map<String, byte[]> classes;

    public ByteArrayProvidingClassLoader(Map<String, byte[]> classes, ClassLoader parent) {
        super(parent);
        this.classes = classes;
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        assert name != null;

        byte[] data = this.classes.get(name);
        if (data == null) {
            throw new ClassNotFoundException(name);
        } else {
            return super.defineClass(name, data, 0, data.length, this.getClass().getProtectionDomain());
        }
    }

    public Map<String, byte[]> getClasses() {
        return classes;
    }
}
