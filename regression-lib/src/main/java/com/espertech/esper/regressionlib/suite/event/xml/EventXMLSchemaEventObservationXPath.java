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
package com.espertech.esper.regressionlib.suite.event.xml;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.suite.event.xml.EventXMLSchemaEventObservationDOM.OBSERVATION_XML;
import static org.junit.Assert.assertEquals;

public class EventXMLSchemaEventObservationXPath {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaEventObservationXPathPreconfig());
        execs.add(new EventXMLSchemaEventObservationXPathCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaEventObservationXPathPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "SensorEventWithXPath", new RegressionPath());
        }
    }

    public static class EventXMLSchemaEventObservationXPathCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSensorEvent = Thread.currentThread().getContextClassLoader().getResource("regression/sensorSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='Sensor', schemaResource='" + schemaUriSensorEvent + "')" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='SensorSchema')" +
                "@XMLSchemaField(name='countTags', xpath='count(/ss:Sensor/ss:Observation/ss:Tag)', type='number')" +
                "@XMLSchemaField(name='countTagsInt', xpath='count(/ss:Sensor/ss:Observation/ss:Tag)', type='number', castToType='int')" +
                "@XMLSchemaField(name='idarray', xpath='//ss:Tag/ss:ID', type='NODESET', castToType='String[]')" +
                "@XMLSchemaField(name='tagArray', xpath='//ss:Tag', type='NODESET', eventTypeName='TagEvent')" +
                "@XMLSchemaField(name='tagOne', xpath='//ss:Tag[position() = 1]', type='node', eventTypeName='TagEvent')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        env.compileDeploy("@name('s0') select countTags, countTagsInt, idarray, tagArray, tagOne from " + eventTypeName, path);
        env.compileDeploy("@name('e0') insert into TagOneStream select tagOne.* from " + eventTypeName, path);
        env.compileDeploy("@name('e1') select ID from TagOneStream", path);
        env.compileDeploy("@name('e2') insert into TagArrayStream select tagArray as mytags from " + eventTypeName, path);
        env.compileDeploy("@name('e3') select mytags[1].ID from TagArrayStream", path);

        Document doc = SupportXML.getDocument(OBSERVATION_XML);
        env.sendEventXMLDOM(doc, eventTypeName);

        SupportEventTypeAssertionUtil.assertConsistency(env.iterator("s0").next());
        SupportEventTypeAssertionUtil.assertConsistency(env.iterator("e0").next());
        SupportEventTypeAssertionUtil.assertConsistency(env.iterator("e1").next());
        SupportEventTypeAssertionUtil.assertConsistency(env.iterator("e2").next());
        SupportEventTypeAssertionUtil.assertConsistency(env.iterator("e3").next());

        Object resultArray = env.iterator("s0").next().get("idarray");
        EPAssertionUtil.assertEqualsExactOrder((Object[]) resultArray, new String[]{"urn:epc:1:2.24.400", "urn:epc:1:2.24.401"});
        EPAssertionUtil.assertProps(env.iterator("s0").next(), "countTags,countTagsInt".split(","), new Object[]{2d, 2});
        assertEquals("urn:epc:1:2.24.400", env.iterator("e1").next().get("ID"));
        assertEquals("urn:epc:1:2.24.401", env.iterator("e3").next().get("mytags[1].ID"));

        env.undeployAll();
    }
}
