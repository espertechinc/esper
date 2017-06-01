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
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.avro.getter.AvroEventBeanGetterDynamicPoly.*;
import static com.espertech.esper.avro.getter.AvroEventBeanGetterDynamicPoly.getAvroFieldValuePoly;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedPoly implements EventPropertyGetterSPI {
    private final int top;
    private final AvroEventPropertyGetter[] getters;

    public AvroEventBeanGetterNestedPoly(int top, AvroEventPropertyGetter[] getters) {
        this.top = top;
        this.getters = getters;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        return getAvroFieldValuePoly(inner, getters);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        return getAvroFieldValuePolyExists(inner, getters);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        return getAvroFieldFragmentPoly(inner, getters);
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingFragment(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getAvroFieldValuePolyCodegen(context, getters), cast(GenericData.Record.class, exprDotMethod(underlyingExpression, "get", constant(top))));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getAvroFieldValuePolyExistsCodegen(context, getters), cast(GenericData.Record.class, exprDotMethod(underlyingExpression, "get", constant(top))));
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getAvroFieldFragmentPolyCodegen(context, getters), cast(GenericData.Record.class, exprDotMethod(underlyingExpression, "get", constant(top))));
    }
}
