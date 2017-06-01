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

public class AvroEventBeanGetterNestedMultiLevel implements EventPropertyGetterSPI {
    private final int top;
    private final int[] path;
    private final EventType fragmentEventType;
    private final EventAdapterService eventAdapterService;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param record record
     * @param top top index
     * @param path path of indexes
     * @return value
     * @throws PropertyAccessException
     */
    public static Object getRecordValueTopWPath(GenericData.Record record, int top, int[] path) throws PropertyAccessException {
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return null;
        }
        for (int i = 0; i < path.length - 1; i++) {
            inner = (GenericData.Record) inner.get(path[i]);
            if (inner == null) {
                return null;
            }
        }
        return inner.get(path[path.length - 1]);
    }

    public AvroEventBeanGetterNestedMultiLevel(int top, int[] path, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        this.top = top;
        this.path = path;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getRecordValueTopWPath((GenericData.Record) eventBean.getUnderlying(), top, path);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        if (fragmentEventType == null) {
            return null;
        }
        Object value = get(eventBean);
        if (value == null) {
            return null;
        }
        return eventAdapterService.adapterForTypedAvro(value, fragmentEventType);
    }

    private String getFragmentCodegen(CodegenContext context) {
        CodegenMember mSvc = context.makeAddMember(EventAdapterService.class, eventAdapterService);
        CodegenMember mType = context.makeAddMember(EventType.class, fragmentEventType);
        return context.addMethod(Object.class, GenericData.Record.class, "record", this.getClass())
                .declareVar(Object.class, "value", codegenUnderlyingGet(ref("record"), context))
                .ifRefNullReturnNull("value")
                .methodReturn(exprDotMethod(ref(mSvc.getMemberName()), "adapterForTypedAvro", ref("value"), ref(mType.getMemberName())));
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
        return staticMethod(AvroEventBeanGetterNestedMultiLevel.class, "getRecordValueTopWPath", underlyingExpression, constant(top), constant(path));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}
