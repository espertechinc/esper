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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEventObjectArrayNestedMap implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        EventAdapterService eventAdapterService = ((EPServiceProviderSPI) epService).getEventAdapterService();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EventType supportBeanType = epService.getEPAdministrator().getConfiguration().getEventType("SupportBean");

        Map<String, Object> lev2def = new HashMap<String, Object>();
        lev2def.put("sb", "SupportBean");
        Map<String, Object> lev1def = new HashMap<String, Object>();
        lev1def.put("lev1name", lev2def);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapNestedObjectArray", new String[]{"lev0name"}, new Object[]{lev1def});
        assertEquals(Object[].class, epService.getEPAdministrator().getConfiguration().getEventType("MyMapNestedObjectArray").getUnderlyingType());

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select lev0name.lev1name.sb.theString as val from MyMapNestedObjectArray").addListener(listener);

        Map<String, Object> lev2data = new HashMap<String, Object>();
        lev2data.put("sb", eventAdapterService.adapterForTypedBean(new SupportBean("E1", 0), supportBeanType));
        Map<String, Object> lev1data = new HashMap<String, Object>();
        lev1data.put("lev1name", lev2data);

        epService.getEPRuntime().sendEvent(new Object[]{lev1data}, "MyMapNestedObjectArray");
        assertEquals("E1", listener.assertOneGetNewAndReset().get("val"));

        try {
            epService.getEPRuntime().sendEvent(new HashMap(), "MyMapNestedObjectArray");
            fail();
        } catch (EPException ex) {
            assertEquals("Event type named 'MyMapNestedObjectArray' has not been defined or is not a Map event type, the name 'MyMapNestedObjectArray' refers to a java.lang.Object(Array) event type", ex.getMessage());
        }
    }
}
