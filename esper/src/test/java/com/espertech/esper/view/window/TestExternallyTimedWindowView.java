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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConstGivenDelta;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.view.SupportBeanClassView;
import com.espertech.esper.supportunit.view.SupportStreamImpl;
import com.espertech.esper.supportunit.view.SupportViewDataChecker;
import junit.framework.TestCase;

public class TestExternallyTimedWindowView extends TestCase {
    private ExternallyTimedWindowView myView;
    private SupportBeanClassView childView;

    public void setUp() throws Exception {
        // Set up timed window view and a test child view, set the time window size to 1 second
        ExprNode node = SupportExprNodeFactory.makeIdentNodeBean("longPrimitive");
        myView = new ExternallyTimedWindowView(new ExternallyTimedWindowViewFactory(), node, node.getForge().getExprEvaluator(), new ExprTimePeriodEvalDeltaConstGivenDelta(1000), null, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        childView = new SupportBeanClassView(SupportBean.class);
        myView.addView(childView);
    }

    public void testIncorrectUse() throws Exception {
        try {
            myView = new ExternallyTimedWindowView(null, SupportExprNodeFactory.makeIdentNodeBean("theString"), null, new ExprTimePeriodEvalDeltaConstGivenDelta(0), null, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());
        } catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    public void testViewPush() {
        // Set up a feed for the view under test - it will have a depth of 3 trades
        SupportStreamImpl stream = new SupportStreamImpl(SupportBean.class, 3);
        stream.addView(myView);

        EventBean[] a = makeBeans("a", 10000, 1);
        stream.insert(a);
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{a[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{a[0]}, myView.iterator());

        EventBean[] b = makeBeans("b", 10500, 2);
        stream.insert(b);
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{b[0], b[1]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{a[0], b[0], b[1]}, myView.iterator());

        EventBean[] c = makeBeans("c", 10900, 1);
        stream.insert(c);
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{c[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{a[0], b[0], b[1], c[0]}, myView.iterator());

        EventBean[] d = makeBeans("d", 10999, 1);
        stream.insert(d);
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{d[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{a[0], b[0], b[1], c[0], d[0]}, myView.iterator());

        EventBean[] e = makeBeans("e", 11000, 2);
        stream.insert(e);
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{a[0]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{e[0], e[1]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{b[0], b[1], c[0], d[0], e[0], e[1]}, myView.iterator());

        EventBean[] f = makeBeans("f", 11500, 1);
        stream.insert(f);
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{b[0], b[1]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{f[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{c[0], d[0], e[0], e[1], f[0]}, myView.iterator());

        EventBean[] g = makeBeans("g", 11899, 1);
        stream.insert(g);
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{g[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{c[0], d[0], e[0], e[1], f[0], g[0]}, myView.iterator());

        EventBean[] h = makeBeans("h", 11999, 3);
        stream.insert(h);
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{c[0], d[0]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{h[0], h[1], h[2]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{e[0], e[1], f[0], g[0], h[0], h[1], h[2]}, myView.iterator());

        EventBean[] i = makeBeans("i", 13001, 1);
        stream.insert(i);
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{e[0], e[1], f[0], g[0], h[0], h[1], h[2]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{i[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{i[0]}, myView.iterator());
    }

    private EventBean[] makeBeans(String id, long timestamp, int numBeans) {
        EventBean[] beans = new EventBean[numBeans];
        for (int i = 0; i < numBeans; i++) {
            SupportBean bean = new SupportBean();
            bean.setLongPrimitive(timestamp);
            bean.setTheString(id + 1);
            beans[i] = SupportEventBeanFactory.createObject(bean);
        }
        return beans;
    }
}
