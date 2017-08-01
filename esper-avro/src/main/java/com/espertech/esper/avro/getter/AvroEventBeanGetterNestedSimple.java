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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedSimple implements EventPropertyGetterSPI {
    private final int posTop;
    private final int posInner;
    private final EventType fragmentType;
    private final EventAdapterService eventAdapterService;

    public AvroEventBeanGetterNestedSimple(int posTop, int posInner, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.posTop = posTop;
        this.posInner = posInner;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return get((GenericData.Record) eventBean.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        if (fragmentType == null) {
            return null;
        }
        Object value = get(eventBean);
        if (value == null) {
            return null;
        }
        return eventAdapterService.adapterForTypedAvro(value, fragmentType);
    }

    private String getFragmentCodegen(CodegenContext context) throws PropertyAccessException {
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(EventType.class, fragmentType);
        return context.addMethod(Object.class, this.getClass()).add(GenericData.Record.class, "record").begin()
                .declareVar(Object.class, "value", underlyingGetCodegen(ref("record"), context))
                .ifRefNullReturnNull("value")
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
        return localMethod(getCodegen(context), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        if (fragmentType == null) {
            return constantNull();
        }
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }

    private Object get(GenericData.Record record) throws PropertyAccessException {
        GenericData.Record inner = (GenericData.Record) record.get(posTop);
        if (inner == null) {
            return null;
        }
        return inner.get(posInner);
    }

    private String getCodegen(CodegenContext context) {
        return context.addMethod(Object.class, this.getClass()).add(GenericData.Record.class, "record").begin()
                .declareVar(GenericData.Record.class, "inner", cast(GenericData.Record.class, exprDotMethod(ref("record"), "get", constant(posTop))))
                .ifRefNullReturnNull("inner")
                .methodReturn(exprDotMethod(ref("inner"), "get", constant(posInner)));
    }
}
