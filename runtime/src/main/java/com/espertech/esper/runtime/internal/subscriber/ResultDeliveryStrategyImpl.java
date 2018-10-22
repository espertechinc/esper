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
package com.espertech.esper.runtime.internal.subscriber;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.event.core.NaturalEventBean;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.runtime.client.EPStatement;
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
    private final Method updateMethod;
    private final Method startMethod;
    private final boolean startMethodHasEPStatement;
    private final Method endMethod;
    private final boolean endMethodHasEPStatement;
    private final Method updateRStreamMethod;
    private final DeliveryConvertor deliveryConvertor;

    /**
     * Ctor.
     *
     * @param subscriber             is the subscriber receiving method invocations
     * @param deliveryConvertor      for converting individual rows
     * @param method                 to deliver the insert stream to
     * @param startMethod            to call to indicate when delivery starts, or null if no such indication is required
     * @param endMethod              to call to indicate when delivery ends, or null if no such indication is required
     * @param rStreamMethod          to deliver the remove stream to, or null if no such indication is required
     * @param statement              statement
     * @param classpathImportService runtime imports
     */
    public ResultDeliveryStrategyImpl(EPStatement statement, Object subscriber, DeliveryConvertor deliveryConvertor, Method method, Method startMethod, Method endMethod, Method rStreamMethod, ClasspathImportService classpathImportService) {
        this.statement = statement;
        this.subscriber = subscriber;
        this.deliveryConvertor = deliveryConvertor;
        this.updateMethod = method;

        if (startMethod != null) {
            this.startMethod = startMethod;
            this.startMethodHasEPStatement = isMethodAcceptsStatement(startMethod);
        } else {
            this.startMethod = null;
            this.startMethodHasEPStatement = false;
        }

        if (endMethod != null) {
            this.endMethod = endMethod;
            this.endMethodHasEPStatement = isMethodAcceptsStatement(endMethod);
        } else {
            this.endMethod = null;
            this.endMethodHasEPStatement = false;
        }

        if (rStreamMethod != null) {
            updateRStreamMethod = rStreamMethod;
        } else {
            updateRStreamMethod = null;
        }

        makeAccessible(this.updateMethod);
        makeAccessible(this.updateRStreamMethod);
        makeAccessible(this.startMethod);
        makeAccessible(this.endMethod);
    }

    public void execute(UniformPair<EventBean[]> result) {
        if (startMethod != null) {
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
                startMethod.invoke(subscriber, parameters);
            } catch (InvocationTargetException e) {
                handle(statement.getName(), log, e, parameters, subscriber, startMethod);
            } catch (Throwable t) {
                handleThrowable(log, t, null, subscriber, startMethod);
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
                        updateMethod.invoke(subscriber, parameters);
                    } catch (InvocationTargetException e) {
                        handle(statement.getName(), log, e, parameters, subscriber, updateMethod);
                    } catch (Throwable t) {
                        handleThrowable(log, t, parameters, subscriber, updateMethod);
                    }
                }
            }
        }

        if ((updateRStreamMethod != null) && (oldData != null) && (oldData.length > 0)) {
            for (int i = 0; i < oldData.length; i++) {
                EventBean theEvent = oldData[i];
                if (theEvent instanceof NaturalEventBean) {
                    NaturalEventBean natural = (NaturalEventBean) theEvent;
                    Object[] parameters = deliveryConvertor.convertRow(natural.getNatural());
                    try {
                        updateRStreamMethod.invoke(subscriber, parameters);
                    } catch (InvocationTargetException e) {
                        handle(statement.getName(), log, e, parameters, subscriber, updateRStreamMethod);
                    } catch (Throwable t) {
                        handleThrowable(log, t, parameters, subscriber, updateRStreamMethod);
                    }
                }
            }
        }

        if (endMethod != null) {
            Object[] parameters = endMethodHasEPStatement ? new Object[]{statement} : null;
            try {
                endMethod.invoke(subscriber, parameters);
            } catch (InvocationTargetException e) {
                handle(statement.getName(), log, e, null, subscriber, endMethod);
            } catch (Throwable t) {
                handleThrowable(log, t, null, subscriber, endMethod);
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
    protected static void handle(String statementName, Logger logger, ReflectiveOperationException e, Object[] parameters, Object subscriber, Method method) {
        Throwable inner = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
        String message = JavaClassHelper.getMessageInvocationTarget(statementName, method.getName(), method.getParameterTypes(), subscriber.getClass().getName(), parameters, inner);
        logger.error(message, inner);
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
    protected static void handleThrowable(Logger logger, Throwable t, Object[] parameters, Object subscriber, Method method) {
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

    private void makeAccessible(Method method) {
        if (method == null) {
            return;
        }
        try {
            method.setAccessible(true);
        } catch (IllegalArgumentException e) {
        }
    }
}
