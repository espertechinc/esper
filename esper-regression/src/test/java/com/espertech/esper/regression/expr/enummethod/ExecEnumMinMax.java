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
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigDecimal;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;

public class ExecEnumMinMax implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMinMaxScalarWithLambda(epService);
        runAssertionMinMaxEvents(epService);
        runAssertionMinMaxScalar(epService);
        runAssertionMinMaxScalarChain(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionMinMaxScalarChain(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(EventWithLongArray.class);

        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select coll.max().minus(1 minute) >= coll.min() as c0 from EventWithLongArray");
        stmt.addListener(listener);
        String[] fields = "c0".split(",");

        epService.getEPRuntime().sendEvent(new EventWithLongArray(new long[] {150000, 140000, 200000, 190000}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true});

        epService.getEPRuntime().sendEvent(new EventWithLongArray(new long[] {150000, 139999, 200000, 190000}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true});

        stmt.destroy();
    }

    private void runAssertionMinMaxScalarWithLambda(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", MyService.class.getName(), "extractNum");

        String[] fields = "val0,val1,val2,val3".split(",");
        String eplFragment = "select " +
                "strvals.min(v => extractNum(v)) as val0, " +
                "strvals.max(v => extractNum(v)) as val1, " +
                "strvals.min(v => v) as val2, " +
                "strvals.max(v => v) as val3 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Integer.class, String.class, String.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 5, "E1", "E5"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 1, "E1", "E1"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        stmtFragment.destroy();
    }

    private void runAssertionMinMaxEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "contained.min(x => p00) as val0, " +
                "contained.max(x => p00) as val1 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, 12});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,12", "E2,0", "E2,2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0, 12});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        stmtFragment.destroy();
    }

    private void runAssertionMinMaxScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "strvals.min() as val0, " +
                "strvals.max() as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{String.class, String.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E5"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        stmtFragment.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epl = "select contained.min() from Bean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.min()': Invalid input for built-in enumeration method 'min' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean_ST0.class.getName() + "' [select contained.min() from Bean]");
    }

    public static class MyService {
        public static int extractNum(String arg) {
            return Integer.parseInt(arg.substring(1));
        }

        public static BigDecimal extractBigDecimal(String arg) {
            return new BigDecimal(arg.substring(1));
        }
    }

    public final static class EventWithLongArray {
        private final long[] coll;

        public EventWithLongArray(long[] coll) {
            this.coll = coll;
        }

        public long[] getColl() {
            return coll;
        }
    }
}
