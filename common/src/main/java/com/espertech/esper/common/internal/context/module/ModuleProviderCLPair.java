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
package com.espertech.esper.common.internal.context.module;

public class ModuleProviderCLPair {
    private final ClassLoader classLoader;
    private final ModuleProvider moduleProvider;

    public ModuleProviderCLPair(ClassLoader classLoader, ModuleProvider moduleProvider) {
        this.classLoader = classLoader;
        this.moduleProvider = moduleProvider;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ModuleProvider getModuleProvider() {
        return moduleProvider;
    }
}
