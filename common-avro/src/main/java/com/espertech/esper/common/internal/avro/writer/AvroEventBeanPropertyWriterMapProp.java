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
package com.espertech.esper.common.internal.avro.writer;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import org.apache.avro.generic.GenericData;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class AvroEventBeanPropertyWriterMapProp extends AvroEventBeanPropertyWriter {

    private final String key;

    public AvroEventBeanPropertyWriterMapProp(int propertyIndex, String key) {
        super(propertyIndex);
        this.key = key;
    }

    @Override
    public void write(Object value, GenericData.Record record) {
        avroWriteMapProp(value, record, key, index);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(AvroEventBeanPropertyWriterMapProp.class, "avroWriteMapProp", assigned, und, constant(key), constant(index));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value  value
     * @param record record
     * @param key    key
     * @param index  index
     */
    public static void avroWriteMapProp(Object value, GenericData.Record record, String key, int index) {
        Object val = record.get(index);
        if (val != null && val instanceof Map) {
            Map map = (Map) val;
            map.put(key, value);
        }
    }
}
