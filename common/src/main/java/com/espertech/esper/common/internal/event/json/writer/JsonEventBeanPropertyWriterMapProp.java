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
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonEventBeanPropertyWriterMapProp extends JsonEventBeanPropertyWriter {

    private final String key;

    public JsonEventBeanPropertyWriterMapProp(JsonDelegateFactory delegateFactory, JsonUnderlyingField field, String key) {
        super(delegateFactory, field);
        this.key = key;
    }

    public void write(Object value, Object und) {
        jsonWriteMapProp(value, delegateFactory.getValue(field.getPropertyNumber(), und), key);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(JsonEventBeanPropertyWriterMapProp.class, "jsonWriteMapProp", assigned, exprDotName(und, field.getFieldName()), constant(key));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value          value
     * @param mapEntry map entry
     * @param key            key
     */
    public static void jsonWriteMapProp(Object value, Object mapEntry, String key) {
        if (!(mapEntry instanceof Map)) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>) mapEntry;
        map.put(key, value);
    }
}
