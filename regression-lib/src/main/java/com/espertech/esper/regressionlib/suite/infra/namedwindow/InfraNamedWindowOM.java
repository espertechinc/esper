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
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowOM {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraCompile());
        execs.add(new InfraOM());
        execs.add(new InfraOMCreateTableSyntax());
        return execs;
    }

    private static class InfraCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = new String[]{"key", "value"};
            String stmtTextCreate = "@name('create') create window MyWindow#keepall as select theString as key, longBoxed as value from " + SupportBean.class.getSimpleName();
            EPStatementObjectModel modelCreate = env.eplToModel(stmtTextCreate);
            env.compileDeploy(modelCreate, path).addListener("create");
            assertEquals("@name('create') create window MyWindow#keepall as select theString as key, longBoxed as value from SupportBean", modelCreate.toEPL());

            String stmtTextOnSelect = "@name('onselect') on SupportBean_B select mywin.* from MyWindow as mywin";
            EPStatementObjectModel modelOnSelect = env.eplToModel(stmtTextOnSelect);
            env.compileDeploy(modelOnSelect, path).addListener("onselect");

            String stmtTextInsert = "@name('insert') insert into MyWindow select theString as key, longBoxed as value from SupportBean";
            EPStatementObjectModel modelInsert = env.eplToModel(stmtTextInsert);
            env.compileDeploy(modelInsert, path).addListener("insert");

            String stmtTextSelectOne = "@name('select') select irstream key, value*2 as value from MyWindow(key is not null)";
            EPStatementObjectModel modelSelect = env.eplToModel(stmtTextSelectOne);
            env.compileDeploy(modelSelect, path).addListener("select");
            assertEquals(stmtTextSelectOne, modelSelect.toEPL());

            // send events
            sendSupportBean(env, "E1", 10L);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

            sendSupportBean(env, "E2", 20L);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol=s1.key";
            EPStatementObjectModel modelDelete = env.eplToModel(stmtTextDelete);
            env.compileDeploy(modelDelete, path).addListener("delete");
            assertEquals("@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol=s1.key", modelDelete.toEPL());

            // send delete event
            sendMarketBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});

            // send delete event again, none deleted now
            sendMarketBean(env, "E1");
            assertFalse(env.listener("select").isInvoked());
            assertFalse(env.listener("create").isInvoked());

            // send delete event
            sendMarketBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});

            // trigger on-select on empty window
            assertFalse(env.listener("onselect").isInvoked());
            env.sendEventBean(new SupportBean_B("B1"));
            assertFalse(env.listener("onselect").isInvoked());

            sendSupportBean(env, "E3", 30L);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E3", 60L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

            // trigger on-select on the filled window
            env.sendEventBean(new SupportBean_B("B2"));
            EPAssertionUtil.assertProps(env.listener("onselect").assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

            env.undeployModuleContaining("delete");
            env.undeployModuleContaining("onselect");
            env.undeployModuleContaining("select");
            env.undeployModuleContaining("insert");
            env.undeployModuleContaining("create");
        }
    }

    private static class InfraOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window object model
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setCreateWindow(CreateWindowClause.create("MyWindow").addView("keepall").setAsEventTypeName("SupportBean"));
            model.setSelectClause(SelectClause.create()
                .addWithAsProvidedName("theString", "key")
                .addWithAsProvidedName("longBoxed", "value"));

            String stmtTextCreate = "create window MyWindow#keepall as select theString as key, longBoxed as value from SupportBean";
            assertEquals(stmtTextCreate, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("create")));
            env.compileDeploy(model, path).addListener("create");

            String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from SupportBean";
            env.eplToModelCompileDeploy(stmtTextInsert, path);

            // Consumer statement object model
            model = new EPStatementObjectModel();
            Expression multi = Expressions.multiply(Expressions.property("value"), Expressions.constant(2));
            model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("key")
                .add(multi, "value"));
            model.setFromClause(FromClause.create(FilterStream.create("MyWindow", Expressions.isNotNull("value"))));
            String eplSelect = "select irstream key, value*2 as value from MyWindow(value is not null)";
            assertEquals(eplSelect, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("select")));
            env.compileDeploy(model, path).addListener("select");

            // send events
            sendSupportBean(env, "E1", 10L);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

            sendSupportBean(env, "E2", 20L);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

            // create delete stmt
            model = new EPStatementObjectModel();
            model.setOnExpr(OnClause.createOnDelete("MyWindow", "s1"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportMarketDataBean", "s0")));
            model.setWhereClause(Expressions.eqProperty("s0.symbol", "s1.key"));

            String stmtTextDelete = "on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol=s1.key";
            assertEquals(stmtTextDelete, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("ondelete")));
            env.compileDeploy(model, path).addListener("ondelete");

            // send delete event
            sendMarketBean(env, "E1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});

            // send delete event again, none deleted now
            sendMarketBean(env, "E1");
            assertFalse(env.listener("select").isInvoked());
            assertFalse(env.listener("create").isInvoked());

            // send delete event
            sendMarketBean(env, "E2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});

            // On-select object model
            model = new EPStatementObjectModel();
            model.setOnExpr(OnClause.createOnSelect("MyWindow", "s1"));
            model.setWhereClause(Expressions.eqProperty("s0.id", "s1.key"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_B", "s0")));
            model.setSelectClause(SelectClause.createStreamWildcard("s1"));

            String stmtTextOnSelect = "on SupportBean_B as s0 select s1.* from MyWindow as s1 where s0.id=s1.key";
            assertEquals(stmtTextOnSelect, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("onselect")));
            env.compileDeploy(model, path).addListener("onselect");

            // send some more events
            sendSupportBean(env, "E3", 30L);
            sendSupportBean(env, "E4", 40L);

            env.sendEventBean(new SupportBean_B("B1"));
            assertFalse(env.listener("onselect").isInvoked());

            // trigger on-select
            env.sendEventBean(new SupportBean_B("E3"));
            EPAssertionUtil.assertProps(env.listener("onselect").assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

            env.undeployAll();
        }
    }

    private static class InfraOMCreateTableSyntax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expected = "create window MyWindowOM#keepall as (a1 string, a2 double, a3 int)";

            // create window object model
            EPStatementObjectModel model = new EPStatementObjectModel();
            CreateWindowClause clause = CreateWindowClause.create("MyWindowOM").addView("keepall");
            clause.addColumn(new SchemaColumnDesc("a1", "string"));
            clause.addColumn(new SchemaColumnDesc("a2", "double"));
            clause.addColumn(new SchemaColumnDesc("a3", "int"));
            model.setCreateWindow(clause);
            assertEquals(expected, model.toEPL());
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongBoxed(longBoxed);
        env.sendEventBean(bean);
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        env.sendEventBean(bean);
    }
}
