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
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedDynamicPoly implements EventPropertyGetterSPI {

    private final String fieldTop;
    private final AvroEventPropertyGetter getter;

    public AvroEventBeanGetterNestedDynamicPoly(String fieldTop, AvroEventPropertyGetter getter) {
        this.fieldTop = fieldTop;
        this.getter = getter;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return get((GenericData.Record) eventBean.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsProperty((GenericData.Record) eventBean.getUnderlying());
    }

    private Object get(GenericData.Record record) throws PropertyAccessException {
        GenericData.Record inner = (GenericData.Record) record.get(fieldTop);
        return inner == null ? null : getter.getAvroFieldValue(inner);
    }

    private String getCodegen(CodegenContext context) {
        return context.addMethod(Object.class, GenericData.Record.class, "record", this.getClass())
                .declareVar(GenericData.Record.class, "inner", cast(GenericData.Record.class, exprDotMethod(ref("record"), "get", constant(fieldTop))))
                .methodReturn(conditional(equalsNull(ref("inner")), constantNull(), getter.codegenUnderlyingGet(ref("inner"), context)));
    }

    private boolean isExistsProperty(GenericData.Record record) {
        Schema.Field field = record.getSchema().getField(fieldTop);
        if (field == null) {
            return false;
        }
        Object inner = record.get(fieldTop);
        if (!(inner instanceof GenericData.Record)) {
            return false;
        }
        return getter.isExistsPropertyAvro((GenericData.Record) inner);
    }

    private String isExistsPropertyCodegen(CodegenContext context) {
        return context.addMethod(boolean.class, GenericData.Record.class, "record", this.getClass())
                .declareVar(Schema.Field.class, "field", exprDotMethodChain(ref("record")).addNoParam("getSchema").addWConst("getField", fieldTop))
                .ifRefNullReturnFalse("field")
                .declareVar(Object.class, "inner", exprDotMethod(ref("record"), "get", constant(fieldTop)))
                .ifRefNotTypeReturnConst("inner", GenericData.Record.class, false)
                .methodReturn(getter.codegenUnderlyingExists(cast(GenericData.Record.class, ref("inner")), context));
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
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
        return localMethod(getCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(isExistsPropertyCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }
}
