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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.*;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowProcessingOrder {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            execs.add(new InfraDispatchBackQueue(rep));
        }
        execs.add(new InfraOrderedDeleteAndSelect());
        return execs;
    }

    private static class InfraDispatchBackQueue implements RegressionExecution {
        private final EventRepresentationChoice eventRepresentationEnum;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public InfraDispatchBackQueue(EventRepresentationChoice eventRepresentationEnum) {
            this.eventRepresentationEnum = eventRepresentationEnum;
        }

        public void run(RegressionEnvironment env) {
            String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedStartValueEvent.class) + " create schema StartValueEvent as (dummy string);\n";
            epl += eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTestForwardEvent.class) + " create schema TestForwardEvent as (prop1 string);\n";
            epl += eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTestInputEvent.class) + " create schema TestInputEvent as (dummy string);\n";
            epl += "insert into TestForwardEvent select'V1' as prop1 from TestInputEvent;\n";
            epl += eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedNamedWin.class) + " create window NamedWin#unique(prop1) (prop1 string, prop2 string);\n";
            epl += "insert into NamedWin select 'V1' as prop1, 'O1' as prop2 from StartValueEvent;\n";
            epl += "on TestForwardEvent update NamedWin as work set prop2 = 'U1' where work.prop1 = 'V1';\n";
            epl += "@name('select') select irstream prop1, prop2 from NamedWin;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("select");

            String[] fields = "prop1,prop2".split(",");
            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{"dummyValue"}, "StartValueEvent");
            } else if (eventRepresentationEnum.isMapEvent()) {
                env.sendEventMap(new HashMap<String, Object>(), "StartValueEvent");
            } else if (eventRepresentationEnum.isAvroEvent()) {
                env.eventService().sendEventAvro(new GenericData.Record(record("soemthing").fields().endRecord()), "StartValueEvent");
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                env.eventService().sendEventJson("{}", "StartValueEvent");
            } else {
                fail();
            }

            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"V1", "O1"});

            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{"dummyValue"}, "TestInputEvent");
            } else if (eventRepresentationEnum.isMapEvent()) {
                env.sendEventMap(new HashMap<String, Object>(), "TestInputEvent");
            } else if (eventRepresentationEnum.isAvroEvent()) {
                env.eventService().sendEventAvro(new GenericData.Record(record("soemthing").fields().endRecord()), "TestInputEvent");
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                env.eventService().sendEventJson("{}", "TestInputEvent");
            } else {
                fail();
            }

            EPAssertionUtil.assertProps(env.listener("select").getLastOldData()[0], fields, new Object[]{"V1", "O1"});
            EPAssertionUtil.assertProps(env.listener("select").getAndResetLastNewData()[0], fields, new Object[]{"V1", "U1"});
            env.undeployAll();
        }
    }

    private static class InfraOrderedDeleteAndSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindow#lastevent as select * from SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on MyWindow e delete from MyWindow win where win.theString=e.theString and e.intPrimitive = 7;\n" +
                "on MyWindow e delete from MyWindow win where win.theString=e.theString and e.intPrimitive = 5;\n" +
                "on MyWindow e insert into ResultStream select e.* from MyWindow;\n" +
                "@name('s0') select * from ResultStream;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 7));
            assertFalse("E1", env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 8));
            assertEquals("E2", env.listener("s0").assertOneGetNewAndReset().get("theString"));

            env.sendEventBean(new SupportBean("E3", 5));
            assertFalse("E3", env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E4", 6));
            assertEquals("E4", env.listener("s0").assertOneGetNewAndReset().get("theString"));

            env.undeployAll();
        }
    }

    public static class MyLocalJsonProvidedStartValueEvent implements Serializable {
        public String dummy;
    }

    public static class MyLocalJsonProvidedTestForwardEvent implements Serializable {
        public String prop1;
    }

    public static class MyLocalJsonProvidedTestInputEvent implements Serializable {
        public String dummy;
    }

    public static class MyLocalJsonProvidedNamedWin implements Serializable {
        public String prop1;
        public String prop2;
    }
}
