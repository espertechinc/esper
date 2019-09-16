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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentClassLoader extends ClassLoader {
    private final Map<String, Class> classes = new HashMap<>(4);
    private final Map<String, List<String>> deploymentIds = new HashMap<>(4);

    public ParentClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class existing = classes.get(name);
        if (existing != null) {
            return existing;
        }
        return super.findClass(name);
    }

    public void add(String underlyingClassName, Class clazz, String optionalDeploymentId, boolean allowDuplicate) {
        if (!allowDuplicate && classes.containsKey(underlyingClassName)) {
            throw new IllegalStateException("Attempt to add duplicate class " + underlyingClassName + " to parent cloass loader");
        }
        classes.put(underlyingClassName, clazz);
        if (optionalDeploymentId != null) {
            List<String> existing = deploymentIds.get(optionalDeploymentId);
            if (existing == null) {
                existing = new ArrayList<>(2);
                deploymentIds.put(optionalDeploymentId, existing);
            }
            existing.add(underlyingClassName);
        }
    }

    public void remove(String deploymentId) {
        List<String> existing = deploymentIds.remove(deploymentId);
        if (existing == null) {
            return;
        }
        for (String className : existing) {
            classes.remove(className);
        }
    }

    public Map<String, Class> getClasses() {
        return classes;
    }
}
