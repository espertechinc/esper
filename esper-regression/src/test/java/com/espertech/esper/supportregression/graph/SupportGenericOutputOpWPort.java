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
package com.espertech.esper.supportregression.graph;

import com.espertech.esper.collection.Pair;

import java.util.ArrayList;
import java.util.List;

public class SupportGenericOutputOpWPort<T> {
    private List<T> received = new ArrayList<T>();
    private List<Integer> receivedPorts = new ArrayList<Integer>();

    public synchronized void onInput(int port, T event) {
        received.add(event);
        receivedPorts.add(port);
    }

    public synchronized Pair<List<T>, List<Integer>> getAndReset() {
        List<T> resultEvents = received;
        List<Integer> resultPorts = receivedPorts;
        received = new ArrayList<T>();
        receivedPorts = new ArrayList<Integer>();
        return new Pair<List<T>, List<Integer>>(resultEvents, resultPorts);
    }
}

