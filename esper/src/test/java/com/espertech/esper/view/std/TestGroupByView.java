/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.view.std;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.epl.SupportExprNodeFactory;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.support.event.SupportEventTypeFactory;
import com.espertech.esper.support.view.*;
import com.espertech.esper.view.EventStream;
import junit.framework.TestCase;

public class TestGroupByView extends TestCase
{
    private GroupByView myGroupByView;
    private SupportBeanClassView ultimateChildView;
    private StatementContext statementContext;
    private AgentInstanceViewFactoryChainContext agentInstanceContext;

    public void setUp() throws Exception
    {
        statementContext = SupportStatementContextFactory.makeContext();
        agentInstanceContext = SupportStatementContextFactory.makeAgentInstanceViewFactoryContext();

        ExprNode[] expressions = SupportExprNodeFactory.makeIdentNodesMD("symbol");
        myGroupByView = new GroupByViewImpl(agentInstanceContext, expressions, ExprNodeUtility.getEvaluators(expressions));

        SupportBeanClassView childView = new SupportBeanClassView(SupportMarketDataBean.class);

        MergeView myMergeView = new MergeView(agentInstanceContext, SupportExprNodeFactory.makeIdentNodesMD("symbol"), SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class), false);

        ultimateChildView = new SupportBeanClassView(SupportMarketDataBean.class);

        // This is the view hierarchy
        myGroupByView.addView(childView);
        childView.addView(myMergeView);
        myMergeView.addView(ultimateChildView);

        SupportBeanClassView.getInstances().clear();
    }

    public void testViewPush()
    {
        // Reset instance lists of child views
        SupportBeanClassView.getInstances().clear();
        SupportMapView.getInstances().clear();

        // Set up a feed for the view under test - it will have a depth of 3 trades
        SupportStreamImpl stream = new SupportStreamImpl(SupportMarketDataBean.class, 3);
        stream.addView(myGroupByView);

        EventBean[] tradeBeans = new EventBean[10];

        // Send an IBM symbol event
        tradeBeans[0] = makeTradeBean("IBM", 70);
        stream.insert(tradeBeans[0]);

        // Expect 1 new beanclass view instance and check its data
        assertEquals(1, SupportBeanClassView.getInstances().size());
        SupportBeanClassView child_1 = SupportBeanClassView.getInstances().get(0);
        SupportViewDataChecker.checkOldData(child_1, null);
        SupportViewDataChecker.checkNewData(child_1, new EventBean[] { tradeBeans[0] });

        // Check the data of the ultimate receiver
        SupportViewDataChecker.checkOldData(ultimateChildView, null);
        SupportViewDataChecker.checkNewData(ultimateChildView, new EventBean[] {tradeBeans[0]});
    }

    public void testUpdate()
    {
        // Set up a feed for the view under test - it will have a depth of 3 trades
        SupportStreamImpl stream = new SupportStreamImpl(SupportMarketDataBean.class, 3);
        stream.addView(myGroupByView);

        // Send old a new events
        EventBean[] newEvents = new EventBean[] { makeTradeBean("IBM", 70), makeTradeBean("GE", 10) };
        EventBean[] oldEvents = new EventBean[] { makeTradeBean("IBM", 65), makeTradeBean("GE", 9) };
        myGroupByView.update(newEvents, oldEvents);

        assertEquals(2, SupportBeanClassView.getInstances().size());
        SupportBeanClassView child_1 = SupportBeanClassView.getInstances().get(0);
        SupportBeanClassView child_2 = SupportBeanClassView.getInstances().get(1);
        SupportViewDataChecker.checkOldData(child_1, new EventBean[] { oldEvents[0] });
        SupportViewDataChecker.checkNewData(child_1, new EventBean[] { newEvents[0] });
        SupportViewDataChecker.checkOldData(child_2, new EventBean[] { oldEvents[1] });
        SupportViewDataChecker.checkNewData(child_2, new EventBean[] { newEvents[1] });

        newEvents = new EventBean[] { makeTradeBean("IBM", 71), makeTradeBean("GE", 11) };
        oldEvents = new EventBean[] { makeTradeBean("IBM", 70), makeTradeBean("GE", 10) };
        myGroupByView.update(newEvents, oldEvents);

        SupportViewDataChecker.checkOldData(child_1, new EventBean[] { oldEvents[0] });
        SupportViewDataChecker.checkNewData(child_1, new EventBean[] { newEvents[0] });
        SupportViewDataChecker.checkOldData(child_2, new EventBean[] { oldEvents[1] });
        SupportViewDataChecker.checkNewData(child_2, new EventBean[] { newEvents[1] });
    }

    public void testInvalid()
    {
        try
        {
            myGroupByView.iterator();
            assertTrue(false);
        }
        catch (UnsupportedOperationException ex)
        {
            // Expected exception
        }
    }

    public void testMakeSubviews() throws Exception
    {
        EventStream eventStream = new SupportStreamImpl(SupportMarketDataBean.class, 4);
        ExprNode[] expressions = SupportExprNodeFactory.makeIdentNodesMD("symbol");
        GroupByView groupView = new GroupByViewImpl(agentInstanceContext, expressions, ExprNodeUtility.getEvaluators(expressions));
        eventStream.addView(groupView);

        Object[] groupByValue = new Object[] {"IBM"};

        // Invalid for no child nodes
        try
        {
            GroupByViewImpl.makeSubViews(groupView, "symbol".split(","), groupByValue, agentInstanceContext);
            assertTrue(false);
        }
        catch (EPException ex)
        {
            // Expected exception
        }

        // Invalid for child node is a merge node - doesn't make sense to group and merge only
        MergeView mergeViewOne = new MergeView(agentInstanceContext, SupportExprNodeFactory.makeIdentNodesMD("symbol"), null, false);
        groupView.addView(mergeViewOne);
        try
        {
            GroupByViewImpl.makeSubViews(groupView, "symbol".split(","), groupByValue, agentInstanceContext);
            assertTrue(false);
        }
        catch (EPException ex)
        {
            // Expected exception
        }

        // Add a size view parent of merge view
        groupView = new GroupByViewImpl(agentInstanceContext, expressions, ExprNodeUtility.getEvaluators(expressions));

        FirstElementView firstElementView_1 = new FirstElementView(null);

        groupView.addView(firstElementView_1);
        groupView.setParent(eventStream);
        mergeViewOne = new MergeView(agentInstanceContext, SupportExprNodeFactory.makeIdentNodesMD("symbol"), null, false);
        firstElementView_1.addView(mergeViewOne);

        Object subViews = GroupByViewImpl.makeSubViews(groupView, "symbol".split(","), groupByValue, agentInstanceContext);

        assertTrue(subViews instanceof FirstElementView);
        assertTrue(subViews != firstElementView_1);

        FirstElementView firstEleView = (FirstElementView) subViews;
        assertEquals(1, firstEleView.getViews().length);
        assertTrue(firstEleView.getViews()[0] instanceof AddPropertyValueOptionalView);

        AddPropertyValueOptionalView md = (AddPropertyValueOptionalView) firstEleView.getViews()[0];
        assertEquals(1, md.getViews().length);
        assertTrue(md.getViews()[0] == mergeViewOne);
    }

    private EventBean makeTradeBean(String symbol, int price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, "");
        return SupportEventBeanFactory.createObject(bean);
    }
}
