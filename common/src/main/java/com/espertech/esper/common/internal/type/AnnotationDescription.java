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
package com.espertech.esper.common.internal.type;

import com.espertech.esper.common.client.annotation.Description;

import java.lang.annotation.Annotation;

public class AnnotationDescription implements Description {
    private final String description;

    public AnnotationDescription(String description) {
        this.description = description;
    }

    public String value() {
        return description;
    }

    public Class<? extends Annotation> annotationType() {
        return Description.class;
    }

    public String toString() {
        return "@Description(\"" + description + "\")";
    }
}
