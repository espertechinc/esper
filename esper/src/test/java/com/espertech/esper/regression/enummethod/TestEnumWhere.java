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

package com.espertech.esper.regression.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.support.bean.SupportCollection;
import com.espertech.esper.support.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collection;

public class TestEnumWhere extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("Bean", SupportBean_ST0_Container.class);
        config.addEventType("SupportCollection", SupportCollection.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testWhereEvents() {

        String epl = "select " +
                "contained.where(x => p00 = 9) as val0," +
                "contained.where((x, i) => x.p00 = 9 and i >= 1) as val1 from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val0,val1".split(","), new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,9", "E2,1", "E3,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,9"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E3");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        LambdaAssertionUtil.assertST0Id(listener, "val0", null);
        LambdaAssertionUtil.assertST0Id(listener, "val1", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "");
        listener.reset();
    }

    public void testWhereString() {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "strvals.where(x => x not like '%1%') as val0, " +
                "strvals.where((x, i) => x not like '%1%' and i > 1) as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E2", "E3");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E4,E2,E1"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E4", "E2");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", new String[0]);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", new String[0]);
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", new String[0]);
        listener.reset();

        stmtFragment.destroy();

        // test boolean
        eplFragment = "select " +
                "boolvals.where(x => x) as val0 " +
                "from SupportCollection";
        stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeBoolean("true,true,false"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", true, true);
        listener.reset();
    }
}
