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

import java.util.Collection;

import static com.espertech.esper.avro.getter.AvroEventBeanGetterIndexed.getAvroIndexedValue;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedIndexRooted implements EventPropertyGetterSPI {
    private final int posTop;
    private final int index;
    private final AvroEventPropertyGetter nested;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param record record
     * @param posTop postop
     * @param index  index
     * @return value
     * @throws PropertyAccessException ex
     */
    public static GenericData.Record getAtIndex(GenericData.Record record, int posTop, int index) throws PropertyAccessException {
        Collection values = (Collection) record.get(posTop);
        Object value = getAvroIndexedValue(values, index);
        if (value == null || !(value instanceof GenericData.Record)) {
            return null;
        }
        return (GenericData.Record) value;
    }

    public AvroEventBeanGetterNestedIndexRooted(int posTop, int index, AvroEventPropertyGetter nested) {
        this.posTop = posTop;
        this.index = index;
        this.nested = nested;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = getAtIndex(record, posTop, index);
        return inner == null ? null : nested.getAvroFieldValue(inner);
    }

    private String getCodegen(CodegenContext context) {
        return context.addMethod(Object.class, GenericData.Record.class, "record", this.getClass())
                .declareVar(GenericData.Record.class, "inner", staticMethodTakingExprAndConst(this.getClass(), "getAtIndex", ref("record"), posTop, index))
                .ifRefNullReturnNull("inner")
                .methodReturn(nested.codegenUnderlyingGet(ref("inner"), context));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(posTop);
        Object value = getAvroIndexedValue(values, index);
        if (value == null || !(value instanceof GenericData.Record)) {
            return null;
        }
        return nested.getAvroFragment((GenericData.Record) value);
    }

    private String getFragmentCodegen(CodegenContext context) {
        return context.addMethod(Object.class, GenericData.Record.class, "record", this.getClass())
                .declareVar(Collection.class, "values", cast(Collection.class, exprDotMethod(ref("record"), "get", constant(posTop))))
                .declareVar(Object.class, "value", staticMethod(AvroEventBeanGetterIndexed.class, "getAvroIndexedValue", ref("values"), constant(index)))
                .ifRefNullReturnNull("value")
                .ifRefNotTypeReturnConst("value", GenericData.Record.class, null)
                .methodReturn(nested.codegenUnderlyingFragment(cast(GenericData.Record.class, ref("value")), context));
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingFragment(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}
