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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;

public class ExecEnumSequenceEqual implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean_ST0_Container", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSelectFrom(epService);
        runAssertionTwoProperties(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionSelectFrom(EPServiceProvider epService) {
        String[] fields = "val0".split(",");
        String eplFragment = "select contained.selectFrom(x => key0).sequenceEqual(contained.selectFrom(y => id)) as val0 " +
                "from SupportBean_ST0_Container";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Boolean.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("I1,E1,0", "I2,E2,0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("I3,I3,0", "X4,X4,0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("I3,I3,0", "X4,Y4,0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("I3,I3,0", "Y4,X4,0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        stmtFragment.destroy();
    }

    private void runAssertionTwoProperties(EPServiceProvider epService) {

        String[] fields = "val0".split(",");
        String eplFragment = "select " +
                "strvals.sequenceEqual(strvalstwo) as val0 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Boolean.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3", "E1,E2,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E3", "E1,E2,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E3", "E1,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3", "E1,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,null,E3", "E1,E2,null,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3", "E1,E2,null"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,null", "E1,E2,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1", ""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("", "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1", "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("", ""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null, ""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("", null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null, null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        stmtFragment.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epl = "select window(*).sequenceEqual(strvals) from SupportCollection#lastevent";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'window(*).sequenceEqual(strvals)': Invalid input for built-in enumeration method 'sequenceEqual' and 1-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type 'SupportCollection' [select window(*).sequenceEqual(strvals) from SupportCollection#lastevent]");
    }
}