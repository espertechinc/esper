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

import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.ISupportAImpl;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportWebEvent;
import com.espertech.esper.regressionlib.support.context.SupportContextPropUtil;
import com.espertech.esper.regressionlib.support.context.SupportSelectorPartitioned;
import com.espertech.esper.regressionlib.support.filter.SupportFilterServiceHelper;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ContextKeySegmented {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextKeySegmentedPatternFilter());
        execs.add(new ContextKeySegmentedJoinRemoveStream());
        execs.add(new ContextKeySegmentedSelector());
        execs.add(new ContextKeySegmentedLargeNumberPartitions());
        execs.add(new ContextKeySegmentedAdditionalFilters());
        execs.add(new ContextKeySegmentedMultiStatementFilterCount());
        execs.add(new ContextKeySegmentedSubtype());
        execs.add(new ContextKeySegmentedJoinMultitypeMultifield());
        execs.add(new ContextKeySegmentedSubselectPrevPrior());
        execs.add(new ContextKeySegmentedPrior());
        execs.add(new ContextKeySegmentedSubqueryFiltered());
        execs.add(new ContextKeySegmentedJoin());
        execs.add(new ContextKeySegmentedPattern());
        execs.add(new ContextKeySegmentedPatternSceneTwo());
        execs.add(new ContextKeySegmentedViewSceneOne());
        execs.add(new ContextKeySegmentedViewSceneTwo());
        execs.add(new ContextKeySegmentedJoinWhereClauseOnPartitionKey());
        execs.add(new ContextKeySegmentedNullSingleKey());
        execs.add(new ContextKeySegmentedNullKeyMultiKey());
        execs.add(new ContextKeySegmentedInvalid());
        execs.add(new ContextKeySegmentedTermByFilter());
        execs.add(new ContextKeySegmentedMatchRecognize());
        execs.add(new ContextKeySegmentedMultikeyWArrayOfPrimitive());
        execs.add(new ContextKeySegmentedMultikeyWArrayTwoField());
        execs.add(new ContextKeySegmentedWInitTermEndEvent());
        execs.add(new ContextKeySegmentedWPatternFireWhenAllocated());
        execs.add(new ContextKeySegmentedWInitTermPatternAsName());
        execs.add(new ContextKeySegmentedTermEventSelect());
        return execs;
    }

    private static class ContextKeySegmentedTermEventSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema UserEvent(userId string, alert string);\n" +
                    "create context UserSessionContext partition by userId from UserEvent\n" +
                    "  initiated by UserEvent(alert = 'A')\n" +
                    "  terminated by UserEvent(alert = 'B') as termEvent;\n" +
                    "@name('s0') context UserSessionContext select *, context.termEvent as term from UserEvent#firstevent\n" +
                    "  output snapshot when terminated;";
            env.compileDeploy(epl).addListener("s0");

            sendUser(env, "U1", "A");
            sendUser(env, "U1", null);
            sendUser(env, "U1", null);
            env.assertListenerNotInvoked("s0");
            env.milestone(0);

            Map<String, Object> term = sendUser(env, "U1", "B");
            env.assertEventNew("s0", event -> assertEquals(term, event.get("term")));

            env.undeployAll();
        }

        private Map<String, Object> sendUser(RegressionEnvironment env, String user, String alert) {
            Map<String, Object> data = CollectionUtil.buildMap("userId", user, "alert", alert);
            env.sendEventMap(data, "UserEvent");
            return data;
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.SERDEREQUIRED);
        }
    }

    private static class ContextKeySegmentedWInitTermPatternAsName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext partition by theString from SupportBean\n" +
                "initiated by SupportBean(intPrimitive = 1) as startevent\n" +
                "terminated by pattern[s=SupportBean(intPrimitive = 2)] as endpattern;\n" +
                "@name('s0') context MyContext select context.startevent.intBoxed as c0, context.endpattern.s.intBoxed as c1 from SupportBean#firstevent output snapshot when terminated;\n";
            env.compileDeploy(epl).addListener("s0");

            sendSBEvent(env, "A", 10, 1);
            sendSBEvent(env, "A", 20, 2);
            env.assertPropsNew("s0", "c0,c1".split(","), new Object[] {10, 20});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedWInitTermEndEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext partition by theString from SupportBean\n" +
                "initiated by SupportBean(intPrimitive = 1) as startevent\n" +
                "terminated by SupportBean(intPrimitive = 0) as endevent;\n" +
                "@name('s0') context MyContext select context.startevent as c0, context.endevent as c1 from SupportBean output all when terminated;\n";
            env.compileDeploy(epl).addListener("s0");

            SupportBean sb1 = sendSBEvent(env, "A", 1);
            SupportBean sb2 = sendSBEvent(env, "A", 0);

            env.assertPropsNew("s0", "c0,c1".split(","), new Object[] {sb1, sb2});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedWPatternFireWhenAllocated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext partition by theString from SupportBean;\n" +
                "@name('s0') context MyContext select context.key1 as key1 from pattern[timer:interval(0)];\n" +
                "context MyContext create variable String lastString = null;\n" +
                "context MyContext on pattern[timer:interval(0)] set lastString = context.key1;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssertKey(env, "E1");
            sendAssertNoReceived(env, "E1");

            env.milestone(0);

            sendAssertNoReceived(env, "E1");
            sendAssertKey(env, "E2");

            env.undeployAll();
        }

        private void sendAssertNoReceived(RegressionEnvironment env, String theString) {
            env.sendEventBean(new SupportBean(theString, 1));
            env.assertListenerNotInvoked("s0");
        }

        private void sendAssertKey(RegressionEnvironment env, String theString) {
            env.sendEventBean(new SupportBean(theString, 0));
            env.assertEqualsNew("s0", "key1", theString);

            env.assertThat(() -> {
                DeploymentIdNamePair pair = new DeploymentIdNamePair(env.deploymentId("s0"), "lastString");
                Set<DeploymentIdNamePair> set = Collections.singleton(pair);
                Map<DeploymentIdNamePair, List<ContextPartitionVariableState>> values = env.runtime().getVariableService().getVariableValue(set, new SupportSelectorPartitioned(theString));
                assertEquals(theString, values.get(pair).iterator().next().getState());
            });
        }
    }

    private static class ContextKeySegmentedMultikeyWArrayTwoField implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context PartitionByArray partition by id, array from SupportEventWithIntArray;\n" +
                "@name('s0') context PartitionByArray select sum(value) as thesum from SupportEventWithIntArray;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssertArray(env, "G1", new int[]{1, 2}, 1, 1);
            sendAssertArray(env, "G2", new int[]{1, 2}, 2, 2);
            sendAssertArray(env, "G1", new int[]{1}, 3, 3);

            env.milestone(0);

            sendAssertArray(env, "G2", new int[]{1, 2}, 10, 2 + 10);
            sendAssertArray(env, "G1", new int[]{1, 2}, 15, 1 + 15);
            sendAssertArray(env, "G1", new int[]{1}, 18, 3 + 18);

            SupportSelectorPartitioned selector = new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"G2", new int[]{1, 2}}));
            env.assertStatement("s0", statement ->  EPAssertionUtil.assertPropsPerRow(statement.iterator(selector), "thesum".split(","), new Object[][]{{2 + 10}}));

            ContextPartitionSelectorFiltered selectorWFilter = contextPartitionIdentifier -> {
                ContextPartitionIdentifierPartitioned partitioned = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
                return partitioned.getKeys()[0].equals("G2") && Arrays.equals((int[]) partitioned.getKeys()[1], new int[]{1, 2});
            };
            env.assertStatement("s0", statement -> EPAssertionUtil.assertPropsPerRow(statement.iterator(selectorWFilter), "thesum".split(","), new Object[][]{{2 + 10}}));

            env.undeployAll();
        }

        private void sendAssertArray(RegressionEnvironment env, String id, int[] array, int value, int expected) {
            env.sendEventBean(new SupportEventWithIntArray(id, array, value));
            env.assertEqualsNew("s0", "thesum", expected);
        }
    }

    private static class ContextKeySegmentedMultikeyWArrayOfPrimitive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context PartitionByArray partition by array from SupportEventWithIntArray;\n" +
                "@name('s0') context PartitionByArray select sum(value) as thesum from SupportEventWithIntArray;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssertArray(env, "E1", new int[]{1, 2}, 10, 10);
            sendAssertArray(env, "E2", new int[]{1, 2}, 11, 21);
            sendAssertArray(env, "E3", new int[]{1}, 12, 12);
            sendAssertArray(env, "E4", new int[]{}, 13, 13);
            sendAssertArray(env, "E5", null, 14, 14);

            env.milestone(0);

            sendAssertArray(env, "E10", null, 20, 14 + 20);
            sendAssertArray(env, "E11", new int[]{1, 2}, 21, 21 + 21);
            sendAssertArray(env, "E12", new int[]{1}, 22, 12 + 22);
            sendAssertArray(env, "E13", new int[]{}, 23, 13 + 23);

            SupportSelectorPartitioned selectorPartition = new SupportSelectorPartitioned(Collections.singletonList(new Object[]{new int[]{1, 2}}));
            env.assertStatement("s0", statement -> EPAssertionUtil.assertPropsPerRow(statement.iterator(selectorPartition), "thesum".split(","), new Object[][]{{21 + 21}}));

            ContextPartitionSelectorFiltered selectorWFilter = contextPartitionIdentifier -> {
                ContextPartitionIdentifierPartitioned partitioned = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
                return Arrays.equals((int[]) partitioned.getKeys()[0], new int[]{1});
            };
            env.assertStatement("s0", statement -> EPAssertionUtil.assertPropsPerRow(statement.iterator(selectorWFilter), "thesum".split(","), new Object[][]{{12 + 22}}));

            env.undeployAll();
        }

        private void sendAssertArray(RegressionEnvironment env, String id, int[] array, int value, int expected) {
            env.sendEventBean(new SupportEventWithIntArray(id, array, value));
            env.assertEqualsNew("s0", "thesum", expected);
        }
    }

    private static class ContextKeySegmentedPatternFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "@public create context IndividualBean partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);

            String eplAnalysis = "@name('s0') context IndividualBean " +
                "select * from pattern [every (event1=SupportBean(stringContainsX(theString) = false) -> event2=SupportBean(stringContainsX(theString) = true))]";
            env.compileDeploy(eplAnalysis, path).addListener("s0");

            env.sendEventBean(new SupportBean("F1", 0));
            env.sendEventBean(new SupportBean("F1", 0));

            env.milestone(0);

            env.sendEventBean(new SupportBean("X1", 0));
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            env.sendEventBean(new SupportBean("X1", 0));
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedMatchRecognize implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();
            String eplContextOne = "@public create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContextOne, path);

            String eplMatchRecog = "@name('s0') context SegmentedByString " +
                "select * from SupportBean\n" +
                "match_recognize ( \n" +
                "  measures A.longPrimitive as a, B.longPrimitive as b\n" +
                "  pattern (A B) \n" +
                "  define " +
                "    A as A.intPrimitive = 1," +
                "    B as B.intPrimitive = 2\n" +
                ")";
            env.compileDeploy(eplMatchRecog, path).addListener("s0");

            env.sendEventBean(makeEvent("A", 1, 10));

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("B", 1, 30));

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("A", 2, 20));
            env.assertPropsNew("s0", "a,b".split(","), new Object[]{10L, 20L});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("B", 2, 40));
            env.assertPropsNew("s0", "a,b".split(","), new Object[]{30L, 40L});

            env.undeployAll();

            // try with "prev"
            path.clear();
            String eplContextTwo = "@public create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContextTwo, path);

            String eplMatchRecogWithPrev = "@name('s0') context SegmentedByString select * from SupportBean " +
                "match_recognize ( " +
                "  measures A.longPrimitive as e1, B.longPrimitive as e2" +
                "  pattern (A B) " +
                "  define A as A.intPrimitive >= prev(A.intPrimitive),B as B.intPrimitive >= prev(B.intPrimitive) " +
                ")";
            env.compileDeploy(eplMatchRecogWithPrev, path).addListener("s0");

            env.sendEventBean(makeEvent("A", 1, 101));
            env.sendEventBean(makeEvent("B", 1, 201));

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("A", 2, 102));
            env.sendEventBean(makeEvent("B", 2, 202));

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("A", 3, 103));
            env.assertPropsNew("s0", "e1,e2".split(","), new Object[]{102L, 103L});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("B", 3, 203));
            env.assertPropsNew("s0", "e1,e2".split(","), new Object[]{202L, 203L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoinRemoveStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            RegressionPath path = new RegressionPath();

            String stmtContext = "@public create context SegmentedBySession partition by sessionId from SupportWebEvent";
            env.compileDeploy(stmtContext, path);

            String epl = "@name('s0') context SegmentedBySession " +
                " select rstream A.pageName as pageNameA , A.sessionId as sessionIdA, B.pageName as pageNameB, C.pageName as pageNameC from " +
                "SupportWebEvent(pageName='Start')#time(30) A " +
                "full outer join " +
                "SupportWebEvent(pageName='Middle')#time(30) B on A.sessionId = B.sessionId " +
                "full outer join " +
                "SupportWebEvent(pageName='End')#time(30) C on A.sessionId  = C.sessionId " +
                "where A.pageName is not null and (B.pageName is null or C.pageName is null) ";
            env.compileDeploy(epl, path);

            env.addListener("s0");

            // Set up statement for finding missing events
            sendWebEventsComplete(env, 0);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(20000);
            sendWebEventsComplete(env, 1);

            env.advanceTime(40000);
            sendWebEventsComplete(env, 2);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(60000);
            sendWebEventsIncomplete(env, 3);

            env.advanceTime(80000);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(100000);
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSelector implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create context PartitionedByString partition by theString from SupportBean", path);
            String[] fields = "c0,c1".split(",");
            env.compileDeploy("@Name('s0') context PartitionedByString select context.key1 as c0, sum(intPrimitive) as c1 from SupportBean#length(5)", path);

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E2", 21));

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1", 10}, {"E2", 41}});

            env.milestone(1);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1", 10}, {"E2", 41}});

            // test iterator targeted
            env.assertStatement("s0", statement -> {
                SupportSelectorPartitioned selector = new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"E2"}));
                EPAssertionUtil.assertPropsPerRow(statement.iterator(selector), statement.safeIterator(selector), fields, new Object[][]{{"E2", 41}});
                assertFalse(statement.iterator(new SupportSelectorPartitioned((List) null)).hasNext());
                assertFalse(statement.iterator(new SupportSelectorPartitioned(Collections.singletonList(new Object[]{"EX"}))).hasNext());
                assertFalse(statement.iterator(new SupportSelectorPartitioned(Collections.emptyList())).hasNext());

                // test iterator filtered
                MySelectorFilteredPartitioned filtered = new MySelectorFilteredPartitioned(new Object[]{"E2"});
                EPAssertionUtil.assertPropsPerRow(statement.iterator(filtered), statement.safeIterator(filtered), fields, new Object[][]{{"E2", 41}});

                // test always-false filter - compare context partition info
                MySelectorFilteredPartitioned filteredFalse = new MySelectorFilteredPartitioned(null);
                assertFalse(statement.iterator(filteredFalse).hasNext());
                EPAssertionUtil.assertEqualsAnyOrder(new Object[]{new Object[]{"E1"}, new Object[]{"E2"}}, filteredFalse.getContexts().toArray());

                try {
                    statement.iterator(new ContextPartitionSelectorCategory() {
                        public Set<String> getLabels() {
                            return null;
                        }
                    });
                    fail();
                } catch (InvalidContextPartitionSelector ex) {
                    TestCase.assertTrue("message: " + ex.getMessage(), ex.getMessage().startsWith("Invalid context partition selector, expected an implementation class of any of [ContextPartitionSelectorAll, ContextPartitionSelectorFiltered, ContextPartitionSelectorById, ContextPartitionSelectorSegmented] interfaces but received com."));
                }
            });

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // invalid filter spec
            epl = "create context SegmentedByAString partition by string from SupportBean(dummy = 1)";
            env.tryInvalidCompile(epl, "Failed to validate filter expression 'dummy=1': Property named 'dummy' is not valid in any stream [");

            // property not found
            epl = "create context SegmentedByAString partition by dummy from SupportBean";
            env.tryInvalidCompile(epl, "For context 'SegmentedByAString' property name 'dummy' not found on type SupportBean [");

            // mismatch number pf properties
            epl = "create context SegmentedByAString partition by theString from SupportBean, id, p00 from SupportBean_S0";
            env.tryInvalidCompile(epl, "For context 'SegmentedByAString' expected the same number of property names for each event type, found 1 properties for event type 'SupportBean' and 2 properties for event type 'SupportBean_S0' [create context SegmentedByAString partition by theString from SupportBean, id, p00 from SupportBean_S0]");

            // incompatible property types
            epl = "create context SegmentedByAString partition by theString from SupportBean, id from SupportBean_S0";
            env.tryInvalidCompile(epl, "For context 'SegmentedByAString' for context 'SegmentedByAString' found mismatch of property types, property 'theString' of type 'String' compared to property 'id' of type 'Integer' [");

            // duplicate type specification
            epl = "create context SegmentedByAString partition by theString from SupportBean, theString from SupportBean";
            env.tryInvalidCompile(epl, "For context 'SegmentedByAString' the event type 'SupportBean' is listed twice [");

            // duplicate type: subtype
            epl = "create context SegmentedByAString partition by baseAB from ISupportBaseAB, a from ISupportA";
            env.tryInvalidCompile(epl, "For context 'SegmentedByAString' the event type 'ISupportA' is listed twice: Event type 'ISupportA' is a subtype or supertype of event type 'ISupportBaseAB' [");

            // validate statement not applicable filters
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create context SegmentedByAString partition by theString from SupportBean", path);
            epl = "context SegmentedByAString select * from SupportBean_S0";
            env.tryInvalidCompile(path, epl, "Segmented context 'SegmentedByAString' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type 'SupportBean_S0' is not one of the types listed [");

            // invalid attempt to partition a named window's streams
            env.compileDeploy("@public create window MyWindow#keepall as SupportBean", path);
            epl = "@public create context SegmentedByWhat partition by theString from MyWindow";
            env.tryInvalidCompile(path, epl, "Partition criteria may not include named windows [@public create context SegmentedByWhat partition by theString from MyWindow]");

            // partitioned with named window
            env.compileDeploy("@public create schema SomeSchema(ipAddress string)", path);
            env.compileDeploy("@public create context TheSomeSchemaCtx Partition By ipAddress From SomeSchema", path);
            epl = "@public context TheSomeSchemaCtx create window MyEvent#time(30 sec) (ipAddress string)";
            env.tryInvalidCompile(path, epl, "Segmented context 'TheSomeSchemaCtx' requires that named windows are associated to an existing event type and that the event type is listed among the partitions defined by the create-context statement");

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedLargeNumberPartitions implements RegressionExecution {
        @Override
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED);
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByAString  partition by theString from SupportBean", path);

            String[] fields = "col1".split(",");
            env.compileDeploy("@name('s0') context SegmentedByAString " +
                "select sum(intPrimitive) as col1," +
                "prev(1, intPrimitive)," +
                "prior(1, intPrimitive)," +
                "(select id from SupportBean_S0#lastevent)" +
                "  from SupportBean#keepall", path);
            env.addListener("s0");

            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
                env.assertPropsNew("s0", fields, new Object[]{i});
            }

            env.undeployAll();
        }
    }

    public static class ContextKeySegmentedAdditionalFilters implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByAString " +
                "partition by theString from SupportBean(intPrimitive>0), p00 from SupportBean_S0(id > 0)", path);

            // first send a view events
            env.sendEventBean(new SupportBean("B1", -1));
            env.sendEventBean(new SupportBean_S0(-2, "S0"));
            env.assertThat(() -> Assert.assertEquals(0, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            String[] fields = "col1,col2".split(",");
            env.compileDeploy("@name('s0') context SegmentedByAString " +
                "select sum(sb.intPrimitive) as col1, sum(s0.id) as col2 " +
                "from pattern [every (s0=SupportBean_S0 or sb=SupportBean)]", path);
            env.addListener("s0");

            env.assertThat(() -> Assert.assertEquals(2, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(-3, "S0"));
            env.sendEventBean(new SupportBean("S0", -1));
            env.sendEventBean(new SupportBean("S1", -2));
            env.assertListenerNotInvoked("s0");
            env.assertThat(() -> Assert.assertEquals(2, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(2, "S0"));
            env.assertPropsNew("s0", fields, new Object[]{null, 2});

            env.milestone(2);

            env.sendEventBean(new SupportBean("S1", 10));
            env.assertPropsNew("s0", fields, new Object[]{10, null});

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(-2, "S0"));
            env.sendEventBean(new SupportBean("S1", -10));
            env.assertListenerNotInvoked("s0");

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(3, "S1"));
            env.assertPropsNew("s0", fields, new Object[]{10, 3});

            env.milestone(5);

            env.sendEventBean(new SupportBean("S0", 9));
            env.assertPropsNew("s0", fields, new Object[]{9, 2});

            env.milestone(6);

            env.undeployAll();
            env.assertThat(() -> assertEquals(0, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.milestone(7);

            // Test unnecessary filter
            String epl = "@public create context CtxSegmented partition by theString from SupportBean;" +
                "context CtxSegmented select * from pattern [every a=SupportBean -> c=SupportBean(c.theString=a.theString)];";
            env.compileDeploy(epl);
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 2));

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedMultiStatementFilterCount implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByAString " +
                "partition by theString from SupportBean, p00 from SupportBean_S0", path);
            env.assertThat(() -> Assert.assertEquals(0, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            // first send a view events
            env.sendEventBean(new SupportBean("B1", 1));
            env.sendEventBean(new SupportBean_S0(10, "S0"));

            String[] fields = new String[]{"col1"};
            env.compileDeploy("@name('s0') context SegmentedByAString select sum(id) as col1 from SupportBean_S0", path);
            env.addListener("s0");

            env.assertThat(() -> Assert.assertEquals(2, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.sendEventBean(new SupportBean_S0(10, "S0"));
            env.assertPropsNew("s0", fields, new Object[]{10});

            env.milestone(0);

            env.assertThat(() -> Assert.assertEquals(3, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.sendEventBean(new SupportBean_S0(8, "S1"));
            env.assertPropsNew("s0", fields, new Object[]{8});

            env.milestone(1);

            env.assertThat(() -> Assert.assertEquals(4, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.sendEventBean(new SupportBean_S0(4, "S0"));
            env.assertPropsNew("s0", fields, new Object[]{14});

            env.milestone(2);

            env.assertThat(() -> Assert.assertEquals(4, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.compileDeploy("@name('s1') context SegmentedByAString select sum(intPrimitive) as col1 from SupportBean", path);
            env.addListener("s1");

            env.assertThat(() -> Assert.assertEquals(6, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.sendEventBean(new SupportBean("S0", 5));
            env.assertPropsNew("s1", fields, new Object[]{5});

            env.assertThat(() -> Assert.assertEquals(6, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.milestone(3);

            env.sendEventBean(new SupportBean("S2", 6));
            env.assertPropsNew("s1", fields, new Object[]{6});

            env.assertThat(() -> Assert.assertEquals(8, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.undeployModuleContaining("s0");
            env.assertThat(() -> Assert.assertEquals(5, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));  // 5 = 3 from context instances and 2 from context itself

            env.milestone(4);

            env.undeployModuleContaining("s1");
            env.assertThat(() -> Assert.assertEquals(0, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.undeployModuleContaining("context");
            env.assertThat(() -> Assert.assertEquals(0, SupportFilterServiceHelper.getFilterSvcCountApprox(env)));

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSubtype implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "col1".split(",");
            String epl = "@Name('context') create context SegmentedByString partition by baseAB from ISupportBaseAB;\n" +
                "@name('s0') context SegmentedByString select count(*) as col1 from ISupportA;\n";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(new ISupportAImpl("A1", "AB1"));
            env.assertPropsNew("s0", fields, new Object[]{1L});

            env.sendEventBean(new ISupportAImpl("A2", "AB1"));
            env.assertPropsNew("s0", fields, new Object[]{2L});

            env.milestone(1);

            env.sendEventBean(new ISupportAImpl("A3", "AB2"));
            env.assertPropsNew("s0", fields, new Object[]{1L});

            env.sendEventBean(new ISupportAImpl("A4", "AB1"));
            env.assertPropsNew("s0", fields, new Object[]{3L});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoinMultitypeMultifield implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedBy2Fields " +
                "partition by theString and intPrimitive from SupportBean, p00 and id from SupportBean_S0", path);

            String[] fields = "c1,c2,c3,c4,c5,c6".split(",");
            env.compileDeploy("@name('s0') context SegmentedBy2Fields " +
                "select theString as c1, intPrimitive as c2, id as c3, p00 as c4, context.key1 as c5, context.key2 as c6 " +
                "from SupportBean#lastevent, SupportBean_S0#lastevent", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(2, "G1"));

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", 2));

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(1, "G2"));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean("G2", 1));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 1, 1, "G2", "G2", 1});

            env.sendEventBean(new SupportBean_S0(2, "G2"));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 2, 2, "G2", "G2", 2});

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(1, "G1"));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 1, 1, "G1", "G1", 1});

            env.milestone(4);

            env.sendEventBean(new SupportBean("G1", 2));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 2, 2, "G1", "G1", 2});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSubselectPrevPrior implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByString partition by theString from SupportBean", path);

            String[] fieldsPrev = new String[]{"theString", "col1"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, (select prev(0, id) from SupportBean_S0#keepall) as col1 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.assertPropsNew("s0", fieldsPrev, new Object[]{"G1", null});

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.sendEventBean(new SupportBean("G1", 11));
            env.assertPropsNew("s0", fieldsPrev, new Object[]{"G1", 1});

            env.sendEventBean(new SupportBean("G2", 20));
            env.assertPropsNew("s0", fieldsPrev, new Object[]{"G2", null});

            env.sendEventBean(new SupportBean_S0(2, "E2"));
            env.sendEventBean(new SupportBean("G2", 21));
            env.assertPropsNew("s0", fieldsPrev, new Object[]{"G2", 2});

            env.sendEventBean(new SupportBean("G1", 12));
            env.assertPropsNew("s0", fieldsPrev, new Object[]{"G1", null});  // since returning multiple rows

            env.undeployModuleContaining("s0");

            String[] fieldsPrior = new String[]{"theString", "col1"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, (select prior(0, id) from SupportBean_S0#keepall) as col1 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"G1", null});

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            env.sendEventBean(new SupportBean("G1", 11));
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"G1", 1});

            env.sendEventBean(new SupportBean("G2", 20));
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"G2", null});    // since category started as soon as statement added

            env.sendEventBean(new SupportBean_S0(2, "E2"));
            env.sendEventBean(new SupportBean("G2", 21));
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"G2", 2}); // since returning multiple rows

            env.sendEventBean(new SupportBean("G1", 12));
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"G1", null});  // since returning multiple rows

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedPrior implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"val0", "val1"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select intPrimitive as val0, prior(1, intPrimitive) as val1 from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.assertPropsNew("s0", fields, new Object[]{10, null});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 20));
            env.assertPropsNew("s0", fields, new Object[]{20, null});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G1", 11));
            env.assertPropsNew("s0", fields, new Object[]{11, 10});

            env.undeployModuleContaining("s0");
            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedSubqueryFiltered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"theString", "intPrimitive", "val0"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select theString, intPrimitive, (select p00 from SupportBean_S0#lastevent as s0 where sb.intPrimitive = s0.id) as val0 " +
                "from SupportBean as sb", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(10, "s1"));
            env.sendEventBean(new SupportBean("G1", 10));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 10, null});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(10, "s2"));
            env.sendEventBean(new SupportBean("G1", 10));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 10, "s2"});

            env.sendEventBean(new SupportBean("G2", 10));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 10, null});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(10, "s3"));
            env.sendEventBean(new SupportBean("G2", 10));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 10, "s3"});

            env.sendEventBean(new SupportBean("G3", 10));
            env.assertPropsNew("s0", fields, new Object[]{"G3", 10, null});

            env.milestone(2);

            env.sendEventBean(new SupportBean("G1", 10));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 10, "s3"});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"sb.theString", "sb.intPrimitive", "s0.id"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from SupportBean#keepall as sb, SupportBean_S0#keepall as s0 " +
                "where intPrimitive = id", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean_S0(20));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 20, 20});

            env.sendEventBean(new SupportBean_S0(30));
            env.sendEventBean(new SupportBean("G3", 30));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean("G1", 30));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 30, 30});

            env.sendEventBean(new SupportBean("G2", 30));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 30, 30});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('context') @public create context SegmentedByString partition by theString from SupportBean", path);

            String[] fields = new String[]{"a.theString", "a.intPrimitive", "b.theString", "b.intPrimitive"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive+1)]", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G1", 20));
            env.sendEventBean(new SupportBean("G2", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            env.assertListenerNotInvoked("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean("G2", 21));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 20, "G2", 21});

            env.sendEventBean(new SupportBean("G1", 11));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 10, "G1", 11});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", 22));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 21, "G2", 22});

            env.undeployModuleContaining("s0");

            // add another statement: contexts already exist, this one uses @Consume
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive+1)@Consume]", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G1", 20));

            env.milestone(2);

            env.sendEventBean(new SupportBean("G2", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean("G2", 21));
            env.assertPropsNew("s0", fields, new Object[]{"G2", 20, "G2", 21});

            env.sendEventBean(new SupportBean("G1", 11));
            env.assertPropsNew("s0", fields, new Object[]{"G1", 10, "G1", 11});

            env.milestone(3);

            env.sendEventBean(new SupportBean("G2", 22));
            env.assertListenerNotInvoked("s0");

            env.undeployModuleContaining("s0");

            // test truly segmented consume
            String[] fieldsThree = new String[]{"a.theString", "a.intPrimitive", "b.id", "b.p00"};
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select * from pattern [every a=SupportBean -> b=SupportBean_S0(id=a.intPrimitive)@Consume]", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 10));
            env.assertListenerNotInvoked("s0");

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(10, "E1"));   // should be 2 output rows
            env.assertPropsPerRowLastNew("s0", fieldsThree, new Object[][]{{"G1", 10, 10, "E1"}, {"G2", 10, 10, "E1"}});

            env.undeployAll();
        }
    }

    public static class ContextKeySegmentedPatternSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@Name('CTX') @public create context SegmentedByString partition by theString from SupportBean, p00 from SupportBean_S0;\n" +
                "@Name('S1') context SegmentedByString " +
                "select a.theString as c0, a.intPrimitive as c1, b.id as c2, b.p00 as c3 from pattern [" +
                "every a=SupportBean -> b=SupportBean_S0(id=a.intPrimitive)];\n";
            env.compileDeploy(epl).addListener("S1");
            String[] fields = "c0,c1,c2,c3".split(",");

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 10));
            env.sendEventBean(new SupportBean("G2", 20));
            env.sendEventBean(new SupportBean_S0(0, "G1"));
            env.sendEventBean(new SupportBean_S0(10, "G2"));
            env.assertListenerNotInvoked("S1");

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(20, "G2"));
            env.assertPropsNew("S1", fields, new Object[]{"G2", 20, 20, "G2"});

            env.milestone(2);

            env.sendEventBean(new SupportBean_S0(20, "G2"));
            env.sendEventBean(new SupportBean_S0(0, "G1"));
            env.assertListenerNotInvoked("S1");

            env.sendEventBean(new SupportBean_S0(10, "G1"));
            env.assertPropsNew("S1", fields, new Object[]{"G1", 10, 10, "G1"});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedViewSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String contextEPL = "@Name('context') @public create context SegmentedByString as partition by theString from SupportBean";
            env.compileDeploy(contextEPL, path);

            String[] fieldsIterate = "intPrimitive".split(",");
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select irstream intPrimitive, prevwindow(items) as pw from SupportBean#length(2) as items", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            assertViewData(env, 10, new Object[][]{{"G1", 10}}, null);
            env.assertPropsPerRowIterator("s0", fieldsIterate, new Object[][]{{10}});

            env.sendEventBean(new SupportBean("G2", 20));
            assertViewData(env, 20, new Object[][]{{"G2", 20}}, null);

            env.sendEventBean(new SupportBean("G1", 11));
            assertViewData(env, 11, new Object[][]{{"G1", 11}, {"G1", 10}}, null);
            env.assertPropsPerRowIterator("s0", fieldsIterate, new Object[][]{{10}, {11}, {20}});

            env.sendEventBean(new SupportBean("G2", 21));
            assertViewData(env, 21, new Object[][]{{"G2", 21}, {"G2", 20}}, null);

            env.sendEventBean(new SupportBean("G1", 12));
            assertViewData(env, 12, new Object[][]{{"G1", 12}, {"G1", 11}}, 10);

            env.sendEventBean(new SupportBean("G2", 22));
            assertViewData(env, 22, new Object[][]{{"G2", 22}, {"G2", 21}}, 20);

            env.undeployModuleContaining("s0");

            // test SODA
            env.undeployAll();
            path.clear();

            env.eplToModelCompileDeploy(contextEPL, path);

            // test built-in properties
            String[] fields = "c1,c2,c3,c4".split(",");
            String ctx = "SegmentedByString";
            env.compileDeploy("@Name('s0') context SegmentedByString " +
                "select context.name as c1, context.id as c2, context.key1 as c3, theString as c4 " +
                "from SupportBean#length(2) as items", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            env.assertPropsNew("s0", fields, new Object[]{ctx, 0, "G1", "G1"});
            SupportContextPropUtil.assertContextProps(env, "context", "SegmentedByString", new int[]{0}, "key1", new Object[][]{{"G1"}});

            env.undeployAll();

            // test grouped delivery
            path.clear();
            env.compileDeploy("@name('var') @public create variable boolean trigger = false", path);
            env.compileDeploy("@public create context MyCtx partition by theString from SupportBean", path);
            env.compileDeploy("@Name('s0') context MyCtx select * from SupportBean#expr(not trigger) for grouped_delivery(theString)", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.runtimeSetVariable("var", "trigger", true);
            env.advanceTime(100);

            env.assertListener("s0", listener -> assertEquals(2, listener.getNewDataList().size()));

            env.undeployAll();
        }
    }

    public static class ContextKeySegmentedViewSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplContext = "@Name('CTX') @public create context SegmentedByString partition by theString from SupportBean";
            env.compileDeploy(eplContext, path);

            String[] fields = "theString,intPrimitive".split(",");
            String eplSelect = "@Name('S1') context SegmentedByString select irstream * from SupportBean#lastevent()";
            env.compileDeploy(eplSelect, path).addListener("S1");

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 1));
            env.assertPropsNew("S1", fields, new Object[]{"G1", 1});

            env.milestone(1);

            env.sendEventBean(new SupportBean("G2", 10));
            env.assertPropsNew("S1", fields, new Object[]{"G2", 10});

            env.milestone(2);

            env.sendEventBean(new SupportBean("G1", 2));
            env.assertPropsIRPair("S1", fields, new Object[]{"G1", 2}, new Object[]{"G1", 1});

            env.milestone(3);

            env.sendEventBean(new SupportBean("G2", 11));
            env.assertPropsIRPair("S1", fields, new Object[]{"G2", 11}, new Object[]{"G2", 10});

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedJoinWhereClauseOnPartitionKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyCtx partition by theString from SupportBean;\n" +
                "@name('select') context MyCtx select * from SupportBean#lastevent as sb, SupportBean_S0#lastevent as s0 " +
                "where theString is 'Test'";
            env.compileDeploy(epl).addListener("select");

            env.sendEventBean(new SupportBean("Test", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean_S0(1));
            env.assertListenerInvoked("select");

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedNullSingleKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create context MyContext partition by theString from SupportBean", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) as cnt from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean(null, 10));
            env.assertEqualsNew("s0", "cnt", 1L);

            env.sendEventBean(new SupportBean(null, 20));
            env.assertEqualsNew("s0", "cnt", 2L);

            env.sendEventBean(new SupportBean("A", 30));
            env.assertEqualsNew("s0", "cnt", 1L);

            env.undeployAll();
        }
    }

    private static class ContextKeySegmentedNullKeyMultiKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create context MyContext partition by theString, intBoxed, intPrimitive from SupportBean", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) as cnt from SupportBean", path);
            env.addListener("s0");

            sendSBEvent(env, "A", null, 1);
            env.assertEqualsNew("s0", "cnt", 1L);

            sendSBEvent(env, "A", null, 1);
            env.assertEqualsNew("s0", "cnt", 2L);

            sendSBEvent(env, "A", 10, 1);
            env.assertEqualsNew("s0", "cnt", 1L);

            env.undeployAll();
        }
    }

    private static void assertViewData(RegressionEnvironment env, int newIntExpected, Object[][] newArrayExpected, Integer oldIntExpected) {
        env.assertListener("s0", listener -> {
            Assert.assertEquals(1, listener.getLastNewData().length);
            Assert.assertEquals(newIntExpected, listener.getLastNewData()[0].get("intPrimitive"));
            SupportBean[] beans = (SupportBean[]) listener.getLastNewData()[0].get("pw");
            assertEquals(newArrayExpected.length, beans.length);
            for (int i = 0; i < beans.length; i++) {
                Assert.assertEquals(newArrayExpected[i][0], beans[i].getTheString());
                Assert.assertEquals(newArrayExpected[i][1], beans[i].getIntPrimitive());
            }

            if (oldIntExpected != null) {
                Assert.assertEquals(1, listener.getLastOldData().length);
                Assert.assertEquals(oldIntExpected, listener.getLastOldData()[0].get("intPrimitive"));
            } else {
                assertNull(listener.getLastOldData());
            }
            listener.reset();
        });
    }

    private static class ContextKeySegmentedTermByFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create context ByP0 as partition by theString from SupportBean terminated by SupportBean(intPrimitive<0)", path);
            env.compileDeploy("@name('s0') context ByP0 select theString, count(*) as cnt from SupportBean(intPrimitive>= 0)", path);

            env.addListener("s0");

            sendAssertSB(1, env, "A", 0);

            env.milestone(0);

            sendAssertSB(2, env, "A", 0);
            sendAssertNone(env, new SupportBean("A", -1));
            sendAssertSB(1, env, "A", 0);

            env.milestone(1);

            sendAssertSB(1, env, "B", 0);
            sendAssertNone(env, new SupportBean("B", -1));

            env.milestone(2);

            sendAssertSB(1, env, "B", 0);

            env.milestone(3);

            sendAssertSB(2, env, "B", 0);
            sendAssertNone(env, new SupportBean("B", -1));
            sendAssertSB(1, env, "B", 0);

            sendAssertNone(env, new SupportBean("C", -1));

            env.undeployAll();
        }
    }

    private static class MySelectorFilteredPartitioned implements ContextPartitionSelectorFiltered {

        private Object[] match;

        private List<Object[]> contexts = new ArrayList<>();
        private LinkedHashSet<Integer> cpids = new LinkedHashSet<>();

        private MySelectorFilteredPartitioned(Object[] match) {
            this.match = match;
        }

        public boolean filter(ContextPartitionIdentifier contextPartitionIdentifier) {
            ContextPartitionIdentifierPartitioned id = (ContextPartitionIdentifierPartitioned) contextPartitionIdentifier;
            if (match == null && cpids.contains(id.getContextPartitionId())) {
                throw new RuntimeException("Already exists context id: " + id.getContextPartitionId());
            }
            cpids.add(id.getContextPartitionId());
            contexts.add(id.getKeys());
            return Arrays.equals(id.getKeys(), match);
        }

        public List<Object[]> getContexts() {
            return contexts;
        }
    }

    private static void sendWebEventsIncomplete(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportWebEvent("Start", String.valueOf(id)));
        env.sendEventBean(new SupportWebEvent("End", String.valueOf(id)));
    }

    private static void sendWebEventsComplete(RegressionEnvironment env, int id) {
        env.sendEventBean(new SupportWebEvent("Start", String.valueOf(id)));
        env.sendEventBean(new SupportWebEvent("Middle", String.valueOf(id)));
        env.sendEventBean(new SupportWebEvent("End", String.valueOf(id)));
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static boolean stringContainsX(String theString) {
        return theString.contains("X");
    }

    private static void sendAssertSB(long expected, RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
        env.assertPropsNew("s0", "theString,cnt".split(","), new Object[]{theString, expected});
    }

    private static void sendAssertNone(RegressionEnvironment env, Object event) {
        env.sendEventBean(event);
        env.assertListenerNotInvoked("s0");
    }

    private static SupportBean sendSBEvent(RegressionEnvironment env, String string, Integer intBoxed, int intPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportBean sendSBEvent(RegressionEnvironment env, String string, int intPrimitive) {
        return sendSBEvent(env, string, null, intPrimitive);
    }
}
