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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

public class ExecRowRecogEmptyPartition implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String[] fields = "value".split(",");
        String text = "select * from MyEvent#length(10) " +
                "match_recognize (" +
                "  partition by value" +
                "  measures E1.value as value" +
                "  pattern (E1 E2 | E2 E1 ) " +
                "  define " +
                "    E1 as E1.theString = 'A', " +
                "    E2 as E2.theString = 'B' " +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("B", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("B", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("B", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4});

        epService.getEPRuntime().sendEvent(new SupportRecogBean("A", 6));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B", 7));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B", 8));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A", 7));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{7});

        /**
         * Comment-in for testing partition removal.
         */
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportRecogBean("A", i));
            //System.out.println(i);
            //epService.getEPRuntime().sendEvent(new SupportRecogBean("B", i));
            //EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {i});
        }
    }
}
