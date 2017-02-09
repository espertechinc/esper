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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.EngineImportException;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastConstructor;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

public class InstanceManufacturerUtil {

    public static Pair<FastConstructor, ExprEvaluator[]> getManufacturer(Class targetClass, EngineImportService engineImportService, ExprEvaluator[] exprEvaluators, Object[] expressionReturnTypes)
            throws ExprValidationException {
        Class[] ctorTypes = new Class[expressionReturnTypes.length];
        ExprEvaluator[] evaluators = new ExprEvaluator[exprEvaluators.length];

        for (int i = 0; i < expressionReturnTypes.length; i++) {
            Object columnType = expressionReturnTypes[i];

            if (columnType instanceof Class || columnType == null) {
                ctorTypes[i] = (Class) expressionReturnTypes[i];
                evaluators[i] = exprEvaluators[i];
                continue;
            }

            if (columnType instanceof EventType) {
                EventType columnEventType = (EventType) columnType;
                final Class returnType = columnEventType.getUnderlyingType();
                final ExprEvaluator inner = exprEvaluators[i];
                evaluators[i] = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                        EventBean theEvent = (EventBean) inner.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                        if (theEvent != null) {
                            return theEvent.getUnderlying();
                        }
                        return null;
                    }

                    public Class getType() {
                        return returnType;
                    }

                };
                ctorTypes[i] = returnType;
                continue;
            }

            // handle case where the select-clause contains an fragment array
            if (columnType instanceof EventType[]) {
                EventType columnEventType = ((EventType[]) columnType)[0];
                final Class componentReturnType = columnEventType.getUnderlyingType();

                final ExprEvaluator inner = exprEvaluators[i];
                evaluators[i] = new ExprEvaluator() {
                    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                        Object result = inner.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                        if (!(result instanceof EventBean[])) {
                            return null;
                        }
                        EventBean[] events = (EventBean[]) result;
                        Object values = Array.newInstance(componentReturnType, events.length);
                        for (int i = 0; i < events.length; i++) {
                            Array.set(values, i, events[i].getUnderlying());
                        }
                        return values;
                    }

                    public Class getType() {
                        return componentReturnType;
                    }

                };
                continue;
            }

            String message = "Invalid assignment of expression " + i + " returning type '" + columnType +
                    "', column and parameter types mismatch";
            throw new ExprValidationException(message);
        }

        try {
            Constructor ctor = engineImportService.resolveCtor(targetClass, ctorTypes);
            FastClass fastClass = FastClass.create(engineImportService.getFastClassClassLoader(targetClass), targetClass);
            return new Pair<FastConstructor, ExprEvaluator[]>(fastClass.getConstructor(ctor), evaluators);
        } catch (EngineImportException ex) {
            throw new ExprValidationException("Failed to find a suitable constructor for class '" + targetClass.getName() + "': " + ex.getMessage(), ex);
        }
    }
}
