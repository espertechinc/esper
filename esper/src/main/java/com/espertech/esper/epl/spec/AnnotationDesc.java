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
package com.espertech.esper.epl.spec;

import com.espertech.esper.collection.Pair;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Describes an annotation.
 */
public class AnnotationDesc implements Serializable {
    private static final long serialVersionUID = 5474641956626793366L;
    private String name;

    // Map of Identifier and value={constant, array of value (Object[]), AnnotationDesc} (exclusive with value)
    private List<Pair<String, Object>> attributes;

    /**
     * Ctor.
     *
     * @param name       name of annotation
     * @param attributes are the attribute values
     */
    public AnnotationDesc(String name, List<Pair<String, Object>> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public AnnotationDesc(String name, String value) {
        this(name, Collections.<Pair<String, Object>>singletonList(new Pair<String, Object>("value", value)));
    }

    /**
     * Returns annotation interface class name.
     *
     * @return name of class, can be fully qualified
     */
    public String getName() {
        return name;
    }

    /**
     * Returns annotation attributes.
     *
     * @return the attribute values
     */
    public List<Pair<String, Object>> getAttributes() {
        return attributes;
    }
}
