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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.*;
import static org.junit.Assert.fail;

public class ExecEPLUpdateMapIndexProps implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSetMapPropsBean(epService);

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionUpdateIStreamSetMapProps(epService, rep);
        }

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionNamedWindowSetMapProps(epService, rep);
        }
    }

    private void runAssertionSetMapPropsBean(EPServiceProvider epService) {
        // test update-istream with bean
        epService.getEPAdministrator().getConfiguration().addEventType(MyMapPropEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        epService.getEPAdministrator().createEPL("insert into MyStream select * from MyMapPropEvent");

        EPStatement stmtUpdOne = epService.getEPAdministrator().createEPL("update istream MyStream set props('abc') = 1, array[2] = 10");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtUpdOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new MyMapPropEvent());
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), "props('abc'),array[2]".split(","), new Object[]{1, 10}, new Object[]{null, null});
    }

    private void runAssertionUpdateIStreamSetMapProps(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) throws Exception {

        // test update-istream with map
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyInfraType(simple String, myarray int[], mymap java.util.Map)");
        EPStatement stmtUpdTwo = epService.getEPAdministrator().createEPL("update istream MyInfraType set simple='A', mymap('abc') = 1, myarray[2] = 10");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtUpdTwo.addListener(listener);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{null, new int[10], new HashMap<String, Object>()}, "MyInfraType");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(makeMapEvent(new HashMap<>(), new int[10]), "MyInfraType");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "MyInfraType"));
            event.put("myarray", Arrays.asList(0, 0, 0, 0, 0));
            event.put("mymap", new HashMap());
            epService.getEPRuntime().sendEventAvro(event, "MyInfraType");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), "simple,mymap('abc'),myarray[2]".split(","), new Object[]{"A", 1, 10}, new Object[]{null, null, 0});

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyInfraType".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void runAssertionNamedWindowSetMapProps(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) throws Exception {

        // test named-window update
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyNWInfraType(simple String, myarray int[], mymap java.util.Map)");
        EPStatement stmtWin = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as MyNWInfraType");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " insert into MyWindow select * from MyNWInfraType");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{null, new int[10], new HashMap<String, Object>()}, "MyNWInfraType");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(makeMapEvent(new HashMap<>(), new int[10]), "MyNWInfraType");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "MyNWInfraType"));
            event.put("myarray", Arrays.asList(0, 0, 0, 0, 0));
            event.put("mymap", new HashMap());
            epService.getEPRuntime().sendEventAvro(event, "MyNWInfraType");
        } else {
            fail();
        }
        epService.getEPAdministrator().createEPL("on SupportBean update MyWindow set simple='A', mymap('abc') = intPrimitive, myarray[2] = intPrimitive");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertPropsPerRow(stmtWin.iterator(), "simple,mymap('abc'),myarray[2]".split(","), new Object[][]{{"A", 10, 10}});

        // test null and array too small
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{null, new int[2], null}, "MyNWInfraType");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(makeMapEvent(null, new int[2]), "MyNWInfraType");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(record("name").fields()
                    .optionalString("simple")
                    .name("myarray").type(array().items().longType()).noDefault()
                    .name("mymap").type(map().values().stringType()).noDefault()
                    .endRecord());
            event.put("myarray", Arrays.asList(0, 0));
            event.put("mymap", null);
            epService.getEPRuntime().sendEventAvro(event, "MyNWInfraType");
        } else {
            fail();
        }
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtWin.iterator(), "simple,mymap('abc'),myarray[2]".split(","), new Object[][]{{"A", 20, 20}, {"A", null, null}});

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyNWInfraType,MyWindow".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private Map<String, Object> makeMapEvent(Map<String, Object> mymap, int[] myarray) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("mymap", mymap);
        map.put("myarray", myarray);
        return map;
    }

    public static class MyMapPropEvent implements Serializable {
        private Map props = new HashMap();
        private Object[] array = new Object[10];

        public void setProps(String name, Object value) {
            props.put(name, value);
        }

        public void setArray(int index, Object value) {
            array[index] = value;
        }

        public Map getProps() {
            return props;
        }

        public void setProps(Map props) {
            this.props = props;
        }

        public Object[] getArray() {
            return array;
        }

        public void setArray(Object[] array) {
            this.array = array;
        }

        public Object getArray(int index) {
            return array[index];
        }
    }
}
