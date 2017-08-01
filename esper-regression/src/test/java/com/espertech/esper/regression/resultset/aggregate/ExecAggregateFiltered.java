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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanNumeric;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecAggregateFiltered implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(BlackWhiteEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanNumeric.class);

        runAssertionBlackWhitePercent(epService);
        runAssertionCountVariations(epService);
        runAssertionAllAggFunctions(epService);
        runAssertionFirstLastEver(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionBlackWhitePercent(EPServiceProvider epService) {
        String[] fields = "cb,cnb,c,pct".split(",");
        String epl = "select count(*,black) as cb, count(*,not black) as cnb, count(*) as c, count(*,black)/count(*) as pct from BlackWhiteEvent#length(3)";
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        epService.getEPRuntime().sendEvent(new BlackWhiteEvent(true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 0L, 1L, 1d});

        epService.getEPRuntime().sendEvent(new BlackWhiteEvent(false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 1L, 2L, 0.5d});

        epService.getEPRuntime().sendEvent(new BlackWhiteEvent(false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 2L, 3L, 1 / 3d});

        epService.getEPRuntime().sendEvent(new BlackWhiteEvent(false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0L, 3L, 3L, 0d});

        SupportModelHelper.compileCreate(epService, epl);
        SupportModelHelper.compileCreate(epService, "select count(distinct black,not black), count(black,black) from BlackWhiteEvent");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCountVariations(EPServiceProvider epService) {
        String[] fields = "c1,c2".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "count(intBoxed, boolPrimitive) as c1," +
                "count(distinct intBoxed, boolPrimitive) as c2 " +
                "from SupportBean#length(3)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBean(100, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 1L});

        epService.getEPRuntime().sendEvent(makeBean(100, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L, 1L});

        epService.getEPRuntime().sendEvent(makeBean(101, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L, 1L});

        epService.getEPRuntime().sendEvent(makeBean(102, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L, 2L});

        epService.getEPRuntime().sendEvent(makeBean(103, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 1L});

        epService.getEPRuntime().sendEvent(makeBean(104, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 1L});

        epService.getEPRuntime().sendEvent(makeBean(105, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0L, 0L});

        stmt.destroy();
    }

    private void runAssertionAllAggFunctions(EPServiceProvider epService) {

        String[] fields = "cavedev,cavg,cmax,cmedian,cmin,cstddev,csum,cfmaxever,cfminever".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select " +
                "avedev(intBoxed, boolPrimitive) as cavedev," +
                "avg(intBoxed, boolPrimitive) as cavg, " +
                "fmax(intBoxed, boolPrimitive) as cmax, " +
                "median(intBoxed, boolPrimitive) as cmedian, " +
                "fmin(intBoxed, boolPrimitive) as cmin, " +
                "stddev(intBoxed, boolPrimitive) as cstddev, " +
                "sum(intBoxed, boolPrimitive) as csum," +
                "fmaxever(intBoxed, boolPrimitive) as cfmaxever, " +
                "fminever(intBoxed, boolPrimitive) as cfminever " +
                "from SupportBean#length(3)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBean(100, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(makeBean(10, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0.0d, 10.0, 10, 10.0, 10, null, 10, 10, 10});

        epService.getEPRuntime().sendEvent(makeBean(11, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0.0d, 10.0, 10, 10.0, 10, null, 10, 10, 10});

        epService.getEPRuntime().sendEvent(makeBean(20, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5.0d, 15.0, 20, 15.0, 10, 7.0710678118654755, 30, 20, 10});

        epService.getEPRuntime().sendEvent(makeBean(30, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5.0d, 25.0, 30, 25.0, 20, 7.0710678118654755, 50, 30, 10});

        // Test all remaining types of "sum"
        stmt.destroy();
        fields = "c1,c2,c3,c4".split(",");
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select " +
                "sum(floatPrimitive, boolPrimitive) as c1," +
                "sum(doublePrimitive, boolPrimitive) as c2, " +
                "sum(longPrimitive, boolPrimitive) as c3, " +
                "sum(shortPrimitive, boolPrimitive) as c4 " +
                "from SupportBean#length(2)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBean(2f, 3d, 4L, (short) 5, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        epService.getEPRuntime().sendEvent(makeBean(3f, 4d, 5L, (short) 6, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3f, 4d, 5L, 6});

        epService.getEPRuntime().sendEvent(makeBean(4f, 5d, 6L, (short) 7, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{7f, 9d, 11L, 13});

        epService.getEPRuntime().sendEvent(makeBean(1f, 1d, 1L, (short) 1, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5f, 6d, 7L, 8});

        // Test min/max-ever
        stmt.destroy();
        fields = "c1,c2".split(",");
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select " +
                "fmax(intBoxed, boolPrimitive) as c1," +
                "fmin(intBoxed, boolPrimitive) as c2 " +
                "from SupportBean");
        stmt.addListener(listener);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        epService.getEPRuntime().sendEvent(makeBean(10, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 10});

        epService.getEPRuntime().sendEvent(makeBean(20, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 10});

        epService.getEPRuntime().sendEvent(makeBean(8, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 10});

        epService.getEPRuntime().sendEvent(makeBean(7, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 7});

        epService.getEPRuntime().sendEvent(makeBean(30, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{20, 7});

        epService.getEPRuntime().sendEvent(makeBean(40, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{40, 7});

        // test big decimal big integer
        stmt.destroy();
        fields = "c1,c2,c3".split(",");
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select " +
                "avg(bigdec, bigint < 100) as c1," +
                "sum(bigdec, bigint < 100) as c2, " +
                "sum(bigint, bigint < 100) as c3 " +
                "from SupportBeanNumeric#length(2)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(new BigInteger("10"), new BigDecimal(20)));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(20), new BigDecimal(20), new BigInteger("10")});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(new BigInteger("101"), new BigDecimal(101)));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(20), new BigDecimal(20), new BigInteger("10")});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(new BigInteger("20"), new BigDecimal(40)));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(40), new BigDecimal(40), new BigInteger("20")});

        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(new BigInteger("30"), new BigDecimal(50)));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(45), new BigDecimal(90), new BigInteger("50")});

        stmt.destroy();
        String epl = "select " +
                "avedev(distinct intBoxed,boolPrimitive) as cavedev, " +
                "avg(distinct intBoxed,boolPrimitive) as cavg, " +
                "fmax(distinct intBoxed,boolPrimitive) as cmax, " +
                "median(distinct intBoxed,boolPrimitive) as cmedian, " +
                "fmin(distinct intBoxed,boolPrimitive) as cmin, " +
                "stddev(distinct intBoxed,boolPrimitive) as cstddev, " +
                "sum(distinct intBoxed,boolPrimitive) as csum " +
                "from SupportBean#length(3)";
        stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        tryAssertionDistinct(epService, listener);

        // test SODA
        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        stmt = (EPStatementSPI) epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(epl, stmt.getText());

        tryAssertionDistinct(epService, listener);

        stmt.destroy();
    }

    private void tryAssertionDistinct(EPServiceProvider epService, SupportUpdateListener listener) {

        String[] fields = "cavedev,cavg,cmax,cmedian,cmin,cstddev,csum".split(",");
        epService.getEPRuntime().sendEvent(makeBean(100, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0d, 100d, 100, 100d, 100, null, 100});

        epService.getEPRuntime().sendEvent(makeBean(100, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0d, 100d, 100, 100d, 100, null, 100});

        epService.getEPRuntime().sendEvent(makeBean(200, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{50d, 150d, 200, 150d, 100, 70.71067811865476, 300});

        epService.getEPRuntime().sendEvent(makeBean(200, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{50d, 150d, 200, 150d, 100, 70.71067811865476, 300});

        epService.getEPRuntime().sendEvent(makeBean(200, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0d, 200d, 200, 200d, 200, null, 200});
    }

    private void runAssertionFirstLastEver(EPServiceProvider epService) {
        tryAssertionFirstLastEver(epService, true);
        tryAssertionFirstLastEver(epService, false);
    }

    private void tryAssertionFirstLastEver(EPServiceProvider epService, boolean soda) {
        String[] fields = "c1,c2,c3".split(",");
        String epl = "select " +
                "firstever(intBoxed,boolPrimitive) as c1, " +
                "lastever(intBoxed,boolPrimitive) as c2, " +
                "countever(*,boolPrimitive) as c3 " +
                "from SupportBean#length(3)";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBean(100, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 0L});

        epService.getEPRuntime().sendEvent(makeBean(100, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100, 100, 1L});

        epService.getEPRuntime().sendEvent(makeBean(200, true));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100, 200, 2L});

        epService.getEPRuntime().sendEvent(makeBean(201, false));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100, 200, 2L});

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select count(*, intPrimitive) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'count(*,intPrimitive)': Invalid filter expression parameter to the aggregation function 'count' is expected to return a boolean value but returns java.lang.Integer [select count(*, intPrimitive) from SupportBean]");

        tryInvalid(epService, "select fmin(intPrimitive) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'min(intPrimitive)': MIN-filtered aggregation function must have a filter expression as a second parameter [select fmin(intPrimitive) from SupportBean]");
    }

    private SupportBean makeBean(float floatPrimitive, double doublePrimitive, long longPrimitive, short shortPrimitive, boolean boolPrimitive) {
        SupportBean sb = new SupportBean();
        sb.setFloatPrimitive(floatPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setShortPrimitive(shortPrimitive);
        sb.setBoolPrimitive(boolPrimitive);
        return sb;
    }

    private SupportBean makeBean(Integer intBoxed, boolean boolPrimitive) {
        SupportBean sb = new SupportBean();
        sb.setIntBoxed(intBoxed);
        sb.setBoolPrimitive(boolPrimitive);
        return sb;
    }

    public static class BlackWhiteEvent {
        private boolean black;

        public BlackWhiteEvent(boolean black) {
            this.black = black;
        }

        public boolean isBlack() {
            return black;
        }
    }
}
