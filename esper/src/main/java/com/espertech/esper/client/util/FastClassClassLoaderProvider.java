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
package com.espertech.esper.client.util;

/**
 * Class loader provider for use with FastClass-instance creation.
 */
public interface FastClassClassLoaderProvider {
    String NAME = "FastClassClassLoaderProvider";

    /**
     * Returns the classloader to use.
     * @param clazz class to generate FastClass for
     * @return class loader
     */
    ClassLoader classloader(Class clazz);
}
