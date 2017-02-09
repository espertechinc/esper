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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.supportunit.bean.*;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestPropertyUtility extends TestCase {
    private final EventAdapterService eventSource = SupportEventAdapterService.getService();

    private static final Map<String, int[]> expectedPropertyGroups = new HashMap<String, int[]>();

    static {
        expectedPropertyGroups.put("p0", new int[]{1, 2, 3});
        expectedPropertyGroups.put("p1", new int[]{0});
        expectedPropertyGroups.put("p2", new int[]{1, 3});
        expectedPropertyGroups.put("p3", new int[]{1});
        expectedPropertyGroups.put("p4", new int[]{2});
        expectedPropertyGroups.put("p5", new int[]{0, 3});
    }

    private EventType types[];
    private String[] fields = "p0,p1,p2,p3,p4,p5".split(",");

    public void setUp() {
        types = new EventType[5];
        types[0] = eventSource.addBeanType("D1", SupportDeltaOne.class, false, false, false);
        types[1] = eventSource.addBeanType("D2", SupportDeltaTwo.class, false, false, false);
        types[2] = eventSource.addBeanType("D3", SupportDeltaThree.class, false, false, false);
        types[3] = eventSource.addBeanType("D4", SupportDeltaFour.class, false, false, false);
        types[4] = eventSource.addBeanType("D5", SupportDeltaFive.class, false, false, false);
    }

    public void testAnalyze() {
        PropertyGroupDesc[] groups = PropertyUtility.analyzeGroups(fields, types, new String[]{"D1", "D2", "D3", "D4", "D5"});
        assertEquals(4, groups.length);

        assertEquals(0, groups[0].getGroupNum());
        EPAssertionUtil.assertEqualsExactOrder(groups[0].getProperties(), new Object[]{"p1", "p5"});
        assertEquals(2, groups[0].getTypes().size());
        assertEquals("D1", groups[0].getTypes().get(types[0]));
        assertEquals("D5", groups[0].getTypes().get(types[4]));

        assertEquals(1, groups[1].getGroupNum());
        EPAssertionUtil.assertEqualsExactOrder(groups[1].getProperties(), new Object[]{"p0", "p2", "p3"});
        assertEquals(1, groups[1].getTypes().size());
        assertEquals("D2", groups[1].getTypes().get(types[1]));

        assertEquals(2, groups[2].getGroupNum());
        EPAssertionUtil.assertEqualsExactOrder(groups[2].getProperties(), new Object[]{"p0", "p4"});
        assertEquals(1, groups[2].getTypes().size());
        assertEquals("D3", groups[2].getTypes().get(types[2]));

        assertEquals(3, groups[3].getGroupNum());
        EPAssertionUtil.assertEqualsExactOrder(groups[3].getProperties(), new Object[]{"p0", "p2", "p5"});
        assertEquals(1, groups[3].getTypes().size());
        assertEquals("D4", groups[3].getTypes().get(types[3]));
    }

    public void testGetGroups() {
        PropertyGroupDesc[] groups = PropertyUtility.analyzeGroups(fields, types, new String[]{"D1", "D2", "D3", "D4", "D5"});
        Map<String, int[]> groupsPerProp = PropertyUtility.getGroupsPerProperty(groups);

        assertEquals(groupsPerProp.size(), expectedPropertyGroups.size());
        for (Map.Entry<String, int[]> entry : expectedPropertyGroups.entrySet()) {
            int[] result = groupsPerProp.get(entry.getKey());
            EPAssertionUtil.assertEqualsExactOrder(entry.getValue(), result);
        }
    }
}
