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
package com.espertech.esper.regressionrun.suite.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.client.stage.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;
import org.apache.avro.SchemaBuilder;

import java.util.Collections;

public class TestSuiteClientStage extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testClientStageSendEvent() {
        RegressionRunner.run(session, ClientStageSendEvent.executions());
    }

    public void testClientStageAdvanceTime() {
        RegressionRunner.run(session, ClientStageAdvanceTime.executions());
    }

    public void testClientStagePrecondition() {
        RegressionRunner.run(session, ClientStagePrecondition.executions());
    }

    public void testClientStageObjectResolution() {
        RegressionRunner.run(session, ClientStageObjectResolution.executions());
    }

    public void testClientStageDeploy() {
        RegressionRunner.run(session, ClientStageDeploy.executions());
    }

    public void testClientStageMgmt() {
        RegressionRunner.run(session, ClientStageMgmt.executions());
    }

    public void testClientStageEventService() {
        RegressionRunner.run(session, ClientStageEventService.executions());
    }

    public void testClientStageEPStageService() {
        RegressionRunner.run(session, ClientStageEPStageService.executions());
    }

    public void testClientStageEPStage() {
        RegressionRunner.run(session, ClientStageEPStage.executions());
    }

    private void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        ConfigurationCommonEventTypeXMLDOM xmlMeta = new ConfigurationCommonEventTypeXMLDOM();
        xmlMeta.setRootElementName("myevent");
        String eventInfraEventSenderSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        xmlMeta.setSchemaText(eventInfraEventSenderSchema);
        configuration.getCommon().addEventType(ClientStageEventService.XML_TYPENAME, xmlMeta);

        configuration.getCommon().addEventType(ClientStageEventService.MAP_TYPENAME, Collections.emptyMap());

        String[] names = {};
        Object[] types = {};
        configuration.getCommon().addEventType(ClientStageEventService.OA_TYPENAME, names, types);
        configuration.getCommon().addEventTypeAvro(ClientStageEventService.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(SchemaBuilder.record(ClientStageEventService.AVRO_TYPENAME).fields().endRecord()));
        configuration.getCommon().getEventMeta().getAvroSettings().setEnableAvro(true);
    }
}
