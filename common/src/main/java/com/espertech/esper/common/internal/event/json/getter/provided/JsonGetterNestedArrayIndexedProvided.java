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
package com.espertech.esper.common.internal.event.json.getter.provided;

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;
import com.espertech.esper.common.internal.event.json.getter.core.JsonGetterNestedArrayIndexedBase;

import java.lang.reflect.Field;

public final class JsonGetterNestedArrayIndexedProvided extends JsonGetterNestedArrayIndexedBase {
    private final Field field;

    public JsonGetterNestedArrayIndexedProvided(int index, JsonEventPropertyGetter innerGetter, String underlyingClassName, Field field) {
        super(index, innerGetter, underlyingClassName);
        this.field = field;
    }

    public String getFieldName() {
        return field.getName();
    }

    public Class getFieldType() {
        return field.getType();
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object item = JsonFieldGetterHelperProvided.getJsonProvidedIndexedProp(object, field, index);
        if (item == null) {
            return null;
        }
        return innerGetter.getJsonProp(item);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object item = JsonFieldGetterHelperProvided.getJsonProvidedIndexedProp(object, field, index);
        if (item == null) {
            return false;
        }
        return innerGetter.getJsonExists(item);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        Object item = JsonFieldGetterHelperProvided.getJsonProvidedIndexedProp(object, field, index);
        if (item == null) {
            return null;
        }
        return innerGetter.getJsonFragment(item);
    }
}
