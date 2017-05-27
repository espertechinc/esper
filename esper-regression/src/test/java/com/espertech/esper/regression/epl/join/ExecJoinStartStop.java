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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecJoinStartStop implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionJoinUniquePerId(epService);
        runAssertionInvalidJoin(epService);
    }

    private void runAssertionJoinUniquePerId(EPServiceProvider epService) {
        String joinStatement = "select * from " +
                SupportMarketDataBean.class.getName() + "(symbol='IBM')#length(3) s0, " +
                SupportMarketDataBean.class.getName() + "(symbol='CSCO')#length(3) s1" +
                " where s0.volume=s1.volume";

        Object[] setOne = new Object[5];
        Object[] setTwo = new Object[5];
        long[] volumesOne = new long[]{10, 20, 20, 40, 50};
        long[] volumesTwo = new long[]{10, 20, 30, 40, 50};
        for (int i = 0; i < setOne.length; i++) {
            setOne[i] = new SupportMarketDataBean("IBM", volumesOne[i], (long) i, "");
            setTwo[i] = new SupportMarketDataBean("CSCO", volumesTwo[i], (long) i, "");
        }

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement, "MyJoin");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, setOne[0]);
        sendEvent(epService, setTwo[0]);
        assertNotNull(listener.getLastNewData());
        listener.reset();

        stmt.stop();
        sendEvent(epService, setOne[1]);
        sendEvent(epService, setTwo[1]);
        assertFalse(listener.isInvoked());

        stmt.start();
        sendEvent(epService, setOne[2]);
        assertFalse(listener.isInvoked());

        stmt.stop();
        sendEvent(epService, setOne[3]);
        sendEvent(epService, setOne[4]);
        sendEvent(epService, setTwo[3]);

        stmt.start();
        sendEvent(epService, setTwo[4]);
        assertFalse(listener.isInvoked());

        // assert type-statement reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportMarketDataBean.class.getName()));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportMarketDataBean.class.getName());
        assertTrue(stmtNames.contains("MyJoin"));

        stmt.destroy();

        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportMarketDataBean.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportMarketDataBean.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(null, stmtNames.toArray());
        assertFalse(stmtNames.contains("MyJoin"));
    }

    private void runAssertionInvalidJoin(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class);

        String invalidJoin = "select * from A, B";
        tryInvalid(epService, invalidJoin,
                "Error starting statement: Joins require that at least one view is specified for each stream, no view was specified for A [select * from A, B]");

        invalidJoin = "select * from A#time(5 min), B";
        tryInvalid(epService, invalidJoin,
                "Error starting statement: Joins require that at least one view is specified for each stream, no view was specified for B [select * from A#time(5 min), B]");

        invalidJoin = "select * from A#time(5 min), pattern[A->B]";
        tryInvalid(epService, invalidJoin,
                "Error starting statement: Joins require that at least one view is specified for each stream, no view was specified for pattern event stream [select * from A#time(5 min), pattern[A->B]]");
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecJoinStartStop.class);
}
