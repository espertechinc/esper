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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import org.apache.avro.generic.GenericData;

import java.util.Collection;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterIndexed implements AvroEventPropertyGetter {
    private final int pos;
    private final int index;
    private final EventType fragmentEventType;
    private final EventAdapterService eventAdapterService;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param values coll
     * @param index  index
     * @return value
     */
    public static Object getAvroIndexedValue(Collection values, int index) {
        if (values == null) {
            return null;
        }
        if (values instanceof List) {
            List list = (List) values;
            return list.size() > index ? list.get(index) : null;
        }
        return values.toArray()[index];
    }

    public AvroEventBeanGetterIndexed(int pos, int index, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        this.pos = pos;
        this.index = index;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(pos);
        return getAvroIndexedValue(values, index);
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        Collection values = (Collection) record.get(pos);
        return getAvroIndexedValue(values, index);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        return getAvroFragment(record);
    }

    public Object getAvroFragment(GenericData.Record record) {
        if (fragmentEventType == null) {
            return null;
        }
        Object value = getAvroFieldValue(record);
        if (value == null) {
            return null;
        }
        return eventAdapterService.adapterForTypedAvro(value, fragmentEventType);
    }

    private String getAvroFragmentCodegen(CodegenContext context) {
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(EventType.class, fragmentEventType);
        return context.addMethod(Object.class, this.getClass()).add(GenericData.Record.class, "record").begin()
                .declareVar(Object.class, "value", underlyingGetCodegen(ref("record"), context))
                .methodReturn(exprDotMethod(ref(mSvc.getMemberName()), "adapterForTypedAvro", ref("value"), ref(mType.getMemberName())));
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
        CodegenExpression values = cast(Collection.class, exprDotMethod(underlyingExpression, "get", constant(pos)));
        return staticMethod(this.getClass(), "getAvroIndexedValue", values, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        if (fragmentEventType == null) {
            return constantNull();
        }
        return localMethod(getAvroFragmentCodegen(context), underlyingExpression);
    }
}
