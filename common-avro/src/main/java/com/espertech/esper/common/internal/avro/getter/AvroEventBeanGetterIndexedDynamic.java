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
package com.espertech.esper.common.internal.avro.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterIndexedDynamic implements AvroEventPropertyGetter {
    private final String propertyName;
    private final int index;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param record       record
     * @param propertyName property
     * @param index        index
     * @return value
     */
    public static Object getAvroFieldValue(GenericData.Record record, String propertyName, int index) {
        Object value = record.get(propertyName);
        if (value == null || !(value instanceof Collection)) {
            return null;
        }
        return AvroEventBeanGetterIndexed.getAvroIndexedValue((Collection) value, index);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param record       record
     * @param propertyName property
     * @return value
     */
    public static boolean isAvroFieldExists(GenericData.Record record, String propertyName) {
        Schema.Field field = record.getSchema().getField(propertyName);
        if (field == null) {
            return false;
        }
        Object value = record.get(propertyName);
        return value == null || value instanceof Collection;
    }

    public AvroEventBeanGetterIndexedDynamic(String propertyName, int index) {
        this.propertyName = propertyName;
        this.index = index;
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        return getAvroFieldValue(record, propertyName, index);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        return getAvroFieldValue(record);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsPropertyAvro((GenericData.Record) eventBean.getUnderlying());
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return isExistsPropertyAvro(record, propertyName, index);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public Object getAvroFragment(GenericData.Record record) {
        return null;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "getAvroFieldValue", underlyingExpression, constant(propertyName), constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "isExistsPropertyAvro", underlyingExpression, constant(propertyName), constant(index));
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param record row
     * @param propertyName property
     * @param index index
     * @return flag
     */
    public static boolean isExistsPropertyAvro(GenericData.Record record, String propertyName, int index) {
        Object value = record.get(propertyName);
        if (!(value instanceof Collection)) {
            return false;
        }
        Collection collection = (Collection) value;
        return index < collection.size();
    }
}
