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
 * Default class loader provider returns the current thread context classloader.
 */
public class ClassLoaderProviderDefault implements ClassLoaderProvider {
    public final static ClassLoaderProviderDefault INSTANCE = new ClassLoaderProviderDefault();

    private ClassLoaderProviderDefault() {
    }

    public ClassLoader classloader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
