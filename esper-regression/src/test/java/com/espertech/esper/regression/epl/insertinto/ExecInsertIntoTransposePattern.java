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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecInsertIntoTransposePattern implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionThisAsColumn(epService);
        runAssertionTransposePOJOEventPattern(epService);
        runAssertionTransposeMapEventPattern(epService);
    }

    private void runAssertionThisAsColumn(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        EPStatement stmt = epService.getEPAdministrator().createEPL("create window OneWindow#time(1 day) as select theString as alertId, this from SupportBean");
        epService.getEPAdministrator().createEPL("insert into OneWindow select '1' as alertId, stream0.quote.this as this " +
                " from pattern [every quote=SupportBean(theString='A')] as stream0");
        epService.getEPAdministrator().createEPL("insert into OneWindow select '2' as alertId, stream0.quote as this " +
                " from pattern [every quote=SupportBean(theString='B')] as stream0");

        epService.getEPRuntime().sendEvent(new SupportBean("A", 10));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"alertId", "this.intPrimitive"}, new Object[][]{{"1", 10}});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"alertId", "this.intPrimitive"}, new Object[][]{{"1", 10}, {"2", 20}});

        stmt = epService.getEPAdministrator().createEPL("create window TwoWindow#time(1 day) as select theString as alertId, * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into TwoWindow select '3' as alertId, quote.* " +
                " from pattern [every quote=SupportBean(theString='C')] as stream0");

        epService.getEPRuntime().sendEvent(new SupportBean("C", 30));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"alertId", "intPrimitive"}, new Object[][]{{"3", 30}});

        stmt.destroy();
    }

    private void runAssertionTransposePOJOEventPattern(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("AEventBean", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BEventBean", SupportBean_B.class);

        String stmtTextOne = "insert into MyStreamABBean select a, b from pattern [a=AEventBean -> b=BEventBean]";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select a.id, b.id from MyStreamABBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.id,b.id".split(","), new Object[]{"A1", "B1"});

        stmt.destroy();
    }

    private void runAssertionTransposeMapEventPattern(EPServiceProvider epService) {
        Map<String, Object> type = makeMap(new Object[][]{{"id", String.class}});

        epService.getEPAdministrator().getConfiguration().addEventType("AEventMap", type);
        epService.getEPAdministrator().getConfiguration().addEventType("BEventMap", type);

        String stmtTextOne = "insert into MyStreamABMap select a, b from pattern [a=AEventMap -> b=BEventMap]";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtTextOne);
        SupportUpdateListener listenerInsertInto = new SupportUpdateListener();
        stmtOne.addListener(listenerInsertInto);
        assertEquals(Map.class, stmtOne.getEventType().getPropertyType("a"));
        assertEquals(Map.class, stmtOne.getEventType().getPropertyType("b"));

        String stmtTextTwo = "select a.id, b.id from MyStreamABMap";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);
        assertEquals(String.class, stmtTwo.getEventType().getPropertyType("a.id"));
        assertEquals(String.class, stmtTwo.getEventType().getPropertyType("b.id"));

        Map<String, Object> eventOne = makeMap(new Object[][]{{"id", "A1"}});
        Map<String, Object> eventTwo = makeMap(new Object[][]{{"id", "B1"}});

        epService.getEPRuntime().sendEvent(eventOne, "AEventMap");
        epService.getEPRuntime().sendEvent(eventTwo, "BEventMap");

        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, "a.id,b.id".split(","), new Object[]{"A1", "B1"});

        theEvent = listenerInsertInto.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, "a,b".split(","), new Object[]{eventOne, eventTwo});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        for (Object[] entry : entries) {
            result.put(entry[0], entry[1]);
        }
        return result;
    }
}
