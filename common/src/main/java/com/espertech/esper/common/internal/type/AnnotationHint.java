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

import com.espertech.esper.common.client.annotation.AppliesTo;
import com.espertech.esper.common.client.annotation.Hint;

import java.lang.annotation.Annotation;

public class AnnotationHint implements Hint {
    private final String value;
    private final AppliesTo applies;
    private final String model;

    public AnnotationHint(String value, AppliesTo applies, String model) {
        this.value = value;
        this.applies = applies;
        this.model = model;
    }

    public String value() {
        return value;
    }

    public AppliesTo applies() {
        return applies;
    }

    public String model() {
        return model;
    }

    public Class<? extends Annotation> annotationType() {
        return Hint.class;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotationHint that = (AnnotationHint) o;

        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (applies != that.applies) return false;
        return model != null ? model.equals(that.model) : that.model == null;
    }

    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (applies != null ? applies.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        return result;
    }
}
