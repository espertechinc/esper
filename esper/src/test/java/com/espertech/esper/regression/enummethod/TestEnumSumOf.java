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
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_Container;
import com.espertech.esper.support.bean.SupportCollection;
import com.espertech.esper.support.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestEnumSumOf extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("Bean", SupportBean_Container.class);
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

    public void testSumEvents() {

        String[] fields = "val0,val1,val2,val3".split(",");
        String eplFragment = "select " +
                "beans.sumOf(x => intBoxed) as val0," +
                "beans.sumOf(x => doubleBoxed) as val1," +
                "beans.sumOf(x => longBoxed) as val2," +
                "beans.sumOf(x => bigDecimal) as val3 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Double.class, Long.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(new SupportBean_Container(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_Container(Collections.<SupportBean>emptyList()));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        List<SupportBean> list = new ArrayList<SupportBean>();
        list.add(make(2,3d,4l,5));
        epService.getEPRuntime().sendEvent(new SupportBean_Container(list));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, 3d, 4L, new BigDecimal(5)});

        list.add(make(4,6d,8l,10));
        epService.getEPRuntime().sendEvent(new SupportBean_Container(list));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2 + 4, 3d + 6d, 4L + 8L, new BigDecimal(5 + 10)});
    }

    public void testSumOfScalar() {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "intvals.sumOf() as val0, " +
                "bdvals.sumOf() as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("1,4,5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1 + 4 + 5, new BigDecimal(1 + 4 + 5)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("3,4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3 + 4, new BigDecimal(3 + 4)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, new BigDecimal(3)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        stmtFragment.destroy();

        // test average with lambda
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", TestEnumMinMax.MyService.class.getName(), "extractNum");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractBigDecimal", TestEnumMinMax.MyService.class.getName(), "extractBigDecimal");

        // lambda with string-array input
        String[] fieldsLambda = "val0,val1".split(",");
        String eplLambda = "select " +
                "strvals.sumOf(v => extractNum(v)) as val0, " +
                "strvals.sumOf(v => extractBigDecimal(v)) as val1 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fieldsLambda, new Class[]{Integer.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{2+1+5+4, new BigDecimal(2+1+5+4)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{1, new BigDecimal(1)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

    }

    private SupportBean make(Integer intBoxed, Double doubleBoxed, Long longBoxed, int bigDecimal) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setBigDecimal(new BigDecimal(bigDecimal));
        return bean;
    }

    public void testInvalid() {
        String epl;

        epl = "select beans.sumof() from Bean";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'beans.sumof()': Invalid input for built-in enumeration method 'sumof' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type 'com.espertech.esper.support.bean.SupportBean' [select beans.sumof() from Bean]");
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
