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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.lang.reflect.Array;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class MapEventBeanPropertyWriterIndexedProp extends MapEventBeanPropertyWriter {

    private final int index;

    public MapEventBeanPropertyWriterIndexedProp(String propertyName, int index) {
        super(propertyName);
        this.index = index;
    }

    @Override
    public void write(Object value, Map<String, Object> map) {
        mapWriteSetArrayProp(propertyName, index, map, value);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(this.getClass(), "mapWriteSetArrayProp", constant(propertyName), constant(index), und, assigned);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param propertyName prop name
     * @param index        index
     * @param map          map
     * @param value        value
     */
    public static void mapWriteSetArrayProp(String propertyName, int index, Map<String, Object> map, Object value) {
        Object arrayEntry = map.get(propertyName);
        if (arrayEntry != null && arrayEntry.getClass().isArray() && Array.getLength(arrayEntry) > index) {
            Array.set(arrayEntry, index, value);
        }
    }
}
