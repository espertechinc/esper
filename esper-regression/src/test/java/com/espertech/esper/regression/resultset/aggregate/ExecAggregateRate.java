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

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.TimerTask;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;

public class ExecAggregateRate implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        runAssertionRateDataNonWindowed(epService);
        runAssertionRateDataWindowed(epService);
    }

    // rate implementation does not require a data window (may have one)
    // advantage: not retaining events, only timestamp data points
    // disadvantage: output rate limiting without snapshot may be less accurate rate
    private void runAssertionRateDataNonWindowed(EPServiceProvider epService) {
        sendTimer(epService, 0);

        String epl = "select rate(10) as myrate from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        assertEquals(epl, model.toEPL());

        tryAssertion(epService, listener);

        tryInvalid(epService, "select rate() from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'rate(*)': The rate aggregation function minimally requires a numeric constant or expression as a parameter. [select rate() from SupportBean]");
        tryInvalid(epService, "select rate(true) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'rate(true)': The rate aggregation function requires a numeric constant or time period as the first parameter in the constant-value notation [select rate(true) from SupportBean]");

        stmt.destroy();
    }

    private void runAssertionRateDataWindowed(EPServiceProvider epService) {
        String[] fields = "myrate,myqtyrate".split(",");
        String epl = "select RATE(longPrimitive) as myrate, RATE(longPrimitive, intPrimitive) as myqtyrate from SupportBean#length(3)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, 1000, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEvent(epService, 1200, 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEvent(epService, 1300, 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEvent(epService, 1500, 14);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 500d, 14 * 1000 / 500d});

        sendEvent(epService, 2000, 11);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 800d, 25 * 1000 / 800d});

        tryInvalid(epService, "select rate(longPrimitive) as myrate from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'rate(longPrimitive)': The rate aggregation function in the timestamp-property notation requires data windows [select rate(longPrimitive) as myrate from SupportBean]");
        tryInvalid(epService, "select rate(current_timestamp) as myrate from SupportBean#time(20)",
                "Error starting statement: Failed to validate select-clause expression 'rate(current_timestamp())': The rate aggregation function does not allow the current engine timestamp as a parameter [select rate(current_timestamp) as myrate from SupportBean#time(20)]");
        tryInvalid(epService, "select rate(theString) as myrate from SupportBean#time(20)",
                "Error starting statement: Failed to validate select-clause expression 'rate(theString)': The rate aggregation function requires a property or expression returning a non-constant long-type value as the first parameter in the timestamp-property notation [select rate(theString) as myrate from SupportBean#time(20)]");

        stmt.destroy();
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "myrate".split(",");

        sendTimer(epService, 1000);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(epService, 1200);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(epService, 1600);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(epService, 1600);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(epService, 9000);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(epService, 9200);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(epService, 10999);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        sendTimer(epService, 11100);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0.7});

        sendTimer(epService, 11101);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0.8});

        sendTimer(epService, 11200);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0.8});

        sendTimer(epService, 11600);
        sendEvent(epService);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0.7});
    }

    /**
     * Comment-in for rate testing with threading
     */
    /*
    public void testRateThreaded() throws Exception {

        Configuration config = new Configuration();
        config.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        RateSendRunnable runnable = new RateSendRunnable(epService.getEPRuntime());
        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);

        //String viewExpr = "select RATE(longPrimitive) as myrate from SupportBean#time(10) output every 1 sec";
        String viewExpr = "select RATE(10) as myrate from SupportBean output snapshot every 1 sec";
        EPStatement stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                System.out.println(newEvents[0].get("myrate"));                
            }
        });

        long rateDelay = 133;   // <== change here
        ScheduledFuture<?> future = timer.scheduleAtFixedRate(runnable, 0, rateDelay, TimeUnit.MILLISECONDS);
        Thread.sleep(2 * 60 * 1000);
        future.cancel(true);
    }
    */
    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService, long longPrimitive, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setLongPrimitive(longPrimitive);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService) {
        SupportBean bean = new SupportBean();
        epService.getEPRuntime().sendEvent(bean);
    }

    public static class RateSendRunnable extends TimerTask {

        private final EPRuntime runtime;

        public RateSendRunnable(EPRuntime runtime) {
            this.runtime = runtime;
        }

        public void run() {
            SupportBean bean = new SupportBean();
            bean.setLongPrimitive(System.currentTimeMillis());
            runtime.sendEvent(bean);
        }
    }
}
