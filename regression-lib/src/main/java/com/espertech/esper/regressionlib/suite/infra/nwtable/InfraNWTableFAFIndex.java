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

import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanTwo;
import com.espertech.esper.regressionlib.support.util.IndexAssertion;
import com.espertech.esper.regressionlib.support.util.IndexAssertionFAF;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class InfraNWTableFAFIndex implements IndexBackingTableInfo {
    private static final Logger log = LoggerFactory.getLogger(InfraNWTableFAFIndex.class);

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraSelectIndexChoiceJoin(true));
        execs.add(new InfraSelectIndexChoiceJoin(false));
        execs.add(new InfraSelectIndexChoice(true));
        execs.add(new InfraSelectIndexChoice(false));
        execs.add(new InfraSelectIndexMultikeyWArray(true));
        execs.add(new InfraSelectIndexMultikeyWArray(false));
        execs.add(new InfraSelectIndexMultikeyWArrayTwoField(true));
        execs.add(new InfraSelectIndexMultikeyWArrayTwoField(false));
        execs.add(new InfraSelectIndexMultikeyWArrayCompositeArray(true));
        execs.add(new InfraSelectIndexMultikeyWArrayCompositeArray(false));
        execs.add(new InfraSelectIndexMultikeyWArrayCompositeTwoArray(true));
        execs.add(new InfraSelectIndexMultikeyWArrayCompositeTwoArray(false));
        return execs;
    }

    private static class InfraSelectIndexMultikeyWArrayCompositeTwoArray implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectIndexMultikeyWArrayCompositeTwoArray(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = namedWindow ?
                "@public create window MyInfra#keepall as (id string, arrayOne string[], arrayTwo string[], value int);\n" :
                "@public create table MyInfra(id string primary key, arrayOne string[], arrayTwo string[], value int);\n";
            epl += "insert into MyInfra select id, stringOne as arrayOne, stringTwo as arrayTwo, value from SupportEventWithManyArray;\n" +
                "create index MyInfraIndex on MyInfra(arrayOne, arrayTwo, value btree);\n";
            env.compileDeploy(epl, path);

            sendManyArray(env, "E1", new String[] {"a", "b"}, new String[] {"c", "d"}, 100);
            sendManyArray(env, "E2", new String[] {"a", "b"}, new String[] {"e", "f"}, 200);
            sendManyArray(env, "E3", new String[] {"a"}, new String[] {"b"}, 300);

            env.milestone(0);

            assertFAF(env, path, "arrayOne = {'a', 'b'} and arrayTwo = {'e', 'f'} and value > 150", "E2");
            assertFAF(env, path, "arrayOne = {'a'} and arrayTwo = {'b'} and value > 150", "E3");
            assertFAF(env, path, "arrayOne = {'a', 'b'} and arrayTwo = {'c', 'd'} and value > 90", "E1");
            assertFAFNot(env, path, "arrayOne = {'a', 'b'} and arrayTwo = {'c', 'd'} and value > 200");
            assertFAFNot(env, path, "arrayOne = {'a', 'b'} and arrayTwo = {'c', 'e'} and value > 90");
            assertFAFNot(env, path, "arrayOne = {'ax', 'b'} and arrayTwo = {'c', 'd'} and value > 90");

            env.undeployAll();
        }

        private void sendManyArray(RegressionEnvironment env, String id, String[] arrayOne, String[] arrayTwo, int value) {
            env.sendEventBean(new SupportEventWithManyArray(id).withStringOne(arrayOne).withStringTwo(arrayTwo).withValue(value));
        }
    }

    private static class InfraSelectIndexMultikeyWArrayCompositeArray implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectIndexMultikeyWArrayCompositeArray(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = namedWindow ?
                "@public create window MyInfra#keepall as (id string, arrayOne string[], value int);\n" :
                "@public create table MyInfra(id string primary key, arrayOne string[], value int);\n";
            epl += "insert into MyInfra select id, stringOne as arrayOne, value from SupportEventWithManyArray;\n" +
                "create index MyInfraIndex on MyInfra(arrayOne, value btree);\n";
            env.compileDeploy(epl, path);

            sendManyArray(env, "E1", new String[] {"a", "b"}, 100);
            sendManyArray(env, "E2", new String[] {"a", "b"}, 200);
            sendManyArray(env, "E3", new String[] {"a"}, 300);

            env.milestone(0);

            assertFAF(env, path, "arrayOne = {'a', 'b'} and value < 150", "E1");
            assertFAF(env, path, "arrayOne = {'a', 'b'} and value > 150", "E2");
            assertFAF(env, path, "arrayOne = {'a'} and value > 200", "E3");
            assertFAFNot(env, path, "arrayOne = {'a'} and value > 400");
            assertFAFNot(env, path, "arrayOne = {'a', 'c'} and value < 150");

            env.undeployAll();
        }

        private void sendManyArray(RegressionEnvironment env, String id, String[] arrayOne, int value) {
            env.sendEventBean(new SupportEventWithManyArray(id).withStringOne(arrayOne).withValue(value));
        }
    }

    private static class InfraSelectIndexMultikeyWArrayTwoField implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectIndexMultikeyWArrayTwoField(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = namedWindow ?
                "@public create window MyInfra#keepall as (id string, arrayOne string[], arrayTwo string[]);\n" :
                "@public create table MyInfra(id string primary key, arrayOne string[], arrayTwo string[]);\n";
            epl += "insert into MyInfra select id, stringOne as arrayOne, stringTwo as arrayTwo from SupportEventWithManyArray;\n" +
                "create index MyInfraIndex on MyInfra(arrayOne, arrayTwo);\n";
            env.compileDeploy(epl, path);

            sendManyArray(env, "E1", new String[] {"a", "b"}, new String[] {"c", "d"});
            sendManyArray(env, "E2", new String[] {"a"}, new String[] {"b"});

            env.milestone(0);

            assertFAF(env, path, "arrayOne = {'a', 'b'} and arrayTwo = {'c', 'd'}", "E1");
            assertFAF(env, path, "arrayOne = {'a'} and arrayTwo = {'b'}", "E2");
            assertFAFNot(env, path, "arrayOne = {'a', 'b', 'c'} and arrayTwo = {'c', 'd'}");
            assertFAFNot(env, path, "arrayOne = {'a', 'b'} and arrayTwo = {'c', 'c'}");

            env.undeployAll();
        }

        private void sendManyArray(RegressionEnvironment env, String id, String[] arrayOne, String[] arrayTwo) {
            env.sendEventBean(new SupportEventWithManyArray(id).withStringOne(arrayOne).withStringTwo(arrayTwo));
        }
    }

    private static class InfraSelectIndexMultikeyWArray implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectIndexMultikeyWArray(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = namedWindow ?
                "@public create window MyInfra#keepall as (id string, array string[]);\n" :
                "@public create table MyInfra(id string primary key, array string[]);\n";
            epl += "insert into MyInfra select id, stringOne as array from SupportEventWithManyArray;\n" +
                   "create index MyInfraIndex on MyInfra(array);\n";
            env.compileDeploy(epl, path);

            sendManyArray(env, "E1", new String[] {"a", "b"});
            sendManyArray(env, "E2", new String[] {"a"});
            sendManyArray(env, "E3", null);

            env.milestone(0);

            assertFAF(env, path, "array = {'a', 'b'}", "E1");
            assertFAF(env, path, "array = {'a'}", "E2");
            assertFAF(env, path, "array is null", "E3");
            assertFAFNot(env, path, "array = {'b'}");

            env.undeployAll();
        }

        private void sendManyArray(RegressionEnvironment env, String id, String[] strings) {
            env.sendEventBean(new SupportEventWithManyArray(id).withStringOne(strings));
        }
    }

    private static class InfraSelectIndexChoiceJoin implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectIndexChoiceJoin(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            Object[] preloadedEventsOne = new Object[]{
                new SupportSimpleBeanOne("E1", 10, 1, 2),
                new SupportSimpleBeanOne("E2", 11, 3, 4),
                new SupportSimpleBeanTwo("E1", 20, 1, 2),
                new SupportSimpleBeanTwo("E2", 21, 3, 4),
            };
            IndexAssertionFAF fafAssertion = new IndexAssertionFAF() {
                public void run(EPFireAndForgetQueryResult result) {
                    String[] fields = "w1.s1,w2.s2,w1.i1,w2.i2".split(",");
                    EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields,
                        new Object[][]{{"E1", "E1", 10, 20}, {"E2", "E2", 11, 21}});
                }
            };

            IndexAssertion[] assertionsSingleProp = new IndexAssertion[]{
                new IndexAssertion(null, "s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "s1 = s2 and l1 = l2", true, fafAssertion),
                new IndexAssertion(null, "l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2", false, fafAssertion),
            };

            // single prop, no index, both declared unique (named window only)
            if (namedWindow) {
                assertIndexChoiceJoin(env, namedWindow, new String[0], preloadedEventsOne, "std:unique(s1)", "std:unique(s2)", assertionsSingleProp);
            }

            // single prop, unique indexes, both declared keepall
            String[] uniqueIndex = new String[]{"create unique index W1I1 on W1(s1)", "create unique index W1I2 on W2(s2)"};
            assertIndexChoiceJoin(env, namedWindow, uniqueIndex, preloadedEventsOne, "win:keepall()", "win:keepall()", assertionsSingleProp);

            // single prop, mixed indexes, both declared keepall
            IndexAssertion[] assertionsMultiProp = new IndexAssertion[]{
                new IndexAssertion(null, "s1 = s2", false, fafAssertion),
                new IndexAssertion(null, "s1 = s2 and l1 = l2", true, fafAssertion),
                new IndexAssertion(null, "l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2", false, fafAssertion),
            };
            if (namedWindow) {
                String[] mixedIndex = new String[]{"create index W1I1 on W1(s1, l1)", "create unique index W1I2 on W2(s2)"};
                assertIndexChoiceJoin(env, namedWindow, mixedIndex, preloadedEventsOne, "std:unique(s1)", "win:keepall()", assertionsSingleProp);

                // multi prop, no index, both declared unique
                assertIndexChoiceJoin(env, namedWindow, new String[0], preloadedEventsOne, "std:unique(s1, l1)", "std:unique(s2, l2)", assertionsMultiProp);
            }

            // multi prop, unique indexes, both declared keepall
            String[] uniqueIndexMulti = new String[]{"create unique index W1I1 on W1(s1, l1)", "create unique index W1I2 on W2(s2, l2)"};
            assertIndexChoiceJoin(env, namedWindow, uniqueIndexMulti, preloadedEventsOne, "win:keepall()", "win:keepall()", assertionsMultiProp);

            // multi prop, mixed indexes, both declared keepall
            if (namedWindow) {
                String[] mixedIndexMulti = new String[]{"create index W1I1 on W1(s1)", "create unique index W1I2 on W2(s2, l2)"};
                assertIndexChoiceJoin(env, namedWindow, mixedIndexMulti, preloadedEventsOne, "std:unique(s1, l1)", "win:keepall()", assertionsMultiProp);
            }
        }

        private static void assertIndexChoiceJoin(RegressionEnvironment env, boolean namedWindow, String[] indexes, Object[] preloadedEvents, String datawindowOne, String datawindowTwo,
                                                  IndexAssertion... assertions) {

            RegressionPath path = new RegressionPath();
            if (namedWindow) {
                env.compileDeploy("create window W1." + datawindowOne + " as SupportSimpleBeanOne", path);
                env.compileDeploy("create window W2." + datawindowTwo + " as SupportSimpleBeanTwo", path);
            } else {
                env.compileDeploy("create table W1 (s1 String primary key, i1 int primary key, d1 double primary key, l1 long primary key)", path);
                env.compileDeploy("create table W2 (s2 String primary key, i2 int primary key, d2 double primary key, l2 long primary key)", path);
            }
            env.compileDeploy("insert into W1 select s1,i1,d1,l1 from SupportSimpleBeanOne", path);
            env.compileDeploy("insert into W2 select s2,i2,d2,l2 from SupportSimpleBeanTwo", path);

            for (String index : indexes) {
                env.compileDeploy(index, path);
            }
            for (Object event : preloadedEvents) {
                env.sendEventBean(event);
            }

            int count = 0;
            for (IndexAssertion assertion : assertions) {
                log.info("======= Testing #" + count++);
                String epl = INDEX_CALLBACK_HOOK +
                    (assertion.getHint() == null ? "" : assertion.getHint()) +
                    "select * from W1 as w1, W2 as w2 " +
                    "where " + assertion.getWhereClause();
                EPFireAndForgetQueryResult result = null;
                try {
                    result = env.compileExecuteFAF(epl, path);
                } catch (Throwable ex) {
                    log.error("Failed to process:" + ex.getMessage(), ex);
                    if (assertion.getEventSendAssertion() == null) {
                        // no assertion, expected
                        assertTrue(ex.getMessage().contains("index hint busted"));
                        continue;
                    }
                    throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
                }

                // assert index and access
                SupportQueryPlanIndexHook.assertJoinAllStreamsAndReset(assertion.getUnique());
                assertion.getFafAssertion().run(result);
            }

            env.undeployAll();
        }
    }

    private static class InfraSelectIndexChoice implements RegressionExecution {
        private final boolean namedWindow;

        public InfraSelectIndexChoice(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            Object[] preloadedEventsOne = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
            IndexAssertionFAF fafAssertion = new IndexAssertionFAF() {
                public void run(EPFireAndForgetQueryResult result) {
                    String[] fields = "s1,i1".split(",");
                    EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E2", 20}});
                }
            };

            // single index one field (plus declared unique)
            String[] noindexes = new String[0];
            assertIndexChoice(env, namedWindow, noindexes, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = 'E2'", null, null, fafAssertion),
                    new IndexAssertion(null, "s1 = 'E2' and l1 = 22", null, null, fafAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = 'E2' and l1 = 22", null, null, fafAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = 'E2' and l1 = 22"), // should bust
                });

            // single index one field (plus declared unique)
            String[] indexOneField = new String[]{"create unique index One on MyInfra (s1)"};
            assertIndexChoice(env, namedWindow, indexOneField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = 'E2'", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                    new IndexAssertion(null, "s1 in ('E2')", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                    new IndexAssertion(null, "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = 'E2' and l1 = 22"), // should bust
                });

            // single index two field (plus declared unique)
            String[] indexTwoField = new String[]{"create unique index One on MyInfra (s1, l1)"};
            assertIndexChoice(env, namedWindow, indexTwoField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = 'E2'", null, null, fafAssertion),
                    new IndexAssertion(null, "s1 = 'E2' and l1 = 22", "One", BACKING_MULTI_UNIQUE, fafAssertion),
                });

            // two index one unique (plus declared unique)
            String[] indexSetTwo = new String[]{
                "create index One on MyInfra (s1)",
                "create unique index Two on MyInfra (s1, d1)"};
            assertIndexChoice(env, namedWindow, indexSetTwo, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = 'E2'", "One", BACKING_SINGLE_DUPS, fafAssertion),
                    new IndexAssertion(null, "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                    new IndexAssertion("@Hint('index(Two,One)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = 'E2' and l1 = 22"),  // busted
                    new IndexAssertion("@Hint('index(explicit,bust)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                    new IndexAssertion(null, "s1 = 'E2' and d1 = 21 and l1 = 22", "Two", BACKING_MULTI_UNIQUE, fafAssertion),
                    new IndexAssertion("@Hint('index(explicit,bust)')", "d1 = 22 and l1 = 22"),   // busted
                });

            // range (unique)
            String[] indexSetThree = new String[]{
                "create index One on MyInfra (l1 btree)",
                "create index Two on MyInfra (d1 btree)"};
            assertIndexChoice(env, namedWindow, indexSetThree, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "l1 between 22 and 23", "One", BACKING_SORTED, fafAssertion),
                    new IndexAssertion(null, "d1 between 21 and 22", "Two", BACKING_SORTED, fafAssertion),
                    new IndexAssertion("@Hint('index(One, bust)')", "d1 between 21 and 22"), // busted
                });
        }
    }

    private static void assertIndexChoice(RegressionEnvironment env, boolean namedWindow, String[] indexes, Object[] preloadedEvents, String datawindow,
                                          IndexAssertion[] assertions) {
        RegressionPath path = new RegressionPath();
        String eplCreate = namedWindow ?
            "create window MyInfra." + datawindow + " as SupportSimpleBeanOne" :
            "create table MyInfra(s1 String primary key, i1 int primary key, d1 double primary key, l1 long primary key)";
        env.compileDeploy(eplCreate, path);
        env.compileDeploy("insert into MyInfra select s1,i1,d1,l1 from SupportSimpleBeanOne", path);
        for (String index : indexes) {
            env.compileDeploy(index, path);
        }
        for (Object event : preloadedEvents) {
            env.sendEventBean(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String epl = INDEX_CALLBACK_HOOK +
                (assertion.getHint() == null ? "" : assertion.getHint()) +
                "select * from MyInfra where " + assertion.getWhereClause();

            if (assertion.getFafAssertion() == null) {
                try {
                    env.compileExecuteFAF(epl, path);
                    fail();
                } catch (RuntimeException ex) {
                    // expected
                }
            } else {
                // assert index and access
                EPFireAndForgetQueryResult result = env.compileExecuteFAF(epl, path);
                SupportQueryPlanIndexHook.assertFAFAndReset(assertion.getExpectedIndexName(), assertion.getIndexBackingClass());
                assertion.getFafAssertion().run(result);
            }
        }

        env.undeployAll();
    }

    private static void assertFAF(RegressionEnvironment env, RegressionPath path, String epl, String expectedId) {
        String faf = "@Hint('index(MyInfraIndex, bust)') select * from MyInfra where " + epl;
        EPFireAndForgetQueryResult result = env.compileExecuteFAF(faf, path);
        assertEquals(1, result.getArray().length);
        assertEquals(expectedId, result.getArray()[0].get("id"));
    }

    private static void assertFAFNot(RegressionEnvironment env, RegressionPath path, String epl) {
        String faf = "@Hint('index(MyInfraIndex, bust)') select * from MyInfra where " + epl;
        EPFireAndForgetQueryResult result = env.compileExecuteFAF(faf, path);
        assertEquals(0, result.getArray().length);
    }
}
