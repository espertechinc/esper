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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientExtendUDFReturnTypeIsEvents implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        AtomicInteger milestone = new AtomicInteger();

        tryAssertionReturnTypeIsEvents(env, "myItemProducerEventBeanArray", milestone);
        tryAssertionReturnTypeIsEvents(env, "myItemProducerEventBeanCollection", milestone);
        tryAssertionReturnTypeIsEventsInvalid(env);
    }

    private static void tryAssertionReturnTypeIsEvents(RegressionEnvironment env, String methodName, AtomicInteger milestone) {

        RegressionPath path = new RegressionPath();
        EPCompiled compiled = env.compileWBusPublicType("create schema MyItem(id string)");
        env.deploy(compiled);
        path.add(compiled);

        env.compileDeploy("@name('s0') select " + methodName + "(theString).where(v => v.id in ('id1', 'id3')) as c0 from SupportBean", path);
        env.addListener("s0");

        env.sendEventBean(new SupportBean("id0,id1,id2,id3,id4", 0));
        Collection<Map> coll = (Collection<Map>) env.listener("s0").assertOneGetNewAndReset().get("c0");
        EPAssertionUtil.assertPropsPerRow(coll.toArray(new Map[coll.size()]), "id".split(","), new Object[][]{{"id1"}, {"id3"}});

        env.undeployAll();
    }

    private static void tryAssertionReturnTypeIsEventsInvalid(RegressionEnvironment env) {

        env.compileDeploy("select myItemProducerInvalidNoType(theString) as c0 from SupportBean");
        SupportMessageAssertUtil.tryInvalidCompile(env, "select myItemProducerInvalidNoType(theString).where(v => v.id='id1') as c0 from SupportBean",
            "Failed to validate select-clause expression 'myItemProducerInvalidNoType(theStri...(68 chars)': Method 'myItemProducerEventBeanArray' returns EventBean-array but does not provide the event type name [");

        // test invalid: event type name invalid
        SupportMessageAssertUtil.tryInvalidCompile(env, "select myItemProducerInvalidWrongType(theString).where(v => v.id='id1') as c0 from SupportBean",
            "Failed to validate select-clause expression 'myItemProducerInvalidWrongType(theS...(74 chars)': Method 'myItemProducerEventBeanArray' returns event type 'dummy' and the event type cannot be found [select myItemProducerInvalidWrongType(theString).where(v => v.id='id1') as c0 from SupportBean]");

        env.undeployAll();
    }

    public static EventBean[] myItemProducerEventBeanArray(String string, EPLMethodInvocationContext context) {
        String[] split = string.split(",");
        EventBean[] events = new EventBean[split.length];
        for (int i = 0; i < split.length; i++) {
            events[i] = context.getEventBeanService().adapterForMap(Collections.singletonMap("id", split[i]), "MyItem");
        }
        return events;
    }

    public static Collection<EventBean> myItemProducerEventBeanCollection(String string, EPLMethodInvocationContext context) {
        return Arrays.asList(myItemProducerEventBeanArray(string, context));
    }
}
