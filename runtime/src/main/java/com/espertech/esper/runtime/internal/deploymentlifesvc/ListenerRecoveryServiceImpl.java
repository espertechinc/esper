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
package com.espertech.esper.runtime.internal.deploymentlifesvc;

import com.espertech.esper.runtime.client.UpdateListener;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class ListenerRecoveryServiceImpl implements ListenerRecoveryService {
    public final static ListenerRecoveryServiceImpl INSTANCE = new ListenerRecoveryServiceImpl();

    public void put(int statementId, String statementName, UpdateListener[] listeners) {
    }

    public Iterator<Map.Entry<Integer, UpdateListener[]>> listeners() {
        return Collections.emptyIterator();
    }

    public void remove(int statementId) {
    }
}
