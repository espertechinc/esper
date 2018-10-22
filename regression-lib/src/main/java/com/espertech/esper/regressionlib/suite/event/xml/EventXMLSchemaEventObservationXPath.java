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

import static com.espertech.esper.regressionlib.suite.event.xml.EventXMLSchemaEventObservationDOM.OBSERVATION_XML;
import static org.junit.Assert.assertEquals;

public class EventXMLSchemaEventObservationXPath implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('s0') select countTags, countTagsInt, idarray, tagArray, tagOne from SensorEventWithXPath", path);
        env.compileDeploy("@name('e0') insert into TagOneStream select tagOne.* from SensorEventWithXPath", path);
        env.compileDeploy("@name('e1') select ID from TagOneStream", path);
        env.compileDeploy("@name('e2') insert into TagArrayStream select tagArray as mytags from SensorEventWithXPath", path);
        env.compileDeploy("@name('e3') select mytags[1].ID from TagArrayStream", path);

        Document doc = SupportXML.getDocument(OBSERVATION_XML);
        env.sendEventXMLDOM(doc, "SensorEventWithXPath");

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
