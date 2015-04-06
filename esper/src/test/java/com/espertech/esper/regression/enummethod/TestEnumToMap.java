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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean_ST0;
import com.espertech.esper.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.support.bean.SupportCollection;
import com.espertech.esper.support.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class TestEnumToMap extends TestCase {

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

    public void testToMap() {

        // - duplicate value allowed, latest value wins
        // - null key & value allowed
        
        String eplFragment = "select contained.toMap(c => id, c=> p00) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val".split(","), new Class[]{Map.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E3,12", "E2,5"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{1, 5, 12});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E3,12", "E2,12", "E1,2"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{2, 12, 12});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0_Container(Collections.singletonList(new SupportBean_ST0(null, null))));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{null, null, null});
        stmtFragment.destroy();

        // test scalar-coll with lambda
        String[] fields = "val0".split(",");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", TestEnumMinMax.MyService.class.getName(), "extractNum");
        String eplLambda = "select " +
                "strvals.toMap(c => c, c => extractNum(c)) as val0 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fields, new Class[]{Map.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E3"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), "E1,E2,E3".split(","), new Object[]{1, 2, 3});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), "E1".split(","), new Object[]{1});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        assertNull(listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        assertEquals(0, ((Map) listener.assertOneGetNewAndReset().get("val0")).size());
    }
}
