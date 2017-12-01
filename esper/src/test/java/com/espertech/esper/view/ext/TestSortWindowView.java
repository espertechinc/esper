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
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.view.SupportBeanClassView;
import com.espertech.esper.supportunit.view.SupportStreamImpl;
import com.espertech.esper.supportunit.view.SupportViewDataChecker;
import junit.framework.TestCase;

public class TestSortWindowView extends TestCase {
    private SortWindowView myView;
    private SupportBeanClassView childView;

    public void setUp() throws Exception {
        // Set up length window view and a test child view
        ExprNode[] expressions = SupportExprNodeFactory.makeIdentNodesMD("volume");
        SortWindowViewFactory factory = new SortWindowViewFactory();
        factory.sortCriteriaExpressions = expressions;
        factory.sortCriteriaEvaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(expressions);
        factory.isDescendingValues = new boolean[]{false};
        factory.comparator = ExprNodeUtilityCore.getComparatorHashableMultiKeys(factory.sortCriteriaExpressions, false, factory.isDescendingValues);
        myView = new SortWindowView(factory, 5, null, null);
        childView = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.addView(childView);
    }

    public void testViewOneProperty() {
        // Set up a feed for the view under test - the depth is 10 events so bean[10] will cause bean[0] to go old
        SupportStreamImpl stream = new SupportStreamImpl(SupportMarketDataBean.class, 10);
        stream.addView(myView);

        EventBean bean[] = new EventBean[12];

        bean[0] = makeBean(1000);
        stream.insert(bean[0]);
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[0]}, myView.iterator());

        bean[1] = makeBean(800);
        bean[2] = makeBean(1200);
        stream.insert(new EventBean[]{bean[1], bean[2]});
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[1], bean[2]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[1], bean[0], bean[2]}, myView.iterator());

        bean[3] = makeBean(1200);
        bean[4] = makeBean(1000);
        bean[5] = makeBean(1400);
        bean[6] = makeBean(1100);
        stream.insert(new EventBean[]{bean[3], bean[4], bean[5], bean[6]});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[5], bean[2]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[3], bean[4], bean[5], bean[6]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[1], bean[4], bean[0], bean[6], bean[3]}, myView.iterator());

        bean[7] = makeBean(800);
        bean[8] = makeBean(700);
        bean[9] = makeBean(1200);
        stream.insert(new EventBean[]{bean[7], bean[8], bean[9]});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[3], bean[9], bean[6]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[7], bean[8], bean[9]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[8], bean[7], bean[1], bean[4], bean[0]}, myView.iterator());

        bean[10] = makeBean(1050);
        stream.insert(new EventBean[]{bean[10]});       // Thus bean[0] will be old data !
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[0]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[10]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[8], bean[7], bean[1], bean[4], bean[10]}, myView.iterator());

        bean[11] = makeBean(2000);
        stream.insert(new EventBean[]{bean[11]});       // Thus bean[1] will be old data !
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[1]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[11]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[8], bean[7], bean[4], bean[10], bean[11]}, myView.iterator());
    }

    public void testViewTwoProperties() throws Exception {
        // Set up a sort windows that sorts on two properties
        ExprNode[] expressions = SupportExprNodeFactory.makeIdentNodesMD("volume", "price");
        SortWindowViewFactory factory = new SortWindowViewFactory();
        factory.sortCriteriaExpressions = expressions;
        factory.sortCriteriaEvaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(expressions);
        factory.isDescendingValues = new boolean[]{false, true};
        factory.comparator = ExprNodeUtilityCore.getComparatorHashableMultiKeys(factory.sortCriteriaExpressions, false, factory.isDescendingValues);
        myView = new SortWindowView(factory, 5, null, null);
        childView = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.addView(childView);

        // Set up a feed for the view under test - the depth is 10 events so bean[10] will cause bean[0] to go old
        SupportStreamImpl stream = new SupportStreamImpl(SupportMarketDataBean.class, 10);
        stream.addView(myView);

        EventBean bean[] = new EventBean[12];

        bean[0] = makeBean(20d, 1000);
        stream.insert(bean[0]);
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[0]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[0]}, myView.iterator());

        bean[1] = makeBean(19d, 800);
        bean[2] = makeBean(18d, 1200);
        stream.insert(new EventBean[]{bean[1], bean[2]});
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[1], bean[2]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[1], bean[0], bean[2]}, myView.iterator());

        bean[3] = makeBean(17d, 1200);
        bean[4] = makeBean(16d, 1000);
        bean[5] = makeBean(15d, 1400);
        bean[6] = makeBean(14d, 1100);
        stream.insert(new EventBean[]{bean[3], bean[4], bean[5], bean[6]});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[5], bean[3]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[3], bean[4], bean[5], bean[6]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[1], bean[0], bean[4], bean[6], bean[2]}, myView.iterator());

        bean[7] = makeBean(13d, 800);
        bean[8] = makeBean(12d, 700);
        bean[9] = makeBean(11d, 1200);
        stream.insert(new EventBean[]{bean[7], bean[8], bean[9]});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[9], bean[2], bean[6]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[7], bean[8], bean[9]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[8], bean[1], bean[7], bean[0], bean[4]}, myView.iterator());

        bean[10] = makeBean(10d, 1050);
        stream.insert(new EventBean[]{bean[10]});       // Thus bean[0] will be old data !
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[0]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[10]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[8], bean[1], bean[7], bean[4], bean[10]}, myView.iterator());

        bean[11] = makeBean(2000);
        stream.insert(new EventBean[]{bean[11]});       // Thus bean[1] will be old data !
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{bean[1]});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{bean[11]});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{bean[8], bean[7], bean[4], bean[10], bean[11]}, myView.iterator());
    }

    private EventBean makeBean(long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean("CSCO.O", 0, volume, "");
        return SupportEventBeanFactory.createObject(bean);
    }

    private EventBean makeBean(double price, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean("CSCO.O", price, volume, "");
        return SupportEventBeanFactory.createObject(bean);
    }
}
