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

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.NaturalEventBean;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A result delivery strategy that uses an "update" method that accepts a pair of map array.
 */
public class ResultDeliveryStrategyMap implements ResultDeliveryStrategy {
    private final static Logger log = LoggerFactory.getLogger(ResultDeliveryStrategyMap.class);
    protected final EPStatement statement;
    protected final Object subscriber;
    protected final FastMethod fastMethod;
    protected final String[] columnNames;

    /**
     * Ctor.
     *
     * @param subscriber  the object to deliver to
     * @param method      the delivery method
     * @param columnNames the column names for the map
     * @param statement   statement
     * @param engineImportService engine imports
     */
    public ResultDeliveryStrategyMap(EPStatement statement, Object subscriber, Method method, String[] columnNames, EngineImportService engineImportService) {
        this.statement = statement;
        this.subscriber = subscriber;
        FastClass fastClass = FastClass.create(engineImportService.getFastClassClassLoader(subscriber.getClass()), subscriber.getClass());
        this.fastMethod = fastClass.getMethod(method);
        this.columnNames = columnNames;
    }

    public void execute(UniformPair<EventBean[]> result) {
        Map[] newData;
        Map[] oldData;

        if (result == null) {
            newData = null;
            oldData = null;
        } else {
            newData = convert(result.getFirst());
            oldData = convert(result.getSecond());
        }

        Object[] parameters = new Object[]{newData, oldData};
        try {
            fastMethod.invoke(subscriber, parameters);
        } catch (InvocationTargetException e) {
            ResultDeliveryStrategyImpl.handle(statement.getName(), log, e, parameters, subscriber, fastMethod);
        }
    }

    protected Map[] convert(EventBean[] events) {
        if ((events == null) || (events.length == 0)) {
            return null;
        }

        Map[] result = new Map[events.length];
        int length = 0;
        for (int i = 0; i < result.length; i++) {
            if (events[i] instanceof NaturalEventBean) {
                NaturalEventBean natural = (NaturalEventBean) events[i];
                result[length] = convert(natural);
                length++;
            }
        }

        if (length == 0) {
            return null;
        }
        if (length != events.length) {
            Map[] reduced = new Map[length];
            System.arraycopy(result, 0, reduced, 0, length);
            result = reduced;
        }
        return result;
    }

    private Map convert(NaturalEventBean natural) {
        Map<String, Object> map = new HashMap<String, Object>();
        Object[] columns = natural.getNatural();
        for (int i = 0; i < columns.length; i++) {
            map.put(columnNames[i], columns[i]);
        }
        return map;
    }
}
