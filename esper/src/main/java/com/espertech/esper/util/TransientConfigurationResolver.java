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
package com.espertech.esper.util;

import com.espertech.esper.client.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TransientConfigurationResolver {
    private static final Logger log = LoggerFactory.getLogger(TransientConfigurationResolver.class);

    public static ClassForNameProvider resolveClassForNameProvider(Map<String, Object> transientConfiguration) {
        return resolve(transientConfiguration, ClassForNameProviderDefault.INSTANCE, ClassForNameProviderDefault.NAME, ClassForNameProvider.class);
    }

    public static FastClassClassLoaderProvider resolveFastClassClassLoaderProvider(Map<String, Object> transientConfiguration) {
        return resolve(transientConfiguration, FastClassClassLoaderProviderDefault.INSTANCE, FastClassClassLoaderProviderDefault.NAME, FastClassClassLoaderProvider.class);
    }

    public static ClassLoaderProvider resolveClassLoader(Map<String, Object> transientConfiguration) {
        return resolve(transientConfiguration, ClassLoaderProviderDefault.INSTANCE, ClassLoaderProviderDefault.NAME, ClassLoaderProvider.class);
    }

    private static <T> T resolve(Map<String, Object> transientConfiguration, T defaultProvider, String name, Class interfaceClass) {
        if (transientConfiguration == null) {
            return defaultProvider;
        }
        Object value = transientConfiguration.get(name);
        if (value == null) {
            return defaultProvider;
        }
        if (!JavaClassHelper.isImplementsInterface(value.getClass(), interfaceClass)) {
            log.warn("For transient configuration '" + name + "' expected an object implementing " + interfaceClass.getName() + " but received " + value.getClass() + ", using default provider");
            return defaultProvider;
        }
        return (T) value;
    }
}
