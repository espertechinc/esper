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
package com.espertech.esper.regression.event.xml;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEventXMLSchemaEventObservationDOM implements RegressionExecution {
    protected final static String CLASSLOADER_SCHEMA_URI = "regression/sensorSchema.xsd";
    protected final static String OBSERVATION_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Sensor xmlns=\"SensorSchema\" >\n" +
            "\t<ID>urn:epc:1:4.16.36</ID>\n" +
            "\t<Observation Command=\"READ_PALLET_TAGS_ONLY\">\n" +
            "\t\t<ID>00000001</ID>\n" +
            "\t\t<Tag>\n" +
            "\t\t\t<ID>urn:epc:1:2.24.400</ID>\n" +
            "\t\t</Tag>\n" +
            "\t\t<Tag>\n" +
            "\t\t\t<ID>urn:epc:1:2.24.401</ID>\n" +
            "\t\t</Tag>\n" +
            "\t</Observation>\n" +
            "</Sensor>";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM typecfg = new ConfigurationEventTypeXMLDOM();
        typecfg.setRootElementName("Sensor");
        String schemaUri = ExecEventXMLSchemaEventObservationDOM.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        typecfg.setSchemaResource(schemaUri);
        configuration.getEngineDefaults().getViewResources().setIterableUnbound(true);
        configuration.addEventType("SensorEvent", typecfg);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtExampleOneText = "select ID, Observation.Command, Observation.ID,\n" +
                "Observation.Tag[0].ID, Observation.Tag[1].ID\n" +
                "from SensorEvent";
        EPStatement stmtExampleOne = epService.getEPAdministrator().createEPL(stmtExampleOneText);

        EPStatement stmtExampleTwo_0 = epService.getEPAdministrator().createEPL("insert into ObservationStream\n" +
                "select ID, Observation from SensorEvent");
        EPStatement stmtExampleTwo_1 = epService.getEPAdministrator().createEPL("select Observation.Command, Observation.Tag[0].ID from ObservationStream");

        EPStatement stmtExampleThree_0 = epService.getEPAdministrator().createEPL("insert into TagListStream\n" +
                "select ID as sensorId, Observation.* from SensorEvent");
        EPStatement stmtExampleThree_1 = epService.getEPAdministrator().createEPL("select sensorId, Command, Tag[0].ID from TagListStream");

        Document doc = SupportXML.getDocument(OBSERVATION_XML);
        EventSender sender = epService.getEPRuntime().getEventSender("SensorEvent");
        sender.sendEvent(doc);

        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleOne.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleTwo_0.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleTwo_1.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleThree_0.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleThree_1.iterator().next());

        EPAssertionUtil.assertProps(stmtExampleTwo_1.iterator().next(), "Observation.Command,Observation.Tag[0].ID".split(","), new Object[]{"READ_PALLET_TAGS_ONLY", "urn:epc:1:2.24.400"});
        EPAssertionUtil.assertProps(stmtExampleThree_1.iterator().next(), "sensorId,Command,Tag[0].ID".split(","), new Object[]{"urn:epc:1:4.16.36", "READ_PALLET_TAGS_ONLY", "urn:epc:1:2.24.400"});

        try {
            epService.getEPAdministrator().createEPL("select Observation.Tag.ID from SensorEvent");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'Observation.Tag.ID': Failed to resolve property 'Observation.Tag.ID' to a stream or nested property in a stream [select Observation.Tag.ID from SensorEvent]", ex.getMessage());
        }
    }

}
