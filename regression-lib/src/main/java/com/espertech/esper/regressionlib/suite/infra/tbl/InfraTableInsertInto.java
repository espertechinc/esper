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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.*;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableInsertInto {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraInsertIntoAndDelete());
        execs.add(new InfraInsertIntoSameModuleUnkeyed());
        execs.add(new InfraInsertIntoTwoModulesUnkeyed());
        execs.add(new InfraInsertIntoSelfAccess());
        execs.add(new InfraNamedWindowMergeInsertIntoTable());
        execs.add(new InfraInsertIntoWildcard());
        execs.add(new InfraInsertIntoFromNamedWindow());
        execs.add(new InfraInsertIntoSameModuleKeyed());
        execs.add(new InfraSplitStream());
        return execs;
    }

    public static class InfraInsertIntoAndDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "pkey0,pkey1,c0".split(",");
            RegressionPath path = new RegressionPath();

            String eplCreateTable = "@Name('S0') create table MyTable(c0 long, pkey1 int primary key, pkey0 string primary key)";
            env.compileDeploy(eplCreateTable, path);

            String eplIntoTable = "@Name('Insert-Into-Table') insert into MyTable select intPrimitive as pkey1, longPrimitive as c0, theString as pkey0 from SupportBean";
            env.compileDeploy(eplIntoTable, path);

            String eplDeleteTable = "@Name('Delete-Table') on SupportBean_S0 delete from MyTable where pkey1 = id and pkey0 = p00";
            env.compileDeploy(eplDeleteTable, path);

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("S0"), fields, new Object[0][]);

            sendSupportBean(env, "E1", 10, 100); // insert E1

            env.milestone(2);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("S0"), fields, new Object[][]{{"E1", 10, 100L}});
            env.sendEventBean(new SupportBean_S0(10, "E1")); // delete E1

            env.milestone(3);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("S0"), fields, new Object[0][]);
            sendSupportBean(env, "E1", 11, 101); // insert E1 again

            env.milestone(4);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("S0"), fields, new Object[][]{{"E1", 11, 101L}});

            sendSupportBean(env, "E2", 20, 200); // insert E2

            env.milestone(5);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("S0"), fields, new Object[][]{{"E1", 11, 101L}, {"E2", 20, 200L}});
            env.sendEventBean(new SupportBean_S0(20, "E2")); // delete E2

            env.milestone(6);

            env.sendEventBean(new SupportBean_S0(11, "E1")); // delete E1

            env.milestone(7);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("S0"), fields, new Object[0][]);

            sendSupportBean(env, "E1", 12, 102); // insert E1
            sendSupportBean(env, "E2", 21, 201); // insert E2
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("S0"), fields, new Object[][]{{"E1", 12, 102L}, {"E2", 21, 201L}});

            env.undeployAll();
        }
    }

    private static class InfraInsertIntoSameModuleUnkeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            String epl = "@name('create') create table MyTableSM(theString string);\n" +
                "@name('tbl-insert') insert into MyTableSM select theString from SupportBean;\n";
            env.compileDeploy(epl);

            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[0][]);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1"}});

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static class InfraInsertIntoSelfAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create table MyTableIISA(pkey string primary key)", path);
            env.compileDeploy("insert into MyTableIISA select theString as pkey from SupportBean where MyTableIISA[theString] is null", path);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey".split(","), new Object[][]{{"E1"}});

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey".split(","), new Object[][]{{"E1"}});

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey".split(","), new Object[][]{{"E1"}});

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey".split(","), new Object[][]{{"E1"}, {"E2"}});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey".split(","), new Object[][]{{"E1"}, {"E2"}});
            env.sendEventBean(new SupportBean("E2", 0));

            env.undeployAll();
        }
    }

    private static class InfraNamedWindowMergeInsertIntoTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create table MyTableNWM(pkey string)", path);
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);
            env.compileDeploy("on SupportBean as sb merge MyWindow when not matched " +
                "then insert into MyTableNWM select sb.theString as pkey", path);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), "pkey".split(","), new Object[][]{{"E1"}});

            env.undeployAll();
        }
    }

    private static class InfraSplitStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('createOne') create table MyTableOne(pkey string primary key, col int)", path);
            env.compileDeploy("@name('createTwo') create table MyTableTwo(pkey string primary key, col int)", path);

            String eplSplit = "@name('split') on SupportBean \n" +
                "  insert into MyTableOne select theString as pkey, intPrimitive as col where intPrimitive > 0\n" +
                "  insert into MyTableTwo select theString as pkey, intPrimitive as col where intPrimitive < 0\n" +
                "  insert into OtherStream select theString as pkey, intPrimitive as col where intPrimitive = 0\n";
            env.compileDeploy(eplSplit, path);

            env.compileDeploy("@name('s1') select * from OtherStream", path).addListener("s1");

            env.sendEventBean(new SupportBean("E1", 1));
            assertSplitStream(env, new Object[][]{{"E1", 1}}, new Object[0][]);

            env.sendEventBean(new SupportBean("E2", -2));
            assertSplitStream(env, new Object[][]{{"E1", 1}}, new Object[][]{{"E2", -2}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", -3));
            assertSplitStream(env, new Object[][]{{"E1", 1}}, new Object[][]{{"E2", -2}, {"E3", -3}});
            assertFalse(env.listener("s1").isInvoked());

            env.sendEventBean(new SupportBean("E4", 0));
            assertSplitStream(env, new Object[][]{{"E1", 1}}, new Object[][]{{"E2", -2}, {"E3", -3}});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "pkey,col".split(","), new Object[]{"E4", 0});

            env.undeployAll();
        }

        private static void assertSplitStream(RegressionEnvironment env, Object[][] tableOneRows, Object[][] tableTwoRows) {
            String[] fields = "pkey,col".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("createOne"), fields, tableOneRows);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("createTwo"), fields, tableTwoRows);
        }
    }

    private static class InfraInsertIntoFromNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#unique(theString) as SupportBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);
            env.compileDeploy("@name('create') create table MyTableIIF(pkey0 string primary key, pkey1 int primary key)", path);
            env.compileDeploy("on SupportBean_S1 insert into MyTableIIF select theString as pkey0, intPrimitive as pkey1 from MyWindow", path);
            String[] fields = "pkey0,pkey1".split(",");

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 10}});

            env.compileExecuteFAF("delete from MyTableIIF", path);

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean_S1(2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 10}, {"E2", 20}});

            env.undeployAll();
        }
    }

    private static class InfraInsertIntoTwoModulesUnkeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create table MyTableIIU(theString string)", path);
            env.compileDeploy("@name('tbl-insert') insert into MyTableIIU select theString from SupportBean", path);

            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[0][]);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1"}});

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1"}});

            try {
                env.sendEventBean(new SupportBean("E2", 0));
                fail();
            } catch (EPException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "java.lang.RuntimeException: Unexpected exception in statement 'tbl-insert': Unique index violation, table 'MyTableIIU' is a declared to hold a single un-keyed row");
            }

            env.undeployAll();
        }
    }

    private static class InfraInsertIntoSameModuleKeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "pkey,thesum".split(",");
            String epl = "@name('create') create table MyTableIIK(" +
                "pkey string primary key," +
                "thesum sum(int));\n";
            epl += "insert into MyTableIIK select theString as pkey from SupportBean;\n";
            epl += "into table MyTableIIK select sum(id) as thesum from SupportBean_S0 group by p00;\n";
            epl += "on SupportBean_S1 insert into MyTableIIK select p10 as pkey;\n";
            epl += "on SupportBean_S2 merge MyTableIIK where p20 = pkey when not matched then insert into MyTableIIK select p20 as pkey;\n";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", null}});

            env.sendEventBean(new SupportBean_S0(10, "E1"));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 10}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 10}, {"E2", null}});

            env.sendEventBean(new SupportBean_S0(20, "E2"));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(11, "E1"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

            // assert on-insert and on-merge
            env.sendEventBean(new SupportBean_S1(0, "E3"));
            env.sendEventBean(new SupportBean_S2(0, "E4"));

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(3, "E3"));
            env.sendEventBean(new SupportBean_S0(4, "E4"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 21}, {"E2", 20}, {"E3", 3}, {"E4", 4}});

            env.undeployAll();
        }
    }

    private static class InfraInsertIntoWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionWildcard(env, false, rep);
            }
        }
    }

    private static void tryAssertionWildcard(RegressionEnvironment env, boolean bean, EventRepresentationChoice rep) {
        RegressionPath path = new RegressionPath();

        EPCompiled schemaCompiled;
        if (bean) {
            schemaCompiled = env.compile("create schema MySchema as " + MyP0P1Event.class.getName(), options -> options.setBusModifierEventType(ctx -> EventTypeBusModifier.BUS).setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC));
        } else {
            schemaCompiled = env.compile(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMySchema.class) + "create schema MySchema (p0 string, p1 string)", options -> options.setBusModifierEventType(ctx -> EventTypeBusModifier.BUS).setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC));
        }
        path.add(schemaCompiled);
        env.deploy(schemaCompiled);

        env.compileDeploy("@name('create') create table TheTable (p0 string, p1 string)", path);
        env.compileDeploy("insert into TheTable select * from MySchema", path);

        if (bean) {
            env.sendEventBean(new MyP0P1Event("a", "b"), "MySchema");
        } else if (rep.isMapEvent()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("p0", "a");
            map.put("p1", "b");
            env.sendEventMap(map, "MySchema");
        } else if (rep.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{"a", "b"}, "MySchema");
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("MySchema")));
            theEvent.put("p0", "a");
            theEvent.put("p1", "b");
            env.eventService().sendEventAvro(theEvent, "MySchema");
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            env.eventService().sendEventJson(new JsonObject().add("p0", "a").add("p1", "b").toString(), "MySchema");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.iterator("create").next(), "p0,p1".split(","), new Object[]{"a", "b"});
        env.undeployAll();
    }

    private static void sendSupportBean(RegressionEnvironment env, String string, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    public static class MyP0P1Event {
        private final String p0;
        private final String p1;

        private MyP0P1Event(String p0, String p1) {
            this.p0 = p0;
            this.p1 = p1;
        }

        public String getP0() {
            return p0;
        }

        public String getP1() {
            return p1;
        }
    }

    public static class MyLocalJsonProvidedMySchema implements Serializable {
        public String p0;
        public String p1;
    }
}
