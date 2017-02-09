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
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportQueryExecNode;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

public class TestQueryPlanExecStrategy extends TestCase {
    private ExecNodeQueryStrategy strategy;
    private SupportQueryExecNode supportQueryExecNode;

    public void setUp() {
        supportQueryExecNode = new SupportQueryExecNode(null);
        strategy = new ExecNodeQueryStrategy(4, 20, supportQueryExecNode);
    }

    public void testLookup() {
        EventBean lookupEvent = SupportEventBeanFactory.createObject(new SupportBean());

        strategy.lookup(new EventBean[]{lookupEvent}, null, null);

        assertSame(lookupEvent, supportQueryExecNode.getLastPrefillPath()[4]);
    }
}
