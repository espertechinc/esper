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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableAccessCore {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraTableAccessCoreUnGroupedWindowAndSum());
        execs.add(new InfraIntegerIndexedPropertyLookAlike());
        execs.add(new InfraFilterBehavior());
        execs.add(new InfraExprSelectClauseRenderingUnnamedCol());
        execs.add(new InfraTopLevelReadGrouped2Keys());
        execs.add(new InfraTopLevelReadUnGrouped());
        execs.add(new InfraExpressionAliasAndDecl());
        execs.add(new InfraGroupedTwoKeyNoContext());
        execs.add(new InfraGroupedThreeKeyNoContext());
        execs.add(new InfraGroupedSingleKeyNoContext());
        execs.add(new InfraUngroupedWContext());
        execs.add(new InfraOrderOfAggregationsAndPush());
        execs.add(new InfraMultiStmtContributing());
        execs.add(new InfraGroupedMixedMethodAndAccess());
        execs.add(new InfraNamedWindowAndFireAndForget());
        execs.add(new InfraSubquery());
        execs.add(new InfraOnMergeExpressions());
        execs.add(new InfraTableAccessCoreSplitStream());
        execs.add(new InfraTableAccessMultikeyWArrayOneArrayKey());
        execs.add(new InfraTableAccessMultikeyWArrayTwoArrayKey());
        return execs;
    }

    private static class InfraTableAccessMultikeyWArrayTwoArrayKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(k1 int[primitive] primary key, k2 int[primitive] primary key, value int);\n" +
                "insert into MyTable select intOne as k1, intTwo as k2, value from SupportEventWithManyArray(id = 'I');\n" +
                "@name('s0') select MyTable[intOne, intTwo].value as c0 from SupportEventWithManyArray(id = 'Q');\n" +
                "@name('s1') select MyTable.keys() as keys from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0").addListener("s1");

            sendManyArrayI(env, new int[] {1, 2}, new int[] {1, 2}, 10);
            sendManyArrayI(env, new int[] {1, 3}, new int[] {1, 1}, 20);
            sendManyArrayI(env, new int[] {1, 2}, new int[] {1, 1}, 30);

            env.milestone(0);

            sendManyArrayQAssert(env, new int[] {1, 2}, new int[] {1, 2}, 10);
            sendManyArrayQAssert(env, new int[] {1, 2}, new int[] {1, 1}, 30);
            sendManyArrayQAssert(env, new int[] {1, 3}, new int[] {1, 1}, 20);
            sendManyArrayQAssert(env, new int[] {1, 2}, new int[] {1, 2, 2}, null);

            env.sendEventBean(new SupportBean());
            Object[] keys = (Object[]) env.listener("s1").assertOneGetNewAndReset().get("keys");
            EPAssertionUtil.assertEqualsAnyOrder(keys, new Object[] {
                new Object[] {new int[] {1, 2}, new int[] {1, 2}},
                new Object[] {new int[] {1, 3}, new int[] {1, 1}},
                new Object[] {new int[] {1, 2}, new int[] {1, 1}},
            });

            env.undeployAll();
        }

        private void sendManyArrayQAssert(RegressionEnvironment env, int[] arrayOne, int[] arrayTwo, Integer expected) {
            env.sendEventBean(new SupportEventWithManyArray("Q").withIntOne(arrayOne).withIntTwo(arrayTwo));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }

        private void sendManyArrayI(RegressionEnvironment env, int[] arrayOne, int[] arrayTwo, int value) {
            env.sendEventBean(new SupportEventWithManyArray("I").withIntOne(arrayOne).withIntTwo(arrayTwo).withValue(value));
        }
    }

    private static class InfraTableAccessMultikeyWArrayOneArrayKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(k int[primitive] primary key, value int);\n" +
                "insert into MyTable select intOne as k, value from SupportEventWithManyArray(id = 'I');\n" +
                "@name('s0') select MyTable[intOne].value as c0 from SupportEventWithManyArray(id = 'Q');\n" +
                "@name('s1') select MyTable.keys() as keys from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0").addListener("s1");

            sendManyArrayI(env, new int[] {1, 2}, 10);
            sendManyArrayI(env, new int[] {2, 1}, 20);
            sendManyArrayI(env, new int[] {1, 2, 1}, 30);

            env.milestone(0);

            sendManyArrayQAssert(env, new int[] {1, 2}, 10);
            sendManyArrayQAssert(env, new int[] {1, 2, 1}, 30);
            sendManyArrayQAssert(env, new int[] {2, 1}, 20);
            sendManyArrayQAssert(env, new int[] {1, 2, 2}, null);

            env.sendEventBean(new SupportBean());
            Object[] keys = (Object[]) env.listener("s1").assertOneGetNewAndReset().get("keys");
            EPAssertionUtil.assertEqualsAnyOrder(keys, new Object[] {new int[] {2, 1}, new int[] {1, 2}, new int[] {1, 2, 1}});

            env.undeployAll();
        }

        private void sendManyArrayQAssert(RegressionEnvironment env, int[] arrayOne, Integer expected) {
            env.sendEventBean(new SupportEventWithManyArray("Q").withIntOne(arrayOne));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }

        private void sendManyArrayI(RegressionEnvironment env, int[] arrayOne, int value) {
            env.sendEventBean(new SupportEventWithManyArray("I").withIntOne(arrayOne).withValue(value));
        }
    }

    private static class InfraIntegerIndexedPropertyLookAlike implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionIntegerIndexedPropertyLookAlike(env, false);
            tryAssertionIntegerIndexedPropertyLookAlike(env, true);
        }

        private static void tryAssertionIntegerIndexedPropertyLookAlike(RegressionEnvironment env, boolean soda) {
            RegressionPath path = new RegressionPath();
            String eplDeclare = "@name('infra') create table varaggIIP (key int primary key, myevents window(*) @type('SupportBean'))";
            env.compileDeploy(soda, eplDeclare, path);
            assertEquals(StatementType.CREATE_TABLE, env.statement("infra").getProperty(StatementProperty.STATEMENTTYPE));
            assertEquals("varaggIIP", env.statement("infra").getProperty(StatementProperty.CREATEOBJECTNAME));

            String eplInto = "into table varaggIIP select window(*) as myevents from SupportBean#length(3) group by intPrimitive";
            env.compileDeploy(soda, eplInto, path);

            String eplSelect = "@name('s0') select varaggIIP[1] as c0, varaggIIP[1].myevents as c1, varaggIIP[1].myevents.last(*) as c2, varaggIIP[1].myevents.last(*,1) as c3 from SupportBean_S0";
            env.compileDeploy(soda, eplSelect, path).addListener("s0");

            SupportBean e1 = makeSendBean(env, "E1", 1, 10L);
            SupportBean e2 = makeSendBean(env, "E2", 1, 20L);

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0));
            assertIntegerIndexed(env.listener("s0").assertOneGetNewAndReset(), new SupportBean[]{e1, e2});

            env.undeployAll();
        }
    }

    private static class InfraFilterBehavior implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varaggFB (total count(*))", path);
            env.compileDeploy("into table varaggFB select count(*) as total from SupportBean_S0", path);
            env.compileDeploy("@name('s0') select * from SupportBean(varaggFB.total = intPrimitive)", path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(0));

            env.sendEventBean(new SupportBean("E1", 1));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean_S0(0));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 2));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E1", 3));
            env.sendEventBean(new SupportBean("E1", 1));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class InfraExprSelectClauseRenderingUnnamedCol implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varaggESC (" +
                "key string primary key, theEvents window(*) @type(SupportBean))", path);

            env.compileDeploy("@name('s0') select " +
                "varaggESC.keys()," +
                "varaggESC[p00].theEvents," +
                "varaggESC[p00]," +
                "varaggESC[p00].theEvents.last(*)," +
                "varaggESC[p00].theEvents.window(*).take(1) from SupportBean_S0", path);

            Object[][] expectedAggType = new Object[][]{
                {"varaggESC.keys()", Object[].class},
                {"varaggESC[p00].theEvents", SupportBean[].class},
                {"varaggESC[p00]", Map.class},
                {"varaggESC[p00].theEvents.last(*)", SupportBean.class},
                {"varaggESC[p00].theEvents.window(*).take(1)", Collection.class},
            };
            EventType eventType = env.statement("s0").getEventType();
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, eventType, SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);
            env.undeployAll();
        }
    }

    private static class InfraTopLevelReadGrouped2Keys implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionTopLevelReadGrouped2Keys(env, false);
            tryAssertionTopLevelReadGrouped2Keys(env, true);
        }

        private static void tryAssertionTopLevelReadGrouped2Keys(RegressionEnvironment env, boolean soda) {
            RegressionPath path = new RegressionPath();
            EPCompiled typeCompiled = env.compile("create objectarray schema MyEventOA as (c0 int, c1 string, c2 int)", options -> options.setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC).setBusModifierEventType(ctx -> EventTypeBusModifier.BUS));
            env.deploy(typeCompiled);
            path.add(typeCompiled);

            env.compileDeploy(soda, "create table windowAndTotalTLP2K (" +
                "keyi int primary key, keys string primary key, thewindow window(*) @type('MyEventOA'), thetotal sum(int))", path);
            env.compileDeploy(soda, "into table windowAndTotalTLP2K " +
                "select window(*) as thewindow, sum(c2) as thetotal from MyEventOA#length(2) group by c0, c1", path);

            env.compileDeploy(soda, "@name('s0') select windowAndTotalTLP2K[id,p00] as val0 from SupportBean_S0", path).addListener("s0");
            assertTopLevelTypeInfo(env.statement("s0"));

            Object[] e1 = new Object[]{10, "G1", 100};
            env.sendEventObjectArray(e1, "MyEventOA");

            String[] fieldsInner = "thewindow,thetotal".split(",");
            env.sendEventBean(new SupportBean_S0(10, "G1"));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e1}, 100);

            Object[] e2 = new Object[]{20, "G2", 200};
            env.sendEventObjectArray(e2, "MyEventOA");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(20, "G2"));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e2}, 200);

            Object[] e3 = new Object[]{20, "G2", 300};
            env.sendEventObjectArray(e3, "MyEventOA");

            env.sendEventBean(new SupportBean_S0(10, "G1"));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, null, null);
            env.sendEventBean(new SupportBean_S0(20, "G2"));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e2, e3}, 500);

            // test typable output
            env.undeployModuleContaining("s0");
            env.compileDeploy("@name('i1') insert into OutStream select windowAndTotalTLP2K[20, 'G2'] as val0 from SupportBean_S0", path);
            env.addListener("i1");

            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertProps(env.listener("i1").assertOneGetNewAndReset(), "val0.thewindow,val0.thetotal".split(","), new Object[]{new Object[][]{e2, e3}, 500});

            env.undeployAll();
        }
    }

    private static class InfraTopLevelReadUnGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportUpdateListener listener = new SupportUpdateListener();
            Object[] e1 = new Object[]{10};
            Object[] e2 = new Object[]{20};
            Object[] e3 = new Object[]{30};

            RegressionPath path = new RegressionPath();
            EPCompiled typeCompiled = env.compile("create objectarray schema MyEventOATLRU(c0 int)", options -> options.setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC).setBusModifierEventType(ctx -> EventTypeBusModifier.BUS));
            env.deploy(typeCompiled);
            path.add(typeCompiled);

            env.compileDeploy("create table windowAndTotalTLRUG (" +
                "thewindow window(*) @type(MyEventOATLRU), thetotal sum(int))", path);
            env.compileDeploy("into table windowAndTotalTLRUG " +
                "select window(*) as thewindow, sum(c0) as thetotal from MyEventOATLRU#length(2)", path);

            env.compileDeploy("@name('s0') select windowAndTotalTLRUG as val0 from SupportBean_S0", path);
            env.addListener("s0");

            env.sendEventObjectArray(e1, "MyEventOATLRU");

            String[] fieldsInner = "thewindow,thetotal".split(",");
            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e1}, 10);

            env.sendEventObjectArray(e2, "MyEventOATLRU");

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e1, e2}, 30);

            env.sendEventObjectArray(e3, "MyEventOATLRU");

            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e2, e3}, 50);

            // test typable output
            env.undeployModuleContaining("s0");

            env.compileDeploy("create schema AggBean as " + AggBean.class.getName() + ";\n" +
                "@name('s0') insert into AggBean select windowAndTotalTLRUG as val0 from SupportBean_S0;\n", path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0.thewindow,val0.thetotal".split(","), new Object[]{new Object[][]{e2, e3}, 50});

            env.undeployAll();
        }
    }

    private static class InfraExpressionAliasAndDecl implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionIntoTableFromExpression(env, milestone);

            tryAssertionExpressionHasTableAccess(env, milestone);

            tryAssertionSubqueryWithExpressionHasTableAccess(env, milestone);
        }

        private static void tryAssertionSubqueryWithExpressionHasTableAccess(RegressionEnvironment env, AtomicInteger milestone) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTableTwo(theString string primary key, intPrimitive int)", path);
            env.compileDeploy("create expression getMyValue{o => (select MyTableTwo[o.p00].intPrimitive from SupportBean_S1#lastevent)}", path);
            env.compileDeploy("insert into MyTableTwo select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("@name('s0') select getMyValue(s0) as c0 from SupportBean_S0 as s0", path).addListener("s0");

            env.sendEventBean(new SupportBean_S1(1000));
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(0, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{2});

            env.undeployAll();
        }

        private static void tryAssertionExpressionHasTableAccess(RegressionEnvironment env, AtomicInteger milestone) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTableOne(theString string primary key, intPrimitive int)", path);
            env.compileDeploy("create expression getMyValue{o => MyTableOne[o.p00].intPrimitive}", path);
            env.compileDeploy("insert into MyTableOne select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("@name('s0') select getMyValue(s0) as c0 from SupportBean_S0 as s0", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(0, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{2});

            env.undeployAll();
        }

        private static void tryAssertionIntoTableFromExpression(RegressionEnvironment env, AtomicInteger milestone) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create expression sumi {a -> sum(intPrimitive)}", path);
            env.compileDeploy("create expression sumd alias for {sum(doublePrimitive)}", path);
            env.compileDeploy("create table varaggITFE (" +
                "sumi sum(int), sumd sum(double), sumf sum(float), suml sum(long))", path);
            env.compileDeploy("expression suml alias for {sum(longPrimitive)} " +
                "into table varaggITFE " +
                "select suml, sum(floatPrimitive) as sumf, sumd, sumi(sb) from SupportBean as sb", path);

            makeSendBean(env, "E1", 10, 100L, 1000d, 10000f);

            String fields = "varaggITFE.sumi,varaggITFE.sumd,varaggITFE.sumf,varaggITFE.suml";
            SupportUpdateListener listener = new SupportUpdateListener();
            env.compileDeploy("@name('s0') select " + fields + " from SupportBean_S0", path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields.split(","), new Object[]{10, 1000d, 10000f, 100L});

            env.milestoneInc(milestone);

            makeSendBean(env, "E1", 11, 101L, 1001d, 10001f);

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields.split(","), new Object[]{21, 2001d, 20001f, 201L});

            env.undeployAll();
        }
    }

    private static class InfraGroupedTwoKeyNoContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplDeclare = "create table varTotalG2K (key0 string primary key, key1 int primary key, total sum(long), cnt count(*))";
            env.compileDeploy(eplDeclare, path);

            String eplBind = "into table varTotalG2K " +
                "select sum(longPrimitive) as total, count(*) as cnt " +
                "from SupportBean group by theString, intPrimitive";
            env.compileDeploy(eplBind, path);

            String eplUse = "@name('s0') select varTotalG2K[p00, id].total as c0, varTotalG2K[p00, id].cnt as c1 from SupportBean_S0";
            env.compileDeploy(eplUse, path).addListener("s0");

            makeSendBean(env, "E1", 10, 100);

            String[] fields = "c0,c1".split(",");
            env.sendEventBean(new SupportBean_S0(10, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100L, 1L});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(0, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});
            env.sendEventBean(new SupportBean_S0(10, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.undeployAll();
        }
    }

    private static class InfraGroupedThreeKeyNoContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplDeclare = "create table varTotalG3K (key0 string primary key, key1 int primary key," +
                "key2 long primary key, total sum(double), cnt count(*))";
            env.compileDeploy(eplDeclare, path);

            String eplBind = "into table varTotalG3K " +
                "select sum(doublePrimitive) as total, count(*) as cnt " +
                "from SupportBean group by theString, intPrimitive, longPrimitive";
            env.compileDeploy(eplBind, path);

            String[] fields = "c0,c1".split(",");
            String eplUse = "@name('s0') select varTotalG3K[p00, id, 100L].total as c0, varTotalG3K[p00, id, 100L].cnt as c1 from SupportBean_S0";
            env.compileDeploy(eplUse, path).addListener("s0");

            makeSendBean(env, "E1", 10, 100, 1000);

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(10, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1000.0, 1L});

            env.milestone(1);

            makeSendBean(env, "E1", 10, 100, 1001);

            env.sendEventBean(new SupportBean_S0(10, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2001.0, 2L});

            env.undeployAll();
        }
    }

    private static class InfraGroupedSingleKeyNoContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionGroupedSingleKeyNoContext(env, false, milestone);
            tryAssertionGroupedSingleKeyNoContext(env, true, milestone);
        }

        private static void tryAssertionGroupedSingleKeyNoContext(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {
            RegressionPath path = new RegressionPath();
            String eplDeclare = "create table varTotalG1K (key string primary key, total sum(int))";
            env.compileDeploy(soda, eplDeclare, path);

            String eplBind = "into table varTotalG1K " +
                "select theString, sum(intPrimitive) as total from SupportBean group by theString";
            env.compileDeploy(soda, eplBind, path);

            String eplUse = "@name('s0') select p00 as c0, varTotalG1K[p00].total as c1 from SupportBean_S0";
            env.compileDeploy(soda, eplUse, path).addListener("s0");

            tryAssertionTopLevelSingle(env, milestone);

            env.undeployAll();
        }
    }

    private static class InfraUngroupedWContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplPart = "create context PartitionedByString partition by theString from SupportBean, p00 from SupportBean_S0;\n" +
                "context PartitionedByString create table varTotalUG (total sum(int));\n" +
                "context PartitionedByString into table varTotalUG select sum(intPrimitive) as total from SupportBean;\n" +
                "@Name('s0') context PartitionedByString select p00 as c0, varTotalUG.total as c1 from SupportBean_S0;\n";
            env.compileDeploy(eplPart);
            env.addListener("s0");

            tryAssertionTopLevelSingle(env, new AtomicInteger());

            env.undeployAll();
        }
    }

    private static class InfraOrderOfAggregationsAndPush implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionOrderOfAggs(env, true, milestone);
            tryAssertionOrderOfAggs(env, false, milestone);
        }
    }

    private static class InfraMultiStmtContributing implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            tryAssertionMultiStmtContributingDifferentAggs(env, false, milestone);
            tryAssertionMultiStmtContributingDifferentAggs(env, true, milestone);

            // contribute to the same aggregation
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table sharedagg (total sum(int))", path);
            env.compileDeploy("@name('i1') into table sharedagg " +
                "select p00 as c0, sum(id) as total from SupportBean_S0", path).addListener("i1");
            env.compileDeploy("@name('i2') into table sharedagg " +
                "select p10 as c0, sum(id) as total from SupportBean_S1", path).addListener("i2");
            env.compileDeploy("@name('s0') select theString as c0, sharedagg.total as total from SupportBean", path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(10, "A"));
            assertMultiStmtContributingTotal(env, env.listener("i1"), "A", 10);

            env.sendEventBean(new SupportBean_S1(-5, "B"));
            assertMultiStmtContributingTotal(env, env.listener("i2"), "B", 5);

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(2, "C"));
            assertMultiStmtContributingTotal(env, env.listener("i1"), "C", 7);

            env.undeployAll();
        }

        private static void assertMultiStmtContributingTotal(RegressionEnvironment env, SupportListener listener, String c0, int total) {
            String[] fields = "c0,total".split(",");
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{c0, total});

            env.sendEventBean(new SupportBean(c0, 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{c0, total});
        }

        private static void tryAssertionMultiStmtContributingDifferentAggs(RegressionEnvironment env, boolean grouped, AtomicInteger milestone) {
            RegressionPath path = new RegressionPath();
            String eplDeclare = "create table varaggMSC (" +
                (grouped ? "key string primary key," : "") +
                "s0sum sum(int), s0cnt count(*), s0win window(*) @type(SupportBean_S0)," +
                "s1sum sum(int), s1cnt count(*), s1win window(*) @type(SupportBean_S1)" +
                ")";
            env.compileDeploy(eplDeclare, path);

            String[] fieldsSelect = "c0,c1,c2,c3,c4,c5".split(",");
            String eplSelectUngrouped = "@name('s0') select varaggMSC.s0sum as c0, varaggMSC.s0cnt as c1," +
                "varaggMSC.s0win as c2, varaggMSC.s1sum as c3, varaggMSC.s1cnt as c4," +
                "varaggMSC.s1win as c5 from SupportBean";
            String eplSelectGrouped = "@name('s0') select varaggMSC[theString].s0sum as c0, varaggMSC[theString].s0cnt as c1," +
                "varaggMSC[theString].s0win as c2, varaggMSC[theString].s1sum as c3, varaggMSC[theString].s1cnt as c4," +
                "varaggMSC[theString].s1win as c5 from SupportBean";
            env.compileDeploy(grouped ? eplSelectGrouped : eplSelectUngrouped, path).addListener("s0");

            String[] fieldsOne = "s0sum,s0cnt,s0win".split(",");
            String eplBindOne = "@name('s1') into table varaggMSC select sum(id) as s0sum, count(*) as s0cnt, window(*) as s0win from SupportBean_S0#length(2) " +
                (grouped ? "group by p00" : "");
            env.compileDeploy(eplBindOne, path).addListener("s1");

            String[] fieldsTwo = "s1sum,s1cnt,s1win".split(",");
            String eplBindTwo = "@name('s2') into table varaggMSC select sum(id) as s1sum, count(*) as s1cnt, window(*) as s1win from SupportBean_S1#length(2) " +
                (grouped ? "group by p10" : "");
            env.compileDeploy(eplBindTwo, path).addListener("s2");

            // contribute S1
            SupportBean_S1 s1Bean1 = makeSendS1(env, 10, "G1");
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fieldsTwo, new Object[]{10, 1L, new Object[]{s1Bean1}});

            env.sendEventBean(new SupportBean("G1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect,
                new Object[]{null, 0L, null, 10, 1L, new Object[]{s1Bean1}});

            env.milestoneInc(milestone);

            // contribute S0
            SupportBean_S0 s0Bean1 = makeSendS0(env, 20, "G1");
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fieldsOne, new Object[]{20, 1L, new Object[]{s0Bean1}});

            env.sendEventBean(new SupportBean("G1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect,
                new Object[]{20, 1L, new Object[]{s0Bean1}, 10, 1L, new Object[]{s1Bean1}});

            // contribute S1 and S0
            SupportBean_S1 s1Bean2 = makeSendS1(env, 11, "G1");
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fieldsTwo, new Object[]{21, 2L, new Object[]{s1Bean1, s1Bean2}});
            SupportBean_S0 s0Bean2 = makeSendS0(env, 21, "G1");
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fieldsOne, new Object[]{41, 2L, new Object[]{s0Bean1, s0Bean2}});

            env.sendEventBean(new SupportBean("G1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect,
                new Object[]{41, 2L, new Object[]{s0Bean1, s0Bean2}, 21, 2L, new Object[]{s1Bean1, s1Bean2}});

            env.milestoneInc(milestone);

            // contribute S1 and S0 (leave)
            SupportBean_S1 s1Bean3 = makeSendS1(env, 12, "G1");
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fieldsTwo, new Object[]{23, 2L, new Object[]{s1Bean2, s1Bean3}});
            SupportBean_S0 s0Bean3 = makeSendS0(env, 22, "G1");
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fieldsOne, new Object[]{43, 2L, new Object[]{s0Bean2, s0Bean3}});

            env.sendEventBean(new SupportBean("G1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect,
                new Object[]{43, 2L, new Object[]{s0Bean2, s0Bean3}, 23, 2L, new Object[]{s1Bean2, s1Bean3}});

            env.undeployAll();
        }
    }

    private static class InfraGroupedMixedMethodAndAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionGroupedMixedMethodAndAccess(env, false, milestone);
            tryAssertionGroupedMixedMethodAndAccess(env, true, milestone);
        }
    }

    private static class InfraNamedWindowAndFireAndForget implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindow#length(2) as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "create table varaggNWFAF (total sum(int));\n" +
                "into table varaggNWFAF select sum(intPrimitive) as total from MyWindow;\n";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("E1", 10));
            EPFireAndForgetQueryResult resultSelect = env.compileExecuteFAF("select varaggNWFAF.total as c0 from MyWindow", path);
            assertEquals(10, resultSelect.getArray()[0].get("c0"));

            EPFireAndForgetQueryResult resultDelete = env.compileExecuteFAF("delete from MyWindow where varaggNWFAF.total = intPrimitive", path);
            assertEquals(1, resultDelete.getArray().length);

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 20));
            EPFireAndForgetQueryResult resultUpdate = env.compileExecuteFAF("update MyWindow set doublePrimitive = 100 where varaggNWFAF.total = intPrimitive", path);
            assertEquals(100d, resultUpdate.getArray()[0].get("doublePrimitive"));

            EPFireAndForgetQueryResult resultInsert = env.compileExecuteFAF("insert into MyWindow (theString, intPrimitive) values ('A', varaggNWFAF.total)", path);
            EPAssertionUtil.assertProps(resultInsert.getArray()[0], "theString,intPrimitive".split(","), new Object[]{"A", 20});

            env.undeployAll();
        }
    }

    private static class InfraSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table subquery_var_agg (key string primary key, total count(*))", path);
            env.compileDeploy("@name('s0') select (select subquery_var_agg[p00].total from SupportBean_S0#lastevent) as c0 " +
                "from SupportBean_S1", path).addListener("s0");
            env.compileDeploy("into table subquery_var_agg select count(*) as total from SupportBean group by theString", path);

            env.sendEventBean(new SupportBean("E1", -1));
            env.sendEventBean(new SupportBean_S0(0, "E1"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(1));
            assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportBean("E1", -1));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S1(2));
            assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class InfraOnMergeExpressions implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table the_table (key string primary key, total count(*), value int)", path);
            env.compileDeploy("into table the_table select count(*) as total from SupportBean group by theString", path);
            env.compileDeploy("on SupportBean_S0 as s0 " +
                "merge the_table as tt " +
                "where s0.p00 = tt.key " +
                "when matched and the_table[s0.p00].total > 0" +
                "  then update set value = 1", path);
            env.compileDeploy("@name('s0') select the_table[p10].value as c0 from SupportBean_S1", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", -1));
            env.sendEventBean(new SupportBean_S0(0, "E1"));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S1(0, "E1"));
            assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class InfraTableAccessCoreSplitStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(k1 string primary key, c1 int);\n" +
                "insert into MyTable select theString as k1, intPrimitive as c1 from SupportBean;\n";
            env.compileDeploy(epl, path);

            epl = "on SupportBean_S0 " +
                "  insert into AStream select MyTable['A'].c1 as c0 where id=1" +
                "  insert into AStream select MyTable['B'].c1 as c0 where id=2;\n";
            env.compileDeploy(epl, path);

            env.compileDeploy("@name('out') select * from AStream", path).addListener("out");

            env.sendEventBean(new SupportBean("A", 10));
            env.sendEventBean(new SupportBean("B", 20));

            env.sendEventBean(new SupportBean_S0(1));
            assertEquals(10, env.listener("out").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportBean_S0(2));
            assertEquals(20, env.listener("out").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    public static class InfraTableAccessCoreUnGroupedWindowAndSum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create objectarray schema MyEvent(c0 int)", path);

            env.compileDeploy("create table windowAndTotal (" +
                "thewindow window(*) @type(MyEvent), thetotal sum(int))", path);
            env.compileDeploy("into table windowAndTotal " +
                "select window(*) as thewindow, sum(c0) as thetotal from MyEvent#length(2)", path);

            env.compileDeploy("@Name('s0') select windowAndTotal as val0 from SupportBean_S0", path).addListener("s0");

            Object[] e1 = new Object[]{10};
            env.sendEventObjectArray(e1, "MyEvent");

            env.milestone(0);

            String[] fieldsInner = "thewindow,thetotal".split(",");
            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e1}, 10);

            env.milestone(1);

            Object[] e2 = new Object[]{20};
            env.sendEventObjectArray(e2, "MyEvent");

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e1, e2}, 30);

            env.milestone(3);

            Object[] e3 = new Object[]{30};
            env.sendEventObjectArray(e3, "MyEvent");

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[][]{e2, e3}, 50);

            env.undeployAll();
        }
    }

    private static void tryAssertionGroupedMixedMethodAndAccess(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {
        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varMyAgg (" +
            "key string primary key, " +
            "c0 count(*), " +
            "c1 count(distinct int), " +
            "c2 window(*) @type('SupportBean'), " +
            "c3 sum(long)" +
            ")";
        env.compileDeploy(soda, eplDeclare, path);

        String eplBind = "into table varMyAgg select " +
            "count(*) as c0, " +
            "count(distinct intPrimitive) as c1, " +
            "window(*) as c2, " +
            "sum(longPrimitive) as c3 " +
            "from SupportBean#length(3) group by theString";
        env.compileDeploy(soda, eplBind, path);

        String eplSelect = "@name('s0') select " +
            "varMyAgg[p00].c0 as c0, " +
            "varMyAgg[p00].c1 as c1, " +
            "varMyAgg[p00].c2 as c2, " +
            "varMyAgg[p00].c3 as c3" +
            " from SupportBean_S0";
        env.compileDeploy(soda, eplSelect, path).addListener("s0");
        String[] fields = "c0,c1,c2,c3".split(",");

        EventType eventType = env.statement("s0").getEventType();
        assertEquals(Long.class, eventType.getPropertyType("c0"));
        assertEquals(Long.class, eventType.getPropertyType("c1"));
        assertEquals(SupportBean[].class, eventType.getPropertyType("c2"));
        assertEquals(Long.class, eventType.getPropertyType("c3"));

        SupportBean b1 = makeSendBean(env, "E1", 10, 100);
        SupportBean b2 = makeSendBean(env, "E1", 11, 101);
        SupportBean b3 = makeSendBean(env, "E1", 10, 102);

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{3L, 2L, new SupportBean[]{b1, b2, b3}, 303L});

        env.sendEventBean(new SupportBean_S0(0, "E2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{null, null, null, null});

        SupportBean b4 = makeSendBean(env, "E2", 20, 200);

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_S0(0, "E2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{1L, 1L, new SupportBean[]{b4}, 200L});

        env.undeployAll();
    }

    private static void tryAssertionTopLevelSingle(RegressionEnvironment env, AtomicInteger milestone) {
        sendEventsAndAssert(env, "A", 10, "A", 10);
        sendEventsAndAssert(env, "A", 11, "A", 21);
        sendEventsAndAssert(env, "B", 20, "A", 21);

        env.milestoneInc(milestone);

        sendEventsAndAssert(env, "B", 21, "B", 41);
        sendEventsAndAssert(env, "C", 30, "A", 21);
        sendEventsAndAssert(env, "D", 40, "C", 30);

        String[] fields = "c0,c1".split(",");
        int[] expected = new int[]{21, 41, 30, 40};
        int count = 0;
        for (String p00 : "A,B,C,D".split(",")) {
            env.sendEventBean(new SupportBean_S0(0, p00));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{p00, expected[count]});
            count++;
        }

        env.sendEventBean(new SupportBean_S0(0, "A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 21});
    }

    private static void sendEventsAndAssert(RegressionEnvironment env, String theString, int intPrimitive, String p00, int total) {
        String[] fields = "c0,c1".split(",");
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        env.sendEventBean(new SupportBean_S0(0, p00));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{p00, total});
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        return makeSendBean(env, theString, intPrimitive, longPrimitive, -1);
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        return makeSendBean(env, theString, intPrimitive, longPrimitive, doublePrimitive, -1);
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive, double doublePrimitive, float floatPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        bean.setFloatPrimitive(floatPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static void assertTopLevelTypeInfo(EPStatement stmt) {
        assertEquals(Map.class, stmt.getEventType().getPropertyType("val0"));
        FragmentEventType fragType = stmt.getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        assertEquals(Object[][].class, fragType.getFragmentType().getPropertyType("thewindow"));
        assertEquals(Integer.class, fragType.getFragmentType().getPropertyType("thetotal"));
    }

    private static void assertIntegerIndexed(EventBean event, SupportBean[] events) {
        EPAssertionUtil.assertEqualsExactOrder(events, (Object[]) event.get("c0.myevents"));
        EPAssertionUtil.assertEqualsExactOrder(events, (Object[]) event.get("c1"));
        assertEquals(events[events.length - 1], event.get("c2"));
        assertEquals(events[events.length - 2], event.get("c3"));
    }

    private static SupportBean_S1 makeSendS1(RegressionEnvironment env, int id, String p10) {
        SupportBean_S1 bean = new SupportBean_S1(id, p10);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportBean_S0 makeSendS0(RegressionEnvironment env, int id, String p00) {
        SupportBean_S0 bean = new SupportBean_S0(id, p00);
        env.sendEventBean(bean);
        return bean;
    }

    private static void tryAssertionOrderOfAggs(RegressionEnvironment env, boolean ungrouped, AtomicInteger milestone) {

        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varaggOOA (" + (ungrouped ? "" : "key string primary key, ") +
            "sumint sum(int), " +
            "sumlong sum(long), " +
            "mysort sorted(intPrimitive) @type(SupportBean)," +
            "mywindow window(*) @type(SupportBean)" +
            ")";
        env.compileDeploy(eplDeclare, path);

        String[] fieldsTable = "sumint,sumlong,mywindow,mysort".split(",");
        String eplSelect = "@name('into') into table varaggOOA select " +
            "sum(longPrimitive) as sumlong, " +
            "sum(intPrimitive) as sumint, " +
            "window(*) as mywindow," +
            "sorted() as mysort " +
            "from SupportBean#length(2) " +
            (ungrouped ? "" : "group by theString ");
        env.compileDeploy(eplSelect, path).addListener("into");

        String[] fieldsSelect = "c0,c1,c2,c3".split(",");
        String groupKey = ungrouped ? "" : "['E1']";
        env.compileDeploy("@name('s0') select " +
            "varaggOOA" + groupKey + ".sumint as c0, " +
            "varaggOOA" + groupKey + ".sumlong as c1," +
            "varaggOOA" + groupKey + ".mywindow as c2," +
            "varaggOOA" + groupKey + ".mysort as c3 from SupportBean_S0", path).addListener("s0");

        SupportBean e1 = makeSendBean(env, "E1", 10, 100);
        EPAssertionUtil.assertProps(env.listener("into").assertOneGetNewAndReset(), fieldsTable,
            new Object[]{10, 100L, new Object[]{e1}, new Object[]{e1}});

        env.milestoneInc(milestone);

        SupportBean e2 = makeSendBean(env, "E1", 5, 50);
        EPAssertionUtil.assertProps(env.listener("into").assertOneGetNewAndReset(), fieldsTable,
            new Object[]{15, 150L, new Object[]{e1, e2}, new Object[]{e2, e1}});

        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect,
            new Object[]{15, 150L, new Object[]{e1, e2}, new Object[]{e2, e1}});

        env.milestoneInc(milestone);

        SupportBean e3 = makeSendBean(env, "E1", 12, 120);
        EPAssertionUtil.assertProps(env.listener("into").assertOneGetNewAndReset(), fieldsTable,
            new Object[]{17, 170L, new Object[]{e2, e3}, new Object[]{e2, e3}});

        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsSelect,
            new Object[]{17, 170L, new Object[]{e2, e3}, new Object[]{e2, e3}});

        env.undeployAll();
    }

    public static class AggSubBean {
        private int thetotal;
        private Object[][] thewindow;

        public int getThetotal() {
            return thetotal;
        }

        public void setThetotal(int thetotal) {
            this.thetotal = thetotal;
        }

        public Object[][] getThewindow() {
            return thewindow;
        }

        public void setThewindow(Object[][] thewindow) {
            this.thewindow = thewindow;
        }
    }

    public static class AggBean {
        private AggSubBean val0;

        public AggSubBean getVal0() {
            return val0;
        }

        public void setVal0(AggSubBean val0) {
            this.val0 = val0;
        }
    }
}
