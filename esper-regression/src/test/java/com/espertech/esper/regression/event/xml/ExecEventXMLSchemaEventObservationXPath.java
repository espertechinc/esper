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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

import static com.espertech.esper.regression.event.xml.ExecEventXMLSchemaEventObservationDOM.CLASSLOADER_SCHEMA_URI;
import static com.espertech.esper.regression.event.xml.ExecEventXMLSchemaEventObservationDOM.OBSERVATION_XML;
import static org.junit.Assert.assertEquals;

public class ExecEventXMLSchemaEventObservationXPath implements RegressionExecution {
    private String schemaUri;

    public void configure(Configuration configuration) throws Exception {
        schemaUri = ExecEventXMLSchemaEventObservationXPath.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();

        ConfigurationEventTypeXMLDOM sensorcfg = new ConfigurationEventTypeXMLDOM();
        sensorcfg.setRootElementName("Sensor");
        sensorcfg.addXPathProperty("countTags", "count(/ss:Sensor/ss:Observation/ss:Tag)", XPathConstants.NUMBER);
        sensorcfg.addXPathProperty("countTagsInt", "count(/ss:Sensor/ss:Observation/ss:Tag)", XPathConstants.NUMBER, "int");
        sensorcfg.addNamespacePrefix("ss", "SensorSchema");
        sensorcfg.addXPathProperty("idarray", "//ss:Tag/ss:ID", XPathConstants.NODESET, "String[]");
        sensorcfg.addXPathPropertyFragment("tagArray", "//ss:Tag", XPathConstants.NODESET, "TagEvent");
        sensorcfg.addXPathPropertyFragment("tagOne", "//ss:Tag[position() = 1]", XPathConstants.NODE, "TagEvent");
        sensorcfg.setSchemaResource(schemaUri);
        configuration.getEngineDefaults().getViewResources().setIterableUnbound(true);
        configuration.addEventType("SensorEvent", sensorcfg);
    }

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeXMLDOM tagcfg = new ConfigurationEventTypeXMLDOM();
        tagcfg.setRootElementName("//Tag");
        tagcfg.setSchemaResource(schemaUri);
        epService.getEPAdministrator().getConfiguration().addEventType("TagEvent", tagcfg);

        EPStatement stmtExampleOne = epService.getEPAdministrator().createEPL("select countTags, countTagsInt, idarray, tagArray, tagOne from SensorEvent");
        EPStatement stmtExampleTwo_0 = epService.getEPAdministrator().createEPL("insert into TagOneStream select tagOne.* from SensorEvent");
        EPStatement stmtExampleTwo_1 = epService.getEPAdministrator().createEPL("select ID from TagOneStream");
        EPStatement stmtExampleTwo_2 = epService.getEPAdministrator().createEPL("insert into TagArrayStream select tagArray as mytags from SensorEvent");
        EPStatement stmtExampleTwo_3 = epService.getEPAdministrator().createEPL("select mytags[1].ID from TagArrayStream");

        Document doc = SupportXML.getDocument(OBSERVATION_XML);
        epService.getEPRuntime().sendEvent(doc);

        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleOne.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleTwo_0.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleTwo_1.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleTwo_2.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtExampleTwo_3.iterator().next());

        Object resultArray = stmtExampleOne.iterator().next().get("idarray");
        EPAssertionUtil.assertEqualsExactOrder((Object[]) resultArray, new String[]{"urn:epc:1:2.24.400", "urn:epc:1:2.24.401"});
        EPAssertionUtil.assertProps(stmtExampleOne.iterator().next(), "countTags,countTagsInt".split(","), new Object[]{2d, 2});
        assertEquals("urn:epc:1:2.24.400", stmtExampleTwo_1.iterator().next().get("ID"));
        assertEquals("urn:epc:1:2.24.401", stmtExampleTwo_3.iterator().next().get("mytags[1].ID"));
    }
}
