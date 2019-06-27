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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.context.*;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ContextHashSegmented {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextHashSegmentedBasic());
        execs.add(new ContextHashSegmentedFilter());
        execs.add(new ContextHashNoPreallocate());
        execs.add(new ContextHashSegmentedManyArg());
        execs.add(new ContextHashSegmentedMulti());
        execs.add(new ContextHashSegmentedBySingleRowFunc());
        execs.add(new ContextHashScoringUseCase());
        execs.add(new ContextHashPartitionSelection());
        execs.add(new ContextHashInvalid());
        return execs;
    }

    private static class ContextHashScoringUseCase implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionScoringUseCase(env, rep, milestone);
            }
        }

        private static void tryAssertionScoringUseCase(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, AtomicInteger milestone) {
            String[] fields = "userId,keyword,sumScore".split(",");
            String epl =
                    eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedScoreCycle.class) + "create schema ScoreCycle (userId string, keyword string, productId string, score long);\n" +
                    eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedUserKeywordTotalStream.class) + "create schema UserKeywordTotalStream (userId string, keyword string, sumScore long);\n" +
                    "\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvided.class) + " create context HashByUserCtx as " +
                    "coalesce by consistent_hash_crc32(userId) from ScoreCycle, " +
                    "consistent_hash_crc32(userId) from UserKeywordTotalStream " +
                    "granularity 1000000;\n" +
                    "\n" +
                    "context HashByUserCtx create window ScoreCycleWindow#unique(productId, keyword) as ScoreCycle;\n" +
                    "\n" +
                    "context HashByUserCtx insert into ScoreCycleWindow select * from ScoreCycle;\n" +
                    "\n" +
                    "@Name('s0') context HashByUserCtx insert into UserKeywordTotalStream \n" +
                    "select userId, keyword, sum(score) as sumScore from ScoreCycleWindow group by keyword;\n" +
                    "\n" +
                    "@Name('outTwo') context HashByUserCtx on UserKeywordTotalStream(sumScore > 10000) delete from ScoreCycleWindow;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath());
            env.addListener("s0");

            makeSendScoreEvent(env, "ScoreCycle", eventRepresentationEnum, "Pete", "K1", "P1", 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"Pete", "K1", 100L});

            makeSendScoreEvent(env, "ScoreCycle", eventRepresentationEnum, "Pete", "K1", "P2", 15);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"Pete", "K1", 115L});

            makeSendScoreEvent(env, "ScoreCycle", eventRepresentationEnum, "Joe", "K1", "P2", 30);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"Joe", "K1", 30L});

            makeSendScoreEvent(env, "ScoreCycle", eventRepresentationEnum, "Joe", "K2", "P1", 40);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"Joe", "K2", 40L});

            makeSendScoreEvent(env, "ScoreCycle", eventRepresentationEnum, "Joe", "K1", "P1", 20);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"Joe", "K1", 50L});

            env.undeployAll();
        }
    }

    public static class ContextHashPartitionSelection implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context MyCtx as coalesce consistent_hash_crc32(theString) from SupportBean granularity 16 preallocate", path);
            env.compileDeploy("@name('s0') context MyCtx select context.id as c0, theString as c1, sum(intPrimitive) as c2 from SupportBean#keepall group by theString", path);
            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, new Object[][]{{5, "E1", 1}});

            env.sendEventBean(new SupportBean("E2", 10));
            env.sendEventBean(new SupportBean("E1", 2));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 100));
            env.sendEventBean(new SupportBean("E3", 101));

            env.sendEventBean(new SupportBean("E1", 3));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), env.statement("s0").safeIterator(), fields, new Object[][]{{5, "E1", 6}, {15, "E2", 10}, {9, "E3", 201}});
            SupportContextPropUtil.assertContextProps(env, "ctx", "MyCtx", new int[]{5, 15, 9}, null, null);

            env.milestone(2);

            // test iterator targeted hash
            SupportSelectorByHashCode selector = new SupportSelectorByHashCode(Collections.singleton(15));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selector), env.statement("s0").safeIterator(selector), fields, new Object[][]{{15, "E2", 10}});
            selector = new SupportSelectorByHashCode(new HashSet<>(Arrays.asList(1, 9, 5)));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(selector), env.statement("s0").safeIterator(selector), fields, new Object[][]{{5, "E1", 6}, {9, "E3", 201}});
            assertFalse(env.statement("s0").iterator(new SupportSelectorByHashCode(Collections.singleton(99))).hasNext());
            assertFalse(env.statement("s0").iterator(new SupportSelectorByHashCode(Collections.emptySet())).hasNext());
            assertFalse(env.statement("s0").iterator(new SupportSelectorByHashCode(null)).hasNext());

            // test iterator filtered
            MySelectorFilteredHash filtered = new MySelectorFilteredHash(Collections.singleton(15));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(filtered), env.statement("s0").safeIterator(filtered), fields, new Object[][]{{15, "E2", 10}});
            filtered = new MySelectorFilteredHash(new HashSet<>(Arrays.asList(1, 9, 5)));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(filtered), env.statement("s0").safeIterator(filtered), fields, new Object[][]{{5, "E1", 6}, {9, "E3", 201}});

            // test always-false filter - compare context partition info
            filtered = new MySelectorFilteredHash(Collections.emptySet());
            assertFalse(env.statement("s0").iterator(filtered).hasNext());
            assertEquals(16, filtered.getContexts().size());

            try {
                env.statement("s0").iterator(new ContextPartitionSelectorSegmented() {
                    public List<Object[]> getPartitionKeys() {
                        return null;
                    }
                });
                fail();
            } catch (InvalidContextPartitionSelector ex) {
                assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorHash] interfaces but received com."));
            }

            env.undeployAll();

            env.milestone(3);
        }
    }

    private static class ContextHashInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // invalid filter spec
            epl = "create context ACtx coalesce hash_code(intPrimitive) from SupportBean(dummy = 1) granularity 10";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

            // invalid hash code function
            epl = "create context ACtx coalesce hash_code_xyz(intPrimitive) from SupportBean granularity 10";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "For context 'ACtx' expected a hash function that is any of {consistent_hash_crc32, hash_code} or a plug-in single-row function or script but received 'hash_code_xyz' [");

            // invalid no-param hash code function
            epl = "create context ACtx coalesce hash_code() from SupportBean granularity 10";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "For context 'ACtx' expected one or more parameters to the hash function, but found no parameter list [");

            // validate statement not applicable filters
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context ACtx coalesce hash_code(intPrimitive) from SupportBean granularity 10", path);
            epl = "context ACtx select * from SupportBean_S0";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Segmented context 'ACtx' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [");

            // invalid attempt to partition a named window's streams
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);
            epl = "create context SegmentedByWhat partition by theString from MyWindow";
            SupportMessageAssertUtil.tryInvalidCompile(env, path, epl, "Partition criteria may not include named windows [create context SegmentedByWhat partition by theString from MyWindow]");

            env.undeployAll();
        }
    }

    private static class ContextHashSegmentedFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String ctx = "HashSegmentedContext";
            String[] fields = "c0,c1".split(",");

            String eplCtx = "@Name('context') create context " + ctx + " as " +
                "coalesce " +
                " consistent_hash_crc32(theString) from SupportBean(intPrimitive > 10) " +
                "granularity 4 " +
                "preallocate";
            env.compileDeploy(eplCtx, path);

            String eplStmt = "@name('s0') context " + ctx + " " + "select context.name as c0, intPrimitive as c1 from SupportBean#lastevent";
            env.compileDeploy(eplStmt, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, 12});
            assertIterator(env, "s0", fields, new Object[][]{{ctx, 12}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E4", 10));

            env.milestone(3);

            env.sendEventBean(new SupportBean("E5", 1));
            assertIterator(env, "s0", fields, new Object[][]{{ctx, 12}});
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E6", 15));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, 15});

            env.undeployAll();
        }
    }

    public static class ContextHashNoPreallocate implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "@Name('CTX') create context CtxHash " +
                "coalesce by consistent_hash_crc32(theString) from SupportBean granularity 16";
            env.compileDeploy(eplContext, path);

            String[] fields = "c0,c1,c2".split(",");
            String eplGrouped = "@Name('s0') context CtxHash " +
                "select context.id as c0, theString as c1, sum(intPrimitive) as c2 from SupportBean group by theString";
            env.compileDeploy(eplGrouped, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0, "E1", 10});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "E2", 11});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "E2", 23});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E1", 14));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0, "E1", 24});

            env.undeployAll();

            env.milestone(3);
        }
    }

    private static class ContextHashSegmentedManyArg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryHash(env, milestone, "consistent_hash_crc32(theString, intPrimitive)");
            tryHash(env, milestone, "hash_code(theString, intPrimitive)");
        }

        private static void tryHash(RegressionEnvironment env, AtomicInteger milestone, String hashFunc) {
            RegressionPath path = new RegressionPath();
            String eplCtxCRC32 = "@Name('context') create context Ctx1 as coalesce " +
                hashFunc + " from SupportBean " +
                "granularity 1000000";
            env.compileDeploy(eplCtxCRC32, path);

            String[] fields = "c1,c2,c3,c4,c5".split(",");
            String eplStmt = "@name('s0') context Ctx1 select intPrimitive as c1, " +
                "sum(longPrimitive) as c2, prev(1, longPrimitive) as c3, prior(1, longPrimitive) as c4," +
                "(select p00 from SupportBean_S0#length(2)) as c5 " +
                "from SupportBean#length(3)";
            env.compileDeploy(eplStmt, path).addListener("s0");

            env.sendEventBean(makeBean("E1", 100, 20L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, 20L, null, null, null});

            env.sendEventBean(makeBean("E1", 100, 21L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, 41L, 20L, 20L, null});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1000, "S0"));

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean("E1", 100, 22L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, 63L, 21L, 21L, "S0"});

            env.undeployAll();
        }
    }

    private static class ContextHashSegmentedMulti implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String ctx = "HashSegmentedContext";
            String eplCtx = "@Name('context') create context " + ctx + " as " +
                "coalesce " +
                " consistent_hash_crc32(theString) from SupportBean, " +
                " consistent_hash_crc32(p00) from SupportBean_S0 " +
                "granularity 4 " +
                "preallocate";
            env.compileDeploy(eplCtx, path);
            // comment-me-in: SupportHashCodeFuncGranularCRC32 codeFunc = new SupportHashCodeFuncGranularCRC32(4);

            String eplStmt = "@name('s0') context " + ctx + " " +
                "select context.name as c0, intPrimitive as c1, id as c2 from SupportBean#keepall as t1, SupportBean_S0#keepall as t2 where t1.theString = t2.p00";
            env.compileDeploy(eplStmt, path).addListener("s0");

            String[] fields = "c0,c1,c2".split(",");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean_S0(1, "E2"));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E3", 11));
            env.sendEventBean(new SupportBean_S0(2, "E4"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(3, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, 10, 3});
            assertIterator(env, "s0", fields, new Object[][]{{ctx, 10, 3}});

            env.sendEventBean(new SupportBean_S0(4, "E4"));

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(5, "E5"));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.sendEventBean(new SupportBean("E2", 12));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{ctx, 12, 1});
            assertIterator(env, "s0", fields, new Object[][]{{ctx, 10, 3}, {ctx, 12, 1}});

            env.undeployAll();
        }
    }

    private static class ContextHashSegmentedBasic implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // Comment-in to see CRC32 code.
            for (int i = 0; i < 10; i++) {
                String key = "E" + i;
                long code = SupportHashCodeFuncGranularCRC32.computeCRC32(key) % 4;
                int hashCode = Integer.valueOf(i).hashCode() % 4;
                //System.out.println(key + " code " + code + " hashCode " + hashCode);
            }

            RegressionPath path = new RegressionPath();
            String ctx = "HashSegmentedContext";
            AtomicInteger milestone = new AtomicInteger();

            // test CRC32 Hash
            String eplCtx = "@Name('context') create context " + ctx + " as " +
                "coalesce consistent_hash_crc32(theString) from SupportBean " +
                "granularity 4 " +
                "preallocate";
            env.compileDeploy(eplCtx, path);

            String eplStmt = "@name('s0') context " + ctx + " " +
                "select context.name as c0, theString as c1, sum(intPrimitive) as c2 from SupportBean#keepall group by theString";
            env.compileDeploy(eplStmt, path).addListener("s0");
            assertEquals(4, SupportFilterHelper.getFilterCountApprox(env));
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 4, null, null, null);

            tryAssertionHash(env, milestone, "s0", ctx); // equivalent to: SupportHashCodeFuncGranularCRC32(4)
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            path.clear();

            // test same with SODA
            env.eplToModelCompileDeploy(eplCtx, path);
            env.compileDeploy(eplStmt, path).addListener("s0");
            tryAssertionHash(env, milestone, "s0", ctx);
            path.clear();

            // test with Java-hashCode String hash
            env.compileDeploy("@Name('context') create context " + ctx + " " +
                "coalesce hash_code(theString) from SupportBean " +
                "granularity 6 " +
                "preallocate", path);

            env.compileDeploy("@name('s0') context " + ctx + " " +
                "select context.name as c0, theString as c1, sum(intPrimitive) as c2 from SupportBean#keepall group by theString", path);
            env.addListener("s0");
            assertEquals(6, SupportFilterHelper.getFilterCountApprox(env));
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 6, null, null, null);

            tryAssertionHash(env, milestone, "s0", ctx);
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));
            path.clear();

            // test no pre-allocate
            env.compileDeploy("@Name('context') create context " + ctx + " " +
                "coalesce hash_code(theString) from SupportBean " +
                "granularity 16", path);

            env.compileDeploy("@name('s0') context " + ctx + " " +
                "select context.name as c0, theString as c1, sum(intPrimitive) as c2 from SupportBean#keepall group by theString", path);
            env.addListener("s0");
            assertEquals(1, SupportFilterHelper.getFilterCountApprox(env));
            AgentInstanceAssertionUtil.assertInstanceCounts(env, "s0", 0, null, null, null);

            tryAssertionHash(env, milestone, "s0", ctx);
            assertEquals(0, SupportFilterHelper.getFilterCountApprox(env));

            env.undeployAll();
        }

        private static void tryAssertionHash(RegressionEnvironment env, AtomicInteger milestone, String stmtNameIterate, String stmtNameContext) {

            String[] fields = "c0,c1,c2".split(",");

            env.sendEventBean(new SupportBean("E1", 5));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{stmtNameContext, "E1", 5});
            assertIterator(env, stmtNameIterate, fields, new Object[][]{{stmtNameContext, "E1", 5}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 6));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{stmtNameContext, "E2", 6});
            assertIterator(env, stmtNameIterate, fields, new Object[][]{{stmtNameContext, "E1", 5}, {stmtNameContext, "E2", 6}});

            env.sendEventBean(new SupportBean("E3", 7));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{stmtNameContext, "E3", 7});
            assertIterator(env, stmtNameIterate, fields, new Object[][]{{stmtNameContext, "E1", 5}, {stmtNameContext, "E3", 7}, {stmtNameContext, "E2", 6}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E4", 8));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{stmtNameContext, "E4", 8});
            assertIterator(env, stmtNameIterate, fields, new Object[][]{{stmtNameContext, "E1", 5}, {stmtNameContext, "E3", 7}, {stmtNameContext, "E4", 8}, {stmtNameContext, "E2", 6}});

            env.sendEventBean(new SupportBean("E5", 9));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{stmtNameContext, "E5", 9});
            assertIterator(env, stmtNameIterate, fields, new Object[][]{{stmtNameContext, "E5", 9}, {stmtNameContext, "E1", 5}, {stmtNameContext, "E3", 7}, {stmtNameContext, "E4", 8}, {stmtNameContext, "E2", 6}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{stmtNameContext, "E1", 15});
            assertIterator(env, stmtNameIterate, fields, new Object[][]{{stmtNameContext, "E5", 9}, {stmtNameContext, "E1", 15}, {stmtNameContext, "E3", 7}, {stmtNameContext, "E4", 8}, {stmtNameContext, "E2", 6}});

            env.sendEventBean(new SupportBean("E4", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{stmtNameContext, "E4", 19});
            assertIterator(env, stmtNameIterate, fields, new Object[][]{{stmtNameContext, "E5", 9}, {stmtNameContext, "E1", 15}, {stmtNameContext, "E3", 7}, {stmtNameContext, "E4", 19}, {stmtNameContext, "E2", 6}});

            assertEquals(1, SupportContextMgmtHelper.getContextCount(env));

            env.undeployModuleContaining("s0");
            assertEquals(1, SupportContextMgmtHelper.getContextCount(env));

            env.undeployAll();
            assertEquals(0, SupportContextMgmtHelper.getContextCount(env));
        }
    }

    private static class ContextHashSegmentedBySingleRowFunc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCtx = "@Name('context') create context HashSegmentedContext as " +
                "coalesce myHash(*) from SupportBean " +
                "granularity 4 " +
                "preallocate";
            env.compileDeploy(eplCtx, path);

            String eplStmt = "@name('s0') context HashSegmentedContext select context.id as c1, myHash(*) as c2, mySecond(*, theString) as c3, "
                + ContextHashSegmented.class.getSimpleName() + ".mySecondFunc(*, theString) as c4 from SupportBean";
            env.compileDeploy(eplStmt, path);
            env.addListener("s0");

            String[] fields = "c1,c2,c3,c4".split(",");

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 3, "E1", "E1"});    // context id matches the number returned by myHashFunc

            env.sendEventBean(new SupportBean("E2", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0, 0, "E2", "E2"});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E3", 7));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, 7, "E3", "E3"});

            env.undeployAll();
        }
    }

    public static int myHashFunc(SupportBean sb) {
        return sb.getIntPrimitive();
    }

    public static String mySecondFunc(SupportBean sb, String text) {
        return text;
    }

    private static void makeSendScoreEvent(RegressionEnvironment env, String typeName, EventRepresentationChoice eventRepresentationEnum, String userId, String keyword, String productId, long score) {
        if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<>();
            theEvent.put("userId", userId);
            theEvent.put("keyword", keyword);
            theEvent.put("productId", productId);
            theEvent.put("score", score);
            env.sendEventMap(theEvent, typeName);
        } else if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{userId, keyword, productId, score}, typeName);
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(typeName)));
            record.put("userId", userId);
            record.put("keyword", keyword);
            record.put("productId", productId);
            record.put("score", score);
            env.sendEventAvro(record, typeName);
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("userId", userId);
            object.add("keyword", keyword);
            object.add("productId", productId);
            object.add("score", score);
            env.sendEventJson(object.toString(), typeName);
        } else {
            fail();
        }
    }

    private static void assertIterator(RegressionEnvironment env, String statementName, String[] fields, Object[][] expected) {
        EventBean[] rows = EPAssertionUtil.iteratorToArray(env.iterator(statementName));
        assertIterator(rows, fields, expected);

        rows = EPAssertionUtil.iteratorToArray(env.statement(statementName).safeIterator());
        assertIterator(rows, fields, expected);
    }

    private static void assertIterator(EventBean[] events, String[] fields, Object[][] expected) {
        Object[][] result = EPAssertionUtil.eventsToObjectArr(events, fields);
        EPAssertionUtil.assertEqualsAnyOrder(expected, result);
    }

    private static SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private static class MySelectorFilteredHash implements ContextPartitionSelectorFiltered {

        private Set<Integer> match;

        private List<Integer> contexts = new ArrayList<>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<>();

        private MySelectorFilteredHash(Set<Integer> match) {
            this.match = match;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierHash id = (ContextPartitionIdentifierHash) contextPartitionIdentifier;
            if (match == null && cpids.contains(id.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + id.getContextPartitionId());
            }
            cpids.add(id.getContextPartitionId());
            contexts.add(id.getHash());
            return match.contains(id.getHash());
        }

        public List<Integer> getContexts() {
            return contexts;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
    }

    public static class MyLocalJsonProvidedScoreCycle implements Serializable {
        public String userId;
        public String keyword;
        public String productId;
        public long score;
    }

    public static class MyLocalJsonProvidedUserKeywordTotalStream implements Serializable {
        public String userId;
        public String keyword;
        public long sumScore;
    }
}
