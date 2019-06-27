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
package com.espertech.esper.common.internal.event.json.getter.fromschema;

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.getter.core.JsonGetterMappedBase;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterMappedSchema extends JsonGetterMappedBase {
    private final JsonUnderlyingField field;

    public JsonGetterMappedSchema(String key, String underlyingClassName, JsonUnderlyingField field) {
        super(key, underlyingClassName);
        this.field = field;
    }

    public String getFieldName() {
        return field.getFieldName();
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperSchema.getJsonMappedProp(object, field.getPropertyNumber(), key);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        return JsonFieldGetterHelperSchema.getJsonMappedExists(object, field.getPropertyNumber(), key);
    }
}
