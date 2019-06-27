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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.util.SupportInfraUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.common.internal.util.CollectionUtil.buildMap;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowInsertFrom {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraCreateNamedAfterNamed());
        execs.add(new InfraInsertWhereTypeAndFilter());
        execs.add(new InfraInsertWhereOMStaggered());
        execs.add(new InfraInvalid());
        execs.add(new InfraVariantStream());
        return execs;
    }

    private static class InfraCreateNamedAfterNamed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('windowOne') create window MyWindow#keepall as SupportBean;\n" +
                "@name('windowTwo')create window MyWindowTwo#keepall as MyWindow;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "@name('selectOne') select theString from MyWindow;\n";
            env.compileDeploy(epl).addListener("selectOne").addListener("windowOne");

            env.sendEventBean(new SupportBean("E1", 1));
            String[] fields = new String[]{"theString"};
            EPAssertionUtil.assertProps(env.listener("windowOne").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("selectOne").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.undeployAll();
        }
    }

    private static class InfraInsertWhereTypeAndFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};
            RegressionPath path = new RegressionPath();

            String epl = "@name('window') create window MyWindowIWT#keepall as SupportBean;\n" +
                "insert into MyWindowIWT select * from SupportBean(intPrimitive > 0);\n";
            env.compileDeploy(epl, path).addListener("window");

            env.milestone(0);

            // populate some data
            assertEquals(0, getCount(env, path, "window", "MyWindowIWT"));
            env.sendEventBean(new SupportBean("A1", 1));
            assertEquals(1, getCount(env, path, "window", "MyWindowIWT"));
            env.sendEventBean(new SupportBean("B2", 1));

            env.milestone(1);

            env.sendEventBean(new SupportBean("C3", 1));
            env.sendEventBean(new SupportBean("A4", 4));
            env.sendEventBean(new SupportBean("C5", 4));
            assertEquals(5, getCount(env, path, "window", "MyWindowIWT"));
            env.listener("window").reset();

            env.milestone(2);

            // create window with keep-all
            String stmtTextCreateTwo = "@name('windowTwo') create window MyWindowTwo#keepall as MyWindowIWT insert";
            env.compileDeploy(stmtTextCreateTwo, path).addListener("windowTwo");
            EPAssertionUtil.assertPropsPerRow(env.iterator("windowTwo"), fields, new Object[][]{{"A1"}, {"B2"}, {"C3"}, {"A4"}, {"C5"}});
            EventType eventTypeTwo = env.iterator("windowTwo").next().getEventType();
            assertFalse(env.listener("windowTwo").isInvoked());
            assertEquals(5, getCount(env, path, "windowTwo", "MyWindowTwo"));
            assertEquals(StatementType.CREATE_WINDOW, env.statement("windowTwo").getProperty(StatementProperty.STATEMENTTYPE));
            assertEquals("MyWindowTwo", env.statement("windowTwo").getProperty(StatementProperty.CREATEOBJECTNAME));

            // create window with keep-all and filter
            String stmtTextCreateThree = "@name('windowThree') create window MyWindowThree#keepall as MyWindowIWT insert where theString like 'A%'";
            env.compileDeploy(stmtTextCreateThree, path).addListener("windowThree");
            EPAssertionUtil.assertPropsPerRow(env.iterator("windowThree"), fields, new Object[][]{{"A1"}, {"A4"}});
            EventType eventTypeThree = env.iterator("windowThree").next().getEventType();
            assertFalse(env.listener("windowThree").isInvoked());

            env.milestone(3);

            assertEquals(2, getCount(env, path, "windowThree", "MyWindowThree"));

            // create window with last-per-id
            String stmtTextCreateFour = "@name('windowFour') create window MyWindowFour#unique(intPrimitive) as MyWindowIWT insert";
            env.compileDeploy(stmtTextCreateFour, path).addListener("windowFour");
            EPAssertionUtil.assertPropsPerRow(env.iterator("windowFour"), fields, new Object[][]{{"C3"}, {"C5"}});
            EventType eventTypeFour = env.iterator("windowFour").next().getEventType();
            assertFalse(env.listener("windowFour").isInvoked());

            env.milestone(4);

            assertEquals(2, getCount(env, path, "windowFour", "MyWindowFour"));

            env.compileDeploy("insert into MyWindowIWT select * from SupportBean(theString like 'A%')", path);
            env.compileDeploy("insert into MyWindowTwo select * from SupportBean(theString like 'B%')", path);
            env.compileDeploy("insert into MyWindowThree select * from SupportBean(theString like 'C%')", path);
            env.compileDeploy("insert into MyWindowFour select * from SupportBean(theString like 'D%')", path);
            assertFalse(env.listener("window").isInvoked() || env.listener("windowTwo").isInvoked() || env.listener("windowThree").isInvoked() || env.listener("windowFour").isInvoked());

            env.sendEventBean(new SupportBean("B9", -9));
            EventBean received = env.listener("windowTwo").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, fields, new Object[]{"B9"});
            if (!env.isHA()) {
                assertSame(eventTypeTwo, received.getEventType());
            }
            assertFalse(env.listener("window").isInvoked() || env.listener("windowThree").isInvoked() || env.listener("windowFour").isInvoked());
            assertEquals(6, getCount(env, path, "windowTwo", "MyWindowTwo"));

            env.milestone(5);

            env.sendEventBean(new SupportBean("A8", -8));
            received = env.listener("window").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, fields, new Object[]{"A8"});
            assertSame(env.statement("window").getEventType(), received.getEventType());
            assertFalse(env.listener("windowTwo").isInvoked() || env.listener("windowThree").isInvoked() || env.listener("windowFour").isInvoked());

            env.milestone(6);

            env.sendEventBean(new SupportBean("C7", -7));
            received = env.listener("windowThree").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, fields, new Object[]{"C7"});
            if (!env.isHA()) {
                assertSame(eventTypeThree, received.getEventType());
            }
            assertFalse(env.listener("windowTwo").isInvoked() || env.listener("window").isInvoked() || env.listener("windowFour").isInvoked());

            env.sendEventBean(new SupportBean("D6", -6));
            received = env.listener("windowFour").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, fields, new Object[]{"D6"});
            if (!env.isHA()) {
                assertSame(eventTypeFour, received.getEventType());
            }
            assertFalse(env.listener("windowTwo").isInvoked() || env.listener("window").isInvoked() || env.listener("windowThree").isInvoked());

            env.undeployAll();
        }
    }

    private static class InfraInsertWhereOMStaggered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionInsertWhereOMStaggered(env, rep);
            }
        }

        private void tryAssertionInsertWhereOMStaggered(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyWindowIWOM.class) + " @name('window') create window MyWindowIWOM#keepall as select a, b from MyMapAB";
            env.compileDeploy(stmtTextCreateOne, path);
            assertTrue(eventRepresentationEnum.matchesClass(env.statement("window").getEventType().getUnderlyingType()));
            env.addListener("window");

            // create insert into
            String stmtTextInsertOne = "insert into MyWindowIWOM select a, b from MyMapAB";
            env.compileDeploy(stmtTextInsertOne, path);

            // populate some data
            env.sendEventMap(buildMap(new Object[][]{{"a", "E1"}, {"b", 2}}), "MyMapAB");
            env.sendEventMap(buildMap(new Object[][]{{"a", "E2"}, {"b", 10}}), "MyMapAB");
            env.sendEventMap(buildMap(new Object[][]{{"a", "E3"}, {"b", 10}}), "MyMapAB");

            // create window with keep-all using OM
            EPStatementObjectModel model = new EPStatementObjectModel();
            eventRepresentationEnum.addAnnotationForNonMap(model);
            Expression where = Expressions.eq("b", 10);
            model.setCreateWindow(CreateWindowClause.create("MyWindowIWOMTwo", View.create("keepall")).insert(true).insertWhereClause(where).setAsEventTypeName("MyWindowIWOM"));
            model.setSelectClause(SelectClause.createWildcard());
            String text = eventRepresentationEnum.getAnnotationTextForNonMap() + " create window MyWindowIWOMTwo#keepall as select * from MyWindowIWOM insert where b=10";
            assertEquals(text.trim(), model.toEPL());

            EPStatementObjectModel modelTwo = env.eplToModel(text);
            assertEquals(text.trim(), modelTwo.toEPL());
            modelTwo.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("windowTwo")));
            env.compileDeploy(modelTwo, path).addListener("windowTwo");

            EPAssertionUtil.assertPropsPerRow(env.iterator("windowTwo"), "a,b".split(","), new Object[][]{{"E2", 10}, {"E3", 10}});

            // test select individual fields and from an insert-from named window
            env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyWindowIWOMThree.class) + " @name('windowThree') create window MyWindowIWOMThree#keepall as select a from MyWindowIWOMTwo insert where a = 'E2'", path);
            EPAssertionUtil.assertPropsPerRow(env.iterator("windowThree"), "a".split(","), new Object[][]{{"E2"}});

            env.undeployAll();
        }
    }

    private static class InfraVariantStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindowVS#keepall as select * from VarStream", path);
            env.compileDeploy("@name('window') create window MyWindowVSTwo#keepall as MyWindowVS", path);

            env.compileDeploy("insert into VarStream select * from SupportBean_A", path);
            env.compileDeploy("insert into VarStream select * from SupportBean_B", path);
            env.compileDeploy("insert into MyWindowVSTwo select * from VarStream", path);
            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B1"));
            EventBean[] events = EPAssertionUtil.iteratorToArray(env.iterator("window"));
            assertEquals("A1", events[0].get("id?"));
            EPAssertionUtil.assertPropsPerRow(env.iterator("window"), "id?".split(","), new Object[][]{{"A1"}, {"B1"}});

            env.undeployAll();
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = "create window MyWindowINV#keepall as SupportBean";
            env.compileDeploy(stmtTextCreateOne, path);

            tryInvalidCompile(env, "create window testWindow3#keepall as SupportBean insert",
                "A named window by name 'SupportBean' could not be located, the insert-keyword requires an existing named window");
            tryInvalidCompile(env, "create window testWindow3#keepall as select * from SupportBean insert where (intPrimitive = 10)",
                "A named window by name 'SupportBean' could not be located, the insert-keyword requires an existing named window");
            tryInvalidCompile(env, path, "create window MyWindowTwo#keepall as MyWindowINV insert where (select intPrimitive from SupportBean#lastevent)",
                "Create window where-clause may not have a subselect");
            tryInvalidCompile(env, path, "create window MyWindowTwo#keepall as MyWindowINV insert where sum(intPrimitive) > 2",
                "Create window where-clause may not have an aggregation function");
            tryInvalidCompile(env, path, "create window MyWindowTwo#keepall as MyWindowINV insert where prev(1, intPrimitive) = 1",
                "Create window where-clause may not have a function that requires view resources (prior, prev)");

            env.undeployAll();
        }
    }

    private static long getCount(RegressionEnvironment env, RegressionPath path, String statementName, String windowName) {
        if (env.isHA()) {
            return (Long) env.compileExecuteFAF("select count(*) as cnt from " + windowName, path).getArray()[0].get("cnt");
        }
        return SupportInfraUtil.getDataWindowCountNoContext(env, statementName, windowName);
    }

    public static class MyLocalJsonProvidedMyWindowIWOM implements Serializable {
        public String a;
        public int b;
    }

    public static class MyLocalJsonProvidedMyWindowIWOMThree implements Serializable {
        public String a;
    }
}
