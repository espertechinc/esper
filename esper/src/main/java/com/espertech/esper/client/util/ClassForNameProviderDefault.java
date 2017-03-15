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
 * Default provider for classname lookups.
 */
public class ClassForNameProviderDefault implements ClassForNameProvider {
    public final static ClassForNameProviderDefault INSTANCE = new ClassForNameProviderDefault();

    private ClassForNameProviderDefault() {
    }

    public Class classForName(String className) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return Class.forName(className, true, cl);
    }
}
