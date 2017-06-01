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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.apache.avro.generic.GenericData;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedMapped implements EventPropertyGetterSPI {
    private final int top;
    private final int pos;
    private final String key;

    public AvroEventBeanGetterNestedMapped(int top, int pos, String key) {
        this.top = top;
        this.pos = pos;
        this.key = key;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return null;
        }
        Map map = (Map) inner.get(pos);
        return AvroEventBeanGetterMapped.getAvroMappedValueWNullCheck(map, key);
    }

    private String getCodegen(CodegenContext context) {
        return context.addMethod(Object.class, GenericData.Record.class, "record", this.getClass())
                .declareVar(GenericData.Record.class, "inner", cast(GenericData.Record.class, exprDotMethod(ref("record"), "get", constant(top))))
                .ifRefNullReturnNull("inner")
                .declareVar(Map.class, "map", cast(Map.class, exprDotMethod(ref("inner"), "get", constant(pos))))
                .methodReturn(staticMethod(AvroEventBeanGetterMapped.class, "getAvroMappedValueWNullCheck", ref("map"), constant(key)));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }
}
