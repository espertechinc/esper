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
package com.espertech.esper.supportregression.context;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.ContextPartitionIdentifier;
import com.espertech.esper.client.context.ContextPartitionIdentifierInitiatedTerminated;
import com.espertech.esper.client.context.ContextPartitionSelectorFiltered;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class SupportSelectorFilteredInitTerm implements ContextPartitionSelectorFiltered {

    private final String matchP00Value;

    private List<Object> startTimes = new ArrayList<Object>();
    private List<Object> p00PropertyValues = new ArrayList<Object>();
    private LinkedHashSet<Integer> cpids = new LinkedHashSet<Integer>();
    private ContextPartitionIdentifierInitiatedTerminated lastValue;

    public SupportSelectorFilteredInitTerm(String matchP00Value) {
        this.matchP00Value = matchP00Value;
    }

    public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
        ContextPartitionIdentifierInitiatedTerminated id = (ContextPartitionIdentifierInitiatedTerminated) contextPartitionIdentifier;
        if (matchP00Value == null && cpids.contains(id.getContextPartitionId())) {
            throw new RuntimeException("Already exists context id: " + id.getContextPartitionId());
        }
        cpids.add(id.getContextPartitionId());
        startTimes.add(id.getStartTime());
        String p00Value = (String) ((EventBean) id.getProperties().get("s0")).get("p00");
        p00PropertyValues.add(p00Value);
        lastValue = id;
        return matchP00Value != null && matchP00Value.equals(p00Value);
    }

    public Object[] getContextsStartTimes() {
        return startTimes.toArray();
    }

    public Object[] getP00PropertyValues() {
        return p00PropertyValues.toArray();
    }
}
