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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.Map;

public class TestEnumSelectFrom extends TestCase {

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

    public void testNew() {

        String eplFragment = "select " +
                "contained.selectFrom(x => new {c0 = id||'x', c1 = key0||'y'}) as val0 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("E1,12,0", "E2,11,0", "E3,2,0"));
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E1x", "12y"}, {"E2x", "11y"}, {"E3x", "2y"}});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("E4,0,1"));
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E4x", "0y"}});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value(null));
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","), null);

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value());
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[0][]);
    }

    private Map[] toMapArray(Object result) {
        if (result == null) {
            return null;
        }
        Collection<Map> val = (Collection<Map>) result;
        return val.toArray(new Map[val.size()]);
    }

    public void testSelect() {

        String eplFragment = "select " +
                "contained.selectFrom(x => id) as val0 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E3,2"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E2", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", new String[0]);
        listener.reset();
        stmtFragment.destroy();

        // test scalar-coll with lambda
        String[] fields = "val0".split(",");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", TestEnumMinMax.MyService.class.getName(), "extractNum");
        String eplLambda = "select " +
                "strvals.selectFrom(v => extractNum(v)) as val0 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", 2, 1, 5, 4);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", 1);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0");
    }

    private void tryInvalid(String epl, String message) {
        try
        {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }
}
