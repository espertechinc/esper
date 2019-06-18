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
package com.espertech.esper.regressionlib.suite.event.json;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class EventJsonCreateSchema {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonCreateSchemaSpecialName());
        execs.add(new EventJsonCreateSchemaInvalid());
        return execs;
    }

    private static class EventJsonCreateSchemaSpecialName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy(
                "@public @buseventtype create json schema JsonEvent(`p q` string, ABC string, abc string, AbC string);\n" +
                    "@name('s0') select * from JsonEvent#keepall").addListener("s0");

            env.sendEventJson(new JsonObject().add("p q", "v1").add("ABC", "v2").add("abc", "v3").add("AbC", "v4").toString(), "JsonEvent");
            assertEvent(env.listener("s0").assertOneGetNewAndReset());

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertEvent(it.next());

            env.undeployAll();
        }

        private void assertEvent(EventBean event) {
            EPAssertionUtil.assertProps(event, "p q,ABC,abc,AbC".split(","), new Object[] {"v1", "v2", "v3", "v4"});
        }
    }

    private static class EventJsonCreateSchemaInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "create objectarray schema InnerEvent();\n create json schema JsonEvent(innervent InnerEvent);\n",
                    "Failed to validate event type 'InnerEvent', expected a Json or Map event type");

            tryInvalidCompile(env, "create json schema InvalidDecl(int fieldname)",
                "Nestable type configuration encountered an unexpected property type name 'fieldname' for property 'int', expected java.lang.Class or java.util.Map or the name of a previously-declared event type");

            tryInvalidCompile(env, "create json schema InvalidDecl(comparable java.lang.Comparable)",
                "Unsupported type 'java.lang.Comparable' for property 'comparable' (use @JsonSchemaField to declare additional information)");
        }
    }
}
