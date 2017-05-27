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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportBeanDupProperty;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ExecEventBeanPropertyResolutionCaseInsensitive implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setClassPropertyResolutionStyle(Configuration.PropertyResolutionStyle.CASE_INSENSITIVE);
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select MYPROPERTY, myproperty, myProperty, MyProperty from " + SupportBeanDupProperty.class.getName());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanDupProperty("lowercamel", "uppercamel", "upper", "lower"));
        EventBean result = listener.assertOneGetNewAndReset();
        assertEquals("upper", result.get("MYPROPERTY"));
        assertEquals("lower", result.get("myproperty"));
        assertTrue(result.get("myProperty").equals("lowercamel") || result.get("myProperty").equals("uppercamel")); // JDK6 versus JDK7 JavaBean inspector
        assertEquals("upper", result.get("MyProperty"));

        stmt = epService.getEPAdministrator().createEPL("select " +
                "NESTED.NESTEDVALUE as val1, " +
                "ARRAYPROPERTY[0] as val2, " +
                "MAPPED('keyOne') as val3, " +
                "INDEXED[0] as val4 " +
                " from " + SupportBeanComplexProps.class.getName());
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("nestedValue", theEvent.get("val1"));
        assertEquals(10, theEvent.get("val2"));
        assertEquals("valueOne", theEvent.get("val3"));
        assertEquals(1, theEvent.get("val4"));
    }
}
