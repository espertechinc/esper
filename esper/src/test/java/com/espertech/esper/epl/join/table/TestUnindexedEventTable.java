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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

public class TestUnindexedEventTable extends TestCase {
    public void testFlow() {
        UnindexedEventTable rep = new UnindexedEventTableImpl(1);

        EventBean[] addOne = SupportEventBeanFactory.makeEvents(new String[]{"a", "b"});
        rep.add(addOne, null);
        rep.remove(new EventBean[]{addOne[0]}, null);
    }
}
