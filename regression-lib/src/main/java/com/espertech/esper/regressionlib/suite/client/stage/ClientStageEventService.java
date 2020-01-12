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
package com.espertech.esper.regressionlib.suite.client.stage;

import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import com.espertech.esper.runtime.client.EPEventService;
import com.espertech.esper.runtime.client.stage.EPStage;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.getStatement;
import static org.junit.Assert.assertNotNull;

public class ClientStageEventService {

    public final static String XML_TYPENAME = "ClientStageEventServiceXML";
    public final static String MAP_TYPENAME = "ClientStageEventServiceMap";
    public final static String OA_TYPENAME = "ClientStageEventServiceOA";
    public final static String AVRO_TYPENAME = "ClientStageEventServiceAvro";
    public final static String JSON_TYPENAME = "ClientStageEventServiceJson";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientStageEventServiceEventSend());
        return execs;
    }

    private static class ClientStageEventServiceEventSend implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStage stage = env.stageService().getStage("ST");
            RegressionPath path = new RegressionPath();

            // Bean
            runAssertion(env, path, stage, "SupportBean", new SupportBean(), svc -> svc.sendEventBean(new SupportBean(), "SupportBean"));

            // Map
            runAssertion(env, path, stage, MAP_TYPENAME, new HashMap(), svc -> svc.sendEventMap(new HashMap<>(), MAP_TYPENAME));

            // Object-Array
            runAssertion(env, path, stage, OA_TYPENAME, new Object[0], svc -> svc.sendEventObjectArray(new Object[0], OA_TYPENAME));

            // XML
            Node node = SupportXML.getDocument("<myevent/>").getDocumentElement();
            runAssertion(env, path, stage, XML_TYPENAME, node, svc -> svc.sendEventXMLDOM(node, XML_TYPENAME));

            // Avro
            Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
            GenericData.Record record = new GenericData.Record(schema);
            runAssertion(env, path, stage, AVRO_TYPENAME, record, svc -> svc.sendEventAvro(record, AVRO_TYPENAME));

            // Json
            runAssertion(env, path, stage, JSON_TYPENAME, "{}", svc -> svc.sendEventJson("{}", JSON_TYPENAME));

            env.undeployAll();
        }
    }

    private static <T> void runAssertion(RegressionEnvironment env,
                                         RegressionPath path,
                                         EPStage stage,
                                         String typename,
                                         Object underlying,
                                         Consumer<EPEventService> sender) {

        String epl = "@public @buseventtype create schema TriggerEvent();\n" +
            "@public @buseventtype @name('schema') create json schema " + JSON_TYPENAME + "();\n" +
            "@name('trigger') select * from TriggerEvent;\n" +
            "@name('s0') select * from " + typename + ";\n";
        env.compileDeploy(epl, path).addListener("s0");
        String deploymentId = env.deploymentId("s0");
        stageIt(env, "ST", deploymentId);

        // test normal send
        sender.accept(stage.getEventService());
        assertUnderlying(env, typename, underlying);

        // test EventSender#send
        EventSender eventSender = stage.getEventService().getEventSender(typename);
        eventSender.sendEvent(underlying);
        assertUnderlying(env, typename, underlying);

        // test EventSender#route
        getStatement("trigger", "ST", env.runtime()).addListener((newData, oldData, stmt, runtime) -> {
            eventSender.routeEvent(underlying);
        });
        stage.getEventService().sendEventMap(new HashMap<>(), "TriggerEvent");
        assertUnderlying(env, typename, underlying);

        unstageIt(env, "ST", deploymentId);

        path.clear();
        env.undeployModuleContaining("s0");
    }

    private static void assertUnderlying(RegressionEnvironment env, String typename, Object underlying) {
        assertNotNull(env.listenerStage("ST", "s0").assertOneGetNewAndReset().getUnderlying());
    }
}
