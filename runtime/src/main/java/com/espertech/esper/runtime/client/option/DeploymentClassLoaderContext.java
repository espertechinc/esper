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
package com.espertech.esper.runtime.client.option;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.context.util.ParentClassLoader;

/**
 * Provides the environment to {@link DeploymentClassLoaderOption}.
 */
public class DeploymentClassLoaderContext {
    private final ParentClassLoader runtimeParentClassLoader;
    private final Configuration configuration;

    /**
     * Ctor.
     * @param runtimeParentClassLoader runtime parent class loader
     * @param configuration configuration
     */
    public DeploymentClassLoaderContext(ParentClassLoader runtimeParentClassLoader, Configuration configuration) {
        this.runtimeParentClassLoader = runtimeParentClassLoader;
        this.configuration = configuration;
    }

    /**
     * Returns the classloader that is the parent class loader for the runtime.
     * @return parent class loader
     */
    public ParentClassLoader getRuntimeParentClassLoader() {
        return runtimeParentClassLoader;
    }

    /**
     * Returns the configuration.
     * @return configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }
}
