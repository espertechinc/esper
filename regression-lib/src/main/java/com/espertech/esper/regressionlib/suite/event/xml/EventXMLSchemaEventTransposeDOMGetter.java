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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EventXMLSchemaEventTransposeDOMGetter {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaEventTransposeDOMGetterPreconfig());
        execs.add(new EventXMLSchemaEventTransposeDOMGetterCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaEventTransposeDOMGetterPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "SimpleEventWSchema", new RegressionPath());
        }
    }

    public static class EventXMLSchemaEventTransposeDOMGetterCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        env.compileDeploy("@name('s0') insert into MyNestedStream select nested1 from " + eventTypeName + "#lastevent", path);
        env.assertStatement("s0", statement -> {
            SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("nested1", Node.class).fragment());
            SupportEventTypeAssertionUtil.assertConsistency(statement.getEventType());
        });

        env.compileDeploy("@name('s1') select nested1.attr1 as attr1, nested1.prop1 as prop1, nested1.prop2 as prop2, nested1.nested2.prop3 as prop3, nested1.nested2.prop3[0] as prop3_0, nested1.nested2 as nested2 from MyNestedStream#lastevent", path);
        env.assertStatement("s1", statement -> {
            SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("prop1", String.class),
                new SupportEventPropDesc("prop2", Boolean.class),
                new SupportEventPropDesc("attr1", String.class),
                new SupportEventPropDesc("prop3", Integer[].class).indexed(),
                new SupportEventPropDesc("prop3_0", Integer.class),
                new SupportEventPropDesc("nested2", Node.class).fragment());
            SupportEventTypeAssertionUtil.assertConsistency(statement.getEventType());
        });

        env.compileDeploy("@name('sw') select * from MyNestedStream", path);
        env.assertStatement("sw", statement -> {
            SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("nested1", Node.class).fragment());
            SupportEventTypeAssertionUtil.assertConsistency(statement.getEventType());
        });

        env.compileDeploy("@name('iw') insert into MyNestedStreamTwo select nested1.* from " + eventTypeName + "#lastevent", path);
        env.assertStatement("iw", statement -> {
            SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("prop1", String.class),
                new SupportEventPropDesc("prop2", Boolean.class),
                new SupportEventPropDesc("attr1", String.class),
                new SupportEventPropDesc("nested2", Node.class).fragment());
            SupportEventTypeAssertionUtil.assertConsistency(statement.getEventType());
        });

        Document doc = SupportXML.makeDefaultEvent("test");
        env.sendEventXMLDOM(doc, eventTypeName);

        env.assertIterator("iw", iterator -> {
            EventBean stmtInsertWildcardBean = iterator.next();
            SupportEventTypeAssertionUtil.assertConsistency(stmtInsertWildcardBean);
            EPAssertionUtil.assertProps(stmtInsertWildcardBean, "prop1,prop2,attr1".split(","),
                new Object[]{"SAMPLE_V1", true, "SAMPLE_ATTR1"});

            EventBean fragmentNested2 = (EventBean) stmtInsertWildcardBean.getFragment("nested2");
            assertEquals(4, fragmentNested2.get("prop3[1]"));
            assertEquals(eventTypeName + ".nested1.nested2", fragmentNested2.getEventType().getName());
        });

        env.assertIterator("s0", iterator -> {
            EventBean stmtInsertBean = iterator.next();
            SupportEventTypeAssertionUtil.assertConsistency(stmtInsertBean);
            EventBean fragmentNested1 = (EventBean) stmtInsertBean.getFragment("nested1");
            assertEquals(5, fragmentNested1.get("nested2.prop3[2]"));
            assertEquals(eventTypeName + ".nested1", fragmentNested1.getEventType().getName());
        });
        env.assertIterator("sw", iterator -> SupportEventTypeAssertionUtil.assertConsistency(iterator.next()));

        env.undeployAll();
    }
}
