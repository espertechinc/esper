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
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bookexample.OrderBeanFactory;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriberMRD;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;

import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static org.junit.Assert.*;

public class InfraNWTableOnMerge {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraOnMergeSimpleInsert(true));
        execs.add(new InfraOnMergeSimpleInsert(false));

        execs.add(new InfraOnMergeMatchNoMatch(true));
        execs.add(new InfraOnMergeMatchNoMatch(false));

        execs.add(new InfraUpdateNestedEvent(true));
        execs.add(new InfraUpdateNestedEvent(false));

        execs.add(new InfraOnMergeInsertStream(true));
        execs.add(new InfraOnMergeInsertStream(false));

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            execs.add(new InfraInsertOtherStream(true, rep));
            execs.add(new InfraInsertOtherStream(false, rep));
        }

        execs.add(new InfraMultiactionDeleteUpdate(true));
        execs.add(new InfraMultiactionDeleteUpdate(false));

        execs.add(new InfraUpdateOrderOfFields(true));
        execs.add(new InfraUpdateOrderOfFields(false));

        execs.add(new InfraSubqueryNotMatched(true));
        execs.add(new InfraSubqueryNotMatched(false));

        execs.add(new InfraPatternMultimatch(true));
        execs.add(new InfraPatternMultimatch(false));

        execs.add(new InfraNoWhereClause(true));
        execs.add(new InfraNoWhereClause(false));

        execs.add(new InfraMultipleInsert(true));
        execs.add(new InfraMultipleInsert(false));

        execs.add(new InfraFlow(true));
        execs.add(new InfraFlow(false));

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            if (!rep.isAvroOrJsonEvent()) {
                execs.add(new InfraInnerTypeAndVariable(true, rep));
                execs.add(new InfraInnerTypeAndVariable(false, rep));
            }
        }

        execs.add(new InfraInvalid(true));
        execs.add(new InfraInvalid(false));

        for (boolean namedWindow : new boolean[]{true, false}) {
            execs.add(new InfraInsertOnly(namedWindow, true, false, false));
            execs.add(new InfraInsertOnly(namedWindow, false, false, false));
            execs.add(new InfraInsertOnly(namedWindow, false, false, true));
            execs.add(new InfraInsertOnly(namedWindow, false, true, false));
            execs.add(new InfraInsertOnly(namedWindow, false, true, true));
        }

        execs.add(new InfraDeleteThenUpdate(true));
        execs.add(new InfraDeleteThenUpdate(false));

        execs.add(new InfraPropertyEvalUpdate(true));
        execs.add(new InfraPropertyEvalUpdate(false));

        execs.add(new InfraPropertyEvalInsertNoMatch(true));
        execs.add(new InfraPropertyEvalUpdate(false));

        return execs;
    }

    private static class InfraPropertyEvalInsertNoMatch implements RegressionExecution {
        private final boolean namedWindow;

        public InfraPropertyEvalInsertNoMatch(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c1,c2".split(",");
            RegressionPath path = new RegressionPath();

            String stmtTextCreateOne = namedWindow ?
                "@name('create') create window MyInfra#keepall() as (c1 string, c2 string)" :
                "@name('create') create table MyInfra(c1 string primary key, c2 string)";
            env.compileDeploy(stmtTextCreateOne, path);

            String epl = "@name('merge') on OrderBean[books] " +
                "merge MyInfra mw " +
                "insert select bookId as c1, title as c2 ";
            env.compileDeploy(epl, path).addListener("merge");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("merge").getLastNewData(), fields, new Object[][]{{"10020", "Enders Game"},
                {"10021", "Foundation 1"}, {"10022", "Stranger in a Strange Land"}});

            env.undeployAll();
        }
    }

    public static class InfraPropertyEvalUpdate implements RegressionExecution {
        private final boolean namedWindow;

        public InfraPropertyEvalUpdate(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"p0", "p1"};
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = namedWindow ?
                "@name('create') create window MyInfra#keepall() as (p0 string, p1 int)" :
                "@name('create') create table MyInfra(p0 string primary key, p1 int)";
            env.compileDeploy(stmtTextCreateOne, path);
            env.compileDeploy("on SupportBean_Container[beans] merge MyInfra where theString=p0 " +
                "when matched then update set p1 = intPrimitive", path);

            env.compileExecuteFAF("insert into MyInfra select 'A' as p0, 1 as p1", path);

            SupportBean b1 = new SupportBean("A", 20);
            SupportBean b2 = new SupportBean("A", 30);
            SupportBean_Container container = new SupportBean_Container(Arrays.asList(b1, b2));
            env.sendEventBean(container);

            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"A", 30}});

            env.undeployAll();
        }
    }


    public static class InfraDeleteThenUpdate implements RegressionExecution {
        private final boolean namedWindow;

        public InfraDeleteThenUpdate(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            /**
             * There is no guarantee whether the delete or the update wins
             */
            String[] fields = new String[]{"p0", "p1"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreateOne = namedWindow ?
                "@name('create') create window MyInfra#keepall() as (p0 string, p1 int)" :
                "@name('create') create table MyInfra(p0 string primary key, p1 int)";
            env.compileDeploy(stmtTextCreateOne, path);

            // create merge
            String stmtTextMerge = "@name('merge') on SupportBean sb merge MyInfra where theString = p0 when matched " +
                "then delete " +
                "then update set p1 = intPrimitive";
            env.compileDeploy(stmtTextMerge, path).addListener("merge");

            env.compileExecuteFAF("insert into MyInfra select 'A' as p0, 1 as p1", path);

            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"A", 1}});

            env.sendEventBean(new SupportBean("A", 10));

            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"A", 10}});
            } else {
                assertFalse(env.iterator("create").hasNext());
            }

            env.undeployAll();
        }
    }


    public static class InfraOnMergeSimpleInsert implements RegressionExecution {
        private final boolean namedWindow;

        public InfraOnMergeSimpleInsert(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"p0", "p1"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreateOne = namedWindow ?
                "@name('create') create window MyInfra#keepall() as (p0 string, p1 int)" :
                "@name('create') create table MyInfra(p0 string primary key, p1 int)";
            env.compileDeploy(stmtTextCreateOne, path);

            // create merge
            String stmtTextMerge = "@name('merge') on SupportBean sb merge MyInfra insert select theString as p0, intPrimitive as p1";
            env.compileDeploy(stmtTextMerge, path).addListener("merge");
            assertEquals(StatementType.ON_MERGE, env.statement("merge").getProperty(StatementProperty.STATEMENTTYPE));

            env.milestone(0);

            // populate some data
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("merge").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E1", 1}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("merge").getLastNewData()[0], fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.undeployAll();
        }
    }

    public static class InfraOnMergeMatchNoMatch implements RegressionExecution {
        private final boolean namedWindow;

        public InfraOnMergeMatchNoMatch(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreateOne = namedWindow ?
                "@name('create') create window MyInfra.win:keepall() as SupportBean" :
                "@name('create') create table MyInfra(theString string primary key, intPrimitive int)";
            env.compileDeploy(stmtTextCreateOne, path).addListener("create");

            // create merge
            String stmtTextMerge = namedWindow ?
                "@name('merge') on SupportBean sb merge MyInfra mw where sb.theString = mw.theString " +
                    "when matched and sb.intPrimitive < 0 then delete " +
                    "when not matched and intPrimitive > 0 then insert select *" +
                    "when matched and sb.intPrimitive > 0 then update set intPrimitive = sb.intPrimitive + mw.intPrimitive" :
                "@name('merge') on SupportBean sb merge MyInfra mw where sb.theString = mw.theString " +
                    "when matched and sb.intPrimitive < 0 then delete " +
                    "when not matched and intPrimitive > 0 then insert select theString, intPrimitive " +
                    "when matched and sb.intPrimitive > 0 then update set intPrimitive = sb.intPrimitive + mw.intPrimitive";
            env.compileDeploy(stmtTextMerge, path).addListener("merge");

            env.milestone(0);

            // populate some data
            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("merge").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E2", 2}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 10));
            EPAssertionUtil.assertProps(env.listener("merge").getLastNewData()[0], fields, new Object[]{"E2", 12});
            EPAssertionUtil.assertProps(env.listener("merge").getLastOldData()[0], fields, new Object[]{"E2", 2});
            env.listener("merge").reset();
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E2", 12}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E2", -1));
            EPAssertionUtil.assertProps(env.listener("merge").assertOneGetOldAndReset(), fields, new Object[]{"E2", 12});
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, null);

            env.milestone(3);

            env.sendEventBean(new SupportBean("E3", 3));
            env.sendEventBean(new SupportBean("E3", 4));
            EPAssertionUtil.assertPropsPerRow(env.iterator("create"), fields, new Object[][]{{"E3", 7}});

            env.undeployAll();

            env.milestone(4);
        }
    }

    private static class InfraInsertOnly implements RegressionExecution {
        private final boolean namedWindow;
        private final boolean useEquivalent;
        private final boolean soda;
        private final boolean useColumnNames;

        public InfraInsertOnly(boolean namedWindow, boolean useEquivalent, boolean soda, boolean useColumnNames) {
            this.namedWindow = namedWindow;
            this.useEquivalent = useEquivalent;
            this.soda = soda;
            this.useColumnNames = useColumnNames;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "p0,p1,".split(",");
            RegressionPath path = new RegressionPath();
            String createEPL = namedWindow ?
                "@Name('Window') create window InsertOnlyInfra#unique(p0) as (p0 string, p1 int)" :
                "@Name('Window') create table InsertOnlyInfra (p0 string primary key, p1 int)";
            env.compileDeploy(createEPL, path);

            String epl;
            if (useEquivalent) {
                epl = "@name('on') on SupportBean merge InsertOnlyInfra where 1=2 when not matched then insert select theString as p0, intPrimitive as p1";
            } else if (useColumnNames) {
                epl = "@name('on') on SupportBean as provider merge InsertOnlyInfra insert(p0, p1) select provider.theString, intPrimitive";
            } else {
                epl = "@name('on') on SupportBean merge InsertOnlyInfra insert select theString as p0, intPrimitive as p1";
            }

            env.compileDeploy(soda, epl, path);
            EventType windowType = env.statement("Window").getEventType();
            EventType onType = env.statement("on").getEventType();
            assertSame(windowType, onType);
            env.addListener("on");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 1}});
            EventBean onEvent = env.listener("on").assertOneGetNewAndReset();
            assertEquals("E1", onEvent.get("p0"));
            assertSame(onEvent.getEventType(), onType);

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
            assertEquals("E2", env.listener("on").assertOneGetNewAndReset().get("p0"));

            env.undeployAll();
        }
    }

    private static class InfraFlow implements RegressionExecution {
        private final boolean namedWindow;

        public InfraFlow(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "theString,intPrimitive,intBoxed".split(",");
            RegressionPath path = new RegressionPath();
            String createEPL = namedWindow ?
                "@Name('Window') create window MyMergeInfra#unique(theString) as SupportBean" :
                "@Name('Window') create table MyMergeInfra (theString string primary key, intPrimitive int, intBoxed int)";
            env.compileDeploy(createEPL, path).addListener("Window");

            env.compileDeploy("@Name('Insert') insert into MyMergeInfra select theString, intPrimitive, intBoxed from SupportBean(boolPrimitive)", path);
            env.compileDeploy("@Name('Delete') on SupportBean_A delete from MyMergeInfra", path);

            String epl = "@Name('Merge') on SupportBean(boolPrimitive=false) as up " +
                "merge MyMergeInfra as mv " +
                "where mv.theString=up.theString " +
                "when matched and up.intPrimitive<0 then " +
                "delete " +
                "when matched and up.intPrimitive=0 then " +
                "update set intPrimitive=0, intBoxed=0 " +
                "when matched then " +
                "update set intPrimitive=up.intPrimitive, intBoxed=up.intBoxed+mv.intBoxed " +
                "when not matched then " +
                "insert select " + (namedWindow ? "*" : "theString, intPrimitive, intBoxed");
            env.compileDeploy(epl, path).addListener("Merge");

            runAssertionFlow(env, namedWindow, fields);

            env.undeployModuleContaining("Merge");
            env.sendEventBean(new SupportBean_A("A1"));
            env.listener("Window").reset();

            env.eplToModelCompileDeploy(epl, path).addListener("Merge");

            runAssertionFlow(env, namedWindow, fields);

            // test stream wildcard
            env.sendEventBean(new SupportBean_A("A2"));
            env.undeployModuleContaining("Merge");
            epl = "@name('Merge') on SupportBean(boolPrimitive = false) as up " +
                "merge MyMergeInfra as mv " +
                "where mv.theString = up.theString " +
                "when not matched then " +
                "insert select " + (namedWindow ? "up.*" : "theString, intPrimitive, intBoxed");
            env.compileDeploy(epl, path).addListener("Merge");

            sendSupportBeanEvent(env, false, "E99", 2, 3); // insert via merge
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E99", 2, 3}});

            // Test ambiguous columns.
            epl = "create schema TypeOne (id long, mylong long, mystring long);\n";
            epl += namedWindow ?
                "create window MyInfraTwo#unique(id) as select * from TypeOne;\n" :
                "create table MyInfraTwo (id long, mylong long, mystring long);\n";
            // The "and not matched" should not complain if "mystring" is ambiguous.
            // The "insert" should not complain as column names have been provided.
            epl += "on TypeOne as t1 merge MyInfraTwo nm where nm.id = t1.id\n" +
                "  when not matched and mystring = 0 then insert select *\n" +
                "  when not matched then insert (id, mylong, mystring) select 0L, 0L, 0L\n";
            env.compileDeploy(epl);

            env.undeployAll();
        }
    }

    private static void runAssertionFlow(RegressionEnvironment env, boolean namedWindow, String[] fields) {
        env.listener("Window").reset();
        env.listener("Merge").reset();

        sendSupportBeanEvent(env, true, "E1", 10, 200); // insert via insert-into
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 200});
        } else {
            assertFalse(env.listener("Window").isInvoked());
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 10, 200}});
        assertFalse(env.listener("Merge").isInvoked());

        sendSupportBeanEvent(env, false, "E1", 11, 201);    // update via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetNew(), fields, new Object[]{"E1", 11, 401});
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetOld(), fields, new Object[]{"E1", 10, 200});
            env.listener("Window").reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 11, 401}});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNew(), fields, new Object[]{"E1", 11, 401});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetOld(), fields, new Object[]{"E1", 10, 200});
        env.listener("Merge").reset();

        env.milestone(0);

        sendSupportBeanEvent(env, false, "E2", 13, 300); // insert via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetNewAndReset(), fields, new Object[]{"E2", 13, 300});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 11, 401}, {"E2", 13, 300}});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"E2", 13, 300});

        sendSupportBeanEvent(env, false, "E2", 14, 301); // update via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetNew(), fields, new Object[]{"E2", 14, 601});
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetOld(), fields, new Object[]{"E2", 13, 300});
            env.listener("Window").reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 11, 401}, {"E2", 14, 601}});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNew(), fields, new Object[]{"E2", 14, 601});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetOld(), fields, new Object[]{"E2", 13, 300});
        env.listener("Merge").reset();

        env.milestone(1);

        sendSupportBeanEvent(env, false, "E2", 15, 302); // update via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetNew(), fields, new Object[]{"E2", 15, 903});
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetOld(), fields, new Object[]{"E2", 14, 601});
            env.listener("Window").reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 11, 401}, {"E2", 15, 903}});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNew(), fields, new Object[]{"E2", 15, 903});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetOld(), fields, new Object[]{"E2", 14, 601});
        env.listener("Merge").reset();

        sendSupportBeanEvent(env, false, "E3", 40, 400); // insert via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetNewAndReset(), fields, new Object[]{"E3", 40, 400});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 11, 401}, {"E2", 15, 903}, {"E3", 40, 400}});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"E3", 40, 400});

        env.milestone(2);

        sendSupportBeanEvent(env, false, "E3", 0, 1000); // reset E3 via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetNew(), fields, new Object[]{"E3", 0, 0});
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetOld(), fields, new Object[]{"E3", 40, 400});
            env.listener("Window").reset();
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 11, 401}, {"E2", 15, 903}, {"E3", 0, 0}});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNew(), fields, new Object[]{"E3", 0, 0});
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetOld(), fields, new Object[]{"E3", 40, 400});
        env.listener("Merge").reset();

        sendSupportBeanEvent(env, false, "E2", -1, 1000); // delete E2 via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetOldAndReset(), fields, new Object[]{"E2", 15, 903});
        }
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetOldAndReset(), fields, new Object[]{"E2", 15, 903});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E1", 11, 401}, {"E3", 0, 0}});

        env.milestone(3);

        sendSupportBeanEvent(env, false, "E1", -1, 1000); // delete E1 via merge
        if (namedWindow) {
            EPAssertionUtil.assertProps(env.listener("Window").assertOneGetOldAndReset(), fields, new Object[]{"E1", 11, 401});
            env.listener("Window").reset();
        }
        EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetOldAndReset(), fields, new Object[]{"E1", 11, 401});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Window"), fields, new Object[][]{{"E3", 0, 0}});
    }

    private static class InfraMultipleInsert implements RegressionExecution {
        private final boolean namedWindow;

        public InfraMultipleInsert(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            EventRepresentationChoice rep = EventRepresentationChoice.getEngineDefault(env.getConfiguration());
            String[] fields = "col1,col2".split(",");

            String epl = "create schema MyEvent as (in1 string, in2 int);\n" +
                "create schema MySchema as (col1 string, col2 int);\n";
            epl += namedWindow ?
                "create window MyInfraMI#keepall as MySchema;\n" :
                "create table MyInfraMI (col1 string primary key, col2 int);\n";
            epl += "@name('Merge') on MyEvent " +
                "merge MyInfraMI " +
                "where col1=in1 " +
                "when not matched and in1 like \"A%\" then " +
                "insert(col1, col2) select in1, in2 " +
                "when not matched and in1 like \"B%\" then " +
                "insert select in1 as col1, in2 as col2 " +
                "when not matched and in1 like \"C%\" then " +
                "insert select \"Z\" as col1, -1 as col2 " +
                "when not matched and in1 like \"D%\" then " +
                "insert select \"x\"||in1||\"x\" as col1, in2*-1 as col2;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("Merge");

            sendMyEvent(env, rep, "E1", 0);
            assertFalse(env.listener("Merge").isInvoked());

            env.milestone(0);

            sendMyEvent(env, rep, "A1", 1);
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"A1", 1});

            sendMyEvent(env, rep, "B1", 2);
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"B1", 2});

            sendMyEvent(env, rep, "C1", 3);
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"Z", -1});

            env.milestone(1);

            sendMyEvent(env, rep, "D1", 4);
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"xD1x", -4});

            sendMyEvent(env, rep, "B1", 2);
            assertFalse(env.listener("Merge").isInvoked());

            env.undeployAll();
        }
    }

    private static class InfraNoWhereClause implements RegressionExecution {
        private final boolean namedWindow;

        public InfraNoWhereClause(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "col1,col2".split(",");
            EventRepresentationChoice rep = EventRepresentationChoice.getEngineDefault(env.getConfiguration());

            String epl = "create schema MyEvent as (in1 string, in2 int);\n" +
                "create schema MySchema as (col1 string, col2 int);\n";
            epl += namedWindow ?
                "@name('create') create window MyInfraNWC#keepall as MySchema;\n" :
                "@name('create') create table MyInfraNWC (col1 string, col2 int);\n";
            epl += "on SupportBean_A delete from MyInfraNWC;\n";
            epl += "on MyEvent me " +
                "merge MyInfraNWC mw " +
                "when not matched and me.in1 like \"A%\" then " +
                "insert(col1, col2) select me.in1, me.in2 " +
                "when not matched and me.in1 like \"B%\" then " +
                "insert select me.in1 as col1, me.in2 as col2 " +
                "when matched and me.in1 like \"C%\" then " +
                "update set col1='Z', col2=-1 " +
                "when not matched then " +
                "insert select \"x\" || me.in1 || \"x\" as col1, me.in2 * -1 as col2;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath());

            sendMyEvent(env, rep, "E1", 2);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"xE1x", -2}});

            sendMyEvent(env, rep, "A1", 3);   // matched : no where clause
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"xE1x", -2}});

            env.sendEventBean(new SupportBean_A("Ax1"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, null);

            env.milestone(0);

            sendMyEvent(env, rep, "A1", 4);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"A1", 4}});

            sendMyEvent(env, rep, "B1", 5);   // matched : no where clause
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"A1", 4}});

            env.milestone(1);

            env.sendEventBean(new SupportBean_A("Ax1"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, null);

            env.milestone(2);

            sendMyEvent(env, rep, "B1", 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"B1", 5}});

            sendMyEvent(env, rep, "C", 6);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"Z", -1}});

            env.undeployAll();
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        private final boolean namedWindow;

        public InfraInvalid(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = namedWindow ?
                "create window MergeInfra#unique(theString) as SupportBean;\n" :
                "create table MergeInfra as (theString string, intPrimitive int, boolPrimitive bool);\n";
            epl += "create schema ABCSchema as (val int);\n";
            epl += namedWindow ?
                "create window ABCInfra#keepall as ABCSchema;\n" :
                "create table ABCInfra (val int);\n";
            env.compileDeploy(epl, path);

            epl = "on SupportBean_A merge MergeInfra as windowevent where id = theString when not matched and exists(select * from MergeInfra mw where mw.theString = windowevent.theString) is not null then insert into ABC select '1'";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "On-Merge not-matched filter expression may not use properties that are provided by the named window event [on SupportBean_A merge MergeInfra as windowevent where id = theString when not matched and exists(select * from MergeInfra mw where mw.theString = windowevent.theString) is not null then insert into ABC select '1']");

            epl = "on SupportBean_A as up merge ABCInfra as mv when not matched then insert (col) select 1";
            if (namedWindow) {
                SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Validation failed in when-not-matched (clause 1): Event type named 'ABCInfra' has already been declared with differing column name or type information: The property 'val' is not provided but required [on SupportBean_A as up merge ABCInfra as mv when not matched then insert (col) select 1]");
            } else {
                SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Validation failed in when-not-matched (clause 1): Column 'col' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?) [");
            }

            epl = "on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then update set intPrimitive = 1";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Incorrect syntax near 'update' (a reserved keyword) expecting 'insert' but found 'update' at line 1 column 9");

            if (namedWindow) {
                epl = "on SupportBean_A as up merge MergeInfra as mv where mv.theString=id when matched then insert select *";
                SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Validation failed in when-not-matched (clause 1): Expression-returned event type 'SupportBean_A' with underlying type '" + SupportBean_A.class.getName() + "' cannot be converted to target event type 'MergeInfra' with underlying type '" + SupportBean.class.getName() + "' [on SupportBean_A as up merge MergeInfra as mv where mv.theString=id when matched then insert select *]");
            }

            epl = "on SupportBean as up merge MergeInfra as mv";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Unexpected end-of-input at line 1 column 4");

            epl = "on SupportBean as up merge MergeInfra as mv where a=b when matched";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Incorrect syntax near end-of-input ('matched' is a reserved keyword) expecting 'then' but found end-of-input at line 1 column 66 [");

            epl = "on SupportBean as up merge MergeInfra as mv where a=b when matched and then delete";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Incorrect syntax near 'then' (a reserved keyword) at line 1 column 71 [on SupportBean as up merge MergeInfra as mv where a=b when matched and then delete]");

            epl = "on SupportBean as up merge MergeInfra as mv where boolPrimitive=true when not matched then insert select *";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Failed to validate where-clause expression 'boolPrimitive=true': Property named 'boolPrimitive' is ambiguous as is valid for more then one stream [on SupportBean as up merge MergeInfra as mv where boolPrimitive=true when not matched then insert select *]");

            epl = "on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select intPrimitive";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Failed to validate select-clause expression 'intPrimitive': Property named 'intPrimitive' is not valid in any stream [on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select intPrimitive]");

            epl = "on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select * where theString = 'A'";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Failed to validate match where-clause expression 'theString=\"A\"': Property named 'theString' is not valid in any stream [on SupportBean_A as up merge MergeInfra as mv where mv.boolPrimitive=true when not matched then insert select * where theString = 'A']");

            env.undeployAll();

            // invalid assignment: wrong event type
            path.clear();
            env.compileDeploy("create map schema Composite as (c0 int)", path);
            env.compileDeploy("create window AInfra#keepall as (c Composite)", path);
            env.compileDeploy("create map schema SomeOther as (c1 int)", path);
            env.compileDeploy("create map schema MyEvent as (so SomeOther)", path);

            SupportMessageAssertUtil.tryInvalidCompile(env, path, "on MyEvent as me update AInfra set c = me.so",
                "Invalid assignment to property 'c' event type 'Composite' from event type 'SomeOther' [on MyEvent as me update AInfra set c = me.so]");

            env.undeployAll();
        }
    }

    private static class InfraInnerTypeAndVariable implements RegressionExecution {
        private final boolean namedWindow;
        private final EventRepresentationChoice eventRepresentationEnum;

        public InfraInnerTypeAndVariable(boolean namedWindow, EventRepresentationChoice eventRepresentationEnum) {
            this.namedWindow = namedWindow;
            this.eventRepresentationEnum = eventRepresentationEnum;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String schema = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyInnerSchema.class) + " create schema MyInnerSchema(in1 string, in2 int);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEventSchema.class) + " create schema MyEventSchema(col1 string, col2 MyInnerSchema)";
            env.compileDeployWBusPublicType(schema, path);

            String eplCreate = namedWindow ?
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyInfraITV.class) + " create window MyInfraITV#keepall as (c1 string, c2 MyInnerSchema)" :
                "create table MyInfraITV as (c1 string primary key, c2 MyInnerSchema)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("@name('createvar') create variable boolean myvar", path);

            String epl = "@name('Merge') on MyEventSchema me " +
                "merge MyInfraITV mw " +
                "where me.col1 = mw.c1 " +
                " when not matched and myvar then " +
                "  insert select col1 as c1, col2 as c2 " +
                " when not matched and myvar = false then " +
                "  insert select 'A' as c1, null as c2 " +
                " when not matched and myvar is null then " +
                "  insert select 'B' as c1, me.col2 as c2 " +
                " when matched then " +
                "  delete";
            env.compileDeploy(epl, path).addListener("Merge");
            String[] fields = "c1,c2.in1,c2.in2".split(",");

            sendMyInnerSchemaEvent(env, eventRepresentationEnum, "X1", "Y1", 10);
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"B", "Y1", 10});

            sendMyInnerSchemaEvent(env, eventRepresentationEnum, "B", "0", 0);    // delete
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetOldAndReset(), fields, new Object[]{"B", "Y1", 10});

            env.milestone(0);

            env.runtime().getVariableService().setVariableValue(env.deploymentId("createvar"), "myvar", true);
            sendMyInnerSchemaEvent(env, eventRepresentationEnum, "X2", "Y2", 11);
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"X2", "Y2", 11});

            env.milestone(1);

            env.runtime().getVariableService().setVariableValue(env.deploymentId("createvar"), "myvar", false);
            sendMyInnerSchemaEvent(env, eventRepresentationEnum, "X3", "Y3", 12);
            EPAssertionUtil.assertProps(env.listener("Merge").assertOneGetNewAndReset(), fields, new Object[]{"A", null, null});

            env.undeployModuleContaining("Merge");
            env.compileDeploy(epl, path);

            SupportSubscriberMRD subscriber = new SupportSubscriberMRD();
            env.statement("Merge").setSubscriber(subscriber);
            env.runtime().getVariableService().setVariableValue(env.deploymentId("createvar"), "myvar", true);

            sendMyInnerSchemaEvent(env, eventRepresentationEnum, "X4", "Y4", 11);
            Object[][] result = subscriber.getInsertStreamList().get(0);
            if (eventRepresentationEnum.isObjectArrayEvent() || !namedWindow) {
                Object[] row = (Object[]) result[0][0];
                assertEquals("X4", row[0]);
                EventBean theEvent = (EventBean) row[1];
                assertEquals("Y4", theEvent.get("in1"));
            } else if (eventRepresentationEnum.isMapEvent()) {
                Map map = (Map) result[0][0];
                assertEquals("X4", map.get("c1"));
                EventBean theEvent = (EventBean) map.get("c2");
                assertEquals("Y4", theEvent.get("in1"));
            } else if (eventRepresentationEnum.isAvroEvent()) {
                GenericData.Record avro = (GenericData.Record) result[0][0];
                assertEquals("X4", avro.get("c1"));
                GenericData.Record theEvent = (GenericData.Record) avro.get("c2");
                assertEquals("Y4", theEvent.get("in1"));
            }

            env.undeployAll();
        }
    }

    private static class InfraPatternMultimatch implements RegressionExecution {
        private final boolean namedWindow;

        public InfraPatternMultimatch(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = "c1,c2".split(",");
            RegressionPath path = new RegressionPath();

            String eplCreate = namedWindow ?
                "@name('create') create window MyInfraPM#keepall as (c1 string, c2 string)" :
                "@name('create') create table MyInfraPM as (c1 string primary key, c2 string primary key)";
            env.compileDeploy(eplCreate, path);

            String epl = "@name('Merge') on pattern[every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B%', intPrimitive = a.intPrimitive)] me " +
                "merge MyInfraPM mw " +
                "where me.a.theString = mw.c1 and me.b.theString = mw.c2 " +
                "when not matched then " +
                "insert select me.a.theString as c1, me.b.theString as c2 ";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("A2", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean("B1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}});

            env.sendEventBean(new SupportBean("A3", 2));

            env.milestone(1);

            env.sendEventBean(new SupportBean("A4", 2));
            env.sendEventBean(new SupportBean("B2", 2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}, {"A3", "B2"}, {"A4", "B2"}});

            env.undeployAll();
        }
    }

    private static class InfraOnMergeInsertStream implements RegressionExecution {
        private final boolean namedWindow;

        public InfraOnMergeInsertStream(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String epl = "create schema WinOMISSchema as (v1 string, v2 int);\n";
            epl += namedWindow ?
                "@name('Create') create window WinOMIS#keepall as WinOMISSchema;\n" :
                "@name('Create') create table WinOMIS as (v1 string primary key, v2 int);\n";
            epl += "on SupportBean_ST0 as st0 merge WinOMIS as win where win.v1=st0.key0 " +
                "when not matched " +
                "then insert into StreamOne select * " +
                "then insert into StreamTwo select st0.id as id, st0.key0 as key0 " +
                "then insert into StreamThree(id, key0) select st0.id, st0.key0 " +
                "then insert into StreamFour select id, key0 where key0=\"K2\" " +
                "then insert into WinOMIS select key0 as v1, p00 as v2;\n";
            epl += "@name('s1') select * from StreamOne;\n";
            epl += "@name('s2') select * from StreamTwo;\n";
            epl += "@name('s3') select * from StreamThree;\n";
            epl += "@name('s4') select * from StreamFour;\n";
            env.compileDeploy(epl).addListener("s1").addListener("s2").addListener("s3").addListener("s4");

            env.sendEventBean(new SupportBean_ST0("ID1", "K1", 1));
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K1"});
            EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K1"});
            EPAssertionUtil.assertProps(env.listener("s3").assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K1"});
            assertFalse(env.listener("s4").isInvoked());

            env.milestone(0);

            env.sendEventBean(new SupportBean_ST0("ID1", "K2", 2));
            EPAssertionUtil.assertProps(env.listener("s4").assertOneGetNewAndReset(), "id,key0".split(","), new Object[]{"ID1", "K2"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("Create"), "v1,v2".split(","), new Object[][]{{"K1", 1}, {"K2", 2}});

            env.undeployAll();
        }
    }

    private static class InfraMultiactionDeleteUpdate implements RegressionExecution {
        private final boolean namedWindow;

        public InfraMultiactionDeleteUpdate(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "@name('Create') create window WinMDU#keepall as SupportBean" :
                "@name('Create') create table WinMDU (theString string primary key, intPrimitive int)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("insert into WinMDU select theString, intPrimitive from SupportBean", path);

            String epl = "@name('merge') on SupportBean_ST0 as st0 merge WinMDU as win where st0.key0=win.theString " +
                "when matched " +
                "then delete where intPrimitive<0 " +
                "then update set intPrimitive=st0.p00 where intPrimitive=3000 or p00=3000 " +
                "then update set intPrimitive=999 where intPrimitive=1000 " +
                "then delete where intPrimitive=1000 " +
                "then update set intPrimitive=1999 where intPrimitive=2000 " +
                "then delete where intPrimitive=2000";
            env.compileDeploy(epl, path);
            String[] fields = "theString,intPrimitive".split(",");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_ST0("ST0", "E1", 0));
            EPAssertionUtil.assertPropsPerRow(env.iterator("Create"), fields, new Object[][]{{"E1", 1}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", -1));
            env.sendEventBean(new SupportBean_ST0("ST0", "E2", 0));
            EPAssertionUtil.assertPropsPerRow(env.iterator("Create"), fields, new Object[][]{{"E1", 1}});

            env.sendEventBean(new SupportBean("E3", 3000));
            env.sendEventBean(new SupportBean_ST0("ST0", "E3", 3));
            EPAssertionUtil.assertPropsPerRow(env.iterator("Create"), fields, new Object[][]{{"E1", 1}, {"E3", 3}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E4", 4));
            env.sendEventBean(new SupportBean_ST0("ST0", "E4", 3000));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create"), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E4", 3000}});

            env.sendEventBean(new SupportBean("E5", 1000));
            env.sendEventBean(new SupportBean_ST0("ST0", "E5", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create"), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E4", 3000}, {"E5", 999}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E6", 2000));
            env.sendEventBean(new SupportBean_ST0("ST0", "E6", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("Create"), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E4", 3000}, {"E5", 999}, {"E6", 1999}});
            env.undeployModuleContaining("merge");

            env.eplToModelCompileDeploy(epl, path);

            env.undeployAll();
        }
    }

    private static class InfraSubqueryNotMatched implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSubqueryNotMatched(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreateOne = namedWindow ?
                "@name('Create') create window InfraOne#unique(string) (string string, intPrimitive int)" :
                "@name('Create') create table InfraOne (string string primary key, intPrimitive int)";
            env.compileDeploy(eplCreateOne, path);
            assertStatelessStmt(env, "Create", false);

            String eplCreateTwo = namedWindow ?
                "create window InfraTwo#unique(val0) (val0 string, val1 int)" :
                "create table InfraTwo (val0 string primary key, val1 int primary key)";
            env.compileDeploy(eplCreateTwo, path);
            env.compileDeploy("insert into InfraTwo select 'W2' as val0, id as val1 from SupportBean_S0", path);

            String epl = "on SupportBean sb merge InfraOne w1 " +
                "where sb.theString = w1.string " +
                "when not matched then insert select 'Y' as string, (select val1 from InfraTwo as w2 where w2.val0 = sb.theString) as intPrimitive";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean_S0(50));  // InfraTwo now has a row {W2, 1}
            env.sendEventBean(new SupportBean("W2", 1));
            EPAssertionUtil.assertPropsPerRow(env.iterator("Create"), "string,intPrimitive".split(","), new Object[][]{{"Y", 50}});

            if (namedWindow) {
                env.sendEventBean(new SupportBean_S0(51));  // InfraTwo now has a row {W2, 1}
                env.sendEventBean(new SupportBean("W2", 2));
                EPAssertionUtil.assertPropsPerRow(env.iterator("Create"), "string,intPrimitive".split(","), new Object[][]{{"Y", 51}});
            }

            env.undeployAll();
        }
    }

    private static class InfraUpdateOrderOfFields implements RegressionExecution {
        private final boolean namedWindow;

        public InfraUpdateOrderOfFields(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String epl = namedWindow ?
                "create window MyInfraUOF#keepall as SupportBean;\n" :
                "create table MyInfraUOF(theString string primary key, intPrimitive int, intBoxed int, doublePrimitive double);\n";
            epl += "insert into MyInfraUOF select theString, intPrimitive, intBoxed, doublePrimitive from SupportBean;\n";
            epl += "@name('Merge') on SupportBean_S0 as sb " +
                "merge MyInfraUOF as mywin where mywin.theString = sb.p00 when matched then " +
                "update set intPrimitive=id, intBoxed=mywin.intPrimitive, doublePrimitive=initial.intPrimitive;\n";

            env.compileDeploy(epl).addListener("Merge");
            String[] fields = "intPrimitive,intBoxed,doublePrimitive".split(",");

            env.sendEventBean(makeSupportBean("E1", 1, 2));
            env.sendEventBean(new SupportBean_S0(5, "E1"));
            EPAssertionUtil.assertProps(env.listener("Merge").getAndResetLastNewData()[0], fields, new Object[]{5, 5, 1.0});

            env.milestone(0);

            env.sendEventBean(makeSupportBean("E2", 10, 20));
            env.sendEventBean(new SupportBean_S0(6, "E2"));
            EPAssertionUtil.assertProps(env.listener("Merge").getAndResetLastNewData()[0], fields, new Object[]{6, 6, 10.0});

            env.sendEventBean(new SupportBean_S0(7, "E1"));
            EPAssertionUtil.assertProps(env.listener("Merge").getAndResetLastNewData()[0], fields, new Object[]{7, 7, 5.0});

            env.undeployAll();
        }
    }

    private static class InfraInsertOtherStream implements RegressionExecution {
        private final boolean namedWindow;
        private final EventRepresentationChoice eventRepresentationEnum;

        public InfraInsertOtherStream(boolean namedWindow, EventRepresentationChoice eventRepresentationEnum) {
            this.namedWindow = namedWindow;
            this.eventRepresentationEnum = eventRepresentationEnum;
        }

        public void run(RegressionEnvironment env) {
            String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEvent.class) + " create schema MyEvent as (name string, value double);\n" +
                (namedWindow ?
                    eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEvent.class) + " create window MyInfraIOS#unique(name) as MyEvent;\n" :
                    "create table MyInfraIOS (name string primary key, value double primary key);\n"
                ) +
                "insert into MyInfraIOS select * from MyEvent;\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedInputEvent.class) + " create schema InputEvent as (col1 string, col2 double);\n" +
                "\n" +
                "on MyEvent as eme\n" +
                "  merge MyInfraIOS as MyInfraIOS where MyInfraIOS.name = eme.name\n" +
                "   when matched then\n" +
                "      insert into OtherStreamOne select eme.name as event_name, MyInfraIOS.value as status\n" +
                "   when not matched then\n" +
                "      insert into OtherStreamOne select eme.name as event_name, 0d as status;\n" +
                "@name('s0') select * from OtherStreamOne;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            makeSendNameValueEvent(env, eventRepresentationEnum, "MyEvent", "name1", 10d);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "event_name,status".split(","), new Object[]{"name1", namedWindow ? 0d : 10d});

            // for named windows we can send same-value keys now
            if (namedWindow) {
                makeSendNameValueEvent(env, eventRepresentationEnum, "MyEvent", "name1", 11d);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "event_name,status".split(","), new Object[]{"name1", 10d});

                makeSendNameValueEvent(env, eventRepresentationEnum, "MyEvent", "name1", 12d);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "event_name,status".split(","), new Object[]{"name1", 11d});
            }

            env.undeployAll();
        }

        private static void makeSendNameValueEvent(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String typeName, String name, double value) {
            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{name, value}, typeName);
            } else if (eventRepresentationEnum.isMapEvent()) {
                Map<String, Object> theEvent = new HashMap<>();
                theEvent.put("name", name);
                theEvent.put("value", value);
                env.sendEventMap(theEvent, typeName);
            } else if (eventRepresentationEnum.isAvroEvent()) {
                GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(typeName)));
                record.put("name", name);
                record.put("value", value);
                env.eventService().sendEventAvro(record, typeName);
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                env.eventService().sendEventJson(new JsonObject().add("name", name).add("value", value).toString(), typeName);
            } else {
                fail();
            }
        }
    }

    private static class InfraUpdateNestedEvent implements RegressionExecution {
        private final boolean namedWindow;

        public InfraUpdateNestedEvent(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            runUpdateNestedEvent(env, namedWindow, "map");
            runUpdateNestedEvent(env, namedWindow, "objectarray");
        }
    }

    private static void runUpdateNestedEvent(RegressionEnvironment env, boolean namedWindow, String metaType) {
        String eplTypes =
            "create " + metaType + " schema Composite as (c0 int);\n" +
                "create " + metaType + " schema AInfraType as (k string, cflat Composite, carr Composite[]);\n" +
                (namedWindow ?
                    "create window AInfra#lastevent as AInfraType;\n" :
                    "create table AInfra (k string, cflat Composite, carr Composite[]);\n") +
                "insert into AInfra select theString as k, null as cflat, null as carr from SupportBean;\n" +
                "create " + metaType + " schema MyEvent as (cf Composite, ca Composite[]);\n" +
                "on MyEvent e merge AInfra when matched then update set cflat = e.cf, carr = e.ca";
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType(eplTypes, path);

        env.sendEventBean(new SupportBean("E1", 1));

        if (metaType.equals("map")) {
            env.sendEventMap(makeNestedMapEvent(), "MyEvent");
        } else {
            env.sendEventObjectArray(makeNestedOAEvent(), "MyEvent");
        }

        env.milestone(0);

        EPFireAndForgetQueryResult result = env.compileExecuteFAF("select cflat.c0 as cf0, carr[0].c0 as ca0, carr[1].c0 as ca1 from AInfra", path);
        EPAssertionUtil.assertProps(result.getArray()[0], "cf0,ca0,ca1".split(","), new Object[]{1, 1, 2});

        env.undeployAll();
    }

    private static Map<String, Object> makeNestedMapEvent() {
        Map<String, Object> cf1 = Collections.singletonMap("c0", 1);
        Map<String, Object> cf2 = Collections.singletonMap("c0", 2);
        Map<String, Object> myEvent = new HashMap<>();
        myEvent.put("cf", cf1);
        myEvent.put("ca", new Map[]{cf1, cf2});
        return myEvent;
    }

    private static Object[] makeNestedOAEvent() {
        Object[] cf1 = new Object[]{1};
        Object[] cf2 = new Object[]{2};
        return new Object[]{cf1, new Object[]{cf1, cf2}};
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    private static void sendMyInnerSchemaEvent(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String col1, String col2in1, int col2in2) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{col1, new Object[]{col2in1, col2in2}}, "MyEventSchema");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> inner = new HashMap<>();
            inner.put("in1", col2in1);
            inner.put("in2", col2in2);
            Map<String, Object> theEvent = new HashMap<>();
            theEvent.put("col1", col1);
            theEvent.put("col2", inner);
            env.sendEventMap(theEvent, "MyEventSchema");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("MyEventSchema"));
            Schema innerSchema = schema.getField("col2").schema();
            GenericData.Record innerRecord = new GenericData.Record(innerSchema);
            innerRecord.put("in1", col2in1);
            innerRecord.put("in2", col2in2);
            GenericData.Record record = new GenericData.Record(schema);
            record.put("col1", col1);
            record.put("col2", innerRecord);
            env.eventService().sendEventAvro(record, "MyEventSchema");
        } else if (eventRepresentationEnum.isJsonEvent()) {
            JsonObject inner = new JsonObject().add("in1", col2in1).add("in2", col2in2);
            JsonObject outer = new JsonObject().add("col1", col1).add("col2", inner);
            env.eventService().sendEventJson(outer.toString(), "MyEventSchema");
        } else {
            fail();
        }
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, boolean boolPrimitive, String theString, int intPrimitive, Integer intBoxed) {
        SupportBean theEvent = new SupportBean(theString, intPrimitive);
        theEvent.setIntBoxed(intBoxed);
        theEvent.setBoolPrimitive(boolPrimitive);
        env.sendEventBean(theEvent);
    }

    private static void sendMyEvent(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String in1, int in2) {
        Map<String, Object> theEvent = new LinkedHashMap<>();
        theEvent.put("in1", in1);
        theEvent.put("in2", in2);
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(theEvent.values().toArray(), "MyEvent");
        } else {
            env.sendEventMap(theEvent, "MyEvent");
        }
    }

    public static class MyLocalJsonProvidedMyEvent implements Serializable {
        public String name;
        public double value;
    }

    public static class MyLocalJsonProvidedInputEvent implements Serializable {
        public String col1;
        public double col2;
    }

    public static class MyLocalJsonProvidedMyInnerSchema implements Serializable {
        public String in1;
        public int in2;
    }

    public static class MyLocalJsonProvidedMyEventSchema implements Serializable {
        public String col1;
        public MyLocalJsonProvidedMyInnerSchema col2;
    }

    public static class MyLocalJsonProvidedMyInfraITV implements Serializable {
        public String c1;
        public MyLocalJsonProvidedMyInnerSchema c2;
    }
}
