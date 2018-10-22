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

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class AvroEventBeanPropertyWriterIndexedProp extends AvroEventBeanPropertyWriter {

    private final int indexTarget;

    public AvroEventBeanPropertyWriterIndexedProp(int propertyIndex, int indexTarget) {
        super(propertyIndex);
        this.indexTarget = indexTarget;
    }

    @Override
    public void write(Object value, GenericData.Record record) {
        avroWriteIndexedProp(value, record, index, indexTarget);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(AvroEventBeanPropertyWriterIndexedProp.class, "avroWriteIndexedProp", assigned, und, constant(index), constant(indexTarget));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value       value
     * @param record      record
     * @param index       index
     * @param indexTarget index to write to
     */
    public static void avroWriteIndexedProp(Object value, GenericData.Record record, int index, int indexTarget) {
        Object val = record.get(index);
        if (val != null && val instanceof List) {
            List list = (List) val;
            if (list.size() > indexTarget) {
                list.set(indexTarget, value);
            }
        }
    }
}
