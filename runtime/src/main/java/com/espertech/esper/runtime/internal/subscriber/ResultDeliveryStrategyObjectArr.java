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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.event.core.NaturalEventBean;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.runtime.client.EPStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A result delivery strategy that uses an "update" method that accepts a pair of object array array.
 */
public class ResultDeliveryStrategyObjectArr implements ResultDeliveryStrategy {
    private final static Logger log = LoggerFactory.getLogger(ResultDeliveryStrategyImpl.class);
    protected final EPStatement statement;
    protected final Object subscriber;
    protected final Method method;

    /**
     * Ctor.
     *
     * @param subscriber             is the subscriber to deliver to
     * @param method                 the method to invoke
     * @param statement              statement
     * @param classpathImportService runtime imports
     */
    public ResultDeliveryStrategyObjectArr(EPStatement statement, Object subscriber, Method method, ClasspathImportService classpathImportService) {
        this.statement = statement;
        this.subscriber = subscriber;
        this.method = method;
    }

    public void execute(UniformPair<EventBean[]> result) {
        Object[][] newData;
        Object[][] oldData;

        if (result == null) {
            newData = null;
            oldData = null;
        } else {
            newData = convert(result.getFirst());
            oldData = convert(result.getSecond());
        }

        Object[] parameters = new Object[]{newData, oldData};
        try {
            method.invoke(subscriber, parameters);
        } catch (InvocationTargetException | IllegalAccessException e) {
            ResultDeliveryStrategyImpl.handle(statement.getName(), log, e, parameters, subscriber, method);
        }
    }

    protected Object[][] convert(EventBean[] events) {
        if ((events == null) || (events.length == 0)) {
            return null;
        }

        Object[][] result = new Object[events.length][];
        int length = 0;
        for (int i = 0; i < result.length; i++) {
            if (events[i] instanceof NaturalEventBean) {
                NaturalEventBean natural = (NaturalEventBean) events[i];
                result[length] = natural.getNatural();
                length++;
            }
        }

        if (length == 0) {
            return null;
        }
        if (length != events.length) {
            Object[][] reduced = new Object[length][];
            System.arraycopy(result, 0, reduced, 0, length);
            result = reduced;
        }
        return result;
    }
}
