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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventType;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportBeanSimple;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.supportunit.filter.SupportFilterHandle;
import com.espertech.esper.supportunit.filter.SupportFilterSpecBuilder;
import junit.framework.TestCase;

public class TestEventTypeIndexBuilder extends TestCase {
    private EventTypeIndex eventTypeIndex;
    private EventTypeIndexBuilder indexBuilder;

    private EventType typeOne;
    private EventType typeTwo;

    private FilterValueSet valueSetOne;
    private FilterValueSet valueSetTwo;

    private FilterHandle callbackOne;
    private FilterHandle callbackTwo;

    private FilterServiceGranularLockFactoryReentrant lockFactory = new FilterServiceGranularLockFactoryReentrant();

    public void setUp() {
        eventTypeIndex = new EventTypeIndex(lockFactory);
        indexBuilder = new EventTypeIndexBuilder(eventTypeIndex, true);

        typeOne = SupportEventTypeFactory.createBeanType(SupportBean.class);
        typeTwo = SupportEventTypeFactory.createBeanType(SupportBeanSimple.class);

        valueSetOne = SupportFilterSpecBuilder.build(typeOne, new Object[0]).getValueSet(null, null, null, null, null);
        valueSetTwo = SupportFilterSpecBuilder.build(typeTwo, new Object[0]).getValueSet(null, null, null, null, null);

        callbackOne = new SupportFilterHandle();
        callbackTwo = new SupportFilterHandle();
    }

    public void testAddRemove() {
        assertNull(eventTypeIndex.get(typeOne));
        assertNull(eventTypeIndex.get(typeTwo));

        FilterServiceEntry entryOne = indexBuilder.add(valueSetOne, callbackOne, lockFactory);
        indexBuilder.add(valueSetTwo, callbackTwo, lockFactory);

        assertTrue(eventTypeIndex.get(typeOne) != null);
        assertTrue(eventTypeIndex.get(typeTwo) != null);

        indexBuilder.remove(callbackOne, entryOne);
        entryOne = indexBuilder.add(valueSetOne, callbackOne, lockFactory);
        indexBuilder.remove(callbackOne, entryOne);
    }
}
