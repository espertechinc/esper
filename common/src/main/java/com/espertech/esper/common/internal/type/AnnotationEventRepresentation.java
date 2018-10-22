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

import com.espertech.esper.common.client.annotation.EventRepresentation;
import com.espertech.esper.common.client.util.EventUnderlyingType;

import java.lang.annotation.Annotation;

public class AnnotationEventRepresentation implements EventRepresentation {
    private final EventUnderlyingType value;

    public AnnotationEventRepresentation(EventUnderlyingType value) {
        this.value = value;
    }

    public EventUnderlyingType value() {
        return value;
    }

    public Class<? extends Annotation> annotationType() {
        return EventRepresentation.class;
    }
}
