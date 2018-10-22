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

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class ObjectArrayEventBeanPropertyWriterIndexedProp extends ObjectArrayEventBeanPropertyWriter {

    private final int indexTarget;

    public ObjectArrayEventBeanPropertyWriterIndexedProp(int propertyIndex, int indexTarget) {
        super(propertyIndex);
        this.indexTarget = indexTarget;
    }

    @Override
    public void write(Object value, Object[] array) {
        objectArrayWriteIndexedProp(value, array, index, indexTarget);
    }

    @Override
    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return staticMethod(ObjectArrayEventBeanPropertyWriterIndexedProp.class, "objectArrayWriteIndexedProp",
                assigned, und, constant(index), constant(indexTarget));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value       value
     * @param array       underlying
     * @param index       from
     * @param indexTarget to
     */
    public static void objectArrayWriteIndexedProp(Object value, Object[] array, int index, int indexTarget) {
        Object arrayEntry = array[index];
        if (arrayEntry != null && arrayEntry.getClass().isArray() && Array.getLength(arrayEntry) > indexTarget) {
            Array.set(arrayEntry, indexTarget, value);
        }
    }
}
