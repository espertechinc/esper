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
package com.espertech.esper.regression.event.variant;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanVariantStream;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecEventVariantStreamAny implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationVariantStream variant = new ConfigurationVariantStream();
        variant.setTypeVariance(ConfigurationVariantStream.TypeVariance.ANY);
        configuration.addVariantStream("MyVariantStream", variant);
        assertTrue(configuration.isVariantStreamExists("MyVariantStream"));
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMetadata(epService);
        runAssertionAnyType(epService);
        runAssertionAnyTypeStaggered(epService);
    }

    private void runAssertionMetadata(EPServiceProvider epService) {

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getValueAddEventService().getValueAddProcessor("MyVariantStream").getValueAddEventType();
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("MyVariantStream", type.getMetadata().getPrimaryName());
        assertEquals("MyVariantStream", type.getMetadata().getPublicName());
        assertEquals("MyVariantStream", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.VARIANT, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfiguredStatic());

        EventType[] valueAddTypes = ((EPServiceProviderSPI) epService).getValueAddEventService().getValueAddedTypes();
        assertEquals(1, valueAddTypes.length);
        assertSame(type, valueAddTypes[0]);

        assertEquals(0, type.getPropertyNames().length);
        assertEquals(0, type.getPropertyDescriptors().length);
    }

    private void runAssertionAnyType(EPServiceProvider epService) {
        assertTrue(epService.getEPAdministrator().getConfiguration().isVariantStreamExists("MyVariantStream"));
        epService.getEPAdministrator().createEPL("insert into MyVariantStream select * from " + SupportBean.class.getName());
        epService.getEPAdministrator().createEPL("insert into MyVariantStream select * from " + SupportBeanVariantStream.class.getName());
        epService.getEPAdministrator().createEPL("insert into MyVariantStream select * from " + SupportBean_A.class.getName());
        epService.getEPAdministrator().createEPL("insert into MyVariantStream select symbol as theString, volume as intPrimitive, feed as id from " + SupportMarketDataBean.class.getName());

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyVariantStream");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertEquals(0, stmt.getEventType().getPropertyNames().length);

        Object eventOne = new SupportBean("E0", -1);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, listener.assertOneGetNewAndReset().getUnderlying());

        Object eventTwo = new SupportBean_A("E1");
        epService.getEPRuntime().sendEvent(eventTwo);
        assertSame(eventTwo, listener.assertOneGetNewAndReset().getUnderlying());

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select theString,id,intPrimitive from MyVariantStream");
        stmt.addListener(listener);
        assertEquals(Object.class, stmt.getEventType().getPropertyType("theString"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("id"));
        assertEquals(Object.class, stmt.getEventType().getPropertyType("intPrimitive"));

        String[] fields = "theString,id,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBeanVariantStream("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", null, 10});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, "E3", null});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("s1", 100, 1000L, "f1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"s1", "f1", 1000L});
        epService.getEPAdministrator().destroyAllStatements();

        // Test inserting a wrapper of underlying plus properties
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create variant schema TheVariantStream as *");
        epService.getEPAdministrator().createEPL("insert into TheVariantStream select 'test' as eventConfigId, * from SupportBean");
        epService.getEPAdministrator().createEPL("select * from TheVariantStream").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EventBean event = listener.assertOneGetNewAndReset();
        assertEquals("test", event.get("eventConfigId"));
        assertEquals(1, event.get("intPrimitive"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAnyTypeStaggered(EPServiceProvider epService) {
        // test insert into staggered with map
        ConfigurationVariantStream configVariantStream = new ConfigurationVariantStream();
        configVariantStream.setTypeVariance(ConfigurationVariantStream.TypeVariance.ANY);
        epService.getEPAdministrator().getConfiguration().addVariantStream("VarStream", configVariantStream);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportMarketDataBean", SupportMarketDataBean.class);

        epService.getEPAdministrator().createEPL("insert into MyStream select theString, intPrimitive from SupportBean");
        epService.getEPAdministrator().createEPL("insert into VarStream select theString as abc from MyStream");
        epService.getEPAdministrator().createEPL("@Name('Target') select * from VarStream#keepall");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        EventBean[] arr = EPAssertionUtil.iteratorToArray(epService.getEPAdministrator().getStatement("Target").iterator());
        EPAssertionUtil.assertPropsPerRow(arr, new String[]{"abc"}, new Object[][]{{"E1"}});

        epService.getEPAdministrator().createEPL("insert into MyStream2 select feed from SupportMarketDataBean");
        epService.getEPAdministrator().createEPL("insert into VarStream select feed as abc from MyStream2");

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 1, 1L, "E2"));

        arr = EPAssertionUtil.iteratorToArray(epService.getEPAdministrator().getStatement("Target").iterator());
        EPAssertionUtil.assertPropsPerRow(arr, new String[]{"abc"}, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPAdministrator().destroyAllStatements();
    }
}
