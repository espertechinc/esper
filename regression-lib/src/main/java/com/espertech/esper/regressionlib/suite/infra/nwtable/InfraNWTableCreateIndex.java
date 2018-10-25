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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadataEntry;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexRepository;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.util.SupportInfraUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class InfraNWTableCreateIndex {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraMultiRangeAndKey(true));
        execs.add(new InfraMultiRangeAndKey(false));
        execs.add(new InfraHashBTreeWidening(true));
        execs.add(new InfraHashBTreeWidening(false));
        execs.add(new InfraWidening(true));
        execs.add(new InfraWidening(false));
        execs.add(new InfraCompositeIndex(true));
        execs.add(new InfraCompositeIndex(false));
        execs.add(new InfraLateCreate(true));
        execs.add(new InfraLateCreate(false));
        execs.add(new InfraLateCreateSceneTwo(true));
        execs.add(new InfraLateCreateSceneTwo(false));
        execs.add(new InfraMultipleColumnMultipleIndex(true));
        execs.add(new InfraMultipleColumnMultipleIndex(false));
        execs.add(new InfraDropCreate(true));
        execs.add(new InfraDropCreate(false));
        execs.add(new InfraOnSelectReUse(true));
        execs.add(new InfraOnSelectReUse(false));
        execs.add(new InfraInvalid(true));
        execs.add(new InfraInvalid(false));
        execs.add(new InfraMultikeyIndexFAF(true));
        execs.add(new InfraMultikeyIndexFAF(false));
        return execs;
    }

    private static class InfraInvalid implements RegressionExecution {
        private final boolean namedWindow;

        public InfraInvalid(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "create window MyInfraOne#keepall as (f1 string, f2 int)" :
                "create table MyInfraOne as (f1 string primary key, f2 int primary key)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("create index MyInfraIndex on MyInfraOne(f1)", path);

            env.compileDeploy("create context ContextOne initiated by SupportBean terminated after 5 sec", path);
            env.compileDeploy("create context ContextTwo initiated by SupportBean terminated after 5 sec", path);
            String eplCreateWContext = namedWindow ?
                "context ContextOne create window MyInfraCtx#keepall as (f1 string, f2 int)" :
                "context ContextOne create table MyInfraCtx as (f1 string primary key, f2 int primary key)";
            env.compileDeploy(eplCreateWContext, path);

            // invalid context
            tryInvalidCompile(env, path, "create unique index IndexTwo on MyInfraCtx(f1)",
                (namedWindow ? "Named window" : "Table") + " by name 'MyInfraCtx' has been declared for context 'ContextOne' and can only be used within the same context");
            tryInvalidCompile(env, path, "context ContextTwo create unique index IndexTwo on MyInfraCtx(f1)",
                (namedWindow ? "Named window" : "Table") + " by name 'MyInfraCtx' has been declared for context 'ContextOne' and can only be used within the same context");

            tryInvalidCompile(env, path, "create index MyInfraIndex on MyInfraOne(f1)",
                "An index by name 'MyInfraIndex' already exists [");

            tryInvalidCompile(env, path, "create index IndexTwo on MyInfraOne(fx)",
                "Property named 'fx' not found");

            tryInvalidCompile(env, path, "create index IndexTwo on MyInfraOne(f1, f1)",
                "Property named 'f1' has been declared more then once [create index IndexTwo on MyInfraOne(f1, f1)]");

            tryInvalidCompile(env, path, "create index IndexTwo on MyWindowX(f1, f1)",
                "A named window or table by name 'MyWindowX' does not exist [create index IndexTwo on MyWindowX(f1, f1)]");

            tryInvalidCompile(env, path, "create index IndexTwo on MyInfraOne(f1 bubu, f2)",
                "Unrecognized advanced-type index 'bubu'");

            tryInvalidCompile(env, path, "create gugu index IndexTwo on MyInfraOne(f2)",
                "Invalid keyword 'gugu' in create-index encountered, expected 'unique' [create gugu index IndexTwo on MyInfraOne(f2)]");

            tryInvalidCompile(env, path, "create unique index IndexTwo on MyInfraOne(f2 btree)",
                "Combination of unique index with btree (range) is not supported [create unique index IndexTwo on MyInfraOne(f2 btree)]");

            // invalid insert-into unique index
            String eplCreateTwo = namedWindow ?
                "@Name('create') create window MyInfraTwo#keepall as SupportBean" :
                "@Name('create') create table MyInfraTwo(theString string primary key, intPrimitive int primary key)";
            env.compileDeploy(eplCreateTwo, path);
            env.compileDeploy("@Name('insert') insert into MyInfraTwo select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("create unique index I1 on MyInfraTwo(theString)", path);
            env.sendEventBean(new SupportBean("E1", 1));
            try {
                env.sendEventBean(new SupportBean("E1", 2));
                fail();
            } catch (Exception ex) {
                String text = namedWindow ?
                    "Unexpected exception in statement 'create': Unique index violation, index 'I1' is a unique index and key 'E1' already exists" :
                    "java.lang.RuntimeException: Unexpected exception in statement 'insert': Unique index violation, index 'I1' is a unique index and key 'E1' already exists";
                assertEquals(text, ex.getMessage());
            }

            if (!namedWindow) {
                env.compileDeploy("create table MyTable (p0 string, sumint sum(int))", path);
                tryInvalidCompile(env, path, "create index MyIndex on MyTable(p0)",
                    "Tables without primary key column(s) do not allow creating an index [");
            }

            env.undeployAll();
        }
    }

    private static class InfraOnSelectReUse implements RegressionExecution {
        private final boolean namedWindow;

        public InfraOnSelectReUse(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = namedWindow ?
                "@name('create') create window MyInfraONR#keepall as (f1 string, f2 int)" :
                "@name('create') create table MyInfraONR as (f1 string primary key, f2 int primary key)";
            env.compileDeploy(stmtTextCreateOne, path);
            env.compileDeploy("insert into MyInfraONR(f1, f2) select theString, intPrimitive from SupportBean", path);
            env.compileDeploy("@name('indexOne') create index MyInfraONRIndex1 on MyInfraONR(f2)", path);
            String[] fields = "f1,f2".split(",");

            env.sendEventBean(new SupportBean("E1", 1));

            env.compileDeploy("@name('s0') on SupportBean_S0 s0 select nw.f1 as f1, nw.f2 as f2 from MyInfraONR nw where nw.f2 = s0.id", path).addListener("s0");
            assertEquals(namedWindow ? 1 : 2, getIndexCount(env, namedWindow, "create", "MyInfraONR"));

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

            // create second identical statement
            env.compileDeploy("@name('stmtTwo') on SupportBean_S0 s0 select nw.f1 as f1, nw.f2 as f2 from MyInfraONR nw where nw.f2 = s0.id", path);
            assertEquals(namedWindow ? 1 : 2, getIndexCount(env, namedWindow, "create", "MyInfraONR"));

            env.undeployModuleContaining("s0");
            assertEquals(namedWindow ? 1 : 2, getIndexCount(env, namedWindow, "create", "MyInfraONR"));

            env.undeployModuleContaining("stmtTwo");
            assertEquals(namedWindow ? 1 : 2, getIndexCount(env, namedWindow, "create", "MyInfraONR"));

            env.undeployModuleContaining("indexOne");

            // two-key index order test
            env.compileDeploy("@name('cw') create window MyInfraFour#keepall as SupportBean", path);
            env.compileDeploy("create index idx1 on MyInfraFour (theString, intPrimitive)", path);
            env.compileDeploy("on SupportBean sb select * from MyInfraFour w where w.theString = sb.theString and w.intPrimitive = sb.intPrimitive", path);
            env.compileDeploy("on SupportBean sb select * from MyInfraFour w where w.intPrimitive = sb.intPrimitive and w.theString = sb.theString", path);
            assertEquals(1, SupportInfraUtil.getIndexCountNoContext(env, true, "cw", "MyInfraFour"));

            env.undeployAll();
        }
    }

    private static class InfraDropCreate implements RegressionExecution {
        private final boolean namedWindow;

        public InfraDropCreate(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = namedWindow ?
                "@name('create') create window MyInfraDC#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "@name('create') create table MyInfraDC as (f1 string primary key, f2 int primary key, f3 string primary key, f4 string primary key)";
            env.compileDeploy(stmtTextCreateOne, path);
            env.compileDeploy("insert into MyInfraDC(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean", path);
            env.compileDeploy("@name('indexOne') create index MyInfraDCIndex1 on MyInfraDC(f1)", path);
            env.compileDeploy("@name('indexTwo') create index MyInfraDCIndex2 on MyInfraDC(f4)", path);
            String[] fields = "f1,f2".split(",");

            env.sendEventBean(new SupportBean("E1", -2));

            env.undeployModuleContaining("indexOne");

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfraDC where f1='E1'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

            result = env.compileExecuteFAF("select * from MyInfraDC where f4='?E1?'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

            env.undeployModuleContaining("indexTwo");

            result = env.compileExecuteFAF("select * from MyInfraDC where f1='E1'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

            result = env.compileExecuteFAF("select * from MyInfraDC where f4='?E1?'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

            path.getCompileds().remove(path.getCompileds().size() - 1);
            env.compileDeploy("@name('IndexThree') create index MyInfraDCIndex2 on MyInfraDC(f4)", path);

            result = env.compileExecuteFAF("select * from MyInfraDC where f1='E1'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

            result = env.compileExecuteFAF("select * from MyInfraDC where f4='?E1?'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

            env.undeployModuleContaining("IndexThree");
            assertEquals(namedWindow ? 0 : 1, getIndexCount(env, namedWindow, "create", "MyInfraDC"));

            env.undeployAll();
        }
    }

    private static class InfraMultipleColumnMultipleIndex implements RegressionExecution {
        private final boolean namedWindow;

        public InfraMultipleColumnMultipleIndex(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = namedWindow ?
                "create window MyInfraMCMI#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfraMCMI as (f1 string primary key, f2 int, f3 string, f4 string)";
            env.compileDeploy(stmtTextCreateOne, path);
            env.compileDeploy("insert into MyInfraMCMI(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean", path);
            env.compileDeploy("create index MyInfraMCMIIndex1 on MyInfraMCMI(f2, f3, f1)", path);
            env.compileDeploy("create index MyInfraMCMIIndex2 on MyInfraMCMI(f2, f3)", path);
            env.compileDeploy("create index MyInfraMCMIIndex3 on MyInfraMCMI(f2)", path);
            String[] fields = "f1,f2,f3,f4".split(",");

            env.sendEventBean(new SupportBean("E1", -2));
            env.sendEventBean(new SupportBean("E2", -4));
            env.sendEventBean(new SupportBean("E3", -3));

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfraMCMI where f3='>E1<'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            result = env.compileExecuteFAF("select * from MyInfraMCMI where f3='>E1<' and f2=-2", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            result = env.compileExecuteFAF("select * from MyInfraMCMI where f3='>E1<' and f2=-2 and f1='E1'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            result = env.compileExecuteFAF("select * from MyInfraMCMI where f2=-2", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            result = env.compileExecuteFAF("select * from MyInfraMCMI where f1='E1'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            result = env.compileExecuteFAF("select * from MyInfraMCMI where f3='>E1<' and f2=-2 and f1='E1' and f4='?E1?'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            env.undeployAll();
        }
    }

    public static class InfraLateCreate implements RegressionExecution {
        private final boolean namedWindow;

        public InfraLateCreate(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"theString", "intPrimitive"};
            RegressionPath path = new RegressionPath();

            // create infra
            String stmtTextCreate = namedWindow ?
                "@Name('Create') create window MyInfra.win:keepall() as SupportBean" :
                "@Name('Create') create table MyInfra(theString string primary key, intPrimitive int primary key)";
            env.compileDeploy(stmtTextCreate, path).addListener("Create");

            // create insert into
            String stmtTextInsertOne = "@Name('Insert') insert into MyInfra select theString, intPrimitive from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            env.sendEventBean(new SupportBean("A1", 1));
            env.sendEventBean(new SupportBean("B2", 2));
            env.sendEventBean(new SupportBean("B2", 1));

            // create index
            String stmtTextCreateIndex = "@Name('Index') create index MyInfra_IDX on MyInfra(theString)";
            env.compileDeploy(stmtTextCreateIndex, path);

            env.milestone(0);

            // perform on-demand query
            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfra where theString = 'B2' order by intPrimitive asc", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"B2", 1}, {"B2", 2}});

            // cleanup
            env.undeployAll();

            env.milestone(1);
        }
    }

    private static class InfraLateCreateSceneTwo implements RegressionExecution {
        private final boolean namedWindow;

        public InfraLateCreateSceneTwo(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreateOne = namedWindow ?
                "create window MyInfraLC#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfraLC as (f1 string primary key, f2 int primary key, f3 string primary key, f4 string primary key)";
            env.compileDeploy(stmtTextCreateOne, path);
            env.compileDeploy("insert into MyInfraLC(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean", path);

            env.sendEventBean(new SupportBean("E1", -4));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", -2));
            env.sendEventBean(new SupportBean("E1", -3));

            env.compileDeploy("create index MyInfraLCIndex on MyInfraLC(f2, f3, f1)", path);
            String[] fields = "f1,f2,f3,f4".split(",");

            env.milestone(1);

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfraLC where f3='>E1<' order by f2 asc", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{
                {"E1", -4, ">E1<", "?E1?"}, {"E1", -3, ">E1<", "?E1?"}, {"E1", -2, ">E1<", "?E1?"}});

            env.undeployAll();
        }
    }

    private static class InfraCompositeIndex implements RegressionExecution {
        private final boolean namedWindow;

        public InfraCompositeIndex(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreate = namedWindow ?
                "create window MyInfraCI#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfraCI as (f1 string primary key, f2 int, f3 string, f4 string)";
            env.compileDeploy(stmtTextCreate, path);
            EPCompiled compiledWindow = path.getCompileds().get(0);
            env.compileDeploy("insert into MyInfraCI(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean", path);
            env.compileDeploy("@name('indexOne') create index MyInfraCIIndex on MyInfraCI(f2, f3, f1)", path);
            String[] fields = "f1,f2,f3,f4".split(",");

            env.sendEventBean(new SupportBean("E1", -2));

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfraCI where f3='>E1<'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            result = env.compileExecuteFAF("select * from MyInfraCI where f3='>E1<' and f2=-2", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            result = env.compileExecuteFAF("select * from MyInfraCI where f3='>E1<' and f2=-2 and f1='E1'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            env.undeployModuleContaining("indexOne");

            // test SODA
            path.clear();
            path.add(compiledWindow);
            env.eplToModelCompileDeploy("create index MyInfraCIIndexTwo on MyInfraCI(f2, f3, f1)", path).undeployAll();
        }
    }

    private static class InfraWidening implements RegressionExecution {
        private final boolean namedWindow;

        public InfraWidening(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // widen to long
            String stmtTextCreate = namedWindow ?
                "create window MyInfraW#keepall as (f1 long, f2 string)" :
                "create table MyInfraW as (f1 long primary key, f2 string primary key)";
            env.compileDeploy(stmtTextCreate, path);
            env.compileDeploy("insert into MyInfraW(f1, f2) select longPrimitive, theString from SupportBean", path);
            env.compileDeploy("create index MyInfraWIndex1 on MyInfraW(f1)", path);
            String[] fields = "f1,f2".split(",");

            sendEventLong(env, "E1", 10L);

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfraW where f1=10", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10L, "E1"}});

            // coerce to short
            stmtTextCreate = namedWindow ?
                "create window MyInfraWTwo#keepall as (f1 short, f2 string)" :
                "create table MyInfraWTwo as (f1 short primary key, f2 string primary key)";
            env.compileDeploy(stmtTextCreate, path);
            env.compileDeploy("insert into MyInfraWTwo(f1, f2) select shortPrimitive, theString from SupportBean", path);
            env.compileDeploy("create index MyInfraWTwoIndex1 on MyInfraWTwo(f1)", path);

            sendEventShort(env, "E1", (short) 2);

            result = env.compileExecuteFAF("select * from MyInfraWTwo where f1=2", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{(short) 2, "E1"}});

            env.undeployAll();
        }
    }

    private static class InfraHashBTreeWidening implements RegressionExecution {
        private final boolean namedWindow;

        public InfraHashBTreeWidening(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            // widen to long
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "create window MyInfraHBTW#keepall as (f1 long, f2 string)" :
                "create table MyInfraHBTW as (f1 long primary key, f2 string primary key)";
            env.compileDeploy(eplCreate, path);

            String eplInsert = "insert into MyInfraHBTW(f1, f2) select longPrimitive, theString from SupportBean";
            env.compileDeploy(eplInsert, path);

            env.compileDeploy("create index MyInfraHBTWIndex1 on MyInfraHBTW(f1 btree)", path);
            String[] fields = "f1,f2".split(",");

            sendEventLong(env, "E1", 10L);
            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfraHBTW where f1>9", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10L, "E1"}});

            // SODA
            String epl = "create index IX1 on MyInfraHBTW(f1, f2 btree)";
            env.eplToModelCompileDeploy(epl, path);

            // SODA with unique
            String eplUnique = "create unique index IX2 on MyInfraHBTW(f1)";
            env.eplToModelCompileDeploy(eplUnique, path);

            // coerce to short
            String eplCreateTwo = namedWindow ?
                "create window MyInfraHBTWTwo#keepall as (f1 short, f2 string)" :
                "create table MyInfraHBTWTwo as (f1 short primary key, f2 string primary key)";
            env.compileDeploy(eplCreateTwo, path);

            String eplInsertTwo = "insert into MyInfraHBTWTwo(f1, f2) select shortPrimitive, theString from SupportBean";
            env.compileDeploy(eplInsertTwo, path);
            env.compileDeploy("create index MyInfraHBTWTwoIndex1 on MyInfraHBTWTwo(f1 btree)", path);

            sendEventShort(env, "E1", (short) 2);

            result = env.compileExecuteFAF("select * from MyInfraHBTWTwo where f1>=2", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{(short) 2, "E1"}});

            env.undeployAll();
        }
    }

    private static class InfraMultiRangeAndKey implements RegressionExecution {
        private final boolean namedWindow;

        public InfraMultiRangeAndKey(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "@name('create') create window MyInfraMRAK#keepall as SupportBeanRange" :
                "@name('create') create table MyInfraMRAK(id string primary key, key string, keyLong long, rangeStartLong long primary key, rangeEndLong long primary key)";
            env.compileDeploy(eplCreate, path);

            String eplInsert = namedWindow ?
                "insert into MyInfraMRAK select * from SupportBeanRange" :
                "on SupportBeanRange t0 merge MyInfraMRAK t1 where t0.id = t1.id when not matched then insert select id, key, keyLong, rangeStartLong, rangeEndLong";
            env.compileDeploy(eplInsert, path);

            env.compileDeploy("create index idx1 on MyInfraMRAK(key hash, keyLong hash, rangeStartLong btree, rangeEndLong btree)", path);
            String[] fields = "id".split(",");

            String query1 = "select * from MyInfraMRAK where rangeStartLong > 1 and rangeEndLong > 2 and keyLong=1 and key='K1' order by id asc";
            runQueryAssertion(env, path, query1, fields, null);

            env.sendEventBean(SupportBeanRange.makeLong("E1", "K1", 1L, 2L, 3L));
            runQueryAssertion(env, path, query1, fields, new Object[][]{{"E1"}});

            env.sendEventBean(SupportBeanRange.makeLong("E2", "K1", 1L, 2L, 4L));
            runQueryAssertion(env, path, query1, fields, new Object[][]{{"E1"}, {"E2"}});

            env.milestone(0);

            env.sendEventBean(SupportBeanRange.makeLong("E3", "K1", 1L, 3L, 3L));
            runQueryAssertion(env, path, query1, fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            String query2 = "select * from MyInfraMRAK where rangeStartLong > 1 and rangeEndLong > 2 and keyLong=1 order by id asc";
            runQueryAssertion(env, path, query2, fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

            assertEquals(namedWindow ? 1 : 2, getIndexCount(env, namedWindow, "create", "MyInfraMRAK"));

            env.undeployAll();
        }
    }

    public static class InfraMultikeyIndexFAF implements RegressionExecution {
        private final boolean isNamedWindow;

        public InfraMultikeyIndexFAF(boolean isNamedWindow) {
            this.isNamedWindow = isNamedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtTextCreate = isNamedWindow ?
                "create window MyInfra.win:keepall() as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfra as (f1 string primary key, f2 int, f3 string, f4 string)";
            env.compileDeploy(stmtTextCreate, path);
            env.compileDeploy("insert into MyInfra(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean", path);
            env.compileDeploy("create index MyInfraIndex on MyInfra(f2, f3, f1)", path);
            String[] fields = "f1,f2,f3,f4".split(",");

            env.sendEventBean(new SupportBean("E1", -2));

            env.milestone(0);

            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfra where f3='>E1<'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            env.milestone(1);

            result = env.compileExecuteFAF("select * from MyInfra where f3='>E1<' and f2=-2", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            env.milestone(2);

            result = env.compileExecuteFAF("select * from MyInfra where f3='>E1<' and f2=-2 and f1='E1'", path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

            env.undeployAll();
        }
    }

    private static void runQueryAssertion(RegressionEnvironment env, RegressionPath path, String epl, String[] fields, Object[][] expected) {
        EPFireAndForgetQueryResult result = env.compileExecuteFAF(epl, path);
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, expected);
    }

    private static void sendEventLong(RegressionEnvironment env, String theString, long longPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        theEvent.setLongPrimitive(longPrimitive);
        env.sendEventBean(theEvent);
    }

    private static void sendEventShort(RegressionEnvironment env, String theString, short shortPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        theEvent.setShortPrimitive(shortPrimitive);
        env.sendEventBean(theEvent);
    }

    private static void makeSendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        env.sendEventBean(b);
    }

    private static void assertCols(RegressionEnvironment env, String listOfP00, Object[][] expected) {
        String[] p00s = listOfP00.split(",");
        assertEquals(p00s.length, expected.length);
        for (int i = 0; i < p00s.length; i++) {
            env.sendEventBean(new SupportBean_S0(0, p00s[i]));
            if (expected[i] == null) {
                assertFalse(env.listener("s0").isInvoked());
            } else {
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "col0,col1".split(","), expected[i]);
            }
        }
    }

    private static int getIndexCount(RegressionEnvironment env, boolean namedWindow, String infraStmtName, String infraName) {
        return SupportInfraUtil.getIndexCountNoContext(env, namedWindow, infraStmtName, infraName);
    }

    private static void assertIndexesRef(RegressionEnvironment env, boolean namedWindow, String name, String csvNames) {
        EventTableIndexMetadataEntry entry = getIndexEntry(env, namedWindow, name);
        if (csvNames.isEmpty()) {
            assertNull(entry);
        } else {
            EPAssertionUtil.assertEqualsAnyOrder(csvNames.split(","), entry.getReferringDeployments());
        }
    }

    private static void assertIndexCountInstance(RegressionEnvironment env, boolean namedWindow, String name, int count) {
        EventTableIndexRepository repo = getIndexInstanceRepo(env, namedWindow, name);
        assertEquals(count, repo.getTables().size());
    }

    private static EventTableIndexRepository getIndexInstanceRepo(RegressionEnvironment env, boolean namedWindow, String name) {
        if (namedWindow) {
            NamedWindowInstance instance = SupportInfraUtil.getInstanceNoContextNW(env, "create", name);
            return instance.getRootViewInstance().getIndexRepository();
        }
        TableInstance instance = SupportInfraUtil.getInstanceNoContextTable(env, "create", name);
        return instance.getIndexRepository();
    }

    private static EventTableIndexMetadataEntry getIndexEntry(RegressionEnvironment env, boolean namedWindow, String name) {
        IndexedPropDesc descOne = new IndexedPropDesc("col0", String.class);
        IndexMultiKey index = new IndexMultiKey(false, Arrays.asList(descOne), Collections.<IndexedPropDesc>emptyList(), null);
        EventTableIndexMetadata meta = getIndexMetaRepo(env, namedWindow, name);
        return meta.getIndexes().get(index);
    }

    private static EventTableIndexMetadata getIndexMetaRepo(RegressionEnvironment env, boolean namedWindow, String name) {
        if (namedWindow) {
            NamedWindow processor = SupportInfraUtil.getNamedWindow(env, "create", name);
            return processor.getEventTableIndexMetadata();
        }
        Table table = SupportInfraUtil.getTable(env, "create", name);
        return table.getEventTableIndexMetadata();
    }
}
