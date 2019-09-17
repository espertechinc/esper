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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static junit.framework.TestCase.*;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;

public class EPLInsertInto {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoAssertionWildcardRecast());
        execs.add(new EPLInsertIntoJoinWildcard());
        execs.add(new EPLInsertIntoWithOutputLimitAndSort());
        execs.add(new EPLInsertIntoStaggeredWithWildcard());
        execs.add(new EPLInsertIntoInsertFromPattern());
        execs.add(new EPLInsertIntoInsertIntoPlusPattern());
        execs.add(new EPLInsertIntoNullType());
        execs.add(new EPLInsertIntoChain());
        execs.add(new EPLInsertIntoMultiBeanToMulti());
        execs.add(new EPLInsertIntoSingleBeanToMulti());
        execs.add(new EPLInsertIntoProvidePartitialCols());
        execs.add(new EPLInsertIntoRStreamOMToStmt());
        execs.add(new EPLInsertIntoNamedColsOMToStmt());
        execs.add(new EPLInsertIntoNamedColsEPLToOMStmt());
        execs.add(new EPLInsertIntoNamedColsSimple());
        execs.add(new EPLInsertIntoNamedColsStateless());
        execs.add(new EPLInsertIntoNamedColsWildcard());
        execs.add(new EPLInsertIntoNamedColsJoin());
        execs.add(new EPLInsertIntoNamedColsJoinWildcard());
        execs.add(new EPLInsertIntoUnnamedSimple());
        execs.add(new EPLInsertIntoUnnamedWildcard());
        execs.add(new EPLInsertIntoUnnamedJoin());
        execs.add(new EPLInsertIntoTypeMismatchInvalid());
        execs.add(new EPLInsertIntoEventRepresentationsSimple());
        return execs;
    }

    private static class EPLInsertIntoEventRepresentationsSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Map<EventRepresentationChoice, Consumer<Object>> assertions = new HashMap<>();
            assertions.put(EventRepresentationChoice.OBJECTARRAY, und -> {
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1", 10}, (Object[]) und);
            });
            Consumer<Object> mapAssertion = und -> EPAssertionUtil.assertPropsMap((Map) und, "theString,intPrimitive".split(","), "E1", 10);
            assertions.put(EventRepresentationChoice.MAP, mapAssertion);
            assertions.put(EventRepresentationChoice.DEFAULT, mapAssertion);
            assertions.put(EventRepresentationChoice.AVRO, und -> {
                GenericData.Record rec = (GenericData.Record) und;
                assertEquals("E1", rec.get("theString"));
                assertEquals(10, rec.get("intPrimitive"));
            });
            assertions.put(EventRepresentationChoice.JSON, und -> {
                JsonEventObject rec = (JsonEventObject) und;
                assertEquals("E1", rec.get("theString"));
                assertEquals(10, rec.get("intPrimitive"));
            });
            assertions.put(EventRepresentationChoice.JSONCLASSPROVIDED, und -> {
                MyLocalJsonProvided rec = (MyLocalJsonProvided) und;
                assertEquals("E1", rec.theString);
                assertEquals(Integer.valueOf(10), rec.intPrimitive);
            });

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionRepresentationSimple(env, rep, assertions);
            }
        }
    }

    private static void tryAssertionRepresentationSimple(RegressionEnvironment env, EventRepresentationChoice rep, Map<EventRepresentationChoice, Consumer<Object>> assertions) {
        String epl = rep.getAnnotationTextWJsonProvided(MyLocalJsonProvided.class) + " insert into SomeStream select theString, intPrimitive from SupportBean;\n" +
            "@name('s0') select * from SomeStream;\n";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 10));
        Consumer<Object> assertion = assertions.get(rep);
        if (assertion == null) {
            fail("No assertion provided for type " + rep);
        }
        assertion.accept(env.listener("s0").assertOneGetNewAndReset().getUnderlying());

        env.undeployAll();
    }

    private static class EPLInsertIntoRStreamOMToStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setInsertInto(InsertIntoClause.create("Event_1_RSOM", new String[0], StreamSelector.RSTREAM_ONLY));
            model.setSelectClause(SelectClause.create().add("intPrimitive", "intBoxed"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean")));
            model = SerializableObjectCopier.copyMayFail(model);
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));

            String epl = "@name('s0') insert rstream into Event_1_RSOM " +
                "select intPrimitive, intBoxed " +
                "from SupportBean";
            assertEquals(epl, model.toEPL());

            EPStatementObjectModel modelTwo = env.eplToModel(model.toEPL());
            model = SerializableObjectCopier.copyMayFail(modelTwo);
            assertEquals(epl, model.toEPL());
        }
    }

    private static class EPLInsertIntoNamedColsOMToStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setInsertInto(InsertIntoClause.create("Event_1_OMS", "delta", "product"));
            model.setSelectClause(SelectClause.create().add(Expressions.minus("intPrimitive", "intBoxed"), "deltaTag")
                .add(Expressions.multiply("intPrimitive", "intBoxed"), "productTag"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView(View.create("length", Expressions.constant(100)))));
            model = SerializableObjectCopier.copyMayFail(model);

            tryAssertsVariant(env, null, model, "Event_1_OMS");

            String epl = "@name('fl') insert into Event_1_OMS(delta, product) " +
                "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                "from SupportBean#length(100)";
            assertEquals(epl, model.toEPL());
            assertEquals(epl, env.statement("fl").getProperty(StatementProperty.EPL));

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedColsEPLToOMStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('fl') insert into Event_1_EPL(delta, product) " +
                "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                "from SupportBean#length(100)";

            EPStatementObjectModel model = env.eplToModel(epl);
            model = SerializableObjectCopier.copyMayFail(model);
            assertEquals(epl, model.toEPL());

            tryAssertsVariant(env, null, model, "Event_1_EPL");
            assertEquals(epl, env.statement("fl").getProperty(StatementProperty.EPL));
            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedColsSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "insert into Event_1VO (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from SupportBean#length(100)";

            tryAssertsVariant(env, stmtText, null, "Event_1VO");
            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedColsStateless implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtTextStateless = "insert into Event_1VOS (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from SupportBean";
            tryAssertsVariant(env, stmtTextStateless, null, "Event_1VOS");
            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedColsWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "insert into Event_1W (delta, product) " +
                "select * from SupportBean#length(100)";
            tryInvalidCompile(env, stmtText, "Wildcard not allowed when insert-into specifies column order");

            // test insert wildcard to wildcard
            String stmtSelectText = "@name('i0') insert into ABCStream select * from SupportBean";
            env.compileDeploy(stmtSelectText).addListener("i0");
            assertTrue(env.statement("i0").getEventType() instanceof BeanEventType);

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals("E1", env.listener("i0").assertOneGetNew().get("theString"));
            assertTrue(env.listener("i0").assertOneGetNew().getUnderlying() instanceof SupportBean);

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedColsJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "insert into Event_1J (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from SupportBean#length(100) as s0," +
                "SupportBean_A#length(100) as s1 " +
                " where s0.theString = s1.id";

            tryAssertsVariant(env, stmtText, null, "Event_1J");
            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedColsJoinWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "insert into Event_1JW (delta, product) " +
                "select * " +
                "from SupportBean#length(100) as s0," +
                "SupportBean_A#length(100) as s1 " +
                " where s0.theString = s1.id";

            try {
                env.compileWCheckedEx(stmtText);
                fail();
            } catch (EPCompileException ex) {
                // Expected
            }
        }
    }

    private static class EPLInsertIntoUnnamedSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "insert into Event_1_2 " +
                "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                "from SupportBean#length(100)";

            tryAssertsVariant(env, stmtText, null, "Event_1_2");
            env.undeployAll();
        }
    }

    private static class EPLInsertIntoUnnamedWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtText = "@name('stmt1') insert into event1 select * from SupportBean#length(100)";
            String otherText = "@name('stmt2') select * from event1#length(10)";

            // Attach listener to feed
            env.compileDeploy(stmtText, path).addListener("stmt1");
            env.compileDeploy(otherText, path).addListener("stmt2");

            SupportBean theEvent = sendEvent(env, 10, 11);
            assertTrue(env.listener("stmt1").getAndClearIsInvoked());
            assertEquals(1, env.listener("stmt1").getLastNewData().length);
            assertEquals(10, env.listener("stmt1").getLastNewData()[0].get("intPrimitive"));
            assertEquals(11, env.listener("stmt1").getLastNewData()[0].get("intBoxed"));
            assertEquals(20, env.listener("stmt1").getLastNewData()[0].getEventType().getPropertyNames().length);
            assertSame(theEvent, env.listener("stmt1").getLastNewData()[0].getUnderlying());

            assertTrue(env.listener("stmt2").getAndClearIsInvoked());
            assertEquals(1, env.listener("stmt2").getLastNewData().length);
            assertEquals(10, env.listener("stmt2").getLastNewData()[0].get("intPrimitive"));
            assertEquals(11, env.listener("stmt2").getLastNewData()[0].get("intBoxed"));
            assertEquals(20, env.listener("stmt2").getLastNewData()[0].getEventType().getPropertyNames().length);
            assertSame(theEvent, env.listener("stmt2").getLastNewData()[0].getUnderlying());

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoUnnamedJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "insert into Event_1_2J " +
                "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                "from SupportBean#length(100) as s0," +
                "SupportBean_A#length(100) as s1 " +
                " where s0.theString = s1.id";

            tryAssertsVariant(env, stmtText, null, "Event_1_2J");

            // assert type metadata
            EventType type = env.statement("fl").getEventType();
            assertEquals(NameAccessModifier.PUBLIC, type.getMetadata().getAccessModifier());
            assertEquals(EventTypeTypeClass.STREAM, type.getMetadata().getTypeClass());
            assertEquals(EventTypeApplicationType.MAP, type.getMetadata().getApplicationType());
            assertEquals("Event_1_2J", type.getMetadata().getName());
            assertEquals(EventTypeBusModifier.NONBUS, type.getMetadata().getBusModifier());

            env.undeployAll();
        }
    }

    private static void tryAssertsVariant(RegressionEnvironment env, String stmtText, EPStatementObjectModel model, String typeName) {
        RegressionPath path = new RegressionPath();
        // Attach listener to feed
        if (model != null) {
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("fl")));
            env.compileDeploy(model, path);
        } else {
            env.compileDeploy("@name('fl') " + stmtText, path);
        }
        env.addListener("fl");

        // send event for joins to match on
        env.sendEventBean(new SupportBean_A("myId"));

        // Attach delta statement to statement and add listener
        stmtText = "@name('rld') select MIN(delta) as minD, max(delta) as maxD " +
            "from " + typeName + "#time(60)";
        env.compileDeploy(stmtText, path).addListener("rld");

        // Attach prodict statement to statement and add listener
        stmtText = "@name('rlp') select min(product) as minP, max(product) as maxP " +
            "from " + typeName + "#time(60)";
        env.compileDeploy(stmtText, path).addListener("rlp");

        env.advanceTime(0); // Set the time to 0 seconds

        // send events
        sendEvent(env, 20, 10);
        assertReceivedFeed(env.listener("fl"), 10, 200);
        assertReceivedMinMax(env.listener("rld"), env.listener("rlp"), 10, 10, 200, 200);

        sendEvent(env, 50, 25);
        assertReceivedFeed(env.listener("fl"), 25, 25 * 50);
        assertReceivedMinMax(env.listener("rld"), env.listener("rlp"), 10, 25, 200, 1250);

        sendEvent(env, 5, 2);
        assertReceivedFeed(env.listener("fl"), 3, 2 * 5);
        assertReceivedMinMax(env.listener("rld"), env.listener("rlp"), 3, 25, 10, 1250);

        env.advanceTime(10 * 1000); // Set the time to 10 seconds

        sendEvent(env, 13, 1);
        assertReceivedFeed(env.listener("fl"), 12, 13);
        assertReceivedMinMax(env.listener("rld"), env.listener("rlp"), 3, 25, 10, 1250);

        env.advanceTime(61 * 1000); // Set the time to 61 seconds
        assertReceivedMinMax(env.listener("rld"), env.listener("rlp"), 12, 12, 13, 13);
    }

    private static void assertReceivedMinMax(SupportListener resultListenerDelta, SupportListener resultListenerProduct, int minDelta, int maxDelta, int minProduct, int maxProduct) {
        assertEquals(1, resultListenerDelta.getNewDataList().size());
        assertEquals(1, resultListenerDelta.getLastNewData().length);
        assertEquals(1, resultListenerProduct.getNewDataList().size());
        assertEquals(1, resultListenerProduct.getLastNewData().length);
        assertEquals(minDelta, resultListenerDelta.getLastNewData()[0].get("minD"));
        assertEquals(maxDelta, resultListenerDelta.getLastNewData()[0].get("maxD"));
        assertEquals(minProduct, resultListenerProduct.getLastNewData()[0].get("minP"));
        assertEquals(maxProduct, resultListenerProduct.getLastNewData()[0].get("maxP"));
        resultListenerDelta.reset();
        resultListenerProduct.reset();
    }

    private static void assertReceivedFeed(SupportListener feedListener, int delta, int product) {
        assertEquals(1, feedListener.getNewDataList().size());
        assertEquals(1, feedListener.getLastNewData().length);
        assertEquals(delta, feedListener.getLastNewData()[0].get("delta"));
        assertEquals(product, feedListener.getLastNewData()[0].get("product"));
        feedListener.reset();
    }

    private static SupportBean sendEvent(RegressionEnvironment env, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString("myId");
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
        return bean;
    }

    private static class EPLInsertIntoTypeMismatchInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // invalid wrapper types
            String epl = "insert into MyStream select * from pattern[a=SupportBean];\n" +
                "insert into MyStream select * from pattern[a=SupportBean_S0];\n";
            tryInvalidCompile(env, epl, "Event type named 'MyStream' has already been declared with differing column name or type information: Type by name 'stmt0_pat_0_0' in property 'a' expected event type 'SupportBean' but receives event type 'SupportBean_S0'");
        }
    }

    private static class EPLInsertIntoMultiBeanToMulti implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') insert into SupportObjectArrayOneDim select window(*) @eventbean as arr from SupportBean#keepall").addListener("s0");
            assertStatelessStmt(env, "s0", false);

            SupportBean e1 = new SupportBean("E1", 1);
            env.sendEventBean(e1);
            SupportObjectArrayOneDim resultOne = (SupportObjectArrayOneDim) env.listener("s0").assertOneGetNewAndReset().getUnderlying();
            EPAssertionUtil.assertEqualsExactOrder(resultOne.getArr(), new Object[]{e1});

            SupportBean e2 = new SupportBean("E2", 2);
            env.sendEventBean(e2);
            SupportObjectArrayOneDim resultTwo = (SupportObjectArrayOneDim) env.listener("s0").assertOneGetNewAndReset().getUnderlying();
            EPAssertionUtil.assertEqualsExactOrder(resultTwo.getArr(), new Object[]{e1, e2});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoSingleBeanToMulti implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema EventOne(sbarr SupportBean[])", path);
            env.compileDeploy("insert into EventOne select maxby(intPrimitive) as sbarr from SupportBean as sb", path);
            env.compileDeploy("@name('s0') select * from EventOne", path).addListener("s0");

            SupportBean bean = new SupportBean("E1", 1);
            env.sendEventBean(bean);
            EventBean[] events = (EventBean[]) env.listener("s0").assertOneGetNewAndReset().get("sbarr");
            assertEquals(1, events.length);
            assertSame(bean, events[0].getUnderlying());

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoAssertionWildcardRecast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // bean to OA/Map/bean
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionWildcardRecast(env, true, null, false, rep);
            }

            try {
                tryAssertionWildcardRecast(env, true, null, true, null);
                fail();
            } catch (RuntimeException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getCause().getMessage(), "Expression-returned event type 'SourceSchema' with underlying type '" + EPLInsertInto.MyP0P1EventSource.class.getName() + "' cannot be converted to target event type 'TargetSchema' with underlying type ");
            }

            // OA
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.OBJECTARRAY, false, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.OBJECTARRAY, false, EventRepresentationChoice.MAP);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.OBJECTARRAY, false, EventRepresentationChoice.AVRO);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.OBJECTARRAY, false, EventRepresentationChoice.JSON);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.OBJECTARRAY, true, null);

            // Map
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.MAP, false, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.MAP, false, EventRepresentationChoice.MAP);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.MAP, false, EventRepresentationChoice.AVRO);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.MAP, false, EventRepresentationChoice.JSON);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.MAP, true, null);

            // Avro
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.MAP);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.AVRO);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.JSON);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, true, null);

            // Json
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSON, false, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSON, false, EventRepresentationChoice.MAP);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSON, false, EventRepresentationChoice.AVRO);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSON, false, EventRepresentationChoice.JSON);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSON, true, null);

            // Json-Provided-Class
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSONCLASSPROVIDED, false, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSONCLASSPROVIDED, false, EventRepresentationChoice.MAP);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSONCLASSPROVIDED, false, EventRepresentationChoice.AVRO);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSONCLASSPROVIDED, false, EventRepresentationChoice.JSONCLASSPROVIDED);
            tryAssertionWildcardRecast(env, false, EventRepresentationChoice.JSONCLASSPROVIDED, true, null);
        }
    }

    private static class EPLInsertIntoJoinWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionJoinWildcard(env, true, null);

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionJoinWildcard(env, false, rep);
            }
        }
    }

    private static class EPLInsertIntoProvidePartitialCols implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String[] fields = "p0,p1".split(",");
            String epl =
                "insert into AStream (p0, p1) select intPrimitive as somename, theString from SupportBean(intPrimitive between 0 and 10);\n" +
                    "insert into AStream (p0) select intPrimitive as somename from SupportBean(intPrimitive > 10);\n" +
                    "@name('s0') select * from AStream;\n";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, null});

            env.sendEventBean(new SupportBean("E2", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5, "E2"});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoWithOutputLimitAndSort implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // NOTICE: we are inserting the RSTREAM (removed events)
            RegressionPath path = new RegressionPath();
            String stmtText = "insert rstream into StockTicks(mySymbol, myPrice) " +
                "select symbol, price from SupportMarketDataBean#time(60) " +
                "output every 5 seconds " +
                "order by symbol asc";
            env.compileDeploy(stmtText, path);

            stmtText = "@name('s0') select mySymbol, sum(myPrice) as pricesum from StockTicks#length(100)";
            env.compileDeploy(stmtText, path).addListener("s0");

            env.advanceTime(0);
            sendEvent(env, "IBM", 50);
            sendEvent(env, "CSC", 10);
            sendEvent(env, "GE", 20);
            env.advanceTime(10 * 1000);
            sendEvent(env, "DEF", 100);
            sendEvent(env, "ABC", 11);
            env.advanceTime(20 * 1000);
            env.advanceTime(30 * 1000);
            env.advanceTime(40 * 1000);
            env.advanceTime(50 * 1000);
            env.advanceTime(55 * 1000);

            assertFalse(env.listener("s0").isInvoked());
            env.advanceTime(60 * 1000);

            assertTrue(env.listener("s0").isInvoked());
            assertEquals(3, env.listener("s0").getNewDataList().size());
            assertEquals("CSC", env.listener("s0").getNewDataList().get(0)[0].get("mySymbol"));
            assertEquals(10.0, env.listener("s0").getNewDataList().get(0)[0].get("pricesum"));
            assertEquals("GE", env.listener("s0").getNewDataList().get(1)[0].get("mySymbol"));
            assertEquals(30.0, env.listener("s0").getNewDataList().get(1)[0].get("pricesum"));
            assertEquals("IBM", env.listener("s0").getNewDataList().get(2)[0].get("mySymbol"));
            assertEquals(80.0, env.listener("s0").getNewDataList().get(2)[0].get("pricesum"));
            env.listener("s0").reset();

            env.advanceTime(65 * 1000);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(70 * 1000);
            assertEquals("ABC", env.listener("s0").getNewDataList().get(0)[0].get("mySymbol"));
            assertEquals(91.0, env.listener("s0").getNewDataList().get(0)[0].get("pricesum"));
            assertEquals("DEF", env.listener("s0").getNewDataList().get(1)[0].get("mySymbol"));
            assertEquals(191.0, env.listener("s0").getNewDataList().get(1)[0].get("pricesum"));

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoStaggeredWithWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementOne = "@name('i0') insert into streamA select * from SupportBeanSimple#length(5)";
            String statementTwo = "@name('i1') insert into streamB select *, myInt+myInt as summed, myString||myString as concat from streamA#length(5)";
            String statementThree = "@name('i2') insert into streamC select * from streamB#length(5)";

            // try one module
            String epl = statementOne + ";\n" + statementTwo + ";\n" + statementThree + ";\n";
            env.compileDeploy(epl);
            assertEvents(env);
            env.undeployAll();

            // try multiple modules
            RegressionPath path = new RegressionPath();
            env.compileDeploy(statementOne, path);
            env.compileDeploy(statementTwo, path);
            env.compileDeploy(statementThree, path);
            assertEvents(env);
            env.undeployAll();
        }

        private void assertEvents(RegressionEnvironment env) {
            env.addListener("i0").addListener("i1").addListener("i2");

            sendSimpleEvent(env, "one", 1);
            assertSimple(env.listener("i0"), "one", 1, null, 0);
            assertSimple(env.listener("i1"), "one", 1, "oneone", 2);
            assertSimple(env.listener("i2"), "one", 1, "oneone", 2);

            sendSimpleEvent(env, "two", 2);
            assertSimple(env.listener("i0"), "two", 2, null, 0);
            assertSimple(env.listener("i1"), "two", 2, "twotwo", 4);
            assertSimple(env.listener("i2"), "two", 2, "twotwo", 4);
        }
    }

    private static class EPLInsertIntoInsertFromPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOneText = "@name('i0') insert into streamA1 select * from pattern [every SupportBean]";
            env.compileDeploy(stmtOneText, path).addListener("i0");

            String stmtTwoText = "@name('i1') insert into streamA1 select * from pattern [every SupportBean]";
            env.compileDeploy(stmtTwoText, path).addListener("i1");

            EventType eventType = env.statement("i0").getEventType();
            assertEquals(Map.class, eventType.getUnderlyingType());

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoInsertIntoPlusPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOneTxt = "@name('s1') insert into InZone " +
                "select 111 as statementId, mac, locationReportId " +
                "from SupportRFIDEvent " +
                "where mac in ('1','2','3') " +
                "and zoneID = '10'";
            env.compileDeploy(stmtOneTxt, path).addListener("s1");

            String stmtTwoTxt = "@name('s2') insert into OutOfZone " +
                "select 111 as statementId, mac, locationReportId " +
                "from SupportRFIDEvent " +
                "where mac in ('1','2','3') " +
                "and zoneID != '10'";
            env.compileDeploy(stmtTwoTxt, path).addListener("s2");

            String stmtThreeTxt = "@name('s3') select 111 as eventSpecId, A.locationReportId as locationReportId " +
                " from pattern [every A=InZone -> (timer:interval(1 sec) and not OutOfZone(mac=A.mac))]";
            env.compileDeploy(stmtThreeTxt, path).addListener("s3");

            // try the alert case with 1 event for the mac in question
            env.advanceTime(0);
            env.sendEventBean(new SupportRFIDEvent("LR1", "1", "10"));
            assertFalse(env.listener("s3").isInvoked());
            env.advanceTime(1000);

            EventBean theEvent = env.listener("s3").assertOneGetNewAndReset();
            assertEquals("LR1", theEvent.get("locationReportId"));

            env.listener("s1").reset();
            env.listener("s2").reset();

            // try the alert case with 2 events for zone 10 within 1 second for the mac in question
            env.sendEventBean(new SupportRFIDEvent("LR2", "2", "10"));
            assertFalse(env.listener("s3").isInvoked());
            env.advanceTime(1500);
            env.sendEventBean(new SupportRFIDEvent("LR3", "2", "10"));
            assertFalse(env.listener("s3").isInvoked());
            env.advanceTime(2000);

            theEvent = env.listener("s3").assertOneGetNewAndReset();
            assertEquals("LR2", theEvent.get("locationReportId"));

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNullType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOneTxt = "@name('s1') insert into InZoneTwo select null as dummy from SupportBean";
            env.compileDeploy(stmtOneTxt, path);
            assertTrue(env.statement("s1").getEventType().isProperty("dummy"));

            String stmtTwoTxt = "@name('s2') select dummy from InZoneTwo";
            env.compileDeploy(stmtTwoTxt, path).addListener("s2");

            env.sendEventBean(new SupportBean());
            assertNull(env.listener("s2").assertOneGetNewAndReset().get("dummy"));

            env.undeployAll();
        }
    }

    public static class EPLInsertIntoChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text = "insert into S0 select irstream symbol, 0 as val from SupportMarketDataBean";
            env.compileDeploy(text, path);

            env.milestone(0);

            text = "insert into S1 select irstream symbol, 1 as val from S0";
            env.compileDeploy(text, path);

            env.milestone(1);

            text = "insert into S2 select irstream symbol, 2 as val from S1";
            env.compileDeploy(text, path);

            env.milestone(2);

            text = "@name('s0') insert into S3 select irstream symbol, 3 as val from S2";
            env.compileDeploy(text, path).addListener("s0");

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.listener("s0").assertNewOldData(new Object[][]{{"symbol", "E1"}, {"val", 3}}, null);

            env.undeployAll();
        }
    }

    private static void assertSimple(SupportListener listener, String myString, int myInt, String additionalString, int additionalInt) {
        assertTrue(listener.getAndClearIsInvoked());
        EventBean eventBean = listener.getLastNewData()[0];
        assertEquals(myString, eventBean.get("myString"));
        assertEquals(myInt, eventBean.get("myInt"));
        if (additionalString != null) {
            assertEquals(additionalString, eventBean.get("concat"));
            assertEquals(additionalInt, eventBean.get("summed"));
        }
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, null, null);
        env.sendEventBean(bean);
    }

    private static void sendSimpleEvent(RegressionEnvironment env, String theString, int val) {
        env.sendEventBean(new SupportBeanSimple(theString, val));
    }

    private static void assertJoinWildcard(EventRepresentationChoice rep, SupportListener listener, Object eventS0, Object eventS1) {
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2, listener.getLastNewData()[0].getEventType().getPropertyNames().length);
        assertTrue(listener.getLastNewData()[0].getEventType().isProperty("s0"));
        assertTrue(listener.getLastNewData()[0].getEventType().isProperty("s1"));
        if (rep != null && (rep.isJsonEvent() || rep.isJsonProvidedClassEvent())) {
            assertEquals(eventS0, listener.getLastNewData()[0].get("s0").toString());
            assertEquals(eventS1, listener.getLastNewData()[0].get("s1").toString());
        } else {
            assertSame(eventS0, listener.getLastNewData()[0].get("s0"));
            assertSame(eventS1, listener.getLastNewData()[0].get("s1"));
        }
        assertTrue(rep == null || rep.matchesClass(listener.getLastNewData()[0].getUnderlying().getClass()));
    }

    private static void tryAssertionJoinWildcard(RegressionEnvironment env, boolean bean, EventRepresentationChoice rep) {
        String schema;
        if (bean) {
            schema = "@name('schema1') create schema S0 as " + SupportBean.class.getName() + ";\n" +
                "@name('schema2') create schema S1 as " + SupportBean_A.class.getName() + ";\n";
        } else if (rep.isMapEvent()) {
            schema = "@name('schema1') create map schema S0 as (theString string);\n" +
                "@name('schema2') create map schema S1 as (id string);\n";
        } else if (rep.isObjectArrayEvent()) {
            schema = "@name('schema1') create objectarray schema S0 as (theString string);\n" +
                "@name('schema2') create objectarray schema S1 as (id string);\n";
        } else if (rep.isAvroEvent()) {
            schema = "@name('schema1') create avro schema S0 as (theString string);\n" +
                "@name('schema2') create avro schema S1 as (id string);\n";
        } else if (rep.isJsonEvent()) {
            schema = "@name('schema1') create json schema S0 as (theString string);\n" +
                "@name('schema2') create json schema S1 as (id string);\n";
        } else if (rep.isJsonProvidedClassEvent()) {
            schema = "@name('schema1') @JsonSchema(className='" + MyLocalJsonProvidedS0.class.getName() + "') create json schema S0 as ();\n" +
                "@name('schema2') @JsonSchema(className='" + MyLocalJsonProvidedS1.class.getName() + "') create json schema S1 as ();\n";
        } else {
            schema = null;
            fail();
        }

        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType(schema, path);

        String textOne = "@name('s1') " + (bean ? "" : rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedJoin.class)) + "insert into event2 select * " +
            "from S0#length(100) as s0, S1#length(5) as s1 " +
            "where s0.theString = s1.id";
        env.compileDeploy(textOne, path).addListener("s1");

        String textTwo = "@name('s2') " + (bean ? "" : rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedJoin.class)) + "select * from event2#length(10)";
        env.compileDeploy(textTwo, path).addListener("s2");

        // send event for joins to match on
        Object eventS1;
        if (bean) {
            eventS1 = new SupportBean_A("myId");
            env.sendEventBean(eventS1, "S1");
        } else if (rep.isMapEvent()) {
            eventS1 = Collections.singletonMap("id", "myId");
            env.sendEventMap((Map) eventS1, "S1");
        } else if (rep.isObjectArrayEvent()) {
            eventS1 = new Object[]{"myId"};
            env.sendEventObjectArray((Object[]) eventS1, "S1");
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(env.deploymentId("schema1"), "S1")));
            theEvent.put("id", "myId");
            eventS1 = theEvent;
            env.sendEventAvro(theEvent, "S1");
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("id", "myId");
            eventS1 = object.toString();
            env.sendEventJson((String) eventS1, "S1");
        } else {
            throw new IllegalArgumentException();
        }

        Object eventS0;
        if (bean) {
            eventS0 = new SupportBean("myId", -1);
            env.sendEventBean(eventS0, "S0");
        } else if (rep.isMapEvent()) {
            eventS0 = Collections.singletonMap("theString", "myId");
            env.sendEventMap((Map) eventS0, "S0");
        } else if (rep.isObjectArrayEvent()) {
            eventS0 = new Object[]{"myId"};
            env.sendEventObjectArray((Object[]) eventS0, "S0");
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventType(env.deploymentId("schema1"), "S0")));
            theEvent.put("theString", "myId");
            eventS0 = theEvent;
            env.sendEventAvro(theEvent, "S0");
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("theString", "myId");
            eventS0 = object.toString();
            env.sendEventJson((String) eventS0, "S0");
        } else {
            throw new IllegalArgumentException();
        }

        assertJoinWildcard(rep, env.listener("s1"), eventS0, eventS1);
        assertJoinWildcard(rep, env.listener("s2"), eventS0, eventS1);

        env.undeployAll();
    }

    private static void tryAssertionWildcardRecast(RegressionEnvironment env, boolean sourceBean, EventRepresentationChoice sourceType,
                                                   boolean targetBean, EventRepresentationChoice targetType) {
        try {
            tryAssertionWildcardRecastInternal(env, sourceBean, sourceType, targetBean, targetType);
        } finally {
            // cleanup
            env.undeployAll();
        }
    }

    private static void tryAssertionWildcardRecastInternal(RegressionEnvironment env, boolean sourceBean, EventRepresentationChoice sourceType,
                                                           boolean targetBean, EventRepresentationChoice targetType) {
        // declare source type
        String schemaEPL;
        if (sourceBean) {
            schemaEPL = "create schema SourceSchema as " + MyP0P1EventSource.class.getName();
        } else {
            schemaEPL = sourceType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSourceSchema.class) + "create schema SourceSchema as (p0 string, p1 int)";
        }
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType(schemaEPL, path);

        // declare target type
        if (targetBean) {
            env.compileDeploy("create schema TargetSchema as " + MyP0P1EventTarget.class.getName(), path);
        } else {
            env.compileDeploy(targetType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTargetContainedSchema.class) + "create schema TargetContainedSchema as (c0 int)", path);
            env.compileDeploy(targetType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTargetSchema.class) + "create schema TargetSchema (p0 string, p1 int, c0 TargetContainedSchema)", path);
        }

        // insert-into and select
        env.compileDeploy("insert into TargetSchema select * from SourceSchema", path);
        env.compileDeploy("@name('s0') select * from TargetSchema", path).addListener("s0");

        // send event
        if (sourceBean) {
            env.sendEventBean(new MyP0P1EventSource("a", 10), "SourceSchema");
        } else if (sourceType.isMapEvent()) {
            Map<String, Object> map = new HashMap<>();
            map.put("p0", "a");
            map.put("p1", 10);
            env.sendEventMap(map, "SourceSchema");
        } else if (sourceType.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{"a", 10}, "SourceSchema");
        } else if (sourceType.isAvroEvent()) {
            Schema schema = record("schema").fields().requiredString("p0").requiredString("p1").requiredString("c0").endRecord();
            GenericData.Record record = new GenericData.Record(schema);
            record.put("p0", "a");
            record.put("p1", 10);
            env.sendEventAvro(record, "SourceSchema");
        } else if (sourceType.isJsonEvent() || sourceType.isJsonProvidedClassEvent()) {
            env.sendEventJson("{\"p0\": \"a\", \"p1\": 10}", "SourceSchema");
        } else {
            fail();
        }

        // assert
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "p0,p1,c0".split(","), new Object[]{"a", 10, null});

        env.undeployAll();
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    public static class MyP0P1EventSource {
        private final String p0;
        private final int p1;

        private MyP0P1EventSource(String p0, int p1) {
            this.p0 = p0;
            this.p1 = p1;
        }

        public String getP0() {
            return p0;
        }

        public int getP1() {
            return p1;
        }
    }

    public static class MyP0P1EventTarget {
        private String p0;
        private int p1;
        private Object c0;

        private MyP0P1EventTarget() {
        }

        private MyP0P1EventTarget(String p0, int p1, Object c0) {
            this.p0 = p0;
            this.p1 = p1;
            this.c0 = c0;
        }

        public String getP0() {
            return p0;
        }

        public void setP0(String p0) {
            this.p0 = p0;
        }

        public int getP1() {
            return p1;
        }

        public void setP1(int p1) {
            this.p1 = p1;
        }

        public Object getC0() {
            return c0;
        }

        public void setC0(Object c0) {
            this.c0 = c0;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public String theString;
        public Integer intPrimitive;
    }

    public static class MyLocalJsonProvidedS0 implements Serializable {
        public String theString;

        public String toString() {
            return "{\"theString\":\"" + theString + "\"}";
        }
    }

    public static class MyLocalJsonProvidedS1 implements Serializable {
        public String id;

        public String toString() {
            return "{\"id\":\"" + id + "\"}";
        }
    }

    public static class MyLocalJsonProvidedJoin implements Serializable {
        public MyLocalJsonProvidedS0 s0;
        public MyLocalJsonProvidedS1 s1;
    }

    public static class MyLocalJsonProvidedSourceSchema implements Serializable {
        public String p0;
        public int p1;
    }

    public static class MyLocalJsonProvidedTargetContainedSchema implements Serializable {
        public int c0;
    }

    public static class MyLocalJsonProvidedTargetSchema implements Serializable {
        public String p0;
        public int p1;
        public MyLocalJsonProvidedTargetContainedSchema c0;
    }
}
