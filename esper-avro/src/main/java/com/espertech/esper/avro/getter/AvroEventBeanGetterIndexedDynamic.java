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
package com.espertech.esper.avro.getter;

import com.espertech.esper.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterIndexedDynamic implements AvroEventPropertyGetter {
    private final String propertyName;
    private final int index;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param record record
     * @param propertyName property
     * @param index index
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
     * @param record record
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
        return isAvroFieldExists(record, propertyName);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public Object getAvroFragment(GenericData.Record record) {
        return null;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethodTakingExprAndConst(this.getClass(), "getAvroFieldValue", underlyingExpression, propertyName, index);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethodTakingExprAndConst(this.getClass(), "isAvroFieldExists", underlyingExpression, propertyName);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }
}
