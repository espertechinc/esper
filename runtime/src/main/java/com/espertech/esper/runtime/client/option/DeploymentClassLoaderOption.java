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

/**
 * Implement this interface to provide a custom class loader for a deployment.
 */
public interface DeploymentClassLoaderOption {
    /**
     * Returns the classloader to use for the deployment.
     * <p>
     * Implementations can use the runtime's parent class loader
     * or can use the configuration transient values that are provided by the context.
     * </p>
     *
     * @param env the deployment context
     * @return class loader (null is not supported)
     */
    ClassLoader getClassLoader(DeploymentClassLoaderContext env);
}
