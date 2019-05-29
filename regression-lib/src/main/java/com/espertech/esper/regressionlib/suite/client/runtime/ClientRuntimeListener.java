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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import com.espertech.esper.runtime.client.EPEventService;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClientRuntimeListener {

    public static final String BEAN_TYPENAME = RoutedBeanEvent.class.getSimpleName();
    public static final String MAP_TYPENAME = ClientRuntimeListener.class.getName() + "_MAP";
    public static final String OA_TYPENAME = ClientRuntimeListener.class.getName() + "_OA";
    public static final String XML_TYPENAME = ClientRuntimeListener.class.getName() + "_XML";
    public static final String AVRO_TYPENAME = ClientRuntimeListener.class.getName() + "_AVRO";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeListenerRoute());
        return execs;
    }

    private static class ClientRuntimeListenerRoute implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('bean') select * from " + BEAN_TYPENAME + ";\n" +
                    "@name('map') select * from " + MAP_TYPENAME + ";\n" +
                    "@name('oa') select * from " + OA_TYPENAME + ";\n" +
                    "@name('xml') select * from " + XML_TYPENAME + ";\n" +
                    "@name('avro') select * from " + AVRO_TYPENAME + ";\n" +
                    "@public @buseventtype create json schema JsonEvent(ident string);\n" +
                    "@name('json') select * from JsonEvent;\n" +
                    "@name('trigger') select * from SupportBean;";
            env.compileDeploy(epl).addListener("map").addListener("oa").addListener("xml").addListener("avro").addListener("bean").addListener("json");

            env.statement("trigger").addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    EPEventService processEvent = runtime.getEventService();
                    String ident = (String) newEvents[0].get("theString");

                    processEvent.routeEventBean(new RoutedBeanEvent(ident), BEAN_TYPENAME);
                    processEvent.routeEventMap(Collections.singletonMap("ident", ident), MAP_TYPENAME);
                    processEvent.routeEventObjectArray(new Object[]{ident}, OA_TYPENAME);

                    String xml = "<myevent ident=\"XXXXXX\"></myevent>\n".replace("XXXXXX", ident);
                    processEvent.routeEventXMLDOM(SupportXML.getDocument(xml).getDocumentElement(), XML_TYPENAME);

                    Schema avroSchema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
                    GenericData.Record datum = new GenericData.Record(avroSchema);
                    datum.put("ident", ident);
                    processEvent.routeEventAvro(datum, AVRO_TYPENAME);

                    processEvent.routeEventJson(new JsonObject().add("ident", ident).toString(), "JsonEvent");
                }
            });

            env.sendEventBean(new SupportBean("xy", -1));
            for (String name : "map,bean,oa,xml,avro,json".split(",")) {
                SupportListener listener = env.listener(name);
                assertTrue("failed for " + name, listener.isInvoked());
                assertEquals("xy", env.listener(name).assertOneGetNewAndReset().get("ident"));
            }

            env.undeployAll();
        }
    }

    public static class RoutedBeanEvent {
        private final String ident;

        public RoutedBeanEvent(String ident) {
            this.ident = ident;
        }

        public String getIdent() {
            return ident;
        }
    }
}
