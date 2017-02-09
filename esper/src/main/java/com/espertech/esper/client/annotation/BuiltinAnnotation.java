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
package com.espertech.esper.client.annotation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * List of built-in annotations.
 */
public class BuiltinAnnotation {

    /**
     * List of built-in annotations.
     */
    public final static Map<String, Class> BUILTIN = new HashMap<String, Class>();

    static {
        for (Class clazz : new Class[]{Audit.class, Description.class, Drop.class, Durable.class, EventRepresentation.class,
            ExternalDW.class, ExternalDWKey.class, ExternalDWListener.class, ExternalDWQuery.class, ExternalDWSetting.class,
            ExternalDWValue.class, Hint.class, Hook.class, IterableUnbound.class, Name.class, NoLock.class, Overflow.class,
            Priority.class, Resilient.class, Tag.class, Transient.class
        }) {
            BUILTIN.put(clazz.getSimpleName().toLowerCase(Locale.ENGLISH), clazz);
        }
    }
}
