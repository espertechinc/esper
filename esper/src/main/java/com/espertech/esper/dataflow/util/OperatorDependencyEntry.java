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
package com.espertech.esper.dataflow.util;

import java.util.LinkedHashSet;
import java.util.Set;

public class OperatorDependencyEntry {

    private Set<Integer> incoming;
    private Set<Integer> outgoing;

    public OperatorDependencyEntry() {
        incoming = new LinkedHashSet<Integer>();
        outgoing = new LinkedHashSet<Integer>();
    }

    public void addIncoming(int num) {
        incoming.add(num);
    }

    public void addOutgoing(int num) {
        outgoing.add(num);
    }

    public Set<Integer> getIncoming() {
        return incoming;
    }

    public Set<Integer> getOutgoing() {
        return outgoing;
    }

    public String toString() {
        return "OperatorDependencyEntry{" +
                "incoming=" + incoming +
                ", outgoing=" + outgoing +
                '}';
    }
}
