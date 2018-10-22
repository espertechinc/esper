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

import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;

public class ModuleProviderResult {
    private final ByteArrayProvidingClassLoader classLoader;
    private final ModuleProvider moduleProvider;

    public ModuleProviderResult(ByteArrayProvidingClassLoader classLoader, ModuleProvider moduleProvider) {
        this.classLoader = classLoader;
        this.moduleProvider = moduleProvider;
    }

    public ByteArrayProvidingClassLoader getClassLoader() {
        return classLoader;
    }

    public ModuleProvider getModuleProvider() {
        return moduleProvider;
    }
}
