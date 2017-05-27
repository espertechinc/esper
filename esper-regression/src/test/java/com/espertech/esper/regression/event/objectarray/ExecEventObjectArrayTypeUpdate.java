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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;
import java.util.Map;

import static com.espertech.esper.regression.event.map.ExecEventMap.makeMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEventObjectArrayTypeUpdate implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.OBJECTARRAY);
        String[] names = {"base1", "base2"};
        Object[] types = {String.class, makeMap(new Object[][]{{"n1", int.class}})};
        configuration.addEventType("MyOAEvent", names, types);
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement statementOne = epService.getEPAdministrator().createEPL(
                "select base1 as v1, base2.n1 as v2, base3? as v3, base2.n2? as v4 from MyOAEvent");
        assertEquals(Object[].class, statementOne.getEventType().getUnderlyingType());
        EPStatement statementOneSelectAll = epService.getEPAdministrator().createEPL("select * from MyOAEvent");
        assertEquals("[base1, base2]", Arrays.toString(statementOneSelectAll.getEventType().getPropertyNames()));
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        statementOne.addListener(listenerOne);
        String[] fields = "v1,v2,v3,v4".split(",");

        epService.getEPRuntime().sendEvent(new Object[]{"abc", makeMap(new Object[][]{{"n1", 10}}), ""}, "MyOAEvent");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"abc", 10, null, null});

        // update type
        String[] namesNew = {"base3", "base2"};
        Object[] typesNew = new Object[]{Long.class, makeMap(new Object[][]{{"n2", String.class}})};
        epService.getEPAdministrator().getConfiguration().updateObjectArrayEventType("MyOAEvent", namesNew, typesNew);

        EPStatement statementTwo = epService.getEPAdministrator().createEPL("select base1 as v1, base2.n1 as v2, base3 as v3, base2.n2 as v4 from MyOAEvent");
        EPStatement statementTwoSelectAll = epService.getEPAdministrator().createEPL("select * from MyOAEvent");
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new Object[]{"def", makeMap(new Object[][]{{"n1", 9}, {"n2", "xyz"}}), 20L}, "MyOAEvent");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"def", 9, 20L, "xyz"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), fields, new Object[]{"def", 9, 20L, "xyz"});

        // assert event type
        assertEquals("[base1, base2, base3]", Arrays.toString(statementOneSelectAll.getEventType().getPropertyNames()));
        assertEquals("[base1, base2, base3]", Arrays.toString(statementTwoSelectAll.getEventType().getPropertyNames()));

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("base3", Long.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("base2", Map.class, null, false, false, false, true, false),
            new EventPropertyDescriptor("base1", String.class, null, false, false, false, false, false),
        }, statementTwoSelectAll.getEventType().getPropertyDescriptors());

        try {
            epService.getEPAdministrator().getConfiguration().updateObjectArrayEventType("dummy", new String[0], new Object[0]);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Error updating Object-array event type: Event type named 'dummy' has not been declared", ex.getMessage());
        }

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        try {
            epService.getEPAdministrator().getConfiguration().updateObjectArrayEventType("SupportBean", new String[0], new Object[0]);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Error updating Object-array event type: Event type by name 'SupportBean' is not an Object-array event type", ex.getMessage());
        }
    }
}
