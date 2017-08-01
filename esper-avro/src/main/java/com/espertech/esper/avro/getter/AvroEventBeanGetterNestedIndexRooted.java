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
        return context.addMethod(Object.class, this.getClass()).add(GenericData.Record.class, "record").begin()
                .declareVar(GenericData.Record.class, "inner", staticMethod(this.getClass(), "getAtIndex", ref("record"), constant(posTop), constant(index)))
                .ifRefNullReturnNull("inner")
                .methodReturn(nested.underlyingGetCodegen(ref("inner"), context));
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
        return context.addMethod(Object.class, this.getClass()).add(GenericData.Record.class, "record").begin()
                .declareVar(Collection.class, "values", cast(Collection.class, exprDotMethod(ref("record"), "get", constant(posTop))))
                .declareVar(Object.class, "value", staticMethod(AvroEventBeanGetterIndexed.class, "getAvroIndexedValue", ref("values"), constant(index)))
                .ifRefNullReturnNull("value")
                .ifRefNotTypeReturnConst("value", GenericData.Record.class, null)
                .methodReturn(nested.underlyingFragmentCodegen(cast(GenericData.Record.class, ref("value")), context));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingFragmentCodegen(castUnderlying(GenericData.Record.class, beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getCodegen(context), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}
