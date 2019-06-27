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
import com.espertech.esper.common.internal.event.json.getter.core.JsonGetterMappedBase;

import java.lang.reflect.Field;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterMappedProvided extends JsonGetterMappedBase {
    private final Field field;

    public JsonGetterMappedProvided(String key, String underlyingClassName, Field field) {
        super(key, underlyingClassName);
        this.field = field;
    }

    public String getFieldName() {
        return field.getName();
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperProvided.getJsonProvidedMappedProp(object, field, key);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperProvided.getJsonProvidedMappedExists(object, field, key);
    }
}
