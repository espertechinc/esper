/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.EventTypeAssertionUtil;
import junit.framework.TestCase;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

public class TestSchemaXMLForObservation extends TestCase
{
    private static String CLASSLOADER_SCHEMA_URI = "regression/sensorSchema.xsd";
    private EPServiceProvider epService;

    private final static String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    public void testObservationExamplePropertyExpression() throws Exception
    {
        ConfigurationEventTypeXMLDOM typecfg = new ConfigurationEventTypeXMLDOM();
        typecfg.setRootElementName("Sensor");
        String schemaUri = TestSchemaXMLForObservation.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        typecfg.setSchemaResource(schemaUri);

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getViewResources().setIterableUnbound(true);
        configuration.addEventType("SensorEvent", typecfg);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

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

        Document doc = SupportXML.getDocument(XML);
        EventSender sender = epService.getEPRuntime().getEventSender("SensorEvent");
        sender.sendEvent(doc);

        EventTypeAssertionUtil.assertConsistency(stmtExampleOne.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleTwo_0.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleTwo_1.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleThree_0.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleThree_1.iterator().next());

        EPAssertionUtil.assertProps(stmtExampleTwo_1.iterator().next(), "Observation.Command,Observation.Tag[0].ID".split(","), new Object[]{"READ_PALLET_TAGS_ONLY", "urn:epc:1:2.24.400"});
        EPAssertionUtil.assertProps(stmtExampleThree_1.iterator().next(), "sensorId,Command,Tag[0].ID".split(","), new Object[]{"urn:epc:1:4.16.36", "READ_PALLET_TAGS_ONLY", "urn:epc:1:2.24.400"});

        try
        {
            epService.getEPAdministrator().createEPL("select Observation.Tag.ID from SensorEvent");
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'Observation.Tag.ID': Failed to resolve property 'Observation.Tag.ID' to a stream or nested property in a stream [select Observation.Tag.ID from SensorEvent]", ex.getMessage());
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testObservationExampleXPathExpr() throws Exception
    {
        String schemaUri = TestSchemaXMLForObservation.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();

        ConfigurationEventTypeXMLDOM sensorcfg = new ConfigurationEventTypeXMLDOM();
        sensorcfg.setRootElementName("Sensor");
        sensorcfg.addXPathProperty("countTags", "count(/ss:Sensor/ss:Observation/ss:Tag)", XPathConstants.NUMBER);
        sensorcfg.addXPathProperty("countTagsInt", "count(/ss:Sensor/ss:Observation/ss:Tag)", XPathConstants.NUMBER, "int");
        sensorcfg.addNamespacePrefix("ss", "SensorSchema");
        sensorcfg.addXPathProperty("idarray", "//ss:Tag/ss:ID", XPathConstants.NODESET, "String[]");
        sensorcfg.addXPathPropertyFragment("tagArray", "//ss:Tag", XPathConstants.NODESET, "TagEvent");
        sensorcfg.addXPathPropertyFragment("tagOne", "//ss:Tag[position() = 1]", XPathConstants.NODE, "TagEvent");
        sensorcfg.setSchemaResource(schemaUri);

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getViewResources().setIterableUnbound(true);
        configuration.addEventType("SensorEvent", sensorcfg);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        ConfigurationEventTypeXMLDOM tagcfg = new ConfigurationEventTypeXMLDOM();
        tagcfg.setRootElementName("//Tag");
        tagcfg.setSchemaResource(schemaUri);
        epService.getEPAdministrator().getConfiguration().addEventType("TagEvent", tagcfg);

        EPStatement stmtExampleOne = epService.getEPAdministrator().createEPL("select countTags, countTagsInt, idarray, tagArray, tagOne from SensorEvent");
        EPStatement stmtExampleTwo_0 = epService.getEPAdministrator().createEPL("insert into TagOneStream select tagOne.* from SensorEvent");
        EPStatement stmtExampleTwo_1 = epService.getEPAdministrator().createEPL("select ID from TagOneStream");
        EPStatement stmtExampleTwo_2 = epService.getEPAdministrator().createEPL("insert into TagArrayStream select tagArray as mytags from SensorEvent");
        EPStatement stmtExampleTwo_3 = epService.getEPAdministrator().createEPL("select mytags[1].ID from TagArrayStream");

        Document doc = SupportXML.getDocument(XML);
        epService.getEPRuntime().sendEvent(doc);

        EventTypeAssertionUtil.assertConsistency(stmtExampleOne.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleTwo_0.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleTwo_1.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleTwo_2.iterator().next());
        EventTypeAssertionUtil.assertConsistency(stmtExampleTwo_3.iterator().next());

        Object resultArray = stmtExampleOne.iterator().next().get("idarray");
        EPAssertionUtil.assertEqualsExactOrder((Object[]) resultArray, new String[]{"urn:epc:1:2.24.400", "urn:epc:1:2.24.401"});
        EPAssertionUtil.assertProps(stmtExampleOne.iterator().next(), "countTags,countTagsInt".split(","), new Object[]{2d, 2});
        assertEquals("urn:epc:1:2.24.400", stmtExampleTwo_1.iterator().next().get("ID"));
        assertEquals("urn:epc:1:2.24.401", stmtExampleTwo_3.iterator().next().get("mytags[1].ID"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
}
