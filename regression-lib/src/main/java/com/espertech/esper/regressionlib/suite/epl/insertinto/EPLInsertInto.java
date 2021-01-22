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
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.*;
import com.espertech.esper.regressionlib.support.bean.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

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
        env.assertEventNew("s0", event -> assertion.accept(event.getUnderlying()));

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
            model.setAnnotations(Collections.singletonList(new AnnotationPart("public")));
            model.setInsertInto(InsertIntoClause.create("Event_1_OMS", "delta", "product"));
            model.setSelectClause(SelectClause.create().add(Expressions.minus("intPrimitive", "intBoxed"), "deltaTag")
                .add(Expressions.multiply("intPrimitive", "intBoxed"), "productTag"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView(View.create("length", Expressions.constant(100)))));
            model = SerializableObjectCopier.copyMayFail(model);

            tryAssertsVariant(env, null, model, "Event_1_OMS");

            String epl = "@name('fl') @public insert into Event_1_OMS(delta, product) " +
                "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                "from SupportBean#length(100)";
            assertEquals(epl, model.toEPL());
            env.assertStatement("fl", statement -> assertEquals(epl, statement.getProperty(StatementProperty.EPL)));

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedColsEPLToOMStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('fl') @public insert into Event_1_EPL(delta, product) " +
                "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                "from SupportBean#length(100)";

            EPStatementObjectModel model = env.eplToModel(epl);
            model = SerializableObjectCopier.copyMayFail(model);
            assertEquals(epl, model.toEPL());

            tryAssertsVariant(env, null, model, "Event_1_EPL");
            env.assertStatement("fl", statement -> assertEquals(epl, statement.getProperty(StatementProperty.EPL)));
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
            env.tryInvalidCompile(stmtText, "Wildcard not allowed when insert-into specifies column order");

            // test insert wildcard to wildcard
            String stmtSelectText = "@name('i0') insert into ABCStream select * from SupportBean";
            env.compileDeploy(stmtSelectText).addListener("i0");
            env.assertStatement("i0", statement -> assertTrue(statement.getEventType() instanceof BeanEventType));

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertListener("i0", listener -> {
                assertEquals("E1", listener.assertOneGetNew().get("theString"));
                assertTrue(listener.assertOneGetNew().getUnderlying() instanceof SupportBean);
            });

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

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
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
            String stmtText = "@name('stmt1') @public insert into event1 select * from SupportBean#length(100)";
            String otherText = "@name('stmt2') select * from event1#length(10)";

            // Attach listener to feed
            env.compileDeploy(stmtText, path).addListener("stmt1");
            env.compileDeploy(otherText, path).addListener("stmt2");

            SupportBean theEvent = sendEvent(env, 10, 11);
            env.assertListener("stmt1", listener -> {
                assertTrue(listener.getAndClearIsInvoked());
                assertEquals(1, listener.getLastNewData().length);
                assertEquals(10, listener.getLastNewData()[0].get("intPrimitive"));
                assertEquals(11, listener.getLastNewData()[0].get("intBoxed"));
                assertEquals(20, listener.getLastNewData()[0].getEventType().getPropertyNames().length);
                assertSame(theEvent, listener.getLastNewData()[0].getUnderlying());
            });

            env.assertListener("stmt2", listener -> {
                assertTrue(listener.getAndClearIsInvoked());
                assertEquals(1, listener.getLastNewData().length);
                assertEquals(10, listener.getLastNewData()[0].get("intPrimitive"));
                assertEquals(11, listener.getLastNewData()[0].get("intBoxed"));
                assertEquals(20, listener.getLastNewData()[0].getEventType().getPropertyNames().length);
                assertSame(theEvent, listener.getLastNewData()[0].getUnderlying());
            });

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
            env.assertStatement("fl", statement -> {
                EventType type = statement.getEventType();
                assertEquals(NameAccessModifier.PUBLIC, type.getMetadata().getAccessModifier());
                assertEquals(EventTypeTypeClass.STREAM, type.getMetadata().getTypeClass());
                assertEquals(EventTypeApplicationType.MAP, type.getMetadata().getApplicationType());
                assertEquals("Event_1_2J", type.getMetadata().getName());
                assertEquals(EventTypeBusModifier.NONBUS, type.getMetadata().getBusModifier());
            });

            env.undeployAll();
        }
    }

    private static void tryAssertsVariant(RegressionEnvironment env, String stmtText, EPStatementObjectModel model, String typeName) {
        RegressionPath path = new RegressionPath();
        // Attach listener to feed
        if (model != null) {
            model.setAnnotations(Arrays.asList(AnnotationPart.nameAnnotation("fl"), new AnnotationPart("public")));
            env.compileDeploy(model, path);
        } else {
            env.compileDeploy("@name('fl') @public " + stmtText, path);
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
        assertReceivedFeed(env, 10, 200);
        assertReceivedMinMax(env, 10, 10, 200, 200);

        sendEvent(env, 50, 25);
        assertReceivedFeed(env, 25, 25 * 50);
        assertReceivedMinMax(env, 10, 25, 200, 1250);

        sendEvent(env, 5, 2);
        assertReceivedFeed(env, 3, 2 * 5);
        assertReceivedMinMax(env, 3, 25, 10, 1250);

        env.advanceTime(10 * 1000); // Set the time to 10 seconds

        sendEvent(env, 13, 1);
        assertReceivedFeed(env, 12, 13);
        assertReceivedMinMax(env, 3, 25, 10, 1250);

        env.advanceTime(61 * 1000); // Set the time to 61 seconds
        assertReceivedMinMax(env, 12, 12, 13, 13);
    }

    private static void assertReceivedMinMax(RegressionEnvironment env, int minDelta, int maxDelta, int minProduct, int maxProduct) {
        env.assertListener("rld", listener -> {
            assertEquals(1, listener.getNewDataList().size());
            assertEquals(1, listener.getLastNewData().length);
            assertEquals(minDelta, listener.getLastNewData()[0].get("minD"));
            assertEquals(maxDelta, listener.getLastNewData()[0].get("maxD"));
            listener.reset();
        });
        env.assertListener("rlp", listener -> {
            assertEquals(1, listener.getNewDataList().size());
            assertEquals(1, listener.getLastNewData().length);
            assertEquals(minProduct, listener.getLastNewData()[0].get("minP"));
            assertEquals(maxProduct, listener.getLastNewData()[0].get("maxP"));
            listener.reset();
        });
    }

    private static void assertReceivedFeed(RegressionEnvironment env, int delta, int product) {
        env.assertListener("fl", listener -> {
            assertEquals(1, listener.getNewDataList().size());
            assertEquals(1, listener.getLastNewData().length);
            assertEquals(delta, listener.getLastNewData()[0].get("delta"));
            assertEquals(product, listener.getLastNewData()[0].get("product"));
            listener.reset();
        });
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
            env.tryInvalidCompile(epl, "Event type named 'MyStream' has already been declared with differing column name or type information: Type by name 'stmt0_pat_0_0' in property 'a' expected event type 'SupportBean' but receives event type 'SupportBean_S0'");
        }
    }

    private static class EPLInsertIntoMultiBeanToMulti implements RegressionExecution {
        @Override
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED);
        }

        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') insert into SupportObjectArrayOneDim select window(*) @eventbean as arr from SupportBean#keepall").addListener("s0");
            assertStatelessStmt(env, "s0", false);

            SupportBean e1 = new SupportBean("E1", 1);
            env.sendEventBean(e1);
            env.assertEventNew("s0", event -> {
                SupportObjectArrayOneDim resultOne = (SupportObjectArrayOneDim) event.getUnderlying();
                EPAssertionUtil.assertEqualsExactOrder(resultOne.getArr(), new Object[]{e1});
            });

            SupportBean e2 = new SupportBean("E2", 2);
            env.sendEventBean(e2);
            env.assertEventNew("s0", event -> {
                SupportObjectArrayOneDim resultTwo = (SupportObjectArrayOneDim) event.getUnderlying();
                EPAssertionUtil.assertEqualsExactOrder(resultTwo.getArr(), new Object[]{e1, e2});
            });

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoAssertionWildcardRecast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // bean to OA/Map/bean
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionWildcardRecast(env, true, null, false, rep);
            }

            env.assertThat(() -> {
                try {
                    tryAssertionWildcardRecast(env, true, null, true, null);
                    fail();
                } catch (RuntimeException ex) {
                    SupportMessageAssertUtil.assertMessage(ex.getCause().getMessage(), "Expression-returned event type 'SourceSchema' with underlying type '" + EPLInsertInto.MyP0P1EventSource.class.getName() + "' cannot be converted to target event type 'TargetSchema' with underlying type ");
                }
            });

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
            env.assertThat(() -> {
                tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.OBJECTARRAY);
                tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.MAP);
                tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.AVRO);
                tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.JSON);
                tryAssertionWildcardRecast(env, false, EventRepresentationChoice.AVRO, true, null);
            });

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
            env.assertPropsNew("s0", fields, new Object[]{20, null});

            env.sendEventBean(new SupportBean("E2", 5));
            env.assertPropsNew("s0", fields, new Object[]{5, "E2"});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoWithOutputLimitAndSort implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // NOTICE: we are inserting the RSTREAM (removed events)
            RegressionPath path = new RegressionPath();
            String stmtText = "@public insert rstream into StockTicks(mySymbol, myPrice) " +
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

            env.assertListenerNotInvoked("s0");
            env.advanceTime(60 * 1000);

            env.assertListener("s0", listener -> {
                assertTrue(listener.isInvoked());
                assertEquals(3, listener.getNewDataList().size());
                assertEquals("CSC", listener.getNewDataList().get(0)[0].get("mySymbol"));
                assertEquals(10.0, listener.getNewDataList().get(0)[0].get("pricesum"));
                assertEquals("GE", listener.getNewDataList().get(1)[0].get("mySymbol"));
                assertEquals(30.0, listener.getNewDataList().get(1)[0].get("pricesum"));
                assertEquals("IBM", listener.getNewDataList().get(2)[0].get("mySymbol"));
                assertEquals(80.0, listener.getNewDataList().get(2)[0].get("pricesum"));
                listener.reset();
            });

            env.advanceTime(65 * 1000);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(70 * 1000);
            env.assertListener("s0", listener -> {
                assertEquals("ABC", listener.getNewDataList().get(0)[0].get("mySymbol"));
                assertEquals(91.0, listener.getNewDataList().get(0)[0].get("pricesum"));
                assertEquals("DEF", listener.getNewDataList().get(1)[0].get("mySymbol"));
                assertEquals(191.0, listener.getNewDataList().get(1)[0].get("pricesum"));
            });

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoStaggeredWithWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementOne = "@name('i0') @public insert into streamA select * from SupportBeanSimple#length(5)";
            String statementTwo = "@name('i1') @public insert into streamB select *, myInt+myInt as summed, myString||myString as concat from streamA#length(5)";
            String statementThree = "@name('i2') @public insert into streamC select * from streamB#length(5)";

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
            assertSimple(env, "i0", "one", 1, null, 0);
            assertSimple(env, "i1", "one", 1, "oneone", 2);
            assertSimple(env, "i2", "one", 1, "oneone", 2);

            sendSimpleEvent(env, "two", 2);
            assertSimple(env, "i0", "two", 2, null, 0);
            assertSimple(env, "i1", "two", 2, "twotwo", 4);
            assertSimple(env, "i2", "two", 2, "twotwo", 4);
        }
    }

    private static class EPLInsertIntoInsertFromPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOneText = "@name('i0') insert into streamA1 select * from pattern [every SupportBean]";
            env.compileDeploy(stmtOneText, path).addListener("i0");

            String stmtTwoText = "@name('i1') insert into streamA1 select * from pattern [every SupportBean]";
            env.compileDeploy(stmtTwoText, path).addListener("i1");

            env.assertStatement("i0", statement -> {
                EventType eventType = statement.getEventType();
                assertEquals(Map.class, eventType.getUnderlyingType());
            });

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoInsertIntoPlusPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOneTxt = "@name('s1') @public insert into InZone " +
                "select 111 as statementId, mac, locationReportId " +
                "from SupportRFIDEvent " +
                "where mac in ('1','2','3') " +
                "and zoneID = '10'";
            env.compileDeploy(stmtOneTxt, path).addListener("s1");

            String stmtTwoTxt = "@name('s2') @public insert into OutOfZone " +
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
            env.assertListenerNotInvoked("s3");
            env.advanceTime(1000);

            env.assertEqualsNew("s3", "locationReportId", "LR1");
            env.listenerReset("s1");
            env.listenerReset("s2");

            // try the alert case with 2 events for zone 10 within 1 second for the mac in question
            env.sendEventBean(new SupportRFIDEvent("LR2", "2", "10"));
            env.assertListenerNotInvoked("s3");

            env.advanceTime(1500);
            env.sendEventBean(new SupportRFIDEvent("LR3", "2", "10"));
            env.assertListenerNotInvoked("s3");

            env.advanceTime(2000);

            env.assertEqualsNew("s3", "locationReportId", "LR2");

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNullType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOneTxt = "@name('s1') @public insert into InZoneTwo select null as dummy from SupportBean";
            env.compileDeploy(stmtOneTxt, path);
            env.assertStatement("s1", statement -> assertNullTypeForDummyField(statement.getEventType()));

            String stmtTwoTxt = "@name('s2') select dummy from InZoneTwo";
            env.compileDeploy(stmtTwoTxt, path).addListener("s2");
            env.assertStatement("s2", statement -> assertNullTypeForDummyField(statement.getEventType()));

            env.sendEventBean(new SupportBean());
            env.assertEqualsNew("s2", "dummy", null);

            env.undeployAll();
        }

        private void assertNullTypeForDummyField(EventType eventType) {
            String fieldName = "dummy";
            assertTrue(eventType.isProperty(fieldName));
            assertNull(eventType.getPropertyType(fieldName));
            assertSame(EPTypeNull.INSTANCE, eventType.getPropertyEPType(fieldName));
            EventPropertyDescriptor desc = eventType.getPropertyDescriptor(fieldName);
            SupportEventPropUtil.assertPropEquals(new SupportEventPropDesc(fieldName, EPTypeNull.INSTANCE), desc);
        }
    }

    public static class EPLInsertIntoChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text = "@public insert into S0 select irstream symbol, 0 as val from SupportMarketDataBean";
            env.compileDeploy(text, path);

            env.milestone(0);

            text = "@public insert into S1 select irstream symbol, 1 as val from S0";
            env.compileDeploy(text, path);

            env.milestone(1);

            text = "@public insert into S2 select irstream symbol, 2 as val from S1";
            env.compileDeploy(text, path);

            env.milestone(2);

            text = "@name('s0') insert into S3 select irstream symbol, 3 as val from S2";
            env.compileDeploy(text, path).addListener("s0");

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.assertPropsNV("s0", new Object[][]{{"symbol", "E1"}, {"val", 3}}, null);

            env.undeployAll();
        }
    }

    private static void assertSimple(RegressionEnvironment env, String stmtName, String myString, int myInt, String additionalString, int additionalInt) {
        env.assertListener(stmtName, listener -> {
            assertTrue(listener.getAndClearIsInvoked());
            EventBean eventBean = listener.getLastNewData()[0];
            assertEquals(myString, eventBean.get("myString"));
            assertEquals(myInt, eventBean.get("myInt"));
            if (additionalString != null) {
                assertEquals(additionalString, eventBean.get("concat"));
                assertEquals(additionalInt, eventBean.get("summed"));
            }
        });
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, null, null);
        env.sendEventBean(bean);
    }

    private static void sendSimpleEvent(RegressionEnvironment env, String theString, int val) {
        env.sendEventBean(new SupportBeanSimple(theString, val));
    }

    private static void assertJoinWildcard(RegressionEnvironment env, String statementName, EventRepresentationChoice rep, Object eventS0, Object eventS1) {
        env.assertListener(statementName, listener -> {
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
        });
    }

    private static void tryAssertionJoinWildcard(RegressionEnvironment env, boolean bean, EventRepresentationChoice rep) {
        String schema;
        if (bean) {
            schema = "@name('schema1') @buseventtype @public create schema S0 as " + SupportBean.class.getName() + ";\n" +
                "@name('schema2') @buseventtype @public create schema S1 as " + SupportBean_A.class.getName() + ";\n";
        } else if (rep.isMapEvent()) {
            schema = "@name('schema1') @buseventtype @public create map schema S0 as (theString string);\n" +
                "@name('schema2') @buseventtype @public create map schema S1 as (id string);\n";
        } else if (rep.isObjectArrayEvent()) {
            schema = "@name('schema1') @buseventtype @public create objectarray schema S0 as (theString string);\n" +
                "@name('schema2') @buseventtype @public create objectarray schema S1 as (id string);\n";
        } else if (rep.isAvroEvent()) {
            schema = "@name('schema1') @buseventtype @public create avro schema S0 as (theString string);\n" +
                "@name('schema2') @buseventtype @public create avro schema S1 as (id string);\n";
        } else if (rep.isJsonEvent()) {
            schema = "@name('schema1') @buseventtype @public create json schema S0 as (theString string);\n" +
                "@name('schema2') @buseventtype @public create json schema S1 as (id string);\n";
        } else if (rep.isJsonProvidedClassEvent()) {
            schema = "@name('schema1') @buseventtype @public @JsonSchema(className='" + MyLocalJsonProvidedS0.class.getName() + "') create json schema S0 as ();\n" +
                "@name('schema2') @buseventtype @public @JsonSchema(className='" + MyLocalJsonProvidedS1.class.getName() + "') create json schema S1 as ();\n";
        } else {
            schema = null;
            fail();
        }

        RegressionPath path = new RegressionPath();
        env.compileDeploy(schema, path);

        String textOne = "@name('s1') @public " + (bean ? "" : rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedJoin.class)) + "insert into event2 select * " +
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
            Schema schemaAvro = env.runtimeAvroSchemaByDeployment("schema1", "S1");
            GenericData.Record theEvent = new GenericData.Record(schemaAvro);
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
            Schema schemaAvro = env.runtimeAvroSchemaByDeployment("schema1", "S0");
            GenericData.Record theEvent = new GenericData.Record(schemaAvro);
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

        assertJoinWildcard(env, "s1", rep, eventS0, eventS1);
        assertJoinWildcard(env, "s2", rep, eventS0, eventS1);

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
            schemaEPL = "@buseventtype @public create schema SourceSchema as " + MyP0P1EventSource.class.getName();
        } else {
            schemaEPL = sourceType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSourceSchema.class) + "@buseventtype @public create schema SourceSchema as (p0 string, p1 int)";
        }
        RegressionPath path = new RegressionPath();
        env.compileDeploy(schemaEPL, path);

        // declare target type
        if (targetBean) {
            env.compileDeploy("@public create schema TargetSchema as " + MyP0P1EventTarget.class.getName(), path);
        } else {
            env.compileDeploy(targetType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTargetContainedSchema.class) + "@public create schema TargetContainedSchema as (c0 int)", path);
            env.compileDeploy(targetType.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTargetSchema.class) + "@public create schema TargetSchema (p0 string, p1 int, c0 TargetContainedSchema)", path);
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
            Schema schema = record("schema").fields().requiredString("p0").requiredInt("p1").optionalString("c0").endRecord();
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
        env.assertEventNew("s0", event -> EPAssertionUtil.assertProps(event, "p0,p1,c0".split(","), new Object[]{"a", 10, null}));

        env.undeployAll();
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyP0P1EventSource implements Serializable {
        private static final long serialVersionUID = 1312655941867865316L;
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

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyP0P1EventTarget implements Serializable {
        private static final long serialVersionUID = 6493236300529060319L;
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

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 8476510089254496176L;
        public String theString;
        public Integer intPrimitive;
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyLocalJsonProvidedS0 implements Serializable {
        private static final long serialVersionUID = -1029029982135418942L;
        public String theString;

        public String toString() {
            return "{\"theString\":\"" + theString + "\"}";
        }
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyLocalJsonProvidedS1 implements Serializable {
        private static final long serialVersionUID = -1403873326379557908L;
        public String id;

        public String toString() {
            return "{\"id\":\"" + id + "\"}";
        }
    }

    public static class MyLocalJsonProvidedJoin implements Serializable {
        private static final long serialVersionUID = -5452770440299300732L;
        public MyLocalJsonProvidedS0 s0;
        public MyLocalJsonProvidedS1 s1;
    }

    public static class MyLocalJsonProvidedSourceSchema implements Serializable {
        private static final long serialVersionUID = -7342903914434088805L;
        public String p0;
        public int p1;
    }

    public static class MyLocalJsonProvidedTargetContainedSchema implements Serializable {
        private static final long serialVersionUID = -1369792710182100703L;
        public int c0;
    }

    public static class MyLocalJsonProvidedTargetSchema implements Serializable {
        private static final long serialVersionUID = 6224013232801311642L;
        public String p0;
        public int p1;
        public MyLocalJsonProvidedTargetContainedSchema c0;
    }
}
