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

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonEventBeanPropertyWriterIndexedProp extends JsonEventBeanPropertyWriter {

    private final int index;

    public JsonEventBeanPropertyWriterIndexedProp(JsonDelegateFactory delegateFactory, JsonUnderlyingField propertyName, int index) {
        super(delegateFactory, propertyName);
        this.index = index;
    }

    @Override
    public void write(Object value, Object und) {
        jsonWriteArrayProp(value, delegateFactory.getValue(field.getPropertyNumber(), und), index);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(this.getClass(), "jsonWriteArrayProp", assigned, exprDotName(und, field.getFieldName()), constant(index));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value      value
     * @param arrayEntry array
     * @param index      index
     */
    public static void jsonWriteArrayProp(Object value, Object arrayEntry, int index) {
        if (arrayEntry != null && arrayEntry.getClass().isArray() && Array.getLength(arrayEntry) > index) {
            Array.set(arrayEntry, index, value);
        }
    }
}
