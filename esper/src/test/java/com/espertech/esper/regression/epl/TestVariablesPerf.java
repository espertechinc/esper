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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.core.service.EPRuntimeSPI;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.util.*;

public class TestVariablesPerf extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testConstantPerformance() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("create const variable String MYCONST = 'E331'");

        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i * -1));
        }

        // test join
        EPStatement stmtJoin = epService.getEPAdministrator().createEPL("select * from SupportBean_S0 s0 unidirectional, MyWindow sb where theString = MYCONST");
        stmtJoin.addListener(listener);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "E" + i));
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sb.intPrimitive".split(","), new Object[]{"E331", -331});
        }
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 500);
        stmtJoin.destroy();

        // test subquery
        EPStatement stmtSubquery = epService.getEPAdministrator().createEPL("select * from SupportBean_S0 where exists (select * from MyWindow where theString = MYCONST)");
        stmtSubquery.addListener(listener);
        
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, "E" + i));
            assertTrue(listener.getAndClearIsInvoked());
        }
        delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 500);
    }
}
