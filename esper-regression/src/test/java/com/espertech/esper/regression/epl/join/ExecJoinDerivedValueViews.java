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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;

public class ExecJoinDerivedValueViews implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select\n" +
                "Math.signum(stream1.slope) as s1,\n" +
                "Math.signum(stream2.slope) as s2\n" +
                "from\n" +
                "SupportBean#length_batch(3)#linest(intPrimitive, longPrimitive) as stream1,\n" +
                "SupportBean#length_batch(2)#linest(intPrimitive, longPrimitive) as stream2").addListener(listener);
        epService.getEPRuntime().sendEvent(makeEvent("E3", 1, 100));
        epService.getEPRuntime().sendEvent(makeEvent("E4", 1, 100));
        assertFalse(listener.isInvoked());
    }

    private SupportBean makeEvent(String id, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(id, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }
}
