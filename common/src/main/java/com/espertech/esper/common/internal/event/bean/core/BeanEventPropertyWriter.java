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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

/**
 * Writer for a property to an event.
 */
public class BeanEventPropertyWriter implements EventPropertyWriterSPI {
    private static final Logger log = LoggerFactory.getLogger(BeanEventPropertyWriter.class);

    private final Class clazz;
    private final Method writerMethod;

    /**
     * Ctor.
     *
     * @param clazz        to write to
     * @param writerMethod write method
     */
    public BeanEventPropertyWriter(Class clazz, Method writerMethod) {
        this.clazz = clazz;
        this.writerMethod = writerMethod;
    }

    public void write(Object value, EventBean target) {
        invoke(new Object[]{value}, target.getUnderlying());
    }

    public void writeValue(Object value, Object target) {
        invoke(new Object[]{value}, target);
    }

    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return exprDotMethod(und, writerMethod.getName(), assigned);
    }

    protected void invoke(Object[] values, Object target) {
        try {
            writerMethod.invoke(target, values);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            handle(e);
        } catch (InvocationTargetException e) {
            handle(e.getTargetException());
        }
    }

    private void handle(Throwable e) {
        String message = "Unexpected exception encountered invoking setter-method '" + writerMethod + "' on class '" +
                clazz.getName() + "' : " + e.getMessage();
        log.error(message, e);
    }
}
