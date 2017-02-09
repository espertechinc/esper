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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.supportunit.epl.SupportJoinSetComposer;
import com.espertech.esper.supportunit.epl.SupportJoinSetProcessor;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class TestJoinExecutionStrategyImpl extends TestCase {
    private JoinExecutionStrategyImpl join;
    private Set<MultiKey<EventBean>> oldEvents;
    private Set<MultiKey<EventBean>> newEvents;
    private SupportJoinSetProcessor filter;
    private SupportJoinSetProcessor indicator;

    public void setUp() {
        oldEvents = new HashSet<MultiKey<EventBean>>();
        newEvents = new HashSet<MultiKey<EventBean>>();

        JoinSetComposer composer = new SupportJoinSetComposer(new UniformPair<Set<MultiKey<EventBean>>>(newEvents, oldEvents));
        filter = new SupportJoinSetProcessor();
        indicator = new SupportJoinSetProcessor();

        join = new JoinExecutionStrategyImpl(composer, filter, indicator, null);
    }

    public void testJoin() {
        join.join(null, null);

        assertSame(newEvents, filter.getLastNewEvents());
        assertSame(oldEvents, filter.getLastOldEvents());
        assertNull(indicator.getLastNewEvents());
        assertNull(indicator.getLastOldEvents());
    }
}
