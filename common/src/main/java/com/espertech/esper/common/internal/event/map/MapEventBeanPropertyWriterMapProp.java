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

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class MapEventBeanPropertyWriterMapProp extends MapEventBeanPropertyWriter {

    private final String key;

    public MapEventBeanPropertyWriterMapProp(String propertyName, String key) {
        super(propertyName);
        this.key = key;
    }

    @Override
    public void write(Object value, Map<String, Object> map) {
        mapWriteSetMapProp(propertyName, key, map, value);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(this.getClass(), "mapWriteSetMapProp", constant(propertyName), constant(key), und, assigned);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param propertyName prop name
     * @param key          key
     * @param map          map
     * @param value        value
     */
    public static void mapWriteSetMapProp(String propertyName, String key, Map<String, Object> map, Object value) {
        Map mapEntry = (Map) map.get(propertyName);
        if (mapEntry != null) {
            mapEntry.put(key, value);
        }
    }
}
