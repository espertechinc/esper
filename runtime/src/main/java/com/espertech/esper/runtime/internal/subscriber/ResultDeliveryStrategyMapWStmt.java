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
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.runtime.client.EPStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class ResultDeliveryStrategyMapWStmt extends ResultDeliveryStrategyMap {

    private final static Logger log = LoggerFactory.getLogger(ResultDeliveryStrategyMapWStmt.class);

    public ResultDeliveryStrategyMapWStmt(EPStatement statement, Object subscriber, Method method, String[] columnNames, ClasspathImportService classpathImportService) {
        super(statement, subscriber, method, columnNames, classpathImportService);
    }

    @Override
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

        Object[] parameters = new Object[]{statement, newData, oldData};
        try {
            method.invoke(subscriber, parameters);
        } catch (InvocationTargetException | IllegalAccessException e) {
            ResultDeliveryStrategyImpl.handle(statement.getName(), log, e, parameters, subscriber, method);
        }
    }
}
