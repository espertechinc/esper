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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;

public class ModuleProviderUtil {
    public static ModuleProviderResult analyze(EPCompiled compiled, ClassLoader classLoaderParent) {
        ByteArrayProvidingClassLoader classLoader = new ByteArrayProvidingClassLoader(compiled.getClasses(), classLoaderParent);
        String resourceClassName = compiled.getManifest().getModuleProviderClassName();

        // load module resource class
        Class clazz;
        try {
            clazz = classLoader.loadClass(resourceClassName);
        } catch (ClassNotFoundException e) {
            throw new EPException(e);
        }

        // instantiate
        ModuleProvider moduleResource;
        try {
            moduleResource = (ModuleProvider) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new EPException(e);
        }

        return new ModuleProviderResult(classLoader, moduleResource);
    }
}
