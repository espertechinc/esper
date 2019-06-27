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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.soda.Expressions;
import com.espertech.esper.common.client.soda.UpdateClause;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanCopyMethod;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriber;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.apache.avro.SchemaBuilder.*;
import static org.junit.Assert.*;

public class EPLOtherUpdateIStream {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherUpdateBean());
        execs.add(new EPLOtherUpdateFieldUpdateOrder());
        execs.add(new EPLOtherUpdateInvalid());
        execs.add(new EPLOtherUpdateInsertIntoWBeanWhere());
        execs.add(new EPLOtherUpdateInsertIntoWMapNoWhere());
        execs.add(new EPLOtherUpdateFieldsWithPriority());
        execs.add(new EPLOtherUpdateNamedWindow());
        execs.add(new EPLOtherUpdateTypeWidener());
        execs.add(new EPLOtherUpdateInsertDirectBeanTypeInheritance());
        execs.add(new EPLOtherUpdateSODA());
        execs.add(new EPLOtherUpdateXMLEvent());
        execs.add(new EPLOtherUpdateWrappedObject());
        execs.add(new EPLOtherUpdateSendRouteSenderPreprocess());
        execs.add(new EPLOtherUpdateCopyMethod());
        execs.add(new EPLOtherUpdateSubquery());
        execs.add(new EPLOtherUpdateUnprioritizedOrder());
        execs.add(new EPLOtherUpdateListenerDeliveryMultiupdate());
        execs.add(new EPLOtherUpdateListenerDeliveryMultiupdateMixed());
        execs.add(new EPLOtherUpdateSubqueryMultikeyWArray());
        execs.add(new EPLOtherUpdateMapIndexProps());
        return execs;
    }

    private static class EPLOtherUpdateSubqueryMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema Arriving(value int);\n" +
                "update istream Arriving set value = (select sum(value) as c0 from SupportEventWithIntArray#keepall group by array);\n" +
                "@name('s0') select * from Arriving;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventWithIntArray("E1", new int[]{1, 2}, 10));
            env.sendEventBean(new SupportEventWithIntArray("E2", new int[]{1, 2}, 11));

            env.milestone(0);
            assertUpdate(env, 21);

            env.sendEventBean(new SupportEventWithIntArray("E3", new int[]{1, 2}, 12));
            assertUpdate(env, 33);

            env.milestone(1);

            env.sendEventBean(new SupportEventWithIntArray("E4", new int[]{1}, 13));
            assertUpdate(env, null);

            env.undeployAll();
        }

        private void assertUpdate(RegressionEnvironment env, Integer expected) {
            env.sendEventMap(new HashMap<>(), "Arriving");
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("value"));
        }
    }

    public static class EPLOtherUpdateBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text = "@Name('Insert') insert into MyStream select * from SupportBean";
            env.compileDeploy(text, path).addListener("Insert");

            text = "@Name('Update') update istream MyStream set intPrimitive=10, theString='O_' || theString where intPrimitive=1";
            env.compileDeploy(text, path).addListener("Update");

            text = "@Name('Select') select * from MyStream";
            env.compileDeploy(text, path).addListener("Select");

            String[] fields = "theString,intPrimitive".split(",");
            env.sendEventBean(new SupportBean("E1", 9));
            EPAssertionUtil.assertProps(env.listener("Select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 9});
            EPAssertionUtil.assertProps(env.listener("Insert").assertOneGetNewAndReset(), fields, new Object[]{"E1", 9});
            assertFalse(env.listener("Update").isInvoked());

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("Select").assertOneGetNewAndReset(), fields, new Object[]{"O_E2", 10});
            EPAssertionUtil.assertProps(env.listener("Insert").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1});
            EPAssertionUtil.assertProps(env.listener("Update").getLastOldData()[0], fields, new Object[]{"E2", 1});
            EPAssertionUtil.assertProps(env.listener("Update").getLastNewData()[0], fields, new Object[]{"O_E2", 10});
            env.listener("Update").reset();

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 1));
            EPAssertionUtil.assertProps(env.listener("Select").assertOneGetNewAndReset(), fields, new Object[]{"O_E3", 10});
            EPAssertionUtil.assertProps(env.listener("Insert").assertOneGetNewAndReset(), fields, new Object[]{"E3", 1});
            EPAssertionUtil.assertProps(env.listener("Update").getLastOldData()[0], fields, new Object[]{"E3", 1});
            EPAssertionUtil.assertProps(env.listener("Update").getLastNewData()[0], fields, new Object[]{"O_E3", 10});
            env.listener("Update").reset();

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateFieldUpdateOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('update') update istream SupportBean " +
                "set intPrimitive=myvar, intBoxed=intPrimitive");
            assertEquals(StatementType.UPDATE, env.statement("update").getProperty(StatementProperty.STATEMENTTYPE));

            env.compileDeploy("@name('s0') select * from SupportBean").addListener("s0");
            String[] fields = "intPrimitive,intBoxed".split(",");

            env.sendEventBean(makeSupportBean("E1", 1, 2));
            EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[0], fields, new Object[]{10, 1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into SupportBeanStream select * from SupportBean", path);
            env.compileDeploy("insert into SupportBeanStreamTwo select * from pattern[a=SupportBean -> b=SupportBean]", path);
            env.compileDeploy("insert into SupportBeanStreamRO select * from SupportBeanReadOnly", path);

            tryInvalidCompile(env, path, "update istream SupportBeanStream set intPrimitive=longPrimitive",
                "Invalid assignment of column 'longPrimitive' of type 'java.lang.Long' to event property 'intPrimitive' typed as 'int', column and parameter types mismatch [update istream SupportBeanStream set intPrimitive=longPrimitive]");
            tryInvalidCompile(env, path, "update istream SupportBeanStream set xxx='abc'",
                "Property 'xxx' is not available for write access [update istream SupportBeanStream set xxx='abc']");
            tryInvalidCompile(env, path, "update istream SupportBeanStream set intPrimitive=null",
                "Invalid assignment of column 'null' of null type to event property 'intPrimitive' typed as 'int', nullable type mismatch [update istream SupportBeanStream set intPrimitive=null]");
            tryInvalidCompile(env, path, "update istream SupportBeanStreamTwo set a.intPrimitive=10",
                "Property 'a.intPrimitive' is not available for write access [update istream SupportBeanStreamTwo set a.intPrimitive=10]");
            tryInvalidCompile(env, path, "update istream SupportBeanStreamRO set side='a'",
                "Property 'side' is not available for write access [update istream SupportBeanStreamRO set side='a']");
            tryInvalidCompile(env, path, "update istream SupportBean set longPrimitive=sum(intPrimitive)",
                "Aggregation functions may not be used within an update-clause [update istream SupportBean set longPrimitive=sum(intPrimitive)]");
            tryInvalidCompile(env, path, "update istream SupportBean set longPrimitive=longPrimitive where sum(intPrimitive) = 1",
                "Aggregation functions may not be used within an update-clause [update istream SupportBean set longPrimitive=longPrimitive where sum(intPrimitive) = 1]");
            tryInvalidCompile(env, path, "update istream SupportBean set longPrimitive=prev(1, longPrimitive)",
                "Previous function cannot be used in this context [update istream SupportBean set longPrimitive=prev(1, longPrimitive)]");
            tryInvalidCompile(env, path, "update istream MyXmlEvent set abc=1",
                "Property 'abc' is not available for write access [update istream MyXmlEvent set abc=1]");
            tryInvalidCompile(env, path, "update istream SupportBeanErrorTestingOne set value='1'",
                "The update-clause requires the underlying event representation to support copy (via Serializable by default) [update istream SupportBeanErrorTestingOne set value='1']");
            tryInvalidCompile(env, path, "update istream SupportBean set longPrimitive=(select p0 from MyMapTypeInv#lastevent where theString=p3)",
                "Failed to plan subquery number 1 querying MyMapTypeInv: Failed to validate filter expression 'theString=p3': Property named 'theString' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\" [update istream SupportBean set longPrimitive=(select p0 from MyMapTypeInv#lastevent where theString=p3)]");
            tryInvalidCompile(env, path, "update istream XYZ.GYH set a=1",
                "Failed to resolve event type, named window or table by name 'XYZ.GYH' [update istream XYZ.GYH set a=1]");
            tryInvalidCompile(env, path, "update istream SupportBean set 1",
                "Missing property assignment expression in assignment number 0 [update istream SupportBean set 1]");

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateInsertIntoWBeanWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('insert') insert into MyStreamBW select * from SupportBean", path);
            env.addListener("insert");

            env.compileDeploy("@name('update_1') update istream MyStreamBW set intPrimitive=10, theString='O_' || theString where intPrimitive=1", path);
            env.addListener("update_1");

            env.compileDeploy("@name('s0') select * from MyStreamBW", path);
            env.addListener("s0");

            String[] fields = "theString,intPrimitive".split(",");
            env.sendEventBean(new SupportBean("E1", 9));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 9});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E1", 9});
            assertFalse(env.listener("update_1").isInvoked());

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"O_E2", 10});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E2", 1});
            EPAssertionUtil.assertProps(env.listener("update_1").assertOneGetOld(), fields, new Object[]{"E2", 1});
            EPAssertionUtil.assertProps(env.listener("update_1").assertOneGetNew(), fields, new Object[]{"O_E2", 10});
            env.listener("update_1").reset();

            env.sendEventBean(new SupportBean("E3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 2});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E3", 2});
            assertFalse(env.listener("update_1").isInvoked());

            env.sendEventBean(new SupportBean("E4", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"O_E4", 10});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E4", 1});
            EPAssertionUtil.assertProps(env.listener("update_1").assertOneGetOld(), fields, new Object[]{"E4", 1});
            EPAssertionUtil.assertProps(env.listener("update_1").assertOneGetNew(), fields, new Object[]{"O_E4", 10});

            env.compileDeploy("@name('update_2') update istream MyStreamBW as xyz set intPrimitive=xyz.intPrimitive + 1000 where intPrimitive=2", path);
            env.addListener("update_2");

            env.sendEventBean(new SupportBean("E5", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5", 1002});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E5", 2});
            EPAssertionUtil.assertProps(env.listener("update_2").assertOneGetOld(), fields, new Object[]{"E5", 2});
            EPAssertionUtil.assertProps(env.listener("update_2").assertOneGetNew(), fields, new Object[]{"E5", 1002});
            env.listener("update_2").reset();

            env.undeployModuleContaining("update_1");

            env.sendEventBean(new SupportBean("E6", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6", 1});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E6", 1});
            assertFalse(env.listener("update_2").isInvoked());

            env.sendEventBean(new SupportBean("E7", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E7", 1002});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E7", 2});
            EPAssertionUtil.assertProps(env.listener("update_2").assertOneGetOld(), fields, new Object[]{"E7", 2});
            EPAssertionUtil.assertProps(env.listener("update_2").assertOneGetNew(), fields, new Object[]{"E7", 1002});
            env.listener("update_2").reset();
            assertFalse(env.iterator("update_2").hasNext());

            SupportListener listenerUpdate2 = env.listener("update_2");
            env.statement("update_2").removeAllListeners();

            env.sendEventBean(new SupportBean("E8", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E8", 1002});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E8", 2});
            assertFalse(listenerUpdate2.isInvoked());

            SupportSubscriber subscriber = new SupportSubscriber();
            env.statement("update_2").setSubscriber(subscriber);

            env.sendEventBean(new SupportBean("E9", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E9", 1002});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E9", 2});
            SupportBean.compare(subscriber.getOldDataListFlattened()[0], "E9", 2);
            SupportBean.compare(subscriber.getNewDataListFlattened()[0], "E9", 1002);
            subscriber.reset();

            env.undeployModuleContaining("update_2");

            env.sendEventBean(new SupportBean("E10", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E10", 2});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E10", 2});

            env.compileDeploy("@name('update_3') update istream MyStreamBW set intPrimitive=intBoxed", path);
            env.addListener("update_3");

            env.sendEventBean(new SupportBean("E11", 2));
            EPAssertionUtil.assertProps(env.listener("update_3").assertOneGetNew(), fields, new Object[]{"E11", 2});
            env.listener("update_3").reset();

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateInsertIntoWMapNoWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('insert') insert into MyStreamII select * from MyMapTypeII", path).addListener("insert");

            EPCompiled update = env.compile("@name('update') update istream MyStreamII set p0=p1, p1=p0", path);
            env.deploy(update);

            env.compileDeploy("@name('s0') select * from MyStreamII", path).addListener("s0");

            String[] fields = "p0,p1,p2".split(",");
            env.sendEventMap(makeMap("p0", 10L, "p1", 1L, "p2", 100L), "MyMapTypeII");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 10L, 100L});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{10L, 1L, 100L});

            env.undeployModuleContaining("update");
            env.deploy(update).addListener("update");

            env.sendEventMap(makeMap("p0", 5L, "p1", 4L, "p2", 101L), "MyMapTypeII");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4L, 5L, 101L});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{5L, 4L, 101L});

            env.undeployModuleContaining("update");

            env.sendEventMap(makeMap("p0", 20L, "p1", 0L, "p2", 102L), "MyMapTypeII");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20L, 0L, 102L});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{20L, 0L, 102L});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateFieldsWithPriority implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionFieldsWithPriority(env, rep);
            }
        }
    }

    private static class EPLOtherUpdateInsertDirectBeanTypeInheritance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl =
                "create schema BaseInterface as " + BaseInterface.class.getName() + ";\n" +
                    "create schema BaseOne as " + BaseOne.class.getName() + ";\n" +
                    "create schema BaseOneA as " + BaseOneA.class.getName() + ";\n" +
                    "create schema BaseOneB as " + BaseOneB.class.getName() + ";\n" +
                    "create schema BaseTwo as " + BaseTwo.class.getName() + ";\n";
            env.compileDeploy(epl, path);

            // test update applies to child types via interface
            env.compileDeploy("@name('insert') insert into BaseOne select p0 as i, p1 as p from MyMapTypeIDB", path);
            env.compileDeploy("@Name('a') update istream BaseInterface set i='XYZ' where i like 'E%'", path);
            env.compileDeploy("@name('s0') select * from BaseOne", path).addListener("s0");

            String[] fields = "i,p".split(",");
            env.sendEventMap(makeMap("p0", "E1", "p1", "E1"), "MyMapTypeIDB");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"XYZ", "E1"});

            env.sendEventMap(makeMap("p0", "F1", "p1", "E2"), "MyMapTypeIDB");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"F1", "E2"});

            env.compileDeploy("@Priority(2) @Name('b') update istream BaseOne set i='BLANK'", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E3"), "MyMapTypeIDB");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"BLANK", "E3"});

            env.compileDeploy("@Priority(3) @Name('c') update istream BaseOneA set i='FINAL'", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E4"), "MyMapTypeIDB");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"BLANK", "E4"});

            env.undeployModuleContaining("insert");
            env.compileDeploy("@name('insert') insert into BaseOneA select p0 as i, p1 as p, 'a' as pa from MyMapTypeIDB", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E5"), "MyMapTypeIDB");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"FINAL", "E5"});

            env.undeployModuleContaining("insert");
            env.compileDeploy("@name('insert') insert into BaseOneB select p0 as i, p1 as p, 'b' as pb from MyMapTypeIDB", path);

            env.sendEventMap(makeMap("p0", "somevalue", "p1", "E6"), "MyMapTypeIDB");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"BLANK", "E6"});

            env.undeployModuleContaining("insert");
            env.compileDeploy("@name('insert') insert into BaseTwo select p0 as i, p1 as p from MyMapTypeIDB", path);

            env.undeployModuleContaining("s0");
            env.compileDeploy("@name('s0') select * from BaseInterface", path).addListener("s0");

            env.sendEventMap(makeMap("p0", "E2", "p1", "E7"), "MyMapTypeIDB");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"i"}, new Object[]{"XYZ"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "p0,p1".split(",");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("@name('window') create window AWindow#keepall select * from MyMapTypeNW", path).addListener("window");
            env.compileDeploy("@name('insert') insert into AWindow select * from MyMapTypeNW", path).addListener("insert");
            env.compileDeploy("@name('select') select * from AWindow", path).addListener("select");
            env.compileDeploy("update istream AWindow set p1='newvalue'", path);

            env.milestone(0);

            env.sendEventMap(makeMap("p0", "E1", "p1", "oldvalue"), "MyMapTypeNW");
            EPAssertionUtil.assertProps(env.listener("window").assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E1", "oldvalue"});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

            env.compileDeploy("@name('onselect') on SupportBean(theString='A') select win.* from AWindow as win", path).addListener("onselect");
            env.sendEventBean(new SupportBean("A", 0));
            EPAssertionUtil.assertProps(env.listener("onselect").assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

            env.milestone(1);

            env.compileDeploy("@name('oninsert') on SupportBean(theString='B') insert into MyOtherStream select win.* from AWindow as win", path).addListener("oninsert");
            env.sendEventBean(new SupportBean("B", 1));
            EPAssertionUtil.assertProps(env.listener("oninsert").assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

            env.milestone(2);

            env.compileDeploy("update istream MyOtherStream set p0='a', p1='b'", path);
            env.compileDeploy("@name('s0') select * from MyOtherStream", path).addListener("s0");
            env.sendEventBean(new SupportBean("B", 1));
            EPAssertionUtil.assertProps(env.listener("oninsert").assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"a", "b"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateTypeWidener implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "theString,longBoxed,intBoxed".split(",");

            env.compileDeploy("insert into AStream select * from SupportBean", path);
            env.compileDeploy("update istream AStream set longBoxed=intBoxed, intBoxed=null", path);
            env.compileDeploy("@name('s0') select * from AStream", path).addListener("s0");

            SupportBean bean = new SupportBean("E1", 0);
            bean.setLongBoxed(888L);
            bean.setIntBoxed(999);
            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 999L, null});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateSendRouteSenderPreprocess implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test map
            env.compileDeploy("@name('s0') select * from MyMapTypeSR").addListener("s0");
            env.compileDeploy("update istream MyMapTypeSR set p0='a'");

            String[] fields = "p0,p1".split(",");
            env.sendEventMap(makeMap("p0", "E1", "p1", "E1"), "MyMapTypeSR");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"a", "E1"});

            env.sendEventMap(makeMap("p0", "E2", "p1", "E2"), "MyMapTypeSR");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"a", "E2"});

            env.compileDeploy("@name('trigger') select * from SupportBean");
            env.statement("trigger").addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    env.eventService().routeEventMap(makeMap("p0", "E3", "p1", "E3"), "MyMapTypeSR");
                }
            });
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"a", "E3"});

            env.compileDeploy("@Drop @name('drop') update istream MyMapTypeSR set p0='a'");
            env.sendEventMap(makeMap("p0", "E4", "p1", "E4"), "MyMapTypeSR");
            env.sendEventMap(makeMap("p0", "E5", "p1", "E5"), "MyMapTypeSR");
            env.sendEventBean(new SupportBean());
            assertFalse(env.listener("s0").isInvoked());

            env.undeployModuleContaining("drop");
            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("trigger");

            // test bean
            env.compileDeploy("@name('s0') select * from SupportBean").addListener("s0");
            env.compileDeploy("update istream SupportBean set intPrimitive=999");

            fields = "theString,intPrimitive".split(",");
            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 999});

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 999});

            env.compileDeploy("@name('trigger') select * from MyMapTypeSR");
            env.statement("trigger").addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    env.eventService().routeEventBean(new SupportBean("E3", 0), "SupportBean");
                }
            });
            env.sendEventMap(makeMap("p0", "", "p1", ""), "MyMapTypeSR");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 999});

            env.compileDeploy("@Drop update istream SupportBean set intPrimitive=1");
            env.sendEventBean(new SupportBean("E4", 0));
            env.sendEventBean(new SupportBean("E4", 0));
            env.sendEventMap(makeMap("p0", "", "p1", ""), "MyMapTypeSR");
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setUpdateClause(UpdateClause.create("MyMapTypeSODA", Expressions.eq(Expressions.property("p1"), Expressions.constant("newvalue"))));
            model.getUpdateClause().setOptionalAsClauseStreamName("mytype");
            model.getUpdateClause().setOptionalWhereClause(Expressions.eq("p0", "E1"));
            assertEquals("update istream MyMapTypeSODA as mytype set p1=\"newvalue\" where p0=\"E1\"", model.toEPL());

            // test map
            env.compileDeploy("@name('s0') select * from MyMapTypeSODA").addListener("s0");
            env.compileDeploy(model);

            String[] fields = "p0,p1".split(",");
            env.sendEventMap(makeMap("p0", "E1", "p1", "E1"), "MyMapTypeSODA");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "newvalue"});

            // test unmap
            String text = "update istream MyMapTypeSODA as mytype set p1=\"newvalue\" where p0=\"E1\"";
            env.eplToModelCompileDeploy(text);

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateXMLEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String xml = "<simpleEvent><prop1>SAMPLE_V1</prop1></simpleEvent>";
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            Document simpleDoc = SupportXML.getDocument(xml);

            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into ABCStreamXML select 1 as valOne, 2 as valTwo, * from MyXMLEvent", path);
            env.compileDeploy("update istream ABCStreamXML set valOne = 987, valTwo=123 where prop1='SAMPLE_V1'", path);
            env.compileDeploy("@name('s0') select * from ABCStreamXML", path).addListener("s0");

            env.sendEventXMLDOM(simpleDoc, "MyXMLEvent");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "valOne,valTwo,prop1".split(","), new Object[]{987, 123, "SAMPLE_V1"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateWrappedObject implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into ABCStreamWO select 1 as valOne, 2 as valTwo, * from SupportBean", path);
            env.compileDeploy("@name('update') update istream ABCStreamWO set valOne = 987, valTwo=123", path);
            env.compileDeploy("@name('s0') select * from ABCStreamWO", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "valOne,valTwo,theString".split(","), new Object[]{987, 123, "E1"});

            env.undeployModuleContaining("update");
            env.compileDeploy("@name('update') update istream ABCStreamWO set theString = 'A'", path);

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "valOne,valTwo,theString".split(","), new Object[]{1, 2, "A"});

            env.undeployModuleContaining("update");
            env.compileDeploy("update istream ABCStreamWO set theString = 'B', valOne = 555", path);

            env.sendEventBean(new SupportBean("E3", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "valOne,valTwo,theString".split(","), new Object[]{555, 2, "B"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateCopyMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into ABCStreamCM select * from SupportBeanCopyMethod", path);
            env.compileDeploy("update istream ABCStreamCM set valOne = 'x', valTwo='y'", path);
            env.compileDeploy("@name('s0') select * from ABCStreamCM", path).addListener("s0");

            env.sendEventBean(new SupportBeanCopyMethod("1", "2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "valOne,valTwo".split(","), new Object[]{"x", "y"});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String[] fields = "theString,intPrimitive".split(",");
            env.compileDeploy("insert into ABCStreamSQ select * from SupportBean", path);
            env.compileDeploy("@name('update') update istream ABCStreamSQ set theString = (select s0 from MyMapTypeSelect#lastevent) where intPrimitive in (select w0 from MyMapTypeWhere#keepall)", path);
            env.compileDeploy("@name('s0') select * from ABCStreamSQ", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 0});

            env.sendEventMap(makeMap("w0", 1), "MyMapTypeWhere");
            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, 1});

            env.sendEventBean(new SupportBean("E3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 2});

            env.sendEventMap(makeMap("s0", "newvalue"), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E4", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"newvalue", 1});

            env.sendEventMap(makeMap("s0", "othervalue"), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E5", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"othervalue", 1});

            // test correlated subquery
            env.undeployModuleContaining("update");
            env.compileDeploy("@name('update') update istream ABCStreamSQ set intPrimitive = (select s1 from MyMapTypeSelect#keepall where s0 = ABCStreamSQ.theString)", path);

            // note that this will log an error (int primitive set to null), which is good, and leave the value unchanged
            env.sendEventBean(new SupportBean("E6", 8));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6", 8});

            env.sendEventMap(makeMap("s0", "E7", "s1", 91), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E7", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E7", 91});

            // test correlated with as-clause
            env.undeployModuleContaining("update");
            env.compileDeploy("@name('update') update istream ABCStreamSQ as mystream set intPrimitive = (select s1 from MyMapTypeSelect#keepall where s0 = mystream.theString)", path);

            // note that this will log an error (int primitive set to null), which is good, and leave the value unchanged
            env.sendEventBean(new SupportBean("E8", 111));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E8", 111});

            env.sendEventMap(makeMap("s0", "E9", "s1", -1), "MyMapTypeSelect");
            env.sendEventBean(new SupportBean("E9", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E9", -1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateUnprioritizedOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "s0,s1".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into ABCStreamUO select * from MyMapTypeUO", path);
            env.compileDeploy("@Name('A') update istream ABCStreamUO set s0='A'", path);
            env.compileDeploy("@Name('B') update istream ABCStreamUO set s0='B'", path);
            env.compileDeploy("@Name('C') update istream ABCStreamUO set s0='C'", path);
            env.compileDeploy("@Name('D') update istream ABCStreamUO set s0='D'", path);
            env.compileDeploy("@name('s0') select * from ABCStreamUO", path).addListener("s0");

            env.sendEventMap(makeMap("s0", "", "s1", 1), "MyMapTypeUO");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"D", 1});

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateListenerDeliveryMultiupdate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportUpdateListener listenerInsert = new SupportUpdateListener();
            SupportUpdateListener[] listeners = new SupportUpdateListener[5];
            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = new SupportUpdateListener();
            }

            RegressionPath path = new RegressionPath();
            String[] fields = "theString,intPrimitive,value1".split(",");
            env.compileDeploy("@name('insert') insert into ABCStreamLD select *, 'orig' as value1 from SupportBean", path).statement("insert").addListener(listenerInsert);
            env.compileDeploy("@Name('A') update istream ABCStreamLD set theString='A', value1='a' where intPrimitive in (1,2)", path).statement("A").addListener(listeners[0]);
            env.compileDeploy("@Name('B') update istream ABCStreamLD set theString='B', value1='b' where intPrimitive in (1,3)", path).statement("B").addListener(listeners[1]);
            env.compileDeploy("@Name('C') update istream ABCStreamLD set theString='C', value1='c' where intPrimitive in (2,3)", path).statement("C").addListener(listeners[2]);
            env.compileDeploy("@name('s0') select * from ABCStreamLD", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E1", 1, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 1, "a"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"A", 1, "a"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 1, "b"});
            assertFalse(listeners[2].isInvoked());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 1, "b"});
            reset(listeners);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E2", 2, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 2, "a"});
            assertFalse(listeners[1].isInvoked());
            EPAssertionUtil.assertProps(listeners[2].assertOneGetOld(), fields, new Object[]{"A", 2, "a"});
            EPAssertionUtil.assertProps(listeners[2].assertOneGetNew(), fields, new Object[]{"C", 2, "c"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"C", 2, "c"});
            reset(listeners);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3, "orig"});
            assertFalse(listeners[0].isInvoked());
            EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"E3", 3, "orig"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 3, "b"});
            EPAssertionUtil.assertProps(listeners[2].assertOneGetOld(), fields, new Object[]{"B", 3, "b"});
            EPAssertionUtil.assertProps(listeners[2].assertOneGetNew(), fields, new Object[]{"C", 3, "c"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"C", 3, "c"});
            reset(listeners);

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateListenerDeliveryMultiupdateMixed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportUpdateListener listenerInsert = new SupportUpdateListener();
            SupportUpdateListener[] listeners = new SupportUpdateListener[5];
            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = new SupportUpdateListener();
            }

            RegressionPath path = new RegressionPath();
            String[] fields = "theString,intPrimitive,value1".split(",");
            env.compileDeploy("@name('insert') insert into ABCStreamLDM select *, 'orig' as value1 from SupportBean", path).statement("insert").addListener(listenerInsert);
            env.compileDeploy("@name('s0') select * from ABCStreamLDM", path).addListener("s0");

            env.compileDeploy("@Name('A') update istream ABCStreamLDM set theString='A', value1='a'", path);
            env.compileDeploy("@Name('B') update istream ABCStreamLDM set theString='B', value1='b'", path).statement("B").addListener(listeners[1]);
            env.compileDeploy("@Name('C') update istream ABCStreamLDM set theString='C', value1='c'", path);
            env.compileDeploy("@Name('D') update istream ABCStreamLDM set theString='D', value1='d'", path).statement("D").addListener(listeners[3]);
            env.compileDeploy("@Name('E') update istream ABCStreamLDM set theString='E', value1='e'", path);

            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E4", 4, "orig"});
            assertFalse(listeners[0].isInvoked());
            EPAssertionUtil.assertProps(listeners[1].assertOneGetOld(), fields, new Object[]{"A", 4, "a"});
            EPAssertionUtil.assertProps(listeners[1].assertOneGetNew(), fields, new Object[]{"B", 4, "b"});
            assertFalse(listeners[2].isInvoked());
            EPAssertionUtil.assertProps(listeners[3].assertOneGetOld(), fields, new Object[]{"C", 4, "c"});
            EPAssertionUtil.assertProps(listeners[3].assertOneGetNew(), fields, new Object[]{"D", 4, "d"});
            assertFalse(listeners[4].isInvoked());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E", 4, "e"});
            reset(listeners);

            env.statement("B").removeAllListeners();
            env.statement("D").removeAllListeners();
            env.statement("A").addListener(listeners[0]);
            env.statement("E").addListener(listeners[4]);

            env.sendEventBean(new SupportBean("E5", 5));
            EPAssertionUtil.assertProps(env.listener("insert").assertOneGetNewAndReset(), fields, new Object[]{"E5", 5, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetOld(), fields, new Object[]{"E5", 5, "orig"});
            EPAssertionUtil.assertProps(listeners[0].assertOneGetNew(), fields, new Object[]{"A", 5, "a"});
            assertFalse(listeners[1].isInvoked());
            assertFalse(listeners[2].isInvoked());
            assertFalse(listeners[3].isInvoked());
            EPAssertionUtil.assertProps(listeners[4].assertOneGetOld(), fields, new Object[]{"D", 5, "d"});
            EPAssertionUtil.assertProps(listeners[4].assertOneGetNew(), fields, new Object[]{"E", 5, "e"});
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E", 5, "e"});
            reset(listeners);

            env.undeployAll();
        }
    }

    private static class EPLOtherUpdateMapIndexProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionSetMapPropsBean(env);

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                runAssertionUpdateIStreamSetMapProps(env, rep);
            }

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                runAssertionNamedWindowSetMapProps(env, rep);
            }
        }
    }

    private static void runAssertionSetMapPropsBean(RegressionEnvironment env) {
        // test update-istream with bean
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType("create schema MyMapPropEvent as " + MyMapPropEvent.class.getName(), path);
        env.compileDeploy("insert into MyStream select * from MyMapPropEvent", path);
        env.compileDeploy("@name('s0') update istream MyStream set props('abc') = 1, array[2] = 10", path).addListener("s0");

        env.sendEventBean(new MyMapPropEvent());
        EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), "props('abc'),array[2]".split(","), new Object[]{1, 10}, new Object[]{null, null});

        env.undeployAll();
    }

    private static void runAssertionUpdateIStreamSetMapProps(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        // test update-istream with map
        RegressionPath path = new RegressionPath();
        String eplType = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMapProp.class) + " @name('type') create schema MyInfraTypeWithMapProp(simple String, myarray int[], mymap java.util.Map)";
        env.compileDeployWBusPublicType(eplType, path);

        env.compileDeploy("@name('update') update istream MyInfraTypeWithMapProp set simple='A', mymap('abc') = 1, myarray[2] = 10", path).addListener("update");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null, new int[10], new HashMap<String, Object>()}, "MyInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeMapEvent(new HashMap<>(), new int[10]), "MyInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(env.deploymentId("type"), "MyInfraTypeWithMapProp")));
            event.put("myarray", Arrays.asList(0, 0, 0, 0, 0));
            event.put("mymap", new HashMap());
            env.sendEventAvro(event, "MyInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonArray myarray = new JsonArray().add(0).add(0).add(0).add(0).add(0);
            JsonObject mymap = new JsonObject();
            JsonObject event = new JsonObject().add("myarray", myarray).add("mymap", mymap);
            env.sendEventJson(event.toString(), "MyInfraTypeWithMapProp");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.listener("update").assertPairGetIRAndReset(), "simple,mymap('abc'),myarray[2]".split(","), new Object[]{"A", 1, 10}, new Object[]{null, null, 0});

        env.undeployAll();
    }

    private static void runAssertionNamedWindowSetMapProps(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        // test named-window update
        RegressionPath path = new RegressionPath();
        String eplTypes = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMapProp.class) + " @name('type') create schema MyNWInfraTypeWithMapProp(simple String, myarray int[], mymap java.util.Map)";
        env.compileDeployWBusPublicType(eplTypes, path);

        env.compileDeploy("@name('window') create window MyWindowWithMapProp#keepall as MyNWInfraTypeWithMapProp", path);
        env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMapProp.class) + " insert into MyWindowWithMapProp select * from MyNWInfraTypeWithMapProp", path);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null, new int[10], new HashMap<String, Object>()}, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeMapEvent(new HashMap<>(), new int[10]), "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(env.deploymentId("type"), "MyNWInfraTypeWithMapProp")));
            event.put("myarray", Arrays.asList(0, 0, 0, 0, 0));
            event.put("mymap", new HashMap());
            env.sendEventAvro(event, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonArray myarray = new JsonArray().add(0).add(0).add(0).add(0).add(0);
            JsonObject mymap = new JsonObject();
            JsonObject event = new JsonObject().add("myarray", myarray).add("mymap", mymap);
            env.sendEventJson(event.toString(), "MyNWInfraTypeWithMapProp");
        } else {
            fail();
        }

        env.compileDeploy("on SupportBean update MyWindowWithMapProp set simple='A', mymap('abc') = intPrimitive, myarray[2] = intPrimitive", path);
        env.sendEventBean(new SupportBean("E1", 10));
        EPAssertionUtil.assertPropsPerRow(env.iterator("window"), "simple,mymap('abc'),myarray[2]".split(","), new Object[][]{{"A", 10, 10}});

        // test null and array too small
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null, new int[2], null}, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeMapEvent(null, new int[2]), "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(record("name").fields()
                .optionalString("simple")
                .name("myarray").type(array().items().longType()).noDefault()
                .name("mymap").type(map().values().stringType()).noDefault()
                .endRecord());
            event.put("myarray", Arrays.asList(0, 0));
            event.put("mymap", null);
            env.sendEventAvro(event, "MyNWInfraTypeWithMapProp");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonArray myarray = new JsonArray().add(0).add(0);
            JsonObject event = new JsonObject().add("myarray", myarray);
            env.sendEventJson(event.toString(), "MyNWInfraTypeWithMapProp");
        } else {
            fail();
        }
        env.sendEventBean(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("window"), "simple,mymap('abc'),myarray[2]".split(","), new Object[][]{{"A", 20, 20}, {"A", null, null}});

        env.undeployAll();
    }

    private static Map<String, Object> makeMapEvent(Map<String, Object> mymap, int[] myarray) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("mymap", mymap);
        map.put("myarray", myarray);
        return map;
    }

    private static void tryAssertionFieldsWithPriority(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        String prefix = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSB.class);
        env.compileDeploy(prefix + " insert into MyStream select theString, intPrimitive from SupportBean(theString not like 'Z%')", path);
        env.compileDeploy(prefix + " insert into MyStream select 'AX'||theString as theString, intPrimitive from SupportBean(theString like 'Z%')", path);
        env.compileDeploy(prefix + " @Name('a') @Priority(12) update istream MyStream set intPrimitive=-2 where intPrimitive=-1", path);
        env.compileDeploy(prefix + " @Name('b') @Priority(11) update istream MyStream set intPrimitive=-1 where theString like 'D%'", path);
        env.compileDeploy(prefix + " @Name('c') @Priority(9) update istream MyStream set intPrimitive=9 where theString like 'A%'", path);
        env.compileDeploy(prefix + " @Name('d') @Priority(8) update istream MyStream set intPrimitive=8 where theString like 'A%' or theString like 'C%'", path);
        env.compileDeploy(" @Name('e') @Priority(10) update istream MyStream set intPrimitive=10 where theString like 'A%'", path);
        env.compileDeploy(" @Name('f') @Priority(7) update istream MyStream set intPrimitive=7 where theString like 'A%' or theString like 'C%'", path);
        env.compileDeploy(" @Name('g') @Priority(6) update istream MyStream set intPrimitive=6 where theString like 'A%'", path);
        env.compileDeploy(" @Name('h') @Drop update istream MyStream set intPrimitive=6 where theString like 'B%'", path);

        env.compileDeploy("@name('s0') select * from MyStream where intPrimitive > 0", path).addListener("s0");

        String[] fields = "theString,intPrimitive".split(",");
        env.sendEventBean(new SupportBean("A1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", 10});

        env.sendEventBean(new SupportBean("B1", 0));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean("C1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"C1", 8});

        env.sendEventBean(new SupportBean("D1", 100));
        assertFalse(env.listener("s0").isInvoked());

        env.undeployModuleContaining("s0");
        env.compileDeploy("@name('s0') select * from MyStream", path).addListener("s0");
        assertTrue(eventRepresentationEnum.matchesClass(env.statement("s0").getEventType().getUnderlyingType()));

        env.sendEventBean(new SupportBean("D1", -2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"D1", -2});

        env.sendEventBean(new SupportBean("Z1", -3));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"AXZ1", 10});

        env.undeployModuleContaining("e");
        env.sendEventBean(new SupportBean("Z2", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"AXZ2", 9});

        env.undeployModuleContaining("c");
        env.undeployModuleContaining("d");
        env.undeployModuleContaining("f");
        env.undeployModuleContaining("g");
        env.sendEventBean(new SupportBean("Z3", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"AXZ3", 0});

        env.undeployAll();
    }

    private static void reset(SupportUpdateListener[] listeners) {
        for (SupportUpdateListener listener : listeners) {
            listener.reset();
        }
    }

    private static Map<String, Object> makeMap(String prop1, Object val1, String prop2, Object val2, String prop3, Object val3) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop1, val1);
        map.put(prop2, val2);
        map.put(prop3, val3);
        return map;
    }

    private static Map<String, Object> makeMap(String prop1, Object val1, String prop2, Object val2) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop1, val1);
        map.put(prop2, val2);
        return map;
    }

    private static Map<String, Object> makeMap(String prop1, Object val1) {
        Map<String, Object> map = new HashMap<>();
        map.put(prop1, val1);
        return map;
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    public static interface BaseInterface extends Serializable {
        public String getI();

        public void setI(String i);
    }

    public static class BaseOne implements BaseInterface, Serializable {
        private String i;
        private String p;

        public BaseOne() {
        }

        public BaseOne(String i, String p) {
            this.i = i;
            this.p = p;
        }

        public String getP() {
            return p;
        }

        public void setP(String p) {
            this.p = p;
        }

        public String getI() {
            return i;
        }

        public void setI(String i) {
            this.i = i;
        }
    }

    public static class BaseTwo implements BaseInterface, Serializable {
        private String i;
        private String p;

        public BaseTwo() {
        }

        public BaseTwo(String p) {
            this.p = p;
        }

        public void setP(String p) {
            this.p = p;
        }

        public String getP() {
            return p;
        }

        public String getI() {
            return i;
        }

        public void setI(String i) {
            this.i = i;
        }
    }

    public static class BaseOneA extends BaseOne {
        private String pa;

        public BaseOneA() {
        }

        public BaseOneA(String i, String p, String pa) {
            super(i, p);
            this.pa = pa;
        }

        public String getPa() {
            return pa;
        }

        public void setPa(String pa) {
            this.pa = pa;
        }
    }

    public static class BaseOneB extends BaseOne {
        private String pb;

        public BaseOneB() {
        }

        public BaseOneB(String i, String p, String pb) {
            super(i, p);
            this.pb = pb;
        }

        public String getPb() {
            return pb;
        }

        public void setPb(String pb) {
            this.pb = pb;
        }
    }

    public static void setIntBoxedValue(SupportBean sb, int value) {
        sb.setIntBoxed(value);
    }

    public static class MyMapPropEvent implements Serializable {
        private Map props = new HashMap();
        private Object[] array = new Object[10];

        public void setProps(String name, Object value) {
            props.put(name, value);
        }

        public void setArray(int index, Object value) {
            array[index] = value;
        }

        public Map getProps() {
            return props;
        }

        public void setProps(Map props) {
            this.props = props;
        }

        public Object[] getArray() {
            return array;
        }

        public void setArray(Object[] array) {
            this.array = array;
        }

        public Object getArray(int index) {
            return array[index];
        }
    }

    public static class MyLocalJsonProvidedMapProp implements Serializable {
        public String simple;
        public Integer[] myarray;
        public Map mymap;
    }

    public static class MyLocalJsonProvidedSB implements Serializable {
        public String theString;
        public int intPrimitive;
    }
}
