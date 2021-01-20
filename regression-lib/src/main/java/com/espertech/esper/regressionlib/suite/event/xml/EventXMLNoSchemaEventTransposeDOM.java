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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventXMLNoSchemaEventTransposeDOM {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaEventXMLPreconfig());
        execs.add(new EventXMLNoSchemaEventXMLCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaEventXMLPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "TestXMLJustRootElementType", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaEventXMLCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {

        env.compileDeploy("@name('insert') insert into MyNestedStream select nested1 from " + eventTypeName, path);
        env.assertStatement("insert", statement -> {
            SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("nested1", String.class));
            SupportEventTypeAssertionUtil.assertConsistency(statement.getEventType());
        });

        env.compileDeploy("@name('s0') select * from " + eventTypeName, path);
        env.assertStatement("s0", statement -> {
            EPAssertionUtil.assertEqualsAnyOrder(new Object[0], statement.getEventType().getPropertyDescriptors());
            SupportEventTypeAssertionUtil.assertConsistency(statement.getEventType());
        });

        Document doc = SupportXML.makeDefaultEvent("test");
        env.sendEventXMLDOM(doc, eventTypeName);

        env.assertIterator("insert", iterator -> {
            EventBean stmtInsertWildcardBean = iterator.next();
            assertNotNull(stmtInsertWildcardBean.get("nested1"));
            SupportEventTypeAssertionUtil.assertConsistency(stmtInsertWildcardBean);
        });
        env.assertIterator("s0", iterator -> {
            EventBean stmtSelectWildcardBean = iterator.next();
            SupportEventTypeAssertionUtil.assertConsistency(stmtSelectWildcardBean);
            assertEquals(0, stmtSelectWildcardBean.getEventType().getPropertyNames().length);
        });

        env.undeployAll();
    }
}
