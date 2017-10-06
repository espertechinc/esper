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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.NaturalEventBean;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A result delivery strategy that uses a matching "update" method and
 * optional start, end, and updateRStream methods, to deliver column-wise to parameters
 * of the update method.
 */
public class ResultDeliveryStrategyImpl implements ResultDeliveryStrategy {
    private final static Logger log = LoggerFactory.getLogger(ResultDeliveryStrategyImpl.class);
    private final EPStatement statement;
    private final Object subscriber;
    private final FastMethod updateMethodFast;
    private final FastMethod startMethodFast;
    private final boolean startMethodHasEPStatement;
    private final FastMethod endMethodFast;
    private final boolean endMethodHasEPStatement;
    private final FastMethod updateRStreamMethodFast;
    private final DeliveryConvertor deliveryConvertor;

    /**
     * Ctor.
     *
     * @param subscriber        is the subscriber receiving method invocations
     * @param deliveryConvertor for converting individual rows
     * @param method            to deliver the insert stream to
     * @param startMethod       to call to indicate when delivery starts, or null if no such indication is required
     * @param endMethod         to call to indicate when delivery ends, or null if no such indication is required
     * @param rStreamMethod     to deliver the remove stream to, or null if no such indication is required
     * @param statement         statement
     * @param engineImportService engine imports
     */
    public ResultDeliveryStrategyImpl(EPStatement statement, Object subscriber, DeliveryConvertor deliveryConvertor, Method method, Method startMethod, Method endMethod, Method rStreamMethod, EngineImportService engineImportService) {
        this.statement = statement;
        this.subscriber = subscriber;
        this.deliveryConvertor = deliveryConvertor;
        FastClass fastClass = FastClass.create(engineImportService.getFastClassClassLoader(subscriber.getClass()), subscriber.getClass());
        this.updateMethodFast = fastClass.getMethod(method);

        if (startMethod != null) {
            this.startMethodFast = fastClass.getMethod(startMethod);
            this.startMethodHasEPStatement = isMethodAcceptsStatement(startMethod);
        } else {
            this.startMethodFast = null;
            this.startMethodHasEPStatement = false;
        }

        if (endMethod != null) {
            this.endMethodFast = fastClass.getMethod(endMethod);
            this.endMethodHasEPStatement = isMethodAcceptsStatement(endMethod);
        } else {
            this.endMethodFast = null;
            this.endMethodHasEPStatement = false;
        }

        if (rStreamMethod != null) {
            updateRStreamMethodFast = fastClass.getMethod(rStreamMethod);
        } else {
            updateRStreamMethodFast = null;
        }
    }

    public void execute(UniformPair<EventBean[]> result) {
        if (startMethodFast != null) {
            int countNew = 0;
            int countOld = 0;
            if (result != null) {
                countNew = count(result.getFirst());
                countOld = count(result.getSecond());
            }

            Object[] parameters;
            if (!startMethodHasEPStatement) {
                parameters = new Object[]{countNew, countOld};
            } else {
                parameters = new Object[]{statement, countNew, countOld};
            }
            try {
                startMethodFast.invoke(subscriber, parameters);
            } catch (InvocationTargetException e) {
                handle(statement.getName(), log, e, parameters, subscriber, startMethodFast);
            } catch (Throwable t) {
                handleThrowable(log, t, null, subscriber, startMethodFast);
            }
        }

        EventBean[] newData = null;
        EventBean[] oldData = null;
        if (result != null) {
            newData = result.getFirst();
            oldData = result.getSecond();
        }

        if ((newData != null) && (newData.length > 0)) {
            for (int i = 0; i < newData.length; i++) {
                EventBean theEvent = newData[i];
                if (theEvent instanceof NaturalEventBean) {
                    NaturalEventBean natural = (NaturalEventBean) theEvent;
                    Object[] parameters = deliveryConvertor.convertRow(natural.getNatural());
                    try {
                        updateMethodFast.invoke(subscriber, parameters);
                    } catch (InvocationTargetException e) {
                        handle(statement.getName(), log, e, parameters, subscriber, updateMethodFast);
                    } catch (Throwable t) {
                        handleThrowable(log, t, parameters, subscriber, updateMethodFast);
                    }
                }
            }
        }

        if ((updateRStreamMethodFast != null) && (oldData != null) && (oldData.length > 0)) {
            for (int i = 0; i < oldData.length; i++) {
                EventBean theEvent = oldData[i];
                if (theEvent instanceof NaturalEventBean) {
                    NaturalEventBean natural = (NaturalEventBean) theEvent;
                    Object[] parameters = deliveryConvertor.convertRow(natural.getNatural());
                    try {
                        updateRStreamMethodFast.invoke(subscriber, parameters);
                    } catch (InvocationTargetException e) {
                        handle(statement.getName(), log, e, parameters, subscriber, updateRStreamMethodFast);
                    } catch (Throwable t) {
                        handleThrowable(log, t, parameters, subscriber, updateRStreamMethodFast);
                    }
                }
            }
        }

        if (endMethodFast != null) {
            Object[] parameters = endMethodHasEPStatement ? new Object[]{statement} : null;
            try {
                endMethodFast.invoke(subscriber, parameters);
            } catch (InvocationTargetException e) {
                handle(statement.getName(), log, e, null, subscriber, endMethodFast);
            } catch (Throwable t) {
                handleThrowable(log, t, null, subscriber, endMethodFast);
            }
        }
    }

    /**
     * Handle the exception, displaying a nice message and converting to {@link EPException}.
     *
     * @param logger        is the logger to use for error logging
     * @param e             is the exception
     * @param parameters    the method parameters
     * @param subscriber    the object to deliver to
     * @param method        the method to call
     * @param statementName statement name
     * @throws EPException converted from the passed invocation exception
     */
    protected static void handle(String statementName, Logger logger, InvocationTargetException e, Object[] parameters, Object subscriber, FastMethod method) {
        String message = JavaClassHelper.getMessageInvocationTarget(statementName, method.getJavaMethod(), subscriber.getClass().getName(), parameters, e.getTargetException());
        logger.error(message, e.getTargetException());
    }

    /**
     * Handle the exception, displaying a nice message and converting to {@link EPException}.
     *
     * @param logger     is the logger to use for error logging
     * @param t          is the throwable
     * @param parameters the method parameters
     * @param subscriber the object to deliver to
     * @param method     the method to call
     * @throws EPException converted from the passed invocation exception
     */
    protected static void handleThrowable(Logger logger, Throwable t, Object[] parameters, Object subscriber, FastMethod method) {
        String message = "Unexpected exception when invoking method '" + method.getName() +
                "' on subscriber class '" + subscriber.getClass().getSimpleName() +
                "' for parameters " + ((parameters == null) ? "null" : Arrays.toString(parameters)) +
                " : " + t.getClass().getSimpleName() + " : " + t.getMessage();
        logger.error(message, t);
    }

    private int count(EventBean[] events) {
        if (events == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < events.length; i++) {
            EventBean theEvent = events[i];
            if (theEvent instanceof NaturalEventBean) {
                count++;
            }
        }
        return count;
    }

    private static boolean isMethodAcceptsStatement(Method method) {
        return method.getParameterTypes().length > 0 && method.getParameterTypes()[0] == EPStatement.class;
    }
}
