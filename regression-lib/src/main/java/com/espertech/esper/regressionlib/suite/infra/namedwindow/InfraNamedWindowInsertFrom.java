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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.util.SupportInfraUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static com.espertech.esper.common.internal.util.CollectionUtil.buildMap;
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
            env.assertPropsNew("windowOne", fields, new Object[]{"E1"});
            env.assertPropsNew("selectOne", fields, new Object[]{"E1"});

            env.undeployAll();
        }
    }

    private static class InfraInsertWhereTypeAndFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString"};
            RegressionPath path = new RegressionPath();

            String epl = "@name('window') @public create window MyWindowIWT#keepall as SupportBean;\n" +
                    "insert into MyWindowIWT select * from SupportBean(intPrimitive > 0);\n";
            env.compileDeploy(epl, path).addListener("window");

            env.milestone(0);

            // populate some data
            env.assertRuntime(runtime -> assertEquals(0, getCount(env, path, "window", "MyWindowIWT")));
            env.sendEventBean(new SupportBean("A1", 1));
            env.assertRuntime(runtime -> assertEquals(1, getCount(env, path, "window", "MyWindowIWT")));
            env.sendEventBean(new SupportBean("B2", 1));

            env.milestone(1);

            env.sendEventBean(new SupportBean("C3", 1));
            env.sendEventBean(new SupportBean("A4", 4));
            env.sendEventBean(new SupportBean("C5", 4));
            env.assertRuntime(runtime -> assertEquals(5, getCount(env, path, "window", "MyWindowIWT")));
            env.listenerReset("window");

            env.milestone(2);

            // create window with keep-all
            String stmtTextCreateTwo = "@name('windowTwo') @public create window MyWindowTwo#keepall as MyWindowIWT insert";
            env.compileDeploy(stmtTextCreateTwo, path).addListener("windowTwo");
            env.assertIterator("windowTwo", iterator -> EPAssertionUtil.assertPropsPerRow(iterator, fields, new Object[][]{{"A1"}, {"B2"}, {"C3"}, {"A4"}, {"C5"}}));
            env.assertListenerNotInvoked("windowTwo");
            env.assertRuntime(runtime -> assertEquals(5, getCount(env, path, "windowTwo", "MyWindowTwo")));
            env.assertStatement("windowTwo", statement -> {
                assertEquals(StatementType.CREATE_WINDOW, statement.getProperty(StatementProperty.STATEMENTTYPE));
                assertEquals("MyWindowTwo", statement.getProperty(StatementProperty.CREATEOBJECTNAME));
            });

            // create window with keep-all and filter
            String stmtTextCreateThree = "@name('windowThree') @public create window MyWindowThree#keepall as MyWindowIWT insert where theString like 'A%'";
            env.compileDeploy(stmtTextCreateThree, path).addListener("windowThree");
            env.assertPropsPerRowIterator("windowThree", fields, new Object[][]{{"A1"}, {"A4"}});
            env.assertListenerNotInvoked("windowThree");

            env.milestone(3);

            env.assertRuntime(runtime -> assertEquals(2, getCount(env, path, "windowThree", "MyWindowThree")));

            // create window with last-per-id
            String stmtTextCreateFour = "@name('windowFour') @public create window MyWindowFour#unique(intPrimitive) as MyWindowIWT insert";
            env.compileDeploy(stmtTextCreateFour, path).addListener("windowFour");
            env.assertPropsPerRowIterator("windowFour", fields, new Object[][]{{"C3"}, {"C5"}});
            env.assertListenerNotInvoked("windowFour");

            env.milestone(4);

            env.assertRuntime(runtime -> assertEquals(2, getCount(env, path, "windowFour", "MyWindowFour")));

            env.compileDeploy("insert into MyWindowIWT select * from SupportBean(theString like 'A%')", path);
            env.compileDeploy("insert into MyWindowTwo select * from SupportBean(theString like 'B%')", path);
            env.compileDeploy("insert into MyWindowThree select * from SupportBean(theString like 'C%')", path);
            env.compileDeploy("insert into MyWindowFour select * from SupportBean(theString like 'D%')", path);
            env.assertListenerNotInvoked("window");
            env.assertListenerNotInvoked("windowTwo");
            env.assertListenerNotInvoked("windowThree");
            env.assertListenerNotInvoked("windowFour");

            env.sendEventBean(new SupportBean("B9", -9));
            env.assertListener("windowTwo", listener -> {
                final EventBean received = listener.assertOneGetNewAndReset();
                EPAssertionUtil.assertProps(received, fields, new Object[]{"B9"});
                if (!env.isHA()) {
                    assertSame(env.statement("windowTwo").getEventType(), received.getEventType());
                }
            });

            env.assertRuntime(runtime -> assertEquals(6, getCount(env, path, "windowTwo", "MyWindowTwo")));
            env.assertListenerNotInvoked("window");
            env.assertListenerNotInvoked("windowThree");
            env.assertListenerNotInvoked("windowFour");

            env.milestone(5);

            env.sendEventBean(new SupportBean("A8", -8));
            env.assertListener("window", listener -> {
                EventBean received = listener.assertOneGetNewAndReset();
                EPAssertionUtil.assertProps(received, fields, new Object[]{"A8"});
                assertSame(env.statement("window").getEventType(), received.getEventType());
            });
            env.assertListenerNotInvoked("windowTwo");
            env.assertListenerNotInvoked("windowThree");
            env.assertListenerNotInvoked("windowFour");

            env.milestone(6);

            env.sendEventBean(new SupportBean("C7", -7));
            env.assertListener("windowThree", listener -> {
                EventBean received = listener.assertOneGetNewAndReset();
                EPAssertionUtil.assertProps(received, fields, new Object[]{"C7"});
                if (!env.isHA()) {
                    assertSame(env.iterator("windowThree").next().getEventType(), received.getEventType());
                }
            });
            env.assertListenerNotInvoked("window");
            env.assertListenerNotInvoked("window");
            env.assertListenerNotInvoked("windowTwo");
            env.assertListenerNotInvoked("windowFour");

            env.sendEventBean(new SupportBean("D6", -6));
            env.assertListener("windowFour", listener -> {
                EventBean received = listener.assertOneGetNewAndReset();
                EPAssertionUtil.assertProps(received, fields, new Object[]{"D6"});
                if (!env.isHA()) {
                    assertSame(env.statement("windowFour").getEventType(), received.getEventType());
                }
            });
            env.assertListenerNotInvoked("window");
            env.assertListenerNotInvoked("windowTwo");
            env.assertListenerNotInvoked("windowThree");

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
            String stmtTextCreateOne = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyWindowIWOM.class) + " @name('window') @public create window MyWindowIWOM#keepall as select a, b from MyMapAB";
            env.compileDeploy(stmtTextCreateOne, path).addListener("window");
            env.assertStatement("window", statement -> assertTrue(eventRepresentationEnum.matchesClass(statement.getEventType().getUnderlyingType())));

            // create insert into
            String stmtTextInsertOne = "@public insert into MyWindowIWOM select a, b from MyMapAB";
            env.compileDeploy(stmtTextInsertOne, path);

            // populate some data
            env.sendEventMap(buildMap(new Object[][]{{"a", "E1"}, {"b", 2}}), "MyMapAB");
            env.sendEventMap(buildMap(new Object[][]{{"a", "E2"}, {"b", 10}}), "MyMapAB");
            env.sendEventMap(buildMap(new Object[][]{{"a", "E3"}, {"b", 10}}), "MyMapAB");

            // create window with keep-all using OM
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setAnnotations(new ArrayList<>(2));
            eventRepresentationEnum.addAnnotationForNonMap(model);
            model.getAnnotations().add(new AnnotationPart("public"));
            Expression where = Expressions.eq("b", 10);
            model.setCreateWindow(CreateWindowClause.create("MyWindowIWOMTwo", View.create("keepall")).insert(true).insertWhereClause(where).setAsEventTypeName("MyWindowIWOM"));
            model.setSelectClause(SelectClause.createWildcard());
            String text = eventRepresentationEnum.getAnnotationTextForNonMap() + " @public create window MyWindowIWOMTwo#keepall as select * from MyWindowIWOM insert where b=10";
            assertEquals(text.trim(), model.toEPL());

            EPStatementObjectModel modelTwo = env.eplToModel(text);
            assertEquals(text.trim(), modelTwo.toEPL());
            modelTwo.setAnnotations(Arrays.asList(AnnotationPart.nameAnnotation("windowTwo"), new AnnotationPart("public")));
            env.compileDeploy(modelTwo, path).addListener("windowTwo");
            env.assertPropsPerRowIterator("windowTwo", "a,b".split(","), new Object[][]{{"E2", 10}, {"E3", 10}});

            // test select individual fields and from an insert-from named window
            env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyWindowIWOMThree.class) + " @name('windowThree') create window MyWindowIWOMThree#keepall as select a from MyWindowIWOMTwo insert where a = 'E2'", path);
            env.assertPropsPerRowIterator("windowThree", "a".split(","), new Object[][]{{"E2"}});

            env.undeployAll();
        }
    }

    private static class InfraVariantStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window MyWindowVS#keepall as select * from VarStream", path);
            env.compileDeploy("@name('window') @public create window MyWindowVSTwo#keepall as MyWindowVS", path);

            env.compileDeploy("insert into VarStream select * from SupportBean_A", path);
            env.compileDeploy("insert into VarStream select * from SupportBean_B", path);
            env.compileDeploy("insert into MyWindowVSTwo select * from VarStream", path);
            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B1"));
            env.assertIterator("window", iterator -> {
                EventBean[] events = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals("A1", events[0].get("id?"));
            });
            env.assertPropsPerRowIterator("window", "id?".split(","), new Object[][]{{"A1"}, {"B1"}});

            env.undeployAll();
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = "@public create window MyWindowINV#keepall as SupportBean";
            env.compileDeploy(stmtTextCreateOne, path);

            env.tryInvalidCompile("create window testWindow3#keepall as SupportBean insert",
                    "A named window by name 'SupportBean' could not be located, the insert-keyword requires an existing named window");
            env.tryInvalidCompile("create window testWindow3#keepall as select * from SupportBean insert where (intPrimitive = 10)",
                    "A named window by name 'SupportBean' could not be located, the insert-keyword requires an existing named window");
            env.tryInvalidCompile(path, "create window MyWindowTwo#keepall as MyWindowINV insert where (select intPrimitive from SupportBean#lastevent)",
                    "Create window where-clause may not have a subselect");
            env.tryInvalidCompile(path, "create window MyWindowTwo#keepall as MyWindowINV insert where sum(intPrimitive) > 2",
                    "Create window where-clause may not have an aggregation function");
            env.tryInvalidCompile(path, "create window MyWindowTwo#keepall as MyWindowINV insert where prev(1, intPrimitive) = 1",
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
