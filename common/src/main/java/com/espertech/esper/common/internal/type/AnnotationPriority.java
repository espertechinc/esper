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

import com.espertech.esper.common.client.annotation.Priority;

import java.lang.annotation.Annotation;

public class AnnotationPriority implements Priority {
    private final int priority;

    public AnnotationPriority(int priority) {
        this.priority = priority;
    }

    public int value() {
        return priority;
    }

    public Class<? extends Annotation> annotationType() {
        return Priority.class;
    }

    public String toString() {
        return "@Priority(\"" + priority + "\")";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotationPriority that = (AnnotationPriority) o;

        return priority == that.priority;
    }

    public int hashCode() {
        return priority;
    }
}
