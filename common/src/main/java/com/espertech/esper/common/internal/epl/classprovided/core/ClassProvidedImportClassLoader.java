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

import com.espertech.esper.common.internal.collection.PathDeploymentEntry;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import com.espertech.esper.common.internal.type.NameAndModule;

import java.util.Map;

public class ClassProvidedImportClassLoader extends ByteArrayProvidingClassLoader {
    private final PathRegistry<String, ClassProvided> pathRegistry;
    private NameAndModule[] imported;

    public ClassProvidedImportClassLoader(Map<String, byte[]> classes, ClassLoader parent, PathRegistry<String, ClassProvided> pathRegistry) {
        super(classes, parent);
        this.pathRegistry = pathRegistry;
    }

    public void setImported(NameAndModule[] imported) {
        this.imported = imported;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        assert name != null;

        if (imported == null || imported.length == 0) {
            return super.findClass(name);
        }
        for (NameAndModule nameAndModule : imported) {
            PathDeploymentEntry<ClassProvided> entry = pathRegistry.getEntryWithModule(nameAndModule.getName(), nameAndModule.getModuleName());
            for (Class clazz : entry.getEntity().getClassesMayNull()) {
                if (clazz.getName().equals(name)) {
                    return clazz;
                }
            }
        }
        return super.findClass(name);
    }
}
