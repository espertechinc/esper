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
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportXML;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventXMLNoSchemaEventTransposeDOM implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        env.compileDeploy("@name('insert') insert into MyNestedStream select nested1 from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("nested1", String.class, null, false, false, false, false, false),
        }, env.statement("insert").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("insert").getEventType());

        env.compileDeploy("@name('s0') select * from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[0], env.statement("s0").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());

        SupportXML.sendDefaultEvent(env.eventService(), "test", "TestXMLSchemaType");
        EventBean stmtInsertWildcardBean = env.iterator("insert").next();
        EventBean stmtSelectWildcardBean = env.iterator("s0").next();
        assertNotNull(stmtInsertWildcardBean.get("nested1"));
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectWildcardBean);
        SupportEventTypeAssertionUtil.assertConsistency(env.iterator("insert").next());

        assertEquals(0, stmtSelectWildcardBean.getEventType().getPropertyNames().length);

        env.undeployAll();
    }
}
