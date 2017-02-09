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
package com.espertech.esper.view;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides subscription list for statement stop callbacks.
 */
public class StatementStopServiceImpl implements StatementStopService {
    private List<StatementStopCallback> statementStopCallbacks;

    /**
     * ctor.
     */
    public StatementStopServiceImpl() {
    }

    public void addSubscriber(StatementStopCallback callback) {
        if (statementStopCallbacks == null) {
            statementStopCallbacks = new LinkedList<StatementStopCallback>();
        }
        statementStopCallbacks.add(callback);
    }

    public void fireStatementStopped() {
        if (statementStopCallbacks == null) {
            return;
        }
        for (StatementStopCallback statementStopCallback : statementStopCallbacks) {
            statementStopCallback.statementStopped();
        }
    }
}
