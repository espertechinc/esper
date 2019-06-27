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
package com.espertech.esper.common.client.util;

import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Map;

/**
 * Enumeration of event representation.
 */
public enum EventUnderlyingType {
    /**
     * Event representation is object-array (Object[]).
     */
    OBJECTARRAY,

    /**
     * Event representation is Map (any java.util.Map interface implementation).
     */
    MAP,

    /**
     * Event representation is Avro (GenericData.Record).
     */
    AVRO,

    /**
     * Event representation is Json with underlying generation.
     */
    JSON;

    private final static String AVRO_TYPE_NAME = JavaClassHelper.APACHE_AVRO_GENERIC_RECORD_CLASSNAME;

    static {
        OBJECTARRAY.underlyingClass = Object[].class;
        OBJECTARRAY.underlyingClassName = Object[].class.getName();
        MAP.underlyingClass = Map.class;
        MAP.underlyingClassName = MAP.underlyingClass.getName();
        AVRO.underlyingClassName = AVRO_TYPE_NAME;
        AVRO.underlyingClass = null;
        JSON.underlyingClass = Object.class;
        JSON.underlyingClassName = Object.class.getName();
    }

    private String underlyingClassName;
    private Class underlyingClass;

    /**
     * Returns the default underlying type.
     *
     * @return default underlying type
     */
    public static EventUnderlyingType getDefault() {
        return MAP;
    }

    /**
     * Returns the class name of the default underlying type.
     *
     * @return default underlying type class name
     */
    public String getUnderlyingClassName() {
        return underlyingClassName;
    }

    /**
     * Returns the class of the default underlying type.
     *
     * @return default underlying type class
     */
    public Class getUnderlyingClass() {
        return underlyingClass;
    }
}
