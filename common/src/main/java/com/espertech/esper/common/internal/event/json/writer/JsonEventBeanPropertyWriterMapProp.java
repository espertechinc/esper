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
package com.espertech.esper.common.internal.event.json.writer;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class JsonEventBeanPropertyWriterMapProp extends JsonEventBeanPropertyWriter {

    private final String key;

    public JsonEventBeanPropertyWriterMapProp(JsonUnderlyingField field, String key) {
        super(field);
        this.key = key;
    }

    public void write(Object value, JsonEventObjectBase und) {
        jsonWriteMapProp(value, und, field.getPropertyNumber(), key);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(JsonEventBeanPropertyWriterMapProp.class, "jsonWriteMapProp", assigned, und, constant(field.getPropertyNumber()), constant(key));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value          value
     * @param und            underlying
     * @param propertyNumber property number
     * @param key            key
     */
    public static void jsonWriteMapProp(Object value, JsonEventObjectBase und, int propertyNumber, String key) {
        Map mapEntry = (Map) und.getNativeValue(propertyNumber);
        if (mapEntry != null) {
            mapEntry.put(key, value);
        }
    }
}
