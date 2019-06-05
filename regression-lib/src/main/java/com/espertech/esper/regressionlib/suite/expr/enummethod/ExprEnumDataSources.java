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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bookexample.BookDesc;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;
import com.espertech.esper.runtime.client.DeploymentOptions;

import java.util.*;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ExprEnumDataSources {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumProperty());
        execs.add(new ExprEnumSubstitutionParameter());
        execs.add(new ExprEnumEnumObject());
        execs.add(new ExprEnumSortedMaxMinBy());
        execs.add(new ExprEnumJoin());
        execs.add(new ExprEnumPrevWindowSorted());
        execs.add(new ExprEnumNamedWindow());
        execs.add(new ExprEnumSubselect());
        execs.add(new ExprEnumAccessAggregation());
        execs.add(new ExprEnumPrevFuncs());
        execs.add(new ExprEnumUDFStaticMethod());
        execs.add(new ExprEnumPropertySchema());
        execs.add(new ExprEnumPropertyInsertIntoAtEventBean());
        execs.add(new ExprEnumPatternFilter());
        execs.add(new ExprEnumVariable());
        execs.add(new ExprEnumTableRow());
        execs.add(new ExprEnumMatchRecognizeDefine());
        execs.add(new ExprEnumMatchRecognizeMeasures(false));
        execs.add(new ExprEnumMatchRecognizeMeasures(true));
        execs.add(new ExprEnumCast());
        return execs;
    }

    private static class ExprEnumCast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema MyLocalEvent as " + MyLocalEvent.class.getName() + ";\n" +
                "@name('s0') select cast(value.someCollection?, java.util.Collection).countOf() as cnt from MyLocalEvent";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new MyLocalEvent(new MyLocalWithCollection(Arrays.asList("a", "b"))));
            assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.undeployAll();
        }
    }

    private static class ExprEnumPropertySchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema OrderDetail(itemId string);\n" +
                "create schema OrderEvent(details OrderDetail[]);\n" +
                "@name('s0') select details.where(i => i.itemId = '001') as c0 from OrderEvent;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            Map<String, Object> detailOne = CollectionUtil.populateNameValueMap("itemId", "002");
            Map<String, Object> detailTwo = CollectionUtil.populateNameValueMap("itemId", "001");
            env.sendEventMap(CollectionUtil.populateNameValueMap("details", new Map[]{detailOne, detailTwo}), "OrderEvent");

            Collection c = (Collection) env.listener("s0").assertOneGetNewAndReset().get("c0");
            EPAssertionUtil.assertEqualsExactOrder(c.toArray(), new Map[]{detailTwo});

            env.undeployAll();
        }
    }

    private static class ExprEnumPropertyInsertIntoAtEventBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create objectarray schema StockTick(id string, price int);\n" +
                "insert into TicksLarge select window(*).where(e => e.price > 100) @eventbean as ticksLargePrice\n" +
                "from StockTick#time(10) having count(*) > 2;\n" +
                "@name('s0') select ticksLargePrice.where(e => e.price < 200) as ticksLargeLess200 from TicksLarge;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            env.sendEventObjectArray(new Object[]{"E1", 90}, "StockTick");
            env.sendEventObjectArray(new Object[]{"E2", 120}, "StockTick");
            env.sendEventObjectArray(new Object[]{"E3", 95}, "StockTick");

            assertEquals(1, ((Collection) env.listener("s0").assertOneGetNewAndReset().get("ticksLargeLess200")).size());

            env.undeployAll();
        }
    }

    private static class ExprEnumMatchRecognizeMeasures implements RegressionExecution {
        private final boolean select;

        public ExprEnumMatchRecognizeMeasures(boolean select) {
            this.select = select;
        }

        public void run(RegressionEnvironment env) {
            String epl;
            if (!select) {
                epl = "select ids from SupportBean match_recognize ( " +
                    "  measures A.selectFrom(o -> o.theString) as ids ";
            } else {
                epl = "select a.selectFrom(o -> o.theString) as ids from SupportBean match_recognize (measures A as a ";
            }
            epl = "@name('s0') " + epl + " pattern (A{3}) define A as A.intPrimitive = 1)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E3", 1));
            assertColl("E1,E2,E3", env.listener("s0").assertOneGetNewAndReset().get("ids"));

            env.sendEventBean(new SupportBean("E4", 1));
            env.sendEventBean(new SupportBean("E5", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E6", 1));
            assertColl("E4,E5,E6", env.listener("s0").assertOneGetNewAndReset().get("ids"));

            env.undeployAll();
        }
    }

    private static class ExprEnumSubstitutionParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            trySubstitutionParameter(env, "?::int[primitive]", new int[]{1, 10, 100});
            trySubstitutionParameter(env, "?::java.lang.Object[]", new Object[]{1, 10, 100});
            trySubstitutionParameter(env, "?::Integer[]", new Integer[]{1, 10, 100});
        }
    }

    private static class ExprEnumTableRow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test table access expression
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTableUnkeyed(theWindow window(*) @type(SupportBean))", path);
            env.compileDeploy("into table MyTableUnkeyed select window(*) as theWindow from SupportBean#time(30)", path);
            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));

            env.compileDeploy("@name('s0')select MyTableUnkeyed.theWindow.anyOf(v=>intPrimitive=10) as c0 from SupportBean_A", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_A("A0"));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class ExprEnumPatternFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern [ ([2]a=SupportBean_ST0) -> b=SupportBean(intPrimitive > a.max(i -> p00))]";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_ST0("E1", 10));
            env.sendEventBean(new SupportBean_ST0("E2", 15));
            env.sendEventBean(new SupportBean("E3", 15));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E4", 16));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a[0].id,a[1].id,b.theString".split(","), new Object[]{"E1", "E2", "E4"});
            env.undeployAll();

            env.compileDeploy("@name('s0') select * from pattern [ a=SupportBean_ST0 until b=SupportBean -> c=SupportBean(intPrimitive > a.sumOf(i => p00))]");
            env.addListener("s0");

            env.sendEventBean(new SupportBean_ST0("E10", 10));
            env.sendEventBean(new SupportBean_ST0("E11", 15));
            env.sendEventBean(new SupportBean("E12", -1));
            env.sendEventBean(new SupportBean("E13", 25));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E14", 26));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a[0].id,a[1].id,b.theString,c.theString".split(","), new Object[]{"E10", "E11", "E12", "E14"});

            env.undeployAll();
        }
    }

    private static class ExprEnumMatchRecognizeDefine implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // try define-clause
            String[] fieldsOne = "a_array[0].theString,a_array[1].theString,b.theString".split(",");
            String textOne = "@name('s0') select * from SupportBean " +
                "match_recognize (" +
                " measures A as a_array, B as b " +
                " pattern (A* B)" +
                " define" +
                " B as A.anyOf(v=> v.intPrimitive = B.intPrimitive)" +
                ")";
            env.compileDeploy(textOne).addListener("s0");
            env.sendEventBean(new SupportBean("A1", 10));
            env.sendEventBean(new SupportBean("A2", 20));
            env.sendEventBean(new SupportBean("A3", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{"A1", "A2", "A3"});

            env.sendEventBean(new SupportBean("A4", 1));
            env.sendEventBean(new SupportBean("A5", 2));
            env.sendEventBean(new SupportBean("A6", 3));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("A7", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsOne, new Object[]{"A4", "A5", "A7"});
            env.undeployAll();

            // try measures-clause
            String[] fieldsTwo = "c0".split(",");
            String textTwo = "@name('s0') select * from SupportBean " +
                "match_recognize (" +
                " measures A.anyOf(v=> v.intPrimitive = B.intPrimitive) as c0 " +
                " pattern (A* B)" +
                " define" +
                " A as A.theString like 'A%'," +
                " B as B.theString like 'B%'" +
                ")";
            env.compileDeploy(textTwo).addListener("s0");

            env.sendEventBean(new SupportBean("A1", 10));
            env.sendEventBean(new SupportBean("A2", 20));
            assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportBean("B1", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{true});

            env.sendEventBean(new SupportBean("A1", 10));
            env.sendEventBean(new SupportBean("A2", 20));
            env.sendEventBean(new SupportBean("B1", 15));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo, new Object[]{false});

            env.undeployAll();
        }
    }

    private static class ExprEnumEnumObject implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') select " +
                "SupportEnumTwo.ENUM_VALUE_1.getMystrings().anyOf(v => v = id) as c0, " +
                "value.getMystrings().anyOf(v => v = '2') as c1 " +
                "from SupportEnumTwoEvent";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEnumTwoEvent("0", SupportEnumTwo.ENUM_VALUE_1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false});

            env.sendEventBean(new SupportEnumTwoEvent("2", SupportEnumTwo.ENUM_VALUE_2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true});

            env.undeployAll();
        }
    }

    private static class ExprEnumSortedMaxMinBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4".split(",");

            String eplWindowAgg = "@name('s0') select " +
                "sorted(theString).allOf(x => x.intPrimitive < 5) as c0," +
                "maxby(theString).allOf(x => x.intPrimitive < 5) as c1," +
                "minby(theString).allOf(x => x.intPrimitive < 5) as c2," +
                "maxbyever(theString).allOf(x => x.intPrimitive < 5) as c3," +
                "minbyever(theString).allOf(x => x.intPrimitive < 5) as c4" +
                " from SupportBean#length(5)";
            env.compileDeploy(eplWindowAgg).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true, true});

            env.undeployAll();
        }
    }

    private static class ExprEnumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select * from SupportSelectorEvent#keepall as sel, SupportContainerEvent#keepall as cont " +
                "where cont.items.anyOf(i => sel.selector = i.selected)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportSelectorEvent("S1", "sel1"));
            env.sendEventBean(new SupportContainerEvent("C1", new SupportContainedItem("I1", "sel1")));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ExprEnumPrevWindowSorted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select prevwindow(st0) as val0, prevwindow(st0).esperInternalNoop() as val1 " +
                "from SupportBean_ST0#sort(3, p00 asc) as st0";
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val0,val1".split(","), new Class[]{SupportBean_ST0[].class, Collection.class});

            env.sendEventBean(new SupportBean_ST0("E1", 5));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E2", 6));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E1,E2");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E3", 4));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E3,E1,E2");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E5", 3));
            LambdaAssertionUtil.assertST0Id(env.listener("s0"), "val1", "E5,E3,E1");
            env.listener("s0").reset();
            env.undeployAll();

            // Scalar version
            String[] fields = new String[]{"val0"};
            String stmtScalar = "@name('s0') select prevwindow(id).where(x => x not like '%ignore%') as val0 " +
                "from SupportBean_ST0#keepall as st0";
            env.compileDeploy(stmtScalar).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class});

            env.sendEventBean(new SupportBean_ST0("E1", 5));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E2ignore", 6));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E3", 4));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E3", "E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("ignoreE5", 3));
            LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), "val0", "E3", "E1");
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindow#keepall as SupportBean_ST0;\n" +
                "on SupportBean_A delete from MyWindow;\n" +
                "insert into MyWindow select * from SupportBean_ST0;\n";
            env.compileDeploy(epl, path);

            env.compileDeploy("@name('s0') select MyWindow.allOf(x => x.p00 < 5) as allOfX from SupportBean#keepall", path);
            env.addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "allOfX".split(","), new Class[]{Boolean.class});

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(new SupportBean_ST0("ST0", "1", 10));
            env.sendEventBean(new SupportBean("E2", 10));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.undeployModuleContaining("s0");
            env.sendEventBean(new SupportBean_A("A1"));

            // test named window correlated
            String eplNamedWindowCorrelated = "@name('s0') select MyWindow(key0 = sb.theString).allOf(x => x.p00 < 5) as allOfX from SupportBean#keepall sb";
            env.compileDeploy(eplNamedWindowCorrelated, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(new SupportBean_ST0("E2", "KEY1", 1));
            env.sendEventBean(new SupportBean("E2", 0));
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(new SupportBean("KEY1", 0));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.undeployAll();
        }
    }

    private static class ExprEnumSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test subselect-wildcard
            String eplSubselect = "@name('s0') select (select * from SupportBean_ST0#keepall).allOf(x => x.p00 < 5) as allOfX from SupportBean#keepall";
            env.compileDeploy(eplSubselect).addListener("s0");

            env.sendEventBean(new SupportBean_ST0("ST0", "1", 0));
            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(new SupportBean_ST0("ST0", "1", 10));
            env.sendEventBean(new SupportBean("E2", 2));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));
            env.undeployAll();

            // test subselect scalar return
            String eplSubselectScalar = "@name('s0') select (select id from SupportBean_ST0#keepall).allOf(x => x  like '%B%') as allOfX from SupportBean#keepall";
            env.compileDeploy(eplSubselectScalar).addListener("s0");

            env.sendEventBean(new SupportBean_ST0("B1", 0));
            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(new SupportBean_ST0("A1", 0));
            env.sendEventBean(new SupportBean("E2", 2));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));
            env.undeployAll();

            // test subselect-correlated scalar return
            String eplSubselectScalarCorrelated = "@name('s0') select (select key0 from SupportBean_ST0#keepall st0 where st0.id = sb.theString).allOf(x => x  like '%hello%') as allOfX from SupportBean#keepall sb";
            env.compileDeploy(eplSubselectScalarCorrelated).addListener("s0");

            env.sendEventBean(new SupportBean_ST0("A1", "hello", 0));
            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(new SupportBean_ST0("A2", "hello", 0));
            env.sendEventBean(new SupportBean("A2", 1));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(new SupportBean_ST0("A3", "test", 0));
            env.sendEventBean(new SupportBean("A3", 1));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));
            env.undeployAll();

            // test subselect multivalue return
            String[] fields = new String[]{"id", "p00"};
            String eplSubselectMultivalue = "@name('s0') select (select id, p00 from SupportBean_ST0#keepall).take(10) as c0 from SupportBean";
            env.compileDeploy(eplSubselectMultivalue).addListener("s0");

            env.sendEventBean(new SupportBean_ST0("B1", 10));
            env.sendEventBean(new SupportBean("E1", 0));
            assertPropsMapRows((Collection) env.listener("s0").assertOneGetNewAndReset().get("c0"), fields, new Object[][]{{"B1", 10}});

            env.sendEventBean(new SupportBean_ST0("B2", 20));
            env.sendEventBean(new SupportBean("E2", 0));
            assertPropsMapRows((Collection) env.listener("s0").assertOneGetNewAndReset().get("c0"), fields, new Object[][]{{"B1", 10}, {"B2", 20}});
            env.undeployAll();

            // test subselect that delivers events
            String epl = "create schema AEvent (symbol string);\n" +
                "create schema BEvent (a AEvent);\n" +
                "@name('s0') select (select a from BEvent#keepall).anyOf(v => symbol = 'GE') as flag from SupportBean;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            env.sendEventMap(makeBEvent("XX"), "BEvent");
            env.sendEventBean(new SupportBean());
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("flag"));

            env.sendEventMap(makeBEvent("GE"), "BEvent");
            env.sendEventBean(new SupportBean());
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("flag"));

            env.undeployAll();
        }
    }

    private static class ExprEnumVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variable string[] myvar = { 'E1', 'E3' };\n" +
                "@name('s0') select * from SupportBean(myvar.anyOf(v => v = theString));\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ExprEnumAccessAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"val0", "val1", "val2", "val3", "val4"};

            // test window(*) and first(*)
            String eplWindowAgg = "@name('s0') select " +
                "window(*).allOf(x => x.intPrimitive < 5) as val0," +
                "first(*).allOf(x => x.intPrimitive < 5) as val1," +
                "first(*, 1).allOf(x => x.intPrimitive < 5) as val2," +
                "last(*).allOf(x => x.intPrimitive < 5) as val3," +
                "last(*, 1).allOf(x => x.intPrimitive < 5) as val4" +
                " from SupportBean#length(2)";
            env.compileDeploy(eplWindowAgg).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, null, true, null});

            env.sendEventBean(new SupportBean("E2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, false, true});

            env.sendEventBean(new SupportBean("E3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true, false});

            env.undeployAll();

            // test scalar: window(*) and first(*)
            String eplWindowAggScalar = "@name('s0') select " +
                "window(intPrimitive).allOf(x => x < 5) as val0," +
                "first(intPrimitive).allOf(x => x < 5) as val1," +
                "first(intPrimitive, 1).allOf(x => x < 5) as val2," +
                "last(intPrimitive).allOf(x => x < 5) as val3," +
                "last(intPrimitive, 1).allOf(x => x < 5) as val4" +
                " from SupportBean#length(2)";
            env.compileDeploy(eplWindowAggScalar).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, null, true, null});

            env.sendEventBean(new SupportBean("E2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false, false, true});

            env.sendEventBean(new SupportBean("E3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true, false});

            env.undeployAll();
        }
    }

    private static class ExprEnumProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test fragment type - collection inside
            String eplFragment = "@name('s0') select contained.allOf(x => x.p00 < 5) as allOfX from SupportBean_ST0_Container#keepall";
            env.compileDeploy(eplFragment).addListener("s0");

            env.sendEventBean(SupportBean_ST0_Container.make3Value("ID1,KEY1,1"));
            assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));

            env.sendEventBean(SupportBean_ST0_Container.make3Value("ID1,KEY1,10"));
            assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("allOfX"));
            env.undeployAll();

            // test array and iterable
            String[] fields = "val0,val1".split(",");
            eplFragment = "@name('s0') select intarray.sumof() as val0, " +
                "intiterable.sumOf() as val1 " +
                " from SupportCollection#keepall";
            env.compileDeploy(eplFragment).addListener("s0");

            env.sendEventBean(SupportCollection.makeNumeric("5,6,7"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5 + 6 + 7, 5 + 6 + 7});

            env.undeployAll();

            // test map event type with object-array prop
            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create schema MySchema (books BookDesc[])", path);

            env.compileDeploy("@name('s0') select books.max(i => i.price) as mymax from MySchema", path);
            env.addListener("s0");

            Map<String, Object> event = Collections.singletonMap("books", new BookDesc[]{new BookDesc("1", "book1", "dave", 1.00, null)});
            env.sendEventMap(event, "MySchema");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "mymax".split(","), new Object[]{1.0});

            env.undeployAll();

            // test method invocation variations returning list/array of string and test UDF +property as well
            runAssertionMethodInvoke(env, "select e.getTheList().anyOf(v => v = selector) as flag from SupportSelectorWithListEvent e");
            runAssertionMethodInvoke(env, "select convertToArray(theList).anyOf(v => v = selector) as flag from SupportSelectorWithListEvent e");
            runAssertionMethodInvoke(env, "select theArray.anyOf(v => v = selector) as flag from SupportSelectorWithListEvent e");
            runAssertionMethodInvoke(env, "select e.getTheArray().anyOf(v => v = selector) as flag from SupportSelectorWithListEvent e");
            runAssertionMethodInvoke(env, "select e.theList.anyOf(v => v = e.selector) as flag from pattern[every e=SupportSelectorWithListEvent]");
            runAssertionMethodInvoke(env, "select e.nestedMyEvent.myNestedList.anyOf(v => v = e.selector) as flag from pattern[every e=SupportSelectorWithListEvent]");
            runAssertionMethodInvoke(env, "select " + SupportSelectorWithListEvent.class.getName() + ".convertToArray(theList).anyOf(v => v = selector) as flag from SupportSelectorWithListEvent e");

            env.undeployAll();
        }
    }

    public static void runAssertionMethodInvoke(RegressionEnvironment env, String epl) {
        String[] fields = "flag".split(",");
        env.compileDeploy("@name('s0') " + epl).addListener("s0");

        env.sendEventBean(new SupportSelectorWithListEvent("1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true});

        env.sendEventBean(new SupportSelectorWithListEvent("4"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false});

        env.undeployAll();
    }

    private static class ExprEnumPrevFuncs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"val0", "val1", "val2"};
            // test prevwindow(*) etc
            String epl = "@name('s0') select " +
                "prevwindow(sb).allOf(x => x.intPrimitive < 5) as val0," +
                "prev(sb,1).allOf(x => x.intPrimitive < 5) as val1," +
                "prevtail(sb,1).allOf(x => x.intPrimitive < 5) as val2" +
                " from SupportBean#length(2) as sb";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, null, null});

            env.sendEventBean(new SupportBean("E2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false});

            env.sendEventBean(new SupportBean("E3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true});

            env.sendEventBean(new SupportBean("E4", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true});
            env.undeployAll();

            // test scalar prevwindow(property) etc
            String eplScalar = "@name('s0') select " +
                "prevwindow(intPrimitive).allOf(x => x < 5) as val0," +
                "prev(intPrimitive,1).allOf(x => x < 5) as val1," +
                "prevtail(intPrimitive,1).allOf(x => x < 5) as val2" +
                " from SupportBean#length(2) as sb";
            env.compileDeploy(eplScalar).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, null, null});

            env.sendEventBean(new SupportBean("E2", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false});

            env.sendEventBean(new SupportBean("E3", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, true});

            env.sendEventBean(new SupportBean("E4", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true});

            env.undeployAll();
        }
    }

    private static class ExprEnumUDFStaticMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val1,val2,val3,val4".split(",");
            String epl = "@name('s0') select " +
                "SupportBean_ST0_Container.makeSampleList().where(x => x.p00 < 5) as val1, " +
                "SupportBean_ST0_Container.makeSampleArray().where(x => x.p00 < 5) as val2, " +
                "makeSampleList().where(x => x.p00 < 5) as val3, " +
                "makeSampleArray().where(x => x.p00 < 5) as val4 " +
                "from SupportBean#length(2) as sb";
            env.compileDeploy(epl).addListener("s0");

            SupportBean_ST0_Container.setSamples(new String[]{"E1,1", "E2,20", "E3,3"});
            env.sendEventBean(new SupportBean());
            for (String field : fields) {
                SupportBean_ST0[] result = toArray((Collection) env.listener("s0").assertOneGetNew().get(field));
                assertEquals("Failed for field " + field, 2, result.length);
            }
            env.listener("s0").reset();

            SupportBean_ST0_Container.setSamples(null);
            env.sendEventBean(new SupportBean());
            for (String field : fields) {
                assertNull(env.listener("s0").assertOneGetNew().get(field));
            }
            env.listener("s0").reset();

            SupportBean_ST0_Container.setSamples(new String[0]);
            env.sendEventBean(new SupportBean());
            for (String field : fields) {
                SupportBean_ST0[] result = toArray((Collection) env.listener("s0").assertOneGetNew().get(field));
                assertEquals(0, result.length);
            }
            env.listener("s0").reset();
            env.undeployAll();

            // test UDF returning scalar values collection
            fields = "val0,val1,val2,val3".split(",");
            String eplScalar = "@name('s0') select " +
                "SupportCollection.makeSampleListString().where(x => x != 'E1') as val0, " +
                "SupportCollection.makeSampleArrayString().where(x => x != 'E1') as val1, " +
                "makeSampleListString().where(x => x != 'E1') as val2, " +
                "makeSampleArrayString().where(x => x != 'E1') as val3 " +
                "from SupportBean#length(2) as sb";
            env.compileDeploy(eplScalar).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

            SupportCollection.setSampleCSV("E1,E2,E3");
            env.sendEventBean(new SupportBean());
            for (String field : fields) {
                LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), field, "E2", "E3");
            }
            env.listener("s0").reset();

            SupportCollection.setSampleCSV(null);
            env.sendEventBean(new SupportBean());
            for (String field : fields) {
                LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), field, null);
            }
            env.listener("s0").reset();

            SupportCollection.setSampleCSV("");
            env.sendEventBean(new SupportBean());
            for (String field : fields) {
                LambdaAssertionUtil.assertValuesArrayScalar(env.listener("s0"), field);
            }
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static void trySubstitutionParameter(RegressionEnvironment env, String substitution, Object parameter) {

        EPCompiled compiled = env.compile("@name('s0') select * from SupportBean(" + substitution + ".sequenceEqual({1, intPrimitive, 100}))");
        env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(prepared -> prepared.setObject(1, parameter)));
        env.addListener("s0");

        env.sendEventBean(new SupportBean("E1", 10));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("E2", 20));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static SupportBean_ST0[] toArray(Collection<SupportBean_ST0> it) {
        if (!it.isEmpty() && it.iterator().next() instanceof EventBean) {
            fail("Iterator provides EventBean instances");
        }
        return it.toArray(new SupportBean_ST0[it.size()]);
    }

    private static Map<String, Object> makeBEvent(String symbol) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("a", Collections.singletonMap("symbol", symbol));
        return map;
    }

    private static void assertPropsMapRows(Collection rows, String[] fields, Object[][] objects) {
        Collection<Map> mapsColl = (Collection<Map>) rows;
        Map[] maps = mapsColl.toArray(new Map[mapsColl.size()]);
        EPAssertionUtil.assertPropsPerRow(maps, fields, objects);
    }

    private static void assertColl(String expected, Object value) {
        EPAssertionUtil.assertEqualsExactOrder(expected.split(","), ((Collection) value).toArray());
    }

    public static class MyLocalEvent {
        private Object value;

        public MyLocalEvent(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class MyLocalWithCollection {
        private final Collection someCollection;

        public MyLocalWithCollection(Collection someCollection) {
            this.someCollection = someCollection;
        }

        public Collection getSomeCollection() {
            return someCollection;
        }
    }
}
