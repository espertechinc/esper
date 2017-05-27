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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriberMRD;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecEPLForGroupDelivery implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        runAssertionInvalid(epService);
        runAssertionSubscriberOnly(epService);
        runAssertionDiscreteDelivery(epService);
        runAssertionGroupDelivery(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select * from SupportBean for ",
                "Incorrect syntax near end-of-input expecting an identifier but found end-of-input at line 1 column 30 [select * from SupportBean for ]");

        tryInvalid(epService, "select * from SupportBean for other_keyword",
                "Error starting statement: Expected any of the [grouped_delivery, discrete_delivery] for-clause keywords after reserved keyword 'for' [select * from SupportBean for other_keyword]");

        tryInvalid(epService, "select * from SupportBean for grouped_delivery",
                "Error starting statement: The for-clause with the grouped_delivery keyword requires one or more grouping expressions [select * from SupportBean for grouped_delivery]");

        tryInvalid(epService, "select * from SupportBean for grouped_delivery()",
                "Error starting statement: The for-clause with the grouped_delivery keyword requires one or more grouping expressions [select * from SupportBean for grouped_delivery()]");

        tryInvalid(epService, "select * from SupportBean for grouped_delivery(dummy)",
                "Error starting statement: Failed to validate for-clause expression 'dummy': Property named 'dummy' is not valid in any stream [select * from SupportBean for grouped_delivery(dummy)]");

        tryInvalid(epService, "select * from SupportBean for discrete_delivery(dummy)",
                "Error starting statement: The for-clause with the discrete_delivery keyword does not allow grouping expressions [select * from SupportBean for discrete_delivery(dummy)]");

        tryInvalid(epService, "select * from SupportBean for discrete_delivery for grouped_delivery(intPrimitive)",
                "Incorrect syntax near 'for' (a reserved keyword) at line 1 column 48 ");
    }

    private void runAssertionSubscriberOnly(EPServiceProvider epService) {
        SupportSubscriberMRD subscriber = new SupportSubscriberMRD();
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream theString,intPrimitive from SupportBean#time_batch(1) for discrete_delivery");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendTimer(epService, 1000);
        assertEquals(3, subscriber.getInsertStreamList().size());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", 1}, subscriber.getInsertStreamList().get(0)[0]);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E2", 2}, subscriber.getInsertStreamList().get(1)[0]);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E3", 1}, subscriber.getInsertStreamList().get(2)[0]);

        stmt.destroy();
        subscriber.reset();
        stmt = epService.getEPAdministrator().createEPL("select irstream theString,intPrimitive from SupportBean#time_batch(1) for grouped_delivery(intPrimitive)");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendTimer(epService, 2000);
        assertEquals(2, subscriber.getInsertStreamList().size());
        assertEquals(2, subscriber.getRemoveStreamList().size());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", 1}, subscriber.getInsertStreamList().get(0)[0]);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E3", 1}, subscriber.getInsertStreamList().get(0)[1]);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E2", 2}, subscriber.getInsertStreamList().get(1)[0]);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", 1}, subscriber.getRemoveStreamList().get(0)[0]);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E3", 1}, subscriber.getRemoveStreamList().get(0)[1]);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E2", 2}, subscriber.getRemoveStreamList().get(1)[0]);

        stmt.destroy();
    }

    private void runAssertionDiscreteDelivery(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(1) for discrete_delivery");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendTimer(epService, 1000);
        assertEquals(3, listener.getNewDataList().size());
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(0), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(1), "theString,intPrimitive".split(","), new Object[][]{{"E2", 2}});
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(2), "theString,intPrimitive".split(","), new Object[][]{{"E3", 1}});
        listener.reset();

        // test no-event delivery
        epService.getEPAdministrator().getConfiguration().addEventType("ObjectEvent", Object.class);
        String epl = "SELECT *  FROM ObjectEvent OUTPUT ALL EVERY 1 seconds for discrete_delivery";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        epService.getEPRuntime().sendEvent(new Object());
        sendTimer(epService, 2000);
        assertTrue(listener.getAndClearIsInvoked());
        sendTimer(epService, 3000);
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionGroupDelivery(EPServiceProvider epService) {
        sendTimer(epService, 0);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(1) for grouped_delivery (intPrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendTimer(epService, 1000);
        assertEquals(2, listener.getNewDataList().size());
        assertEquals(2, listener.getNewDataList().get(0).length);
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(0), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}, {"E3", 1}});
        assertEquals(1, listener.getNewDataList().get(1).length);
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(1), "theString,intPrimitive".split(","), new Object[][]{{"E2", 2}});

        // test sorted
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(1) order by intPrimitive desc for grouped_delivery (intPrimitive)");
        stmt.addListener(listener);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        sendTimer(epService, 2000);
        assertEquals(2, listener.getNewDataList().size());
        assertEquals(1, listener.getNewDataList().get(0).length);
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(0), "theString,intPrimitive".split(","), new Object[][]{{"E2", 2}});
        assertEquals(2, listener.getNewDataList().get(1).length);
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(1), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}, {"E3", 1}});

        // test multiple criteria
        stmt.destroy();
        String stmtText = "select theString, doubleBoxed, enumValue from SupportBean#time_batch(1) order by theString, doubleBoxed, enumValue for grouped_delivery(doubleBoxed, enumValue)";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        listener.reset();

        sendEvent(epService, "E1", 10d, SupportEnum.ENUM_VALUE_2); // A (1)
        sendEvent(epService, "E2", 11d, SupportEnum.ENUM_VALUE_1); // B (2)
        sendEvent(epService, "E3", 9d, SupportEnum.ENUM_VALUE_2);  // C (3)
        sendEvent(epService, "E4", 10d, SupportEnum.ENUM_VALUE_2); // A
        sendEvent(epService, "E5", 10d, SupportEnum.ENUM_VALUE_1); // D (4)
        sendEvent(epService, "E6", 10d, SupportEnum.ENUM_VALUE_1); // D
        sendEvent(epService, "E7", 11d, SupportEnum.ENUM_VALUE_1); // B
        sendEvent(epService, "E8", 10d, SupportEnum.ENUM_VALUE_1); // D
        sendTimer(epService, 3000);
        assertEquals(4, listener.getNewDataList().size());
        String[] fields = "theString,doubleBoxed,enumValue".split(",");
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(0), fields,
                new Object[][]{{"E1", 10d, SupportEnum.ENUM_VALUE_2}, {"E4", 10d, SupportEnum.ENUM_VALUE_2}});
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(1), fields,
                new Object[][]{{"E2", 11d, SupportEnum.ENUM_VALUE_1}, {"E7", 11d, SupportEnum.ENUM_VALUE_1}});
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(2), fields,
                new Object[][]{{"E3", 9d, SupportEnum.ENUM_VALUE_2}});
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(3), fields,
                new Object[][]{{"E5", 10d, SupportEnum.ENUM_VALUE_1}, {"E6", 10d, SupportEnum.ENUM_VALUE_1}, {"E8", 10d, SupportEnum.ENUM_VALUE_1}});

        // test SODA
        stmt.destroy();
        listener.reset();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        sendEvent(epService, "E1", 10d, SupportEnum.ENUM_VALUE_2); // A (1)
        sendEvent(epService, "E2", 11d, SupportEnum.ENUM_VALUE_1); // B (2)
        sendEvent(epService, "E3", 11d, SupportEnum.ENUM_VALUE_1); // B (2)
        sendTimer(epService, 4000);
        assertEquals(2, listener.getNewDataList().size());
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(0), fields,
                new Object[][]{{"E1", 10d, SupportEnum.ENUM_VALUE_2}});
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataList().get(1), fields,
                new Object[][]{{"E2", 11d, SupportEnum.ENUM_VALUE_1}, {"E3", 11d, SupportEnum.ENUM_VALUE_1}});

        stmt.destroy();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void sendEvent(EPServiceProvider epService, String theString, Double doubleBoxed, SupportEnum enumVal) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setEnumValue(enumVal);
        epService.getEPRuntime().sendEvent(bean);
    }
}