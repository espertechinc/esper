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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import net.sf.cglib.reflect.FastConstructor;

import java.lang.reflect.InvocationTargetException;

public class InstanceManufacturerFastCtor implements InstanceManufacturer {
    private final Class targetClass;
    private final FastConstructor ctor;
    private final ExprEvaluator[] expr;

    public InstanceManufacturerFastCtor(Class targetClass, FastConstructor ctor, ExprEvaluator[] expr) {
        this.targetClass = targetClass;
        this.ctor = ctor;
        this.expr = expr;
    }

    public Object make(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] row = new Object[expr.length];
        for (int i = 0; i < row.length; i++) {
            row[i] = expr[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }
        return makeUnderlyingFromFastCtor(row, ctor, targetClass);
    }

    public static Object makeUnderlyingFromFastCtor(Object[] properties, FastConstructor ctor, Class target) {
        try {
            return ctor.newInstance(properties);
        } catch (InvocationTargetException e) {
            throw new EPException("InvocationTargetException received invoking constructor for type '" + target.getName() + "': " + e.getTargetException().getMessage(), e.getTargetException());
        }
    }
}
