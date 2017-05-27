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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanDupProperty;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static org.junit.Assert.*;

public class ExecEventBeanPropertyResolutionCaseDistinctInsensitive implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setClassPropertyResolutionStyle(Configuration.PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE);
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select MYPROPERTY, myproperty, myProperty from " + SupportBeanDupProperty.class.getName());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanDupProperty("lowercamel", "uppercamel", "upper", "lower"));
        EventBean result = listener.assertOneGetNewAndReset();
        assertEquals("upper", result.get("MYPROPERTY"));
        assertEquals("lower", result.get("myproperty"));
        assertTrue(result.get("myProperty").equals("lowercamel") || result.get("myProperty").equals("uppercamel")); // JDK6 versus JDK7 JavaBean inspector

        try {
            epService.getEPAdministrator().createEPL("select MyProperty from " + SupportBeanDupProperty.class.getName());
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Unexpected exception starting statement: Unable to determine which property to use for \"MyProperty\" because more than one property matched [");
            // expected
        }
    }
}
