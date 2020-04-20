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
package com.espertech.esper.regressionlib.suite.expr.filter;

import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_IntAlphabetic;
import com.espertech.esper.regressionlib.support.bean.SupportBean_StringAlphabetic;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExprFilterOptimizableOrRewrite {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterOrRewriteTwoOr());
        executions.add(new ExprFilterOrRewriteOrRewriteThreeOr());
        executions.add(new ExprFilterOrRewriteOrRewriteWithAnd());
        executions.add(new ExprFilterOrRewriteOrRewriteThreeWithOverlap());
        executions.add(new ExprFilterOrRewriteOrRewriteFourOr());
        executions.add(new ExprFilterOrRewriteOrRewriteEightOr());
        executions.add(new ExprFilterOrRewriteAndRewriteNotEqualsOr());
        executions.add(new ExprFilterOrRewriteAndRewriteNotEqualsConsolidate());
        executions.add(new ExprFilterOrRewriteAndRewriteNotEqualsWithOrConsolidateSecond());
        executions.add(new ExprFilterOrRewriteAndRewriteInnerOr());
        executions.add(new ExprFilterOrRewriteOrRewriteAndOrMulti());
        executions.add(new ExprFilterOrRewriteBooleanExprSimple());
        executions.add(new ExprFilterOrRewriteBooleanExprAnd());
        executions.add(new ExprFilterOrRewriteSubquery());
        executions.add(new ExprFilterOrRewriteHint());
        executions.add(new ExprFilterOrRewriteContextPartitionedSegmented());
        executions.add(new ExprFilterOrRewriteContextPartitionedHash());
        executions.add(new ExprFilterOrRewriteContextPartitionedCategory());
        executions.add(new ExprFilterOrRewriteContextPartitionedInitiatedSameEvent());
        executions.add(new ExprFilterOrRewriteContextPartitionedInitiated());
        return executions;
    }

    public static class ExprFilterOrRewriteHint implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Hint('MAX_FILTER_WIDTH=0') @name('s0') select * from SupportBean_IntAlphabetic((b=1 or c=1) and (d=1 or e=1))";
            env.compileDeployAddListenerMile(epl, "s0", 0);
            SupportFilterHelper.assertFilterSingle(env.statement("s0"), epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION);
            env.undeployAll();
        }
    }

    public static class ExprFilterOrRewriteSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select (select * from SupportBean_IntAlphabetic(a=1 or b=1)#keepall) as c0 from SupportBean";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            SupportBean_IntAlphabetic iaOne = intEvent(1, 1);
            env.sendEventBean(iaOne);
            env.sendEventBean(new SupportBean());
            assertEquals(iaOne, env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    public static class ExprFilterOrRewriteContextPartitionedCategory implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('ctx') create context MyContext \n" +
                "  group a=1 or b=1 as g1,\n" +
                "  group c=1 as g1\n" +
                "  from SupportBean_IntAlphabetic;" +
                "@name('s0') context MyContext select * from SupportBean_IntAlphabetic(d=1 or e=1)";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            sendAssertEvents(env,
                new Object[]{intEvent(1, 0, 0, 0, 1), intEvent(0, 1, 0, 1, 0), intEvent(0, 0, 1, 1, 1)},
                new Object[]{intEvent(0, 0, 0, 1, 0), intEvent(1, 0, 0, 0, 0), intEvent(0, 0, 1, 0, 0)}
            );

            env.undeployAll();
        }
    }

    public static class ExprFilterOrRewriteContextPartitionedHash implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext " +
                "coalesce by consistent_hash_crc32(a) from SupportBean_IntAlphabetic(b=1) granularity 16 preallocate;" +
                "@name('s0') context MyContext select * from SupportBean_IntAlphabetic(c=1 or d=1)";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            sendAssertEvents(env,
                new Object[]{intEvent(100, 1, 0, 1), intEvent(100, 1, 1, 0)},
                new Object[]{intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
            );
            env.undeployAll();
        }
    }

    public static class ExprFilterOrRewriteContextPartitionedSegmented implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext partition by a from SupportBean_IntAlphabetic(b=1 or c=1);" +
                "@name('s0') context MyContext select * from SupportBean_IntAlphabetic(d=1)";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            sendAssertEvents(env,
                new Object[]{intEvent(100, 1, 0, 1), intEvent(100, 0, 1, 1)},
                new Object[]{intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
            );
            env.undeployAll();
        }
    }

    public static class ExprFilterOrRewriteBooleanExprAnd implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filters = new String[]{
                "(a='a' or a like 'A%') and (b='b' or b like 'B%')",
            };
            for (String filter : filters) {
                String epl = "@name('s0') select * from SupportBean_StringAlphabetic(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean_StringAlphabetic", new FilterItem[][]{
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
                    {new FilterItem("b", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
                    {FilterItem.getBoolExprFilterItem()},
                });

                sendAssertEvents(env,
                    new Object[]{stringEvent("a", "b"), stringEvent("A1", "b"), stringEvent("a", "B1"), stringEvent("A1", "B1")},
                    new Object[]{stringEvent("x", "b"), stringEvent("a", "x"), stringEvent("A1", "C"), stringEvent("C", "B1")}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteBooleanExprSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filters = new String[]{
                "a like 'a%' and (b='b' or c='c')",
            };
            for (String filter : filters) {
                String epl = "@name('s0') select * from SupportBean_StringAlphabetic(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean_StringAlphabetic", new FilterItem[][]{
                    {new FilterItem("b", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
                    {new FilterItem("c", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
                });

                sendAssertEvents(env,
                    new Object[]{stringEvent("a1", "b", null), stringEvent("a1", null, "c")},
                    new Object[]{stringEvent("x", "b", null), stringEvent("a1", null, null), stringEvent("a1", null, "x")}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteAndRewriteNotEqualsWithOrConsolidateSecond implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filters = new String[]{
                "a!=1 and a!=2 and ((a!=3 and a!=4) or (a!=5 and a!=6))",
            };
            for (String filter : filters) {
                String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), FilterItem.getBoolExprFilterItem()},
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), FilterItem.getBoolExprFilterItem()},
                });

                sendAssertEvents(env,
                    new Object[]{intEvent(3), intEvent(4), intEvent(0)},
                    new Object[]{intEvent(2), intEvent(1)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteAndRewriteNotEqualsConsolidate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filters = new String[]{
                "a!=1 and a!=2 and (a!=3 or a!=4)",
            };
            for (String filter : filters) {
                String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("a", FilterOperator.NOT_EQUAL)},
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("a", FilterOperator.NOT_EQUAL)},
                });

                sendAssertEvents(env,
                    new Object[]{intEvent(3), intEvent(4), intEvent(0)},
                    new Object[]{intEvent(2), intEvent(1)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteAndRewriteNotEqualsOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filters = new String[]{
                "a!=1 and a!=2 and (b=1 or c=1)",
            };
            for (String filter : filters) {
                String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("b", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("c", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new Object[]{intEvent(3, 1, 0), intEvent(3, 0, 1), intEvent(0, 1, 0)},
                    new Object[]{intEvent(2, 0, 0), intEvent(1, 0, 0), intEvent(3, 0, 0)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteAndRewriteInnerOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filtersAB = new String[]{
                "theString='a' and (intPrimitive=1 or longPrimitive=10)",
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new SupportBean[]{makeEvent("a", 1, 0), makeEvent("a", 0, 10), makeEvent("a", 1, 10)},
                    new SupportBean[]{makeEvent("x", 0, 0), makeEvent("a", 2, 20), makeEvent("x", 1, 10)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteOrRewriteAndOrMulti implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filtersAB = new String[]{
                "a=1 and (b=1 or c=1) and (d=1 or e=1)",
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL), new FilterItem("d", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("c", FilterOperator.EQUAL), new FilterItem("d", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("c", FilterOperator.EQUAL), new FilterItem("e", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL), new FilterItem("e", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new Object[]{intEvent(1, 1, 0, 1, 0), intEvent(1, 0, 1, 0, 1), intEvent(1, 1, 0, 0, 1), intEvent(1, 0, 1, 1, 0)},
                    new Object[]{intEvent(1, 0, 0, 1, 0), intEvent(1, 0, 0, 1, 0), intEvent(1, 1, 1, 0, 0), intEvent(0, 1, 1, 1, 1)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteOrRewriteEightOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filtersAB = new String[]{
                "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100 or boolPrimitive=true or " +
                    "intBoxed=2 or longBoxed=20 or doubleBoxed=200",
                "longBoxed=20 or theString = 'a' or boolPrimitive=true or intBoxed=2 or longPrimitive=10 or doublePrimitive=100 or " +
                    "intPrimitive=1 or doubleBoxed=200",
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("doublePrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("boolPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("intBoxed", FilterOperator.EQUAL)},
                    {new FilterItem("longBoxed", FilterOperator.EQUAL)},
                    {new FilterItem("doubleBoxed", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new SupportBean[]{makeEvent("a", 1, 10, 100, true, 2, 20, 200), makeEvent("a", 0, 0, 0, true, 0, 0, 0),
                        makeEvent("a", 0, 0, 0, true, 0, 20, 0), makeEvent("x", 0, 0, 100, false, 0, 0, 0),
                        makeEvent("x", 1, 0, 0, false, 0, 0, 200), makeEvent("x", 0, 0, 0, false, 0, 0, 200),
                    },
                    new SupportBean[]{makeEvent("x", 0, 0, 0, false, 0, 0, 0)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteOrRewriteFourOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filtersAB = new String[]{
                "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100",
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("doublePrimitive", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new SupportBean[]{makeEvent("a", 1, 10, 100), makeEvent("x", 0, 0, 100), makeEvent("x", 0, 10, 100), makeEvent("a", 0, 0, 0)},
                    new SupportBean[]{makeEvent("x", 0, 0, 0)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteContextPartitionedInitiated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('ctx') create context MyContext initiated by SupportBean(theString='A' or intPrimitive=1) terminated after 24 hours;\n"
                + "@name('s0') context MyContext select * from SupportBean;\n";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            env.sendEventBean(new SupportBean("A", 1));
            env.listener("s0").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    public static class ExprFilterOrRewriteContextPartitionedInitiatedSameEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext initiated by SupportBean terminated after 24 hours;" +
                "@name('s0') context MyContext select * from SupportBean(theString='A' or intPrimitive=1)";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            env.sendEventBean(new SupportBean("A", 1));
            env.listener("s0").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    public static class ExprFilterOrRewriteOrRewriteThreeOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filtersAB = new String[]{
                "theString = 'a' or intPrimitive = 1 or longPrimitive = 2",
                "2 = longPrimitive or 1 = intPrimitive or theString = 'a'"
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new SupportBean[]{makeEvent("a", 0, 0), makeEvent("b", 1, 0), makeEvent("c", 0, 2), makeEvent("c", 0, 2)},
                    new SupportBean[]{makeEvent("v", 0, 0), makeEvent("c", 2, 1)}
                );

                env.undeployAll();
            }
        }
    }

    private static void sendAssertEvents(RegressionEnvironment env, Object[] matches, Object[] nonMatches) {
        env.listener("s0").reset();
        for (Object match : matches) {
            env.sendEventBean(match);
            assertSame(match, env.listener("s0").assertOneGetNewAndReset().getUnderlying());
        }
        env.listener("s0").reset();
        for (Object nonMatch : nonMatches) {
            env.sendEventBean(nonMatch);
            assertFalse(env.listener("s0").isInvoked());
        }
    }

    public static class ExprFilterOrRewriteOrRewriteThreeWithOverlap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filtersAB = new String[]{
                "theString = 'a' or theString = 'b' or intPrimitive=1",
                "intPrimitive = 1 or theString = 'b' or theString = 'a'",
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new SupportBean[]{makeEvent("a", 1), makeEvent("b", 0), makeEvent("x", 1)},
                    new SupportBean[]{makeEvent("x", 0)}
                );
                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteTwoOr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            // test 'or' rewrite
            String[] filtersAB = new String[]{
                "select * from SupportBean(theString = 'a' or intPrimitive = 1)",
                "select * from SupportBean(theString = 'a' or 1 = intPrimitive)",
                "select * from SupportBean('a' = theString or 1 = intPrimitive)",
                "select * from SupportBean('a' = theString or intPrimitive = 1)",
            };

            for (String filter : filtersAB) {
                env.compileDeployAddListenerMile("@name('s0')" + filter, "s0", milestone.getAndIncrement());

                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                });

                env.sendEventBean(new SupportBean("a", 0));
                env.listener("s0").assertOneGetNewAndReset();
                env.sendEventBean(new SupportBean("b", 1));
                env.listener("s0").assertOneGetNewAndReset();
                env.sendEventBean(new SupportBean("c", 0));
                assertFalse(env.listener("s0").getAndClearIsInvoked());

                env.undeployAll();
            }
        }
    }

    public static class ExprFilterOrRewriteOrRewriteWithAnd implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] filtersAB = new String[]{
                "(theString = 'a' and intPrimitive = 1) or (theString = 'b' and intPrimitive = 2)",
                "(intPrimitive = 1 and theString = 'a') or (intPrimitive = 2 and theString = 'b')",
                "(theString = 'b' and intPrimitive = 2) or (theString = 'a' and intPrimitive = 1)",
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                SupportFilterHelper.assertFilterByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                });

                sendAssertEvents(env,
                    new SupportBean[]{makeEvent("a", 1), makeEvent("b", 2)},
                    new SupportBean[]{makeEvent("x", 0), makeEvent("a", 0), makeEvent("a", 2), makeEvent("b", 1)}
                );
                env.undeployAll();
            }
        }
    }

    private static SupportBean makeEvent(String theString, int intPrimitive) {
        return makeEvent(theString, intPrimitive, 0L);
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        return makeEvent(theString, intPrimitive, longPrimitive, 0d);
    }

    private static SupportBean_IntAlphabetic intEvent(int a) {
        return new SupportBean_IntAlphabetic(a);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b) {
        return new SupportBean_IntAlphabetic(a, b);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b, int c, int d) {
        return new SupportBean_IntAlphabetic(a, b, c, d);
    }

    private static SupportBean_StringAlphabetic stringEvent(String a, String b) {
        return new SupportBean_StringAlphabetic(a, b);
    }

    private static SupportBean_StringAlphabetic stringEvent(String a, String b, String c) {
        return new SupportBean_StringAlphabetic(a, b, c);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b, int c) {
        return new SupportBean_IntAlphabetic(a, b, c);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b, int c, int d, int e) {
        return new SupportBean_IntAlphabetic(a, b, c, d, e);
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        event.setDoublePrimitive(doublePrimitive);
        return event;
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive,
                                         boolean boolPrimitive, int intBoxed, long longBoxed, double doubleBoxed) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        event.setDoublePrimitive(doublePrimitive);
        event.setBoolPrimitive(boolPrimitive);
        event.setLongBoxed(longBoxed);
        event.setDoubleBoxed(doubleBoxed);
        event.setIntBoxed(intBoxed);
        return event;
    }
}
