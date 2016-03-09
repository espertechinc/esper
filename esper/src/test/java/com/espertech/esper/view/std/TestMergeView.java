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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.epl.SupportExprNodeFactory;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.support.event.SupportEventTypeFactory;
import com.espertech.esper.support.view.SupportBeanClassView;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.support.view.SupportStreamImpl;
import com.espertech.esper.support.view.SupportViewDataChecker;
import junit.framework.TestCase;

public class TestMergeView extends TestCase
{
    private MergeView myView;
    private SupportBeanClassView childView;

    public void setUp() throws Exception
    {
        // Set up length window view and a test child view
        myView = new MergeView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(),
                SupportExprNodeFactory.makeIdentNodesMD("symbol"),
                SupportEventTypeFactory.createBeanType(SupportBean.class), false);

        childView = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.addView(childView);
    }

    public void testViewPush()
    {
        SupportStreamImpl stream = new SupportStreamImpl(SupportMarketDataBean.class, 2);
        stream.addView(myView);

        EventBean[] tradeBeans = new EventBean[10];

        // Send events, expect just forwarded
        tradeBeans[0] = makeTradeBean("IBM", 70);
        stream.insert(tradeBeans[0]);

        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[] { tradeBeans[0] });

        // Send some more events, expect forwarded
        tradeBeans[1] = makeTradeBean("GE", 90);
        tradeBeans[2] = makeTradeBean("CSCO", 20);
        stream.insert(new EventBean[] { tradeBeans[1], tradeBeans[2] });

        SupportViewDataChecker.checkOldData(childView, new EventBean[] { tradeBeans[0] });
        SupportViewDataChecker.checkNewData(childView, new EventBean[] { tradeBeans[1], tradeBeans[2] });
    }

    public void testCopyView() throws Exception
    {
        SupportBeanClassView parent = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.setParent(parent);

        MergeView copied = (MergeView) myView.cloneView();
        assertEquals(myView.getGroupFieldNames(), copied.getGroupFieldNames());
        assertEquals(myView.getEventType(), SupportEventTypeFactory.createBeanType(SupportBean.class));
    }

    private EventBean makeTradeBean(String symbol, int price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, "");
        return SupportEventBeanFactory.createObject(bean);
    }
}
