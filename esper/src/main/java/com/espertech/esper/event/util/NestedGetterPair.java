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
package com.espertech.esper.event.util;

import com.espertech.esper.client.EventPropertyGetter;

/**
 * Value-object for rendering support of a nested property value.
 */
public class NestedGetterPair {
    private String name;
    private EventPropertyGetter getter;
    private RendererMeta metadata;
    private boolean isArray;

    /**
     * Ctor.
     *
     * @param getter   for retrieving the value
     * @param name     property name
     * @param metadata the nested properties metadata
     * @param isArray  indicates whether this is an indexed property
     */
    public NestedGetterPair(EventPropertyGetter getter, String name, RendererMeta metadata, boolean isArray) {
        this.getter = getter;
        this.name = name;
        this.metadata = metadata;
        this.isArray = isArray;
    }

    /**
     * Returns the getter.
     *
     * @return getter
     */
    public EventPropertyGetter getGetter() {
        return getter;
    }

    /**
     * Returns the property name.
     *
     * @return property name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the nested property's metadata.
     *
     * @return metadata
     */
    public RendererMeta getMetadata() {
        return metadata;
    }

    /**
     * Returns true if an indexed nested property.
     *
     * @return indicator whether indexed
     */
    public boolean isArray() {
        return isArray;
    }
}
