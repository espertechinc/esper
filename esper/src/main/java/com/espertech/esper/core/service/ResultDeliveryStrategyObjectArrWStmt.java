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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A result delivery strategy that uses an "update" method that accepts a pair of object array array.
 */
public class ResultDeliveryStrategyObjectArrWStmt extends ResultDeliveryStrategyObjectArr {
    private final static Logger log = LoggerFactory.getLogger(ResultDeliveryStrategyObjectArrWStmt.class);

    public ResultDeliveryStrategyObjectArrWStmt(EPStatement statement, Object subscriber, Method method, EngineImportService engineImportService) {
        super(statement, subscriber, method, engineImportService);
    }

    @Override
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

        Object[] parameters = new Object[]{statement, newData, oldData};
        try {
            fastMethod.invoke(subscriber, parameters);
        } catch (InvocationTargetException e) {
            ResultDeliveryStrategyImpl.handle(statement.getName(), log, e, parameters, subscriber, fastMethod);
        }
    }
}
