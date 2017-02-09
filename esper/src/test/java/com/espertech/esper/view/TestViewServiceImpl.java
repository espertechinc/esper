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
package com.espertech.esper.view;

import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportBean_A;
import com.espertech.esper.supportunit.view.SupportStreamImpl;
import com.espertech.esper.supportunit.view.SupportViewSpecFactory;
import junit.framework.TestCase;

public class TestViewServiceImpl extends TestCase {
    private ViewServiceImpl viewService;

    private Viewable viewOne;
    private Viewable viewTwo;
    private Viewable viewThree;
    private Viewable viewFour;
    private Viewable viewFive;

    private EventStream streamOne;
    private EventStream streamTwo;

    public void setUp() throws Exception {
        streamOne = new SupportStreamImpl(SupportBean.class, 1);
        streamTwo = new SupportStreamImpl(SupportBean_A.class, 1);

        viewService = new ViewServiceImpl();

        AgentInstanceViewFactoryChainContext context = SupportStatementContextFactory.makeAgentInstanceViewFactoryContext();

        viewOne = viewService.createViews(streamOne, SupportViewSpecFactory.makeFactoryListOne(streamOne.getEventType()), context, false).getFinalViewable();
        viewTwo = viewService.createViews(streamOne, SupportViewSpecFactory.makeFactoryListTwo(streamOne.getEventType()), context, false).getFinalViewable();
        viewThree = viewService.createViews(streamOne, SupportViewSpecFactory.makeFactoryListThree(streamOne.getEventType()), context, false).getFinalViewable();
        viewFour = viewService.createViews(streamOne, SupportViewSpecFactory.makeFactoryListFour(streamOne.getEventType()), context, false).getFinalViewable();
        viewFive = viewService.createViews(streamTwo, SupportViewSpecFactory.makeFactoryListFive(streamTwo.getEventType()), context, false).getFinalViewable();
    }

    public void testCheckChainReuse() {
        // Child views of first and second level must be the same
        assertEquals(2, streamOne.getViews().length);
        View child1_1 = streamOne.getViews()[0];
        View child2_1 = streamOne.getViews()[0];
        assertTrue(child1_1 == child2_1);

        assertEquals(2, child1_1.getViews().length);
        View child1_1_1 = child1_1.getViews()[0];
        View child2_1_1 = child2_1.getViews()[0];
        assertTrue(child1_1_1 == child2_1_1);

        assertEquals(2, child1_1_1.getViews().length);
        assertEquals(2, child2_1_1.getViews().length);
        assertTrue(child2_1_1.getViews()[0] != child2_1_1.getViews()[1]);

        // Create one more view chain
        View child3_1 = streamOne.getViews()[0];
        assertTrue(child3_1 == child1_1);
        assertEquals(2, child3_1.getViews().length);
        View child3_1_1 = child3_1.getViews()[1];
        assertTrue(child3_1_1 != child2_1_1);
    }

    public void testRemove() {
        assertEquals(2, streamOne.getViews().length);
        assertEquals(1, streamTwo.getViews().length);

        viewService.remove(streamOne, viewOne);
        viewService.remove(streamOne, viewTwo);
        viewService.remove(streamOne, viewThree);
        viewService.remove(streamOne, viewFour);

        viewService.remove(streamTwo, viewFive);

        assertEquals(0, streamOne.getViews().length);
        assertEquals(0, streamTwo.getViews().length);
    }

    public void testRemoveInvalid() {
        try {
            viewService.remove(streamOne, viewOne);
            viewService.remove(streamOne, viewOne);
            TestCase.fail();
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }
}