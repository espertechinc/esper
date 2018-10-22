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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class ObjectArrayEventBeanPropertyWriterMapProp extends ObjectArrayEventBeanPropertyWriter {

    private final String key;

    public ObjectArrayEventBeanPropertyWriterMapProp(int propertyIndex, String key) {
        super(propertyIndex);
        this.key = key;
    }

    @Override
    public void write(Object value, Object[] array) {
        objectArrayWriteMapProp(value, array, index, key);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(ObjectArrayEventBeanPropertyWriterMapProp.class, "objectArrayWriteMapProp", assigned, und, constant(index), constant(key));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value value
     * @param array underlying
     * @param index index
     * @param key   key
     */
    public static void objectArrayWriteMapProp(Object value, Object[] array, int index, String key) {
        Map mapEntry = (Map) array[index];
        if (mapEntry != null) {
            mapEntry.put(key, value);
        }
    }
}
