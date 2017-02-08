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
package com.espertech.esper.epl.join.base;

import java.util.SortedSet;
import java.util.TreeSet;

public class HistoricalViewableDesc {
    private boolean hasHistorical;
    private final SortedSet<Integer>[] dependenciesPerHistorical;
    private final boolean[] isHistorical;

    public HistoricalViewableDesc(int numStreams) {
        this.dependenciesPerHistorical = new SortedSet[numStreams];
        this.isHistorical = new boolean[numStreams];
    }

    public void setHistorical(int streamNum, SortedSet<Integer> dependencies) {
        hasHistorical = true;
        isHistorical[streamNum] = true;
        if (dependenciesPerHistorical[streamNum] != null) {
            throw new RuntimeException("Dependencies for stream " + streamNum + "already initialized");
        }
        dependenciesPerHistorical[streamNum] = new TreeSet<Integer>();
        if (dependencies != null) {
            dependenciesPerHistorical[streamNum].addAll(dependencies);
        }
    }

    public boolean isHasHistorical() {
        return hasHistorical;
    }

    public SortedSet<Integer>[] getDependenciesPerHistorical() {
        return dependenciesPerHistorical;
    }

    public boolean[] getHistorical() {
        return isHistorical;
    }
}
