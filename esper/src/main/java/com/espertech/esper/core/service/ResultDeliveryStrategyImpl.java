/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.event.NaturalEventBean;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A result delivery strategy that uses a matching "update" method and
 * optional start, end, and updateRStream methods, to deliver column-wise to parameters
 * of the update method.
 */
public class ResultDeliveryStrategyImpl implements ResultDeliveryStrategy
{
    private static Log log = LogFactory.getLog(ResultDeliveryStrategyImpl.class);
    private final String statementName;
    private final Object subscriber;
    private final FastMethod updateFastMethod;
    private final FastMethod startFastMethod;
    private final FastMethod endFastMethod;
    private final FastMethod updateRStreamFastMethod;
    private final DeliveryConvertor deliveryConvertor;

    /**
     * Ctor.
     * @param subscriber is the subscriber receiving method invocations
     * @param deliveryConvertor for converting individual rows
     * @param method to deliver the insert stream to
     * @param startMethod to call to indicate when delivery starts, or null if no such indication is required
     * @param endMethod to call to indicate when delivery ends, or null if no such indication is required
     * @param rStreamMethod to deliver the remove stream to, or null if no such indication is required
     */
    public ResultDeliveryStrategyImpl(String statementName, Object subscriber, DeliveryConvertor deliveryConvertor, Method method, Method startMethod, Method endMethod, Method rStreamMethod)
    {
        this.statementName = statementName;
        this.subscriber = subscriber;
        this.deliveryConvertor = deliveryConvertor;
        FastClass fastClass = FastClass.create(Thread.currentThread().getContextClassLoader(), subscriber.getClass());
        this.updateFastMethod = fastClass.getMethod(method);

        if (startMethod != null)
        {
            startFastMethod = fastClass.getMethod(startMethod);
        }
        else
        {
            startFastMethod = null;
        }

        if (endMethod != null)
        {
            endFastMethod = fastClass.getMethod(endMethod);
        }
        else
        {
            endFastMethod = null;
        }

        if (rStreamMethod != null)
        {
            updateRStreamFastMethod = fastClass.getMethod(rStreamMethod);
        }
        else
        {
            updateRStreamFastMethod = null;
        }
    }

    public void execute(UniformPair<EventBean[]> result)
    {
        if (startFastMethod != null)
        {
            int countNew = 0;
            int countOld = 0;
            if (result != null) {
                countNew = count(result.getFirst());
                countOld = count(result.getSecond());
            }

            Object[] parameters = new Object[] {countNew, countOld};
            try {
                startFastMethod.invoke(subscriber, parameters);
            }
            catch (InvocationTargetException e) {
                handle(statementName, log, e, parameters, subscriber, startFastMethod);
            }
            catch (Throwable t) {
                handleThrowable(log, t, null, subscriber, startFastMethod);
            }
        }

        EventBean[] newData = null;
        EventBean[] oldData = null;
        if (result != null)
        {
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
                        updateFastMethod.invoke(subscriber, parameters);
                    }
                    catch (InvocationTargetException e) {
                        handle(statementName, log, e, parameters, subscriber, updateFastMethod);
                    }
                    catch (Throwable t) {
                        handleThrowable(log, t, parameters, subscriber, updateFastMethod);
                    }
                }
            }
        }

        if ((updateRStreamFastMethod != null) && (oldData != null) && (oldData.length > 0)) {
            for (int i = 0; i < oldData.length; i++) {
                EventBean theEvent = oldData[i];
                if (theEvent instanceof NaturalEventBean) {
                    NaturalEventBean natural = (NaturalEventBean) theEvent;
                    Object[] parameters = deliveryConvertor.convertRow(natural.getNatural());
                    try {
                        updateRStreamFastMethod.invoke(subscriber, parameters);
                    }
                    catch (InvocationTargetException e) {
                        handle(statementName, log, e, parameters, subscriber, updateRStreamFastMethod);
                    }
                    catch (Throwable t) {
                        handleThrowable(log, t, parameters, subscriber, updateRStreamFastMethod);
                    }
                }
            }
        }

        if (endFastMethod != null) {
            try {
                endFastMethod.invoke(subscriber, null);
            }
            catch (InvocationTargetException e) {
                handle(statementName, log, e, null, subscriber, endFastMethod);
            }
            catch (Throwable t) {
                handleThrowable(log, t, null, subscriber, endFastMethod);
            }
        }
    }

    /**
     * Handle the exception, displaying a nice message and converting to {@link EPException}.
     * @param logger is the logger to use for error logging
     * @param e is the exception
     * @param parameters the method parameters
     * @param subscriber the object to deliver to
     * @param method the method to call
     * @throws EPException converted from the passed invocation exception
     */
    protected static void handle(String statementName, Log logger, InvocationTargetException e, Object[] parameters, Object subscriber, FastMethod method) {
        String message = JavaClassHelper.getMessageInvocationTarget(statementName, method.getJavaMethod(), subscriber.getClass().getName(), parameters, e);
        logger.error(message, e.getTargetException());
    }

    /**
     * Handle the exception, displaying a nice message and converting to {@link EPException}.
     * @param logger is the logger to use for error logging
     * @param t is the throwable
     * @param parameters the method parameters
     * @param subscriber the object to deliver to
     * @param method the method to call
     * @throws EPException converted from the passed invocation exception
     */
    protected static void handleThrowable(Log logger, Throwable t, Object[] parameters, Object subscriber, FastMethod method) {
        String message = "Unexpected exception when invoking method '" + method.getName() +
                "' on subscriber class '" + subscriber.getClass().getSimpleName() +
                "' for parameters " + ((parameters == null) ? "null" : Arrays.toString(parameters)) +
                " : " + t.getClass().getSimpleName() + " : " + t.getMessage();
        logger.error(message, t);
    }

    private int count(EventBean[] events) {
        if (events == null)
        {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < events.length; i++)
        {
            EventBean theEvent = events[i];
            if (theEvent instanceof NaturalEventBean)
            {
                count++;
            }
        }
        return count;
    }
}
