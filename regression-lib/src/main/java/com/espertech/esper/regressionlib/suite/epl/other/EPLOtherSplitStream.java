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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bookexample.OrderBeanFactory;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;

public class EPLOtherSplitStream {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherSplitStream2SplitNoDefaultOutputFirst());
        execs.add(new EPLOtherSplitStreamInvalid());
        execs.add(new EPLOtherSplitStreamFromClause());
        execs.add(new EPLOtherSplitStreamSplitPremptiveNamedWindow());
        execs.add(new EPLOtherSplitStream1SplitDefault());
        execs.add(new EPLOtherSplitStreamSubquery());
        execs.add(new EPLOtherSplitStream2SplitNoDefaultOutputAll());
        execs.add(new EPLOtherSplitStream3SplitOutputAll());
        execs.add(new EPLOtherSplitStream3SplitDefaultOutputFirst());
        execs.add(new EPLOtherSplitStream4Split());
        execs.add(new EPLOtherSplitStreamSubqueryMultikeyWArray());
        execs.add(new EPLOtherSplitStreamSingleInsert());
        return execs;
    }

    public static class EPLOtherSplitStreamSingleInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context mycontext initiated by SupportBean as criteria;" +
                "context mycontext on SupportBean_S0 as event" +
                "  insert into SomeOtherStream select context.id as cid, context.criteria as criteria, event as event;" +
                "@name('s0') select * from SomeOtherStream;";
            env.compileDeploy(epl).addListener("s0");

            SupportBean criteria = new SupportBean("E1", 0);
            env.sendEventBean(criteria);
            SupportBean_S0 trigger = new SupportBean_S0(1);
            env.sendEventBean(trigger);

            env.assertEventNew("s0", bean -> {
                assertSame(criteria, bean.get("criteria"));
                assertSame(trigger, bean.get("event"));
            });

            env.undeployAll();
        }
    }

    private static class EPLOtherSplitStreamSubqueryMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema AValue(value int);\n" +
                "on SupportBean\n" +
                "  insert into AValue select (select sum(value) as c0 from SupportEventWithIntArray#keepall group by array) as value where intPrimitive > 0\n" +
                "  insert into AValue select 0 as value where intPrimitive <= 0;\n" +
                "@name('s0') select * from AValue;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventWithIntArray("E1", new int[]{1, 2}, 10));
            env.sendEventBean(new SupportEventWithIntArray("E2", new int[]{1, 2}, 11));

            env.milestone(0);
            assertSplitResult(env, 21);

            env.sendEventBean(new SupportEventWithIntArray("E3", new int[]{1, 2}, 12));
            assertSplitResult(env, 33);

            env.milestone(1);

            env.sendEventBean(new SupportEventWithIntArray("E4", new int[]{1}, 13));
            assertSplitResult(env, null);

            env.undeployAll();
        }

        private void assertSplitResult(RegressionEnvironment env, Integer expected) {
            env.sendEventBean(new SupportBean("X", 0));
            env.assertEqualsNew("s0", "value", 0);

            env.sendEventBean(new SupportBean("Y", 1));
            env.assertEqualsNew("s0", "value", expected);
        }
    }

    private static class EPLOtherSplitStreamInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("on SupportBean select * where intPrimitive=1 insert into BStream select * where 1=2",
                "Required insert-into clause is not provided, the clause is required for split-stream syntax");

            env.tryInvalidCompile("on SupportBean insert into AStream select * where intPrimitive=1 group by string insert into BStream select * where 1=2",
                "A group-by clause, having-clause or order-by clause is not allowed for the split stream syntax");

            env.tryInvalidCompile("on SupportBean insert into AStream select * where intPrimitive=1 insert into BStream select avg(intPrimitive) where 1=2",
                "Aggregation functions are not allowed in this context");
        }
    }

    private static class EPLOtherSplitStreamFromClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionFromClauseBeginBodyEnd(env);
            tryAssertionFromClauseAsMultiple(env);
            tryAssertionFromClauseOutputFirstWhere(env);
            tryAssertionFromClauseDocSample(env);
        }
    }

    private static class EPLOtherSplitStreamSplitPremptiveNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionSplitPremptiveNamedWindow(env, rep);
            }
        }
    }

    private static class EPLOtherSplitStream1SplitDefault implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // test wildcard
            String stmtOrigText = "@name('insert') @public on SupportBean insert into AStream select *";
            env.compileDeploy(stmtOrigText, path).addListener("insert");

            env.compileDeploy("@name('s0') select * from AStream", path).addListener("s0");

            sendSupportBean(env, "E1", 1);
            env.assertEqualsNew("s0", "theString", "E1");
            env.assertListenerNotInvoked("insert");

            // test select
            stmtOrigText = "@name('s1') @public on SupportBean insert into BStreamABC select 3*intPrimitive as value";
            env.compileDeploy(stmtOrigText, path);

            env.compileDeploy("@name('s2') select value from BStreamABC", path).addListener("s2");

            sendSupportBean(env, "E1", 6);
            env.assertEqualsNew("s2", "value", 18);

            // assert type is original type
            env.assertStatement("insert", statement -> assertEquals(SupportBean.class, statement.getEventType().getUnderlyingType()));
            env.assertIterator("insert", iterator -> assertFalse(iterator.hasNext()));

            env.undeployAll();
        }
    }

    private static class EPLOtherSplitStream2SplitNoDefaultOutputFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOrigText = "@name('split') @public on SupportBean " +
                "insert into AStream2SP select * where intPrimitive=1 " +
                "insert into BStream2SP select * where intPrimitive=1 or intPrimitive=2";
            env.compileDeploy(stmtOrigText, path).addListener("split");
            tryAssertion(env, path);
            path.clear();

            // statement object model
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setAnnotations(Arrays.asList(new AnnotationPart("Audit"), new AnnotationPart("public")));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean")));
            model.setInsertInto(InsertIntoClause.create("AStream2SP"));
            model.setSelectClause(SelectClause.createWildcard());
            model.setWhereClause(Expressions.eq("intPrimitive", 1));
            OnInsertSplitStreamClause clause = OnClause.createOnInsertSplitStream();
            model.setOnExpr(clause);
            OnInsertSplitStreamItem item = OnInsertSplitStreamItem.create(
                InsertIntoClause.create("BStream2SP"),
                SelectClause.createWildcard(),
                Expressions.or(Expressions.eq("intPrimitive", 1), Expressions.eq("intPrimitive", 2)));
            clause.addItem(item);
            model.setAnnotations(Arrays.asList(AnnotationPart.nameAnnotation("split"), new AnnotationPart("public")));
            assertEquals(stmtOrigText, model.toEPL());
            env.compileDeploy(model, path).addListener("split");
            tryAssertion(env, path);
            path.clear();

            env.eplToModelCompileDeploy(stmtOrigText, path).addListener("split");
            tryAssertion(env, path);
        }
    }

    private static class EPLOtherSplitStreamSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOrigText = "@name('split') @public on SupportBean " +
                "insert into AStreamSub select (select p00 from SupportBean_S0#lastevent) as string where intPrimitive=(select id from SupportBean_S0#lastevent) " +
                "insert into BStreamSub select (select p01 from SupportBean_S0#lastevent) as string where intPrimitive<>(select id from SupportBean_S0#lastevent) or (select id from SupportBean_S0#lastevent) is null";
            env.compileDeploy(stmtOrigText, path).addListener("split");

            env.compileDeploy("@name('s0') select * from AStreamSub", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from BStreamSub", path).addListener("s1");

            sendSupportBean(env, "E1", 1);
            env.assertListenerNotInvoked("s0");
            env.assertEqualsNew("s1", "string", null);

            env.sendEventBean(new SupportBean_S0(10, "x", "y"));

            sendSupportBean(env, "E2", 10);
            env.assertEqualsNew("s0", "string", "x");
            env.assertListenerNotInvoked("s1");

            sendSupportBean(env, "E3", 9);
            env.assertListenerNotInvoked("s0");
            env.assertEqualsNew("s1", "string", "y");

            env.undeployAll();
        }
    }

    private static class EPLOtherSplitStream2SplitNoDefaultOutputAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOrigText = "@name('split') @public on SupportBean " +
                "insert into AStream2S select theString where intPrimitive=1 " +
                "insert into BStream2S select theString where intPrimitive=1 or intPrimitive=2 " +
                "output all";
            env.compileDeploy(stmtOrigText, path).addListener("split");

            env.compileDeploy("@name('s0') select * from AStream2S", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from BStream2S", path).addListener("s1");

            env.assertThat(() -> {
                assertNotSame(env.statement("s0").getEventType(), env.statement("s1").getEventType());
                assertSame(env.statement("s0").getEventType().getUnderlyingType(), env.statement("s1").getEventType().getUnderlyingType());
            });

            sendSupportBean(env, "E1", 1);
            env.assertEqualsNew("s0", "theString", "E1");
            env.assertEqualsNew("s1", "theString", "E1");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E2", 2);
            env.assertListenerNotInvoked("s0");
            env.assertEqualsNew("s1", "theString", "E2");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E3", 1);
            env.assertEqualsNew("s0", "theString", "E3");
            env.assertEqualsNew("s1", "theString", "E3");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E4", -999);
            env.assertListenerNotInvoked("s0");
            env.assertListenerNotInvoked("s1");
            env.assertEqualsNew("split", "theString", "E4");

            env.undeployAll();
        }
    }

    private static class EPLOtherSplitStream3SplitOutputAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOrigText = "@name('split') @public on SupportBean " +
                "insert into AStream2S select theString || '_1' as theString where intPrimitive in (1, 2) " +
                "insert into BStream2S select theString || '_2' as theString where intPrimitive in (2, 3) " +
                "insert into CStream2S select theString || '_3' as theString " +
                "output all";
            env.compileDeploy(stmtOrigText, path).addListener("split");

            env.compileDeploy("@name('s0') select * from AStream2S", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from BStream2S", path).addListener("s1");
            env.compileDeploy("@name('s2') select * from CStream2S", path).addListener("s2");

            sendSupportBean(env, "E1", 2);
            env.assertEqualsNew("s0", "theString", "E1_1");
            env.assertEqualsNew("s1", "theString", "E1_2");
            env.assertEqualsNew("s2", "theString", "E1_3");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E2", 1);
            env.assertEqualsNew("s0", "theString", "E2_1");
            env.assertListenerNotInvoked("s1");
            env.assertEqualsNew("s2", "theString", "E2_3");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E3", 3);
            env.assertListenerNotInvoked("s0");
            env.assertEqualsNew("s1", "theString", "E3_2");
            env.assertEqualsNew("s2", "theString", "E3_3");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E4", -999);
            env.assertListenerNotInvoked("s0");
            env.assertListenerNotInvoked("s1");
            env.assertEqualsNew("s2", "theString", "E4_3");
            env.assertListenerNotInvoked("split");

            env.undeployAll();
        }
    }

    private static class EPLOtherSplitStream3SplitDefaultOutputFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOrigText = "@name('split') @public on SupportBean as mystream " +
                "insert into AStream34 select mystream.theString||'_1' as theString where intPrimitive=1 " +
                "insert into BStream34 select mystream.theString||'_2' as theString where intPrimitive=2 " +
                "insert into CStream34 select theString||'_3' as theString";
            env.compileDeploy(stmtOrigText, path).addListener("split");

            env.compileDeploy("@name('s0') select * from AStream34", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from BStream34", path).addListener("s1");
            env.compileDeploy("@name('s2') select * from CStream34", path).addListener("s2");

            env.assertThat(() -> {
                assertNotSame(env.statement("s0").getEventType(), env.statement("s1").getEventType());
                assertSame(env.statement("s0").getEventType().getUnderlyingType(), env.statement("s1").getEventType().getUnderlyingType());
            });

            sendSupportBean(env, "E1", 1);
            env.assertEqualsNew("s0", "theString", "E1_1");
            env.assertListenerNotInvoked("s1");
            env.assertListenerNotInvoked("s2");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E2", 2);
            env.assertListenerNotInvoked("s0");
            env.assertEqualsNew("s1", "theString", "E2_2");
            env.assertListenerNotInvoked("s2");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E3", 1);
            env.assertEqualsNew("s0", "theString", "E3_1");
            env.assertListenerNotInvoked("s1");
            env.assertListenerNotInvoked("s2");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E4", -999);
            env.assertListenerNotInvoked("s0");
            env.assertListenerNotInvoked("s1");
            env.assertEqualsNew("s2", "theString", "E4_3");
            env.assertListenerNotInvoked("split");

            env.undeployAll();
        }
    }

    private static class EPLOtherSplitStream4Split implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String stmtOrigText = "@name('split') @public on SupportBean " +
                "insert into AStream34 select theString||'_1' as theString where intPrimitive=10 " +
                "insert into BStream34 select theString||'_2' as theString where intPrimitive=20 " +
                "insert into CStream34 select theString||'_3' as theString where intPrimitive<0 " +
                "insert into DStream34 select theString||'_4' as theString";
            env.compileDeploy(stmtOrigText, path).addListener("split");

            env.compileDeploy("@name('s0') select * from AStream34", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from BStream34", path).addListener("s1");
            env.compileDeploy("@name('s2') select * from CStream34", path).addListener("s2");
            env.compileDeploy("@name('s3') select * from DStream34", path).addListener("s3");

            sendSupportBean(env, "E5", -999);
            env.assertListenerNotInvoked("s0");
            env.assertListenerNotInvoked("s1");
            env.assertEqualsNew("s2", "theString", "E5_3");
            env.assertListenerNotInvoked("s3");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E6", 9999);
            env.assertListenerNotInvoked("s0");
            env.assertListenerNotInvoked("s1");
            env.assertListenerNotInvoked("s2");
            env.assertEqualsNew("s3", "theString", "E6_4");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E7", 20);
            env.assertListenerNotInvoked("s0");
            env.assertEqualsNew("s1", "theString", "E7_2");
            env.assertListenerNotInvoked("s2");
            env.assertListenerNotInvoked("s3");
            env.assertListenerNotInvoked("split");

            sendSupportBean(env, "E8", 10);
            env.assertEqualsNew("s0", "theString", "E8_1");
            env.assertListenerNotInvoked("s1");
            env.assertListenerNotInvoked("s2");
            env.assertListenerNotInvoked("s3");
            env.assertListenerNotInvoked("split");

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void tryAssertion(RegressionEnvironment env, RegressionPath path) {
        env.compileDeploy("@name('s0') select * from AStream2SP", path).addListener("s0");
        env.compileDeploy("@name('s1') select * from BStream2SP", path).addListener("s1");

        env.assertThat(() -> {
            assertNotSame(env.statement("s0").getEventType(), env.statement("s1").getEventType());
            assertSame(env.statement("s0").getEventType().getUnderlyingType(), env.statement("s1").getEventType().getUnderlyingType());
        });

        sendSupportBean(env, "E1", 1);
        env.assertEqualsNew("s0", "theString", "E1");
        env.assertListenerNotInvoked("s1");

        sendSupportBean(env, "E2", 2);
        env.assertListenerNotInvoked("s0");
        env.assertEqualsNew("s1", "theString", "E2");

        sendSupportBean(env, "E3", 1);
        env.assertEqualsNew("s0", "theString", "E3");
        env.assertListenerNotInvoked("s1");

        sendSupportBean(env, "E4", -999);
        env.assertListenerNotInvoked("s0");
        env.assertListenerNotInvoked("s1");
        env.assertEqualsNew("split", "theString", "E4");

        env.undeployAll();
    }

    private static void tryAssertionSplitPremptiveNamedWindow(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTrigger.class) + " @public @buseventtype create schema TypeTrigger(trigger int)", path);

        env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTypeTwo.class) + " @public create schema TypeTwo(col2 int)", path);
        env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedTypeTwo.class) + " @public create window WinTwo#keepall as TypeTwo", path);

        String stmtOrigText = "@public on TypeTrigger " +
            "insert into OtherStream select 1 " +
            "insert into WinTwo(col2) select 2 " +
            "output all";
        env.compileDeploy(stmtOrigText, path);

        env.compileDeploy("@name('s0') on OtherStream select col2 from WinTwo", path).addListener("s0");

        // populate WinOne
        env.sendEventBean(new SupportBean("E1", 2));

        // fire trigger
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{null}, "TypeTrigger");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(new HashMap(), "TypeTrigger");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(SchemaBuilder.record("name").fields().optionalInt("trigger").endRecord());
            env.sendEventAvro(event, "TypeTrigger");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            env.sendEventJson("{}", "TypeTrigger");
        } else {
            fail();
        }

        env.assertEqualsNew("s0", "col2", 2);

        env.undeployAll();
    }

    private static void tryAssertionFromClauseBeginBodyEnd(RegressionEnvironment env) {
        tryAssertionFromClauseBeginBodyEnd(env, false);
        tryAssertionFromClauseBeginBodyEnd(env, true);
    }

    private static void tryAssertionFromClauseAsMultiple(RegressionEnvironment env) {
        tryAssertionFromClauseAsMultiple(env, false);
        tryAssertionFromClauseAsMultiple(env, true);
    }

    private static void tryAssertionFromClauseAsMultiple(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        String epl = "@public on OrderBean as oe " +
            "insert into StartEvent select oe.orderdetail.orderId as oi " +
            "insert into ThenEvent select * from [select oe.orderdetail.orderId as oi, itemId from orderdetail.items] as item " +
            "insert into MoreEvent select oe.orderdetail.orderId as oi, item.itemId as itemId from [select oe, * from orderdetail.items] as item " +
            "output all";
        env.compileDeploy(soda, epl, path);

        env.compileDeploy("@name('s0') select * from StartEvent", path).addListener("s0");
        env.compileDeploy("@name('s1') select * from ThenEvent", path).addListener("s1");
        env.compileDeploy("@name('s2') select * from MoreEvent", path).addListener("s2");

        env.sendEventBean(OrderBeanFactory.makeEventOne());
        String[] fieldsOrderId = "oi".split(",");
        String[] fieldsItems = "oi,itemId".split(",");
        env.assertPropsNew("s0", fieldsOrderId, new Object[]{"PO200901"});
        Object[][] expected = new Object[][]{{"PO200901", "A001"}, {"PO200901", "A002"}, {"PO200901", "A003"}};
        env.assertListener("s1", listener -> EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fieldsItems, expected));
        env.assertListener("s2", listener -> EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fieldsItems, expected));

        env.undeployAll();
    }

    private static void tryAssertionFromClauseBeginBodyEnd(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        String epl = "@name('split') @public on OrderBean " +
            "insert into BeginEvent select orderdetail.orderId as orderId " +
            "insert into OrderItem select * from [select orderdetail.orderId as orderId, * from orderdetail.items] " +
            "insert into EndEvent select orderdetail.orderId as orderId " +
            "output all";
        env.compileDeploy(soda, epl, path);
        env.assertStatement("split", statement -> assertEquals(StatementType.ON_SPLITSTREAM, statement.getProperty(StatementProperty.STATEMENTTYPE)));

        env.compileDeploy("@name('s0') select * from BeginEvent", path).addListener("s0");
        env.compileDeploy("@name('s1') select * from OrderItem", path).addListener("s1");
        env.compileDeploy("@name('s2') select * from EndEvent", path).addListener("s2");

        env.assertThat(() -> {
            EventType orderItemType = env.runtime().getEventTypeService().getEventType(env.deploymentId("split"), "OrderItem");
            assertEquals("[amount, itemId, price, productId, orderId]", Arrays.toString(orderItemType.getPropertyNames()));
        });

        env.sendEventBean(OrderBeanFactory.makeEventOne());
        assertFromClauseWContained(env, "PO200901", new Object[][]{{"PO200901", "A001"}, {"PO200901", "A002"}, {"PO200901", "A003"}});

        env.sendEventBean(OrderBeanFactory.makeEventTwo());
        assertFromClauseWContained(env, "PO200902", new Object[][]{{"PO200902", "B001"}});

        env.sendEventBean(OrderBeanFactory.makeEventFour());
        assertFromClauseWContained(env, "PO200904", new Object[0][]);

        env.undeployAll();
    }

    private static void tryAssertionFromClauseOutputFirstWhere(RegressionEnvironment env) {
        tryAssertionFromClauseOutputFirstWhere(env, false);
        tryAssertionFromClauseOutputFirstWhere(env, true);
    }

    private static void tryAssertionFromClauseOutputFirstWhere(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        String[] fieldsOrderId = "oe.orderdetail.orderId".split(",");
        String epl = "@public on OrderBean as oe " +
            "insert into HeaderEvent select orderdetail.orderId as orderId where 1=2 " +
            "insert into StreamOne select * from [select oe, * from orderdetail.items] where productId=\"10020\" " +
            "insert into StreamTwo select * from [select oe, * from orderdetail.items] where productId=\"10022\" " +
            "insert into StreamThree select * from [select oe, * from orderdetail.items] where productId in (\"10020\",\"10025\",\"10022\")";
        env.compileDeploy(soda, epl, path);

        String[] listenerEPL = new String[]{"select * from StreamOne", "select * from StreamTwo", "select * from StreamThree"};
        for (int i = 0; i < listenerEPL.length; i++) {
            env.compileDeploy("@name('s" + i + "')" + listenerEPL[i], path).addListener("s" + i);
        }

        env.sendEventBean(OrderBeanFactory.makeEventOne());
        env.assertPropsNew("s0", fieldsOrderId, new Object[]{"PO200901"});
        env.assertListenerNotInvoked("s1");
        env.assertListenerNotInvoked("s2");

        env.sendEventBean(OrderBeanFactory.makeEventTwo());
        env.assertListenerNotInvoked("s0");
        env.assertPropsNew("s1", fieldsOrderId, new Object[]{"PO200902"});
        env.assertListenerNotInvoked("s2");

        env.sendEventBean(OrderBeanFactory.makeEventThree());
        env.assertListenerNotInvoked("s0");
        env.assertListenerNotInvoked("s1");
        env.assertPropsNew("s2", fieldsOrderId, new Object[]{"PO200903"});

        env.sendEventBean(OrderBeanFactory.makeEventFour());
        env.assertListenerNotInvoked("s0");
        env.assertListenerNotInvoked("s1");
        env.assertListenerNotInvoked("s2");

        env.undeployAll();
    }

    private static void tryAssertionFromClauseDocSample(RegressionEnvironment env) {
        String epl =
            "create schema MyOrderItem(itemId string);\n" +
                "@public @buseventtype create schema MyOrderEvent(orderId string, items MyOrderItem[]);\n" +
                "on MyOrderEvent\n" +
                "  insert into MyOrderBeginEvent select orderId\n" +
                "  insert into MyOrderItemEvent select * from [select orderId, * from items]\n" +
                "  insert into MyOrderEndEvent select orderId\n" +
                "  output all;\n" +
                "create context MyOrderContext \n" +
                "  initiated by MyOrderBeginEvent as obe\n" +
                "  terminated by MyOrderEndEvent(orderId = obe.orderId);\n" +
                "@Name('count') context MyOrderContext select count(*) as orderItemCount from MyOrderItemEvent output when terminated;\n";
        env.compileDeploy(epl, new RegressionPath()).addListener("count");

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", "1010");
        event.put("items", new Map[]{Collections.singletonMap("itemId", "A0001")});
        env.sendEventMap(event, "MyOrderEvent");

        env.assertEqualsNew("count", "orderItemCount", 1L);

        env.undeployAll();
    }

    private static void assertFromClauseWContained(RegressionEnvironment env, String orderId, Object[][] expected) {
        String[] fieldsOrderId = "orderId".split(",");
        String[] fieldsItems = "orderId,itemId".split(",");
        env.assertListener("s0", listener -> EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOrderId, new Object[]{orderId}));
        env.assertListener("s1", listener -> EPAssertionUtil.assertPropsPerRow(listener.getAndResetDataListsFlattened().getFirst(), fieldsItems, expected));
        env.assertListener("s2", listener -> EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOrderId, new Object[]{orderId}));
    }

    public static class MyLocalJsonProvidedTrigger implements Serializable {
        private static final long serialVersionUID = -7842295459760043998L;
        public int trigger;
    }

    public static class MyLocalJsonProvidedTypeTwo implements Serializable {
        private static final long serialVersionUID = 8245078724083821798L;
        public int col2;
    }
}
