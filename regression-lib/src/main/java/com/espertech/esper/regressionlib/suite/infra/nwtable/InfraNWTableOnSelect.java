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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.util.IndexAssertion;
import com.espertech.esper.regressionlib.support.util.IndexAssertionEventSend;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

public class InfraNWTableOnSelect implements IndexBackingTableInfo {
    private static final Logger log = LoggerFactory.getLogger(InfraNWTableOnSelect.class);

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraOnSelectIndexSimple(true));
        execs.add(new InfraOnSelectIndexSimple(false));

        execs.add(new InfraOnSelectIndexChoice(true));
        execs.add(new InfraOnSelectIndexChoice(false));

        execs.add(new InfraWindowAgg(true));
        execs.add(new InfraWindowAgg(false));

        execs.add(new InfraSelectAggregationHavingStreamWildcard(true));
        execs.add(new InfraSelectAggregationHavingStreamWildcard(false));

        execs.add(new InfraPatternTimedSelect(true));
        execs.add(new InfraPatternTimedSelect(false));

        execs.add(new InfraInvalid(true));
        execs.add(new InfraInvalid(false));

        execs.add(new InfraSelectCondition(true));
        execs.add(new InfraSelectCondition(false));

        execs.add(new InfraSelectJoinColumnsLimit(true));
        execs.add(new InfraSelectJoinColumnsLimit(false));

        execs.add(new InfraSelectAggregation(true));
        execs.add(new InfraSelectAggregation(false));

        execs.add(new InfraSelectAggregationCorrelated(true));
        execs.add(new InfraSelectAggregationCorrelated(false));

        execs.add(new InfraSelectAggregationGrouping(true));
        execs.add(new InfraSelectAggregationGrouping(false));

        execs.add(new InfraSelectCorrelationDelete(true));
        execs.add(new InfraSelectCorrelationDelete(false));

        execs.add(new InfraPatternCorrelation(true));
        execs.add(new InfraPatternCorrelation(false));

        execs.add(new InfraOnSelectMultikeyWArray(true));
        execs.add(new InfraOnSelectMultikeyWArray(false));
        return execs;
    }

    private static class InfraOnSelectMultikeyWArray implements RegressionExecution {
        private final boolean namedWindow;

        public InfraOnSelectMultikeyWArray(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"a", "b"};
            RegressionPath path = new RegressionPath();

            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfraPC#keepall as (id string, array int[], value int)" :
                "@name('create') create table MyInfraPC(id string primary key, array int[], value int)";
            env.compileDeploy(stmtTextCreate, path);

            String stmtTextSelect = "@name('s0') on SupportBean select array, sum(value) as thesum from MyInfraPC group by array";
            env.compileDeploy(stmtTextSelect, path).addListener("s0");

            env.compileExecuteFAF("insert into MyInfraPC values('E1', {1, 2}, 10)", path);
            env.compileExecuteFAF("insert into MyInfraPC values('E2', {1, 2}, 11)", path);

            env.milestone(0);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "thesum".split(","), new Object[] {21});

            env.compileExecuteFAF("insert into MyInfraPC values('E3', {1, 2}, 21)", path);
            env.compileExecuteFAF("insert into MyInfraPC values('E4', {1}, 22)", path);

            env.milestone(1);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "thesum".split(","), new Object[][] {{42}, {22}});

            env.undeployAll();
        }
    }

    public static class InfraOnSelectIndexSimple implements RegressionExecution {
        private final boolean namedWindow;

        public InfraOnSelectIndexSimple(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            if (namedWindow) {
                env.compileDeploy("create window MyInfra.win:length(5) as (numericKey int, value string)", path);
            } else {
                env.compileDeploy("create table MyInfra(numericKey int primary key, value string)", path);
            }
            env.compileDeploy("create index MyIndex on MyInfra(value)", path);
            env.compileDeploy("insert into MyInfra select intPrimitive as numericKey, theString as value from SupportBean", path);

            String epl = "@name('out') on SupportBean_S0 as s0 select value from MyInfra where value = p00";
            env.compileDeploy(epl, path).addListener("out");

            sendSupportBean(env, "E1", 1);
            sendSupportBean_S0(env, 1, "E1");
            EPAssertionUtil.assertProps(env.listener("out").assertOneGetNewAndReset(), "value".split(","), new Object[]{"E1"});

            env.milestone(0);

            sendSupportBean(env, "E2", 2);
            sendSupportBean_S0(env, 2, "E2");
            EPAssertionUtil.assertProps(env.listener("out").assertOneGetNewAndReset(), "value".split(","), new Object[]{"E2"});

            env.undeployAll();
        }
    }

    private static class InfraPatternCorrelation implements RegressionExecution {
        private final boolean namedWindow;

        public InfraPatternCorrelation(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"a", "b"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfraPC#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('create') create table MyInfraPC(a string primary key, b int primary key)";
            env.compileDeploy(stmtTextCreate, path);

            // create select stmt
            String stmtTextSelect = "@name('select') on pattern [every ea=SupportBean_A or every eb=SupportBean_B] select mywin.* from MyInfraPC as mywin where a = coalesce(ea.id, eb.id)";
            env.compileDeploy(stmtTextSelect, path).addListener("select");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfraPC select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // send 3 event
            sendSupportBean(env, "E1", 1);

            env.milestone(0);

            sendSupportBean(env, "E2", 2);
            sendSupportBean(env, "E3", 3);
            assertFalse(env.listener("select").isInvoked());

            // fire trigger
            sendSupportBean_A(env, "X1");
            assertFalse(env.listener("select").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(env.iterator("select"), fields, null);
            }

            env.milestone(1);

            sendSupportBean_B(env, "E2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

            sendSupportBean_A(env, "E1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            env.milestone(2);

            sendSupportBean_B(env, "E3");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

            env.undeployAll();
        }
    }

    private static class InfraSelectCorrelationDelete implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectCorrelationDelete(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"a", "b"};

            String epl = namedWindow ?
                "@name('create') create window MyInfraSCD#keepall as select theString as a, intPrimitive as b from SupportBean;\n" :
                "@name('create') create table MyInfraSCD(a string primary key, b int primary key);\n";
            epl += "@name('select') on SupportBean_A select mywin.* from MyInfraSCD as mywin where id = a;\n";
            epl += "insert into MyInfraSCD select theString as a, intPrimitive as b from SupportBean;\n";
            epl += "@name('delete') on SupportBean_B delete from MyInfraSCD where a = id;\n";
            env.compileDeploy(epl).addListener("select");

            // send 3 event
            sendSupportBean(env, "E1", 1);
            sendSupportBean(env, "E2", 2);
            sendSupportBean(env, "E3", 3);
            assertFalse(env.listener("select").isInvoked());

            env.milestone(0);

            // fire trigger
            sendSupportBean_A(env, "X1");
            assertFalse(env.listener("select").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

            sendSupportBean_A(env, "E2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

            env.milestone(1);

            sendSupportBean_A(env, "E1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

            // delete event
            sendSupportBean_B(env, "E1");
            assertFalse(env.listener("select").isInvoked());

            sendSupportBean_A(env, "E1");
            assertFalse(env.listener("select").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E2", 2}, {"E3", 3}});

            env.milestone(2);

            sendSupportBean_A(env, "E2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

            env.undeployModuleContaining("select");
        }
    }

    private static class InfraSelectAggregationGrouping implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectAggregationGrouping(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"a", "sumb"};

            RegressionPath path = new RegressionPath();
            String epl = namedWindow ?
                "@name('create') create window MyInfraSAG#keepall as select theString as a, intPrimitive as b from SupportBean;\n" :
                "@name('create') create table MyInfraSAG(a string primary key, b int primary key);\n";
            epl += "@name('select') on SupportBean_A select a, sum(b) as sumb from MyInfraSAG group by a order by a desc;\n";
            epl += "@name('selectTwo') on SupportBean_A select a, sum(b) as sumb from MyInfraSAG group by a having sum(b) > 5 order by a desc;\n";
            epl += "@name('insert') insert into MyInfraSAG select theString as a, intPrimitive as b from SupportBean;\n";
            env.compileDeploy(epl, path).addListener("select").addListener("selectTwo");

            // fire trigger
            sendSupportBean_A(env, "A1");
            assertFalse(env.listener("select").isInvoked());
            assertFalse(env.listener("selectTwo").isInvoked());

            // send 3 events
            sendSupportBean(env, "E1", 1);
            sendSupportBean(env, "E2", 2);

            env.milestone(0);

            sendSupportBean(env, "E1", 5);
            assertFalse(env.listener("select").isInvoked());
            assertFalse(env.listener("selectTwo").isInvoked());

            // fire trigger
            sendSupportBean_A(env, "A1");
            EPAssertionUtil.assertPropsPerRow(env.listener("select").getLastNewData(), fields, new Object[][]{{"E2", 2}, {"E1", 6}});
            assertNull(env.listener("select").getLastOldData());
            env.listener("select").reset();
            EPAssertionUtil.assertPropsPerRow(env.listener("selectTwo").getLastNewData(), fields, new Object[][]{{"E1", 6}});
            assertNull(env.listener("select").getLastOldData());
            env.listener("select").reset();

            env.milestone(1);

            // send 3 events
            sendSupportBean(env, "E4", -1);
            sendSupportBean(env, "E2", 10);
            sendSupportBean(env, "E1", 100);
            assertFalse(env.listener("select").isInvoked());

            env.milestone(2);

            sendSupportBean_A(env, "A2");
            EPAssertionUtil.assertPropsPerRow(env.listener("select").getLastNewData(), fields, new Object[][]{{"E4", -1}, {"E2", 12}, {"E1", 106}});

            // create delete stmt, delete E2
            String stmtTextDelete = "on SupportBean_B delete from MyInfraSAG where id = a";
            env.compileDeploy(stmtTextDelete, path);
            sendSupportBean_B(env, "E2");

            sendSupportBean_A(env, "A3");
            EPAssertionUtil.assertPropsPerRow(env.listener("select").getLastNewData(), fields, new Object[][]{{"E4", -1}, {"E1", 106}});
            assertNull(env.listener("select").getLastOldData());
            env.listener("select").reset();
            EPAssertionUtil.assertPropsPerRow(env.listener("selectTwo").getLastNewData(), fields, new Object[][]{{"E1", 106}});
            assertNull(env.listener("selectTwo").getLastOldData());
            env.listener("selectTwo").reset();

            EventType resultType = env.statement("select").getEventType();
            assertEquals(2, resultType.getPropertyNames().length);
            assertEquals(String.class, resultType.getPropertyType("a"));
            assertEquals(Integer.class, resultType.getPropertyType("sumb"));

            env.undeployAll();
        }
    }

    private static class InfraSelectAggregationCorrelated implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectAggregationCorrelated(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"sumb"};

            String epl = namedWindow ?
                "@name('create') create window MyInfraSAC#keepall as select theString as a, intPrimitive as b from SupportBean;\n" :
                "@name('create') create table MyInfraSAC(a string primary key, b int primary key);\n";
            epl += "@name('select') on SupportBean_A select sum(b) as sumb from MyInfraSAC where a = id;\n";
            epl += "insert into MyInfraSAC select theString as a, intPrimitive as b from SupportBean;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            // send 3 event
            sendSupportBean(env, "E1", 1);
            sendSupportBean(env, "E2", 2);

            env.milestone(0);

            sendSupportBean(env, "E3", 3);
            assertFalse(env.listener("select").isInvoked());

            // fire trigger
            sendSupportBean_A(env, "A1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.milestone(1);

            // fire trigger
            sendSupportBean_A(env, "E2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{2});

            sendSupportBean(env, "E2", 10);

            env.milestone(2);

            sendSupportBean_A(env, "E2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{12});

            EventType resultType = env.statement("select").getEventType();
            assertEquals(1, resultType.getPropertyNames().length);
            assertEquals(Integer.class, resultType.getPropertyType("sumb"));

            env.undeployModuleContaining("create");
        }
    }

    private static class InfraSelectAggregation implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectAggregation(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"sumb"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfraSA#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('create') create table MyInfraSA (a string primary key, b int primary key)";
            env.compileDeploy(stmtTextCreate, path);

            // create select stmt
            String stmtTextSelect = "@name('select') on SupportBean_A select sum(b) as sumb from MyInfraSA";
            env.compileDeploy(stmtTextSelect, path).addListener("select");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfraSA select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // send 3 event
            sendSupportBean(env, "E1", 1);
            sendSupportBean(env, "E2", 2);
            sendSupportBean(env, "E3", 3);
            assertFalse(env.listener("select").isInvoked());

            env.milestone(0);

            // fire trigger
            sendSupportBean_A(env, "A1");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{6});

            // create delete stmt
            String stmtTextDelete = "on SupportBean_B delete from MyInfraSA where id = a";
            env.compileDeploy(stmtTextDelete, path);

            // Delete E2
            sendSupportBean_B(env, "E2");

            env.milestone(1);

            // fire trigger
            sendSupportBean_A(env, "A2");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{4});

            sendSupportBean(env, "E4", 10);
            sendSupportBean_A(env, "A3");
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{14});

            EventType resultType = env.statement("select").getEventType();
            assertEquals(1, resultType.getPropertyNames().length);
            assertEquals(Integer.class, resultType.getPropertyType("sumb"));

            env.undeployAll();
        }
    }

    private static class InfraSelectJoinColumnsLimit implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectJoinColumnsLimit(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"triggerid", "wina", "b"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfraSA#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('create') create table MyInfraSA (a string primary key, b int)";
            env.compileDeploy(stmtTextCreate, path);

            // create select stmt
            String stmtTextSelect = "@name('select') on SupportBean_A as trigger select trigger.id as triggerid, win.a as wina, b from MyInfraSA as win order by wina";
            env.compileDeploy(stmtTextSelect, path).addListener("select");

            // create insert into
            String stmtTextInsertOne = "insert into MyInfraSA select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // send 3 event
            sendSupportBean(env, "E1", 1);
            sendSupportBean(env, "E2", 2);
            assertFalse(env.listener("select").isInvoked());

            env.milestone(0);

            // fire trigger
            sendSupportBean_A(env, "A1");
            assertEquals(2, env.listener("select").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("select").getLastNewData()[0], fields, new Object[]{"A1", "E1", 1});
            EPAssertionUtil.assertProps(env.listener("select").getLastNewData()[1], fields, new Object[]{"A1", "E2", 2});

            // try limit clause
            env.undeployModuleContaining("select");
            stmtTextSelect = "@name('select') on SupportBean_A as trigger select trigger.id as triggerid, win.a as wina, b from MyInfraSA as win order by wina limit 1";
            env.compileDeploy(stmtTextSelect, path).addListener("select");

            env.milestone(1);

            sendSupportBean_A(env, "A1");
            assertEquals(1, env.listener("select").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("select").getLastNewData()[0], fields, new Object[]{"A1", "E1", 1});

            env.undeployAll();
        }
    }

    private static class InfraSelectCondition implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectCondition(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fieldsCreate = new String[]{"a", "b"};
            String[] fieldsOnSelect = new String[]{"a", "b", "id"};
            RegressionPath path = new RegressionPath();

            // create window
            String infraName = "MyInfraSC" + (namedWindow ? "NW" : "Tbl");
            String stmtTextCreate = namedWindow ?
                "@name('create') create window " + infraName + "#keepall as select theString as a, intPrimitive as b from SupportBean" :
                "@name('create') create table " + infraName + " (a string primary key, b int)";
            env.compileDeploy(stmtTextCreate, path);

            // create select stmt
            String stmtTextSelect = "@name('select') on SupportBean_A select mywin.*, id from " + infraName + " as mywin where " + infraName + ".b < 3 order by a asc";
            env.compileDeploy(stmtTextSelect, path).addListener("select");
            assertEquals(StatementType.ON_SELECT, env.statement("select").getProperty(StatementProperty.STATEMENTTYPE));

            // create insert into
            String stmtTextInsertOne = "@name('insert') insert into " + infraName + " select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // send 3 event
            sendSupportBean(env, "E1", 1);

            env.milestone(0);

            sendSupportBean(env, "E2", 2);
            sendSupportBean(env, "E3", 3);
            assertFalse(env.listener("select").isInvoked());

            // fire trigger
            sendSupportBean_A(env, "A1");
            assertEquals(2, env.listener("select").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("select").getLastNewData()[0], fieldsCreate, new Object[]{"E1", 1});
            EPAssertionUtil.assertProps(env.listener("select").getAndResetLastNewData()[1], fieldsCreate, new Object[]{"E2", 2});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fieldsCreate, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});
            assertFalse(env.iterator("select").hasNext());

            sendSupportBean(env, "E4", 0);

            env.milestone(1);

            sendSupportBean_A(env, "A2");
            assertEquals(3, env.listener("select").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("select").getLastNewData()[0], fieldsOnSelect, new Object[]{"E1", 1, "A2"});
            EPAssertionUtil.assertProps(env.listener("select").getLastNewData()[1], fieldsOnSelect, new Object[]{"E2", 2, "A2"});
            EPAssertionUtil.assertProps(env.listener("select").getAndResetLastNewData()[2], fieldsOnSelect, new Object[]{"E4", 0, "A2"});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fieldsCreate, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}, {"E4", 0}});
            assertFalse(env.iterator("select").hasNext());

            env.undeployModuleContaining("select");
            env.undeployModuleContaining("insert");
            env.undeployModuleContaining("create");
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        private final boolean namedWindow;

        public InfraInvalid(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreate = namedWindow ?
                "create window MyInfraInvalid#keepall as select * from SupportBean" :
                "create table MyInfraInvalid (theString string, intPrimitive int)";
            env.compileDeploy(stmtTextCreate, path);

            SupportMessageAssertUtil.tryInvalidCompile(env, path, "on SupportBean_A select * from MyInfraInvalid where sum(intPrimitive) > 100",
                "Error validating expression: An aggregate function may not appear in a WHERE clause (use the HAVING clause) [");

            SupportMessageAssertUtil.tryInvalidCompile(env, path, "on SupportBean_A insert into MyStream select * from DUMMY",
                "A named window or table 'DUMMY' has not been declared [");

            SupportMessageAssertUtil.tryInvalidCompile(env, path, "on SupportBean_A select prev(1, theString) from MyInfraInvalid",
                "Failed to validate select-clause expression 'prev(1,theString)': Previous function cannot be used in this context [");

            env.undeployAll();
        }
    }

    private static class InfraPatternTimedSelect implements RegressionExecution {
        private final boolean namedWindow;

        public InfraPatternTimedSelect(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            // test for JIRA ESPER-332
            sendTimer(0, env);
            RegressionPath path = new RegressionPath();

            String stmtTextCreate = namedWindow ?
                "create window MyInfraPTS#keepall as select * from SupportBean" :
                "create table MyInfraPTS as (theString string)";
            env.compileDeploy(stmtTextCreate, path);

            String stmtCount = "on pattern[every timer:interval(10 sec)] select count(eve), eve from MyInfraPTS as eve";
            env.compileDeploy(stmtCount, path);

            String stmtTextOnSelect = "@name('select') on pattern [every timer:interval(10 sec)] select theString from MyInfraPTS having count(theString) > 0";
            env.compileDeploy(stmtTextOnSelect, path).addListener("select");

            String stmtTextInsertOne = namedWindow ?
                "insert into MyInfraPTS select * from SupportBean" :
                "insert into MyInfraPTS select theString from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            sendTimer(11000, env);
            assertFalse(env.listener("select").isInvoked());

            env.milestone(0);

            sendTimer(21000, env);
            assertFalse(env.listener("select").isInvoked());

            sendSupportBean(env, "E1", 1);
            sendTimer(31000, env);
            assertEquals("E1", env.listener("select").assertOneGetNewAndReset().get("theString"));

            env.undeployAll();
        }
    }

    private static class InfraSelectAggregationHavingStreamWildcard implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectAggregationHavingStreamWildcard(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // create window
            String stmtTextCreate = namedWindow ?
                "create window MyInfraSHS#keepall as (a string, b int)" :
                "create table MyInfraSHS as (a string primary key, b int primary key)";
            env.compileDeploy(stmtTextCreate, path);

            String stmtTextInsertOne = "insert into MyInfraSHS select theString as a, intPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            String stmtTextSelect = "@name('select') on SupportBean_A select mwc.* as mwcwin from MyInfraSHS mwc where id = a group by a having sum(b) = 20";
            env.compileDeploy(stmtTextSelect, path).addListener("select");
            assertFalse(((EPStatementSPI) env.statement("select")).getStatementContext().isStatelessSelect());

            // send 3 event
            sendSupportBean(env, "E1", 16);
            sendSupportBean(env, "E2", 2);

            env.milestone(0);

            sendSupportBean(env, "E1", 4);

            // fire trigger
            sendSupportBean_A(env, "E1");
            EventBean[] events = env.listener("select").getLastNewData();
            assertEquals(2, events.length);
            assertEquals("E1", events[0].get("mwcwin.a"));
            assertEquals("E1", events[1].get("mwcwin.a"));

            env.undeployAll();
        }
    }

    private static class InfraWindowAgg implements RegressionExecution {
        private final boolean namedWindow;

        public InfraWindowAgg(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "create window MyInfraWA#keepall as SupportBean" :
                "create table MyInfraWA(theString string primary key, intPrimitive int)";
            env.compileDeploy(eplCreate, path);
            String eplInsert = namedWindow ?
                "insert into MyInfraWA select * from SupportBean" :
                "insert into MyInfraWA select theString, intPrimitive from SupportBean";
            env.compileDeploy(eplInsert, path);
            env.compileDeploy("on SupportBean_S1 as s1 delete from MyInfraWA where s1.p10 = theString", path);

            String epl = "@name('select') on SupportBean_S0 as s0 " +
                "select window(win.*) as c0," +
                "window(win.*).where(v => v.intPrimitive < 2) as c1, " +
                "window(win.*).toMap(k=>k.theString,v=>v.intPrimitive) as c2 " +
                "from MyInfraWA as win";
            env.compileDeploy(epl, path).addListener("select");

            SupportBean[] beans = new SupportBean[3];
            for (int i = 0; i < beans.length; i++) {
                beans[i] = new SupportBean("E" + i, i);
            }

            env.sendEventBean(beans[0]);
            env.sendEventBean(beans[1]);
            env.sendEventBean(new SupportBean_S0(10));
            assertReceived(env, namedWindow, beans, new int[]{0, 1}, new int[]{0, 1}, "E0,E1".split(","), new Object[]{0, 1});

            // add bean
            env.sendEventBean(beans[2]);
            env.sendEventBean(new SupportBean_S0(10));
            assertReceived(env, namedWindow, beans, new int[]{0, 1, 2}, new int[]{0, 1}, "E0,E1,E2".split(","), new Object[]{0, 1, 2});

            env.milestone(0);

            // delete bean
            env.sendEventBean(new SupportBean_S1(11, "E1"));
            env.sendEventBean(new SupportBean_S0(12));
            assertReceived(env, namedWindow, beans, new int[]{0, 2}, new int[]{0}, "E0,E2".split(","), new Object[]{0, 2});

            // delete another bean
            env.sendEventBean(new SupportBean_S1(13, "E0"));
            env.sendEventBean(new SupportBean_S0(14));
            assertReceived(env, namedWindow, beans, new int[]{2}, new int[0], "E2".split(","), new Object[]{2});

            env.milestone(1);

            // delete last bean
            env.sendEventBean(new SupportBean_S1(15, "E2"));
            env.sendEventBean(new SupportBean_S0(16));
            assertReceived(env, namedWindow, beans, null, null, null, null);

            env.undeployAll();
        }
    }

    private static class InfraOnSelectIndexChoice implements RegressionExecution {
        private final boolean namedWindow;

        public InfraOnSelectIndexChoice(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String backingUniqueS1 = "unique hash={s1(string)} btree={} advanced={}";
            String backingUniqueS1L1 = "unique hash={s1(string),l1(long)} btree={} advanced={}";
            String backingNonUniqueS1 = "non-unique hash={s1(string)} btree={} advanced={}";
            String backingUniqueS1D1 = "unique hash={s1(string),d1(double)} btree={} advanced={}";
            String backingBtreeI1 = "non-unique hash={} btree={i1(int)} advanced={}";
            String backingBtreeD1 = "non-unique hash={} btree={d1(double)} advanced={}";
            String expectedIdxNameS1 = namedWindow ? null : "MyInfra";

            Object[] preloadedEventsOne = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
            IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "ssb2.s2,ssb1.s1,ssb1.i1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 50, 21, 22));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 20});
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 60, 11, 12));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10});
                }
            };

            // single index one field (std:unique(s1))
            assertIndexChoice(env, namedWindow, new String[0], preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = s2", expectedIdxNameS1, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", expectedIdxNameS1, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", expectedIdxNameS1, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2")// busted
                });

            // single index one field (std:unique(s1))
            if (namedWindow) {
                String[] indexOneField = new String[]{"create unique index One on MyInfra (s1)"};
                assertIndexChoice(env, namedWindow, indexOneField, preloadedEventsOne, "std:unique(s1)",
                    new IndexAssertion[]{
                        new IndexAssertion(null, "s1 = s2", "One", backingUniqueS1, eventSendAssertion),
                        new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1, eventSendAssertion),
                        new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1, eventSendAssertion),
                        new IndexAssertion("@Hint('index(Two,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2")// busted
                    });
            }

            // single index two field  (std:unique(s1))
            String[] indexTwoField = new String[]{"create unique index One on MyInfra (s1, l1)"};
            assertIndexChoice(env, namedWindow, indexTwoField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", expectedIdxNameS1, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1L1, eventSendAssertion),
                });
            assertIndexChoice(env, namedWindow, indexTwoField, preloadedEventsOne, "win:keepall()",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", expectedIdxNameS1, namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1L1, eventSendAssertion),
                });

            // two index one unique  (std:unique(s1))
            String[] indexSetTwo = new String[]{
                "create index One on MyInfra (s1)",
                "create unique index Two on MyInfra (s1, d1)"};
            assertIndexChoice(env, namedWindow, indexSetTwo, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2"), // busted
                    new IndexAssertion("@Hint('index(explicit,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and d1 = ssb2.d2 and l1 = ssb2.l2", namedWindow ? "Two" : "MyInfra", namedWindow ? backingUniqueS1D1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(explicit,bust)')", "d1 = ssb2.d2 and l1 = ssb2.l2"), // busted
                });

            // two index one unique  (win:keepall)
            assertIndexChoice(env, namedWindow, indexSetTwo, preloadedEventsOne, "win:keepall()",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2"), // busted
                    new IndexAssertion("@Hint('index(explicit,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and d1 = ssb2.d2 and l1 = ssb2.l2", namedWindow ? "Two" : "MyInfra", namedWindow ? backingUniqueS1D1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(explicit,bust)')", "d1 = ssb2.d2 and l1 = ssb2.l2"), // busted
                });

            // range  (std:unique(s1))
            IndexAssertionEventSend noAssertion = new IndexAssertionEventSend() {
                public void run() {
                }
            };
            String[] indexSetThree = new String[]{
                "create index One on MyInfra (i1 btree)",
                "create index Two on MyInfra (d1 btree)"};
            assertIndexChoice(env, namedWindow, indexSetThree, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "i1 between 1 and 10", "One", backingBtreeI1, noAssertion),
                    new IndexAssertion(null, "d1 between 1 and 10", "Two", backingBtreeD1, noAssertion),
                    new IndexAssertion("@Hint('index(One, bust)')", "d1 between 1 and 10")// busted
                });

            // rel op
            Object[] preloadedEventsRelOp = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12)};
            IndexAssertionEventSend relOpAssertion = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "ssb2.s2,ssb1.s1,ssb1.i1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("EX", 0, 0, 0));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EX", "E1", 10});
                }
            };
            assertIndexChoice(env, namedWindow, new String[0], preloadedEventsRelOp, "win:keepall()",
                new IndexAssertion[]{
                    new IndexAssertion(null, "9 < i1", null, namedWindow ? backingBtreeI1 : null, relOpAssertion),
                    new IndexAssertion(null, "10 <= i1", null, namedWindow ? backingBtreeI1 : null, relOpAssertion),
                    new IndexAssertion(null, "i1 <= 10", null, namedWindow ? backingBtreeI1 : null, relOpAssertion),
                    new IndexAssertion(null, "i1 < 11", null, namedWindow ? backingBtreeI1 : null, relOpAssertion),
                    new IndexAssertion(null, "11 > i1", null, namedWindow ? backingBtreeI1 : null, relOpAssertion),
                });
        }
    }

    private static void assertIndexChoice(RegressionEnvironment env, boolean namedWindow, String[] indexes, Object[] preloadedEvents, String datawindow,
                                          IndexAssertion[] assertions) {
        RegressionPath path = new RegressionPath();
        String eplCreate = namedWindow ?
            "@name('create-window') create window MyInfra." + datawindow + " as SupportSimpleBeanOne" :
            "@name('create-table') create table MyInfra(s1 string primary key, i1 int, d1 double, l1 long)";
        env.compileDeploy(eplCreate, path);

        env.compileDeploy("insert into MyInfra select s1,i1,d1,l1 from SupportSimpleBeanOne", path);
        for (String index : indexes) {
            env.compileDeploy("@name('create-index') " + index, path);
        }
        for (Object event : preloadedEvents) {
            env.sendEventBean(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String consumeEpl = INDEX_CALLBACK_HOOK +
                (assertion.getHint() == null ? "" : assertion.getHint()) +
                "@name('s0') on SupportSimpleBeanTwo as ssb2 " +
                "select * " +
                "from MyInfra as ssb1 where " + assertion.getWhereClause();

            String epl = "@name('s0') " + consumeEpl;
            if (assertion.getEventSendAssertion() == null) {
                try {
                    env.compileWCheckedEx(epl, path);
                    fail();
                } catch (EPCompileException ex) {
                    assertTrue(ex.getMessage().contains("index hint busted"));
                }
            } else {
                env.compileDeploy(epl, path).addListener("s0");
                SupportQueryPlanIndexHook.assertOnExprTableAndReset(assertion.getExpectedIndexName(), assertion.getIndexBackingClass());
                assertion.getEventSendAssertion().run();
                env.undeployModuleContaining("s0");
            }
        }

        env.undeployAll();
    }

    private static void assertReceived(RegressionEnvironment env, boolean namedWindow, SupportBean[] beans, int[] indexesAll, int[] indexesWhere, String[] mapKeys, Object[] mapValues) {
        EventBean received = env.listener("select").assertOneGetNewAndReset();
        Object[] expectedAll;
        Object[] expectedWhere;
        if (!namedWindow) {
            expectedAll = SupportBean.getOAStringAndIntPerIndex(beans, indexesAll);
            expectedWhere = SupportBean.getOAStringAndIntPerIndex(beans, indexesWhere);
            EPAssertionUtil.assertEqualsAnyOrder(expectedAll, (Object[]) received.get("c0"));
            Collection receivedColl = (Collection) received.get("c1");
            EPAssertionUtil.assertEqualsAnyOrder(expectedWhere, receivedColl == null ? null : receivedColl.toArray());
        } else {
            expectedAll = SupportBean.getBeansPerIndex(beans, indexesAll);
            expectedWhere = SupportBean.getBeansPerIndex(beans, indexesWhere);
            EPAssertionUtil.assertEqualsExactOrder(expectedAll, (Object[]) received.get("c0"));
            EPAssertionUtil.assertEqualsExactOrder(expectedWhere, (Collection) received.get("c1"));
        }
        EPAssertionUtil.assertPropsMap((Map) received.get("c2"), mapKeys, mapValues);
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String id) {
        SupportBean_A bean = new SupportBean_A(id);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean_B(RegressionEnvironment env, String id) {
        SupportBean_B bean = new SupportBean_B(id);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean_S0(RegressionEnvironment env, int id, String p00) {
        env.sendEventBean(new SupportBean_S0(id, p00));
    }

    private static void sendTimer(long timeInMSec, RegressionEnvironment env) {
        env.advanceTime(timeInMSec);
    }
}
