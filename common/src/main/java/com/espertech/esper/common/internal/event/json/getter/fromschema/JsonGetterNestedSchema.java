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
import com.espertech.esper.common.internal.event.json.getter.core.JsonEventPropertyGetter;
import com.espertech.esper.common.internal.event.json.getter.core.JsonGetterNestedBase;

public final class JsonGetterNestedSchema extends JsonGetterNestedBase {
    private final JsonUnderlyingField field;

    public JsonGetterNestedSchema(JsonEventPropertyGetter innerGetter, String underlyingClassName, JsonUnderlyingField field) {
        super(innerGetter, underlyingClassName);
        this.field = field;
    }

    public String getFieldName() {
        return field.getFieldName();
    }

    public Class getFieldType() {
        return field.getPropertyType();
    }

    public Object getJsonProp(Object object) throws PropertyAccessException {
        Object value = JsonFieldGetterHelperSchema.getJsonSimpleProp(field, object);
        if (value == null) {
            return null;
        }
        return innerGetter.getJsonProp(value);
    }

    public boolean getJsonExists(Object object) throws PropertyAccessException {
        Object value = JsonFieldGetterHelperSchema.getJsonSimpleProp(field, object);
        if (value == null) {
            return false;
        }
        return innerGetter.getJsonExists(value);
    }

    public Object getJsonFragment(Object object) throws PropertyAccessException {
        Object value = JsonFieldGetterHelperSchema.getJsonSimpleProp(field, object);
        if (value == null) {
            return null;
        }
        return innerGetter.getJsonFragment(value);
    }
}
