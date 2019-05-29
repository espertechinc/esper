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

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class JsonEventBeanPropertyWriterIndexedProp extends JsonEventBeanPropertyWriter {

    private final int index;

    public JsonEventBeanPropertyWriterIndexedProp(JsonUnderlyingField propertyName, int index) {
        super(propertyName);
        this.index = index;
    }

    @Override
    public void write(Object value, JsonEventObjectBase und) {
        jsonWriteArrayProp(value, und, field.getPropertyNumber(), index);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(this.getClass(), "jsonWriteArrayProp", assigned, und, constant(field.getPropertyNumber()), constant(index));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value          value
     * @param und            underlying
     * @param propertyNumber property num
     * @param index          index
     */
    public static void jsonWriteArrayProp(Object value, JsonEventObjectBase und, int propertyNumber, int index) {
        Object arrayEntry = und.getNativeValue(propertyNumber);
        if (arrayEntry != null && arrayEntry.getClass().isArray() && Array.getLength(arrayEntry) > index) {
            Array.set(arrayEntry, index, value);
        }
    }
}
