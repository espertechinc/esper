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
 * Default class loader provider for use with FastClass-instance creation, uses current thread context class loader
 */
public class FastClassClassLoaderProviderDefault implements FastClassClassLoaderProvider {
    public final static FastClassClassLoaderProviderDefault INSTANCE = new FastClassClassLoaderProviderDefault();

    private FastClassClassLoaderProviderDefault() {
    }

    public ClassLoader classloader(Class clazz) {
        return Thread.currentThread().getContextClassLoader();
    }
}
