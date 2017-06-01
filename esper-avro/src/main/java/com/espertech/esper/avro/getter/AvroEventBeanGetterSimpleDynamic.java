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
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterSimpleDynamic implements AvroEventPropertyGetter {
    private final String propertyName;

    public AvroEventBeanGetterSimpleDynamic(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getAvroFieldValue(GenericData.Record record) throws PropertyAccessException {
        return record.get(propertyName);
    }

    public Object get(EventBean theEvent) {
        return getAvroFieldValue((GenericData.Record) theEvent.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsPropertyAvro((GenericData.Record) eventBean.getUnderlying());
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return record.getSchema().getField(propertyName) != null;
    }

    public Object getFragment(EventBean obj) {
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
        return CodegenExpressionBuilder.exprDotMethod(underlyingExpression, "get", constant(propertyName));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return notEqualsNull(exprDotMethodChain(underlyingExpression).addNoParam("getSchema").addWConst("getField", propertyName));
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }
}

