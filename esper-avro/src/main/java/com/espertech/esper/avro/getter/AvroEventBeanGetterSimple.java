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
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.event.EventAdapterService;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterSimple implements AvroEventPropertyGetter {
    private final int propertyIndex;
    private final EventType fragmentType;
    private final EventAdapterService eventAdapterService;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param value value
     * @param eventAdapterService svc
     * @param fragmentType type
     * @return fragment
     */
    public static Object getFragmentAvro(Object value, EventAdapterService eventAdapterService, EventType fragmentType) {
        if (fragmentType == null) {
            return null;
        }
        if (value instanceof GenericData.Record) {
            return eventAdapterService.adapterForTypedAvro(value, fragmentType);
        }
        if (value instanceof Collection) {
            Collection coll = (Collection) value;
            EventBean[] events = new EventBean[coll.size()];
            int index = 0;
            for (Object item : coll) {
                events[index++] = eventAdapterService.adapterForTypedAvro(item, fragmentType);
            }
            return events;
        }
        return null;
    }

    public AvroEventBeanGetterSimple(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.propertyIndex = propertyIndex;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object getAvroFieldValue(GenericData.Record record) throws PropertyAccessException {
        return record.get(propertyIndex);
    }

    public Object get(EventBean theEvent) {
        return getAvroFieldValue((GenericData.Record) theEvent.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return true;
    }

    public Object getFragment(EventBean obj) {
        Object value = get(obj);
        return getFragmentAvro(value, eventAdapterService, fragmentType);
    }

    public Object getAvroFragment(GenericData.Record record) {
        Object value = getAvroFieldValue(record);
        return getFragmentAvro(value, eventAdapterService, fragmentType);
    }

    private String getAvroFragmentCodegen(CodegenContext context) {
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(EventType.class, fragmentType);
        return context.addMethod(Object.class, GenericData.Record.class, "record", this.getClass())
                .declareVar(Object.class, "value", codegenUnderlyingGet(ref("record"), context))
                .methodReturn(staticMethod(this.getClass(), "getFragmentAvro", ref("value"), ref(mSvc.getMemberName()), ref(mType.getMemberName())));
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
        return CodegenExpressionBuilder.exprDotMethod(underlyingExpression, "get", constant(propertyIndex));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        if (fragmentType == null) {
            return constantNull();
        }
        return localMethod(getAvroFragmentCodegen(context), underlyingExpression);
    }
}

