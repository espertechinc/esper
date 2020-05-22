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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.filter.*;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.common.internal.filterspec.FilterOperator.EQUAL;
import static com.espertech.esper.common.internal.filterspec.FilterOperator.REBOOL;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterOptimizableHelper.hasFilterIndexPlanAdvanced;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterPlanHook.*;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterServiceHelper.*;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static org.junit.Assert.*;

public class ExprFilterOptimizableConditionNegateConfirm {
    private final static String HOOK = "@Hook(type=HookType.INTERNAL_FILTERSPEC, hook='" + SupportFilterPlanHook.class.getName() + "')";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterAndOrUnwinding());
        executions.add(new ExprFilterOnePathNegate1Eq2WithDataflow());
        executions.add(new ExprFilterOnePathNegate1Eq2WithStage());
        executions.add(new ExprFilterOnePathNegate1Eq2WithContextFilter());
        executions.add(new ExprFilterOnePathNegate1Eq2WithContextCategory());
        executions.add(new ExprFilterOnePathOrLeftLRightV());
        executions.add(new ExprFilterOnePathOrLeftLRightVWithPattern());
        executions.add(new ExprFilterOnePathAndLeftLRightV());
        executions.add(new ExprFilterOnePathAndLeftLRightVWithPattern());
        executions.add(new ExprFilterOnePathAndLeftLOrVRightLOrV());
        executions.add(new ExprFilterOnePathOrLeftVRightAndWithLL());
        executions.add(new ExprFilterOnePathOrWithLVV());
        executions.add(new ExprFilterOnePathAndWithOrLVVOrLVOrLV());
        executions.add(new ExprFilterTwoPathOrWithLLV());
        executions.add(new ExprFilterTwoPathOrLeftLRightAndLWithV());
        executions.add(new ExprFilterTwoPathOrLeftOrLVRightOrLV());
        executions.add(new ExprFilterTwoPathAndLeftOrLLRightV());
        executions.add(new ExprFilterTwoPathAndLeftOrLVRightOrLL());
        executions.add(new ExprFilterThreePathOrWithAndLVAndLVAndLV());
        executions.add(new ExprFilterFourPathAndWithOrLLOrLL());
        executions.add(new ExprFilterFourPathAndWithOrLLOrLLWithV());
        executions.add(new ExprFilterFourPathAndWithOrLLOrLLWithV());
        executions.add(new ExprFilterFourPathAndWithOrLLOrLLOrVV());
        executions.add(new ExprFilterTwoPathAndLeftOrLVVRightLL());
        executions.add(new ExprFilterSixPathAndLeftOrLLVRightOrLL());
        executions.add(new ExprFilterEightPathLeftOrLLVRightOrLLV());
        executions.add(new ExprFilterAnyPathCompileMore());
        return executions;
    }

    private static class ExprFilterAnyPathCompileMore implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            {
                String confirm = "context.s0.p00=\"x\" or context.s0.p01=\"y\"";
                SupportFilterPlanPath pathOne = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a", confirm), makeTriplet("p11", EQUAL, "c"));
                SupportFilterPlanPath pathTwo = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a", confirm), makeTriplet("p12", EQUAL, "d"));
                SupportFilterPlan plan = new SupportFilterPlan(pathOne, pathTwo);
                runAssertion(env, plan, advanced, "(p10='a' or context.s0.p00='x' or context.s0.p01='y') and (p11='c' or p12='d')");
            }
        }

        private void runAssertion(RegressionEnvironment env, SupportFilterPlan plan, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S2;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compile(epl);
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", plan);
            }
        }
    }

    private static class ExprFilterTwoPathAndLeftOrLVVRightLL implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or context.s0.p00='x' or context.s0.p00='y') and (p11='b' or p12='c')");
            runAssertion(env, milestone, advanced, "('c'=p12 or p11='b') and (context.s0.p00='x' or context.s0.p00='y' or 'a'=p10)");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S2;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");

            String pathWhenXOrY = "context.s0.p00=\"x\" or context.s0.p00=\"y\"";
            SupportFilterPlanPath pathOne = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a", pathWhenXOrY), makeTriplet("p11", EQUAL, "b"));
            SupportFilterPlanPath pathTwo = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a", pathWhenXOrY), makeTriplet("p12", EQUAL, "c"));
            SupportFilterPlan plan = new SupportFilterPlan(pathOne, pathTwo);
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", plan);
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p11", EQUAL)},
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p12", EQUAL)},
                });
            }
            sendS1Assert(env, 10, "a", "-", "-", false);
            sendS1Assert(env, 11, "a", "-", "c", true);
            sendS1Assert(env, 12, "a", "b", "-", true);
            sendS1Assert(env, 13, "-", "b", "c", false);
            env.sendEventBean(new SupportBean_S2(1));

            env.sendEventBean(new SupportBean_S0(2, "x"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p11", EQUAL)},
                    new FilterItem[]{new FilterItem("p12", EQUAL)}
                });
            }
            sendS1Assert(env, 20, "-", "-", "-", false);
            sendS1Assert(env, 21, "-", "-", "c", true);
            sendS1Assert(env, 22, "-", "b", "-", true);
            sendS1Assert(env, 23, "-", "b", "c", true);
            env.sendEventBean(new SupportBean_S2(2));

            env.undeployAll();
        }
    }

    private static class ExprFilterEightPathLeftOrLLVRightOrLLV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or p11='b' or context.s0.p00='x') and (p12='c' or p13='d' or context.s0.p00='y')");
            runAssertion(env, milestone, advanced, "(p11='b' or context.s0.p00='x' or p10='a') and (context.s0.p00='y' or p12='c' or p13='d')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S2;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");

            String whenNotXAndNotY = "not context.s0.p00=\"x\" and not context.s0.p00=\"y\"";
            String whenYAndNotX = "context.s0.p00=\"y\" and not context.s0.p00=\"x\"";
            String whenXAndNotY = "context.s0.p00=\"x\" and not context.s0.p00=\"y\"";
            String confirm = "context.s0.p00=\"x\" and context.s0.p00=\"y\"";
            SupportFilterPlanPath pathOne = new SupportFilterPlanPath(whenNotXAndNotY, makeTriplet("p10", EQUAL, "a"), makeTriplet("p12", EQUAL, "c"));
            SupportFilterPlanPath pathTwo = new SupportFilterPlanPath(whenNotXAndNotY, makeTriplet("p10", EQUAL, "a"), makeTriplet("p13", EQUAL, "d"));
            SupportFilterPlanPath pathThree = new SupportFilterPlanPath(whenNotXAndNotY, makeTriplet("p11", EQUAL, "b"), makeTriplet("p12", EQUAL, "c"));
            SupportFilterPlanPath pathFour = new SupportFilterPlanPath(whenNotXAndNotY, makeTriplet("p11", EQUAL, "b"), makeTriplet("p13", EQUAL, "d"));
            SupportFilterPlanPath pathFive = new SupportFilterPlanPath(whenYAndNotX, makeTriplet("p10", EQUAL, "a"));
            SupportFilterPlanPath pathSix = new SupportFilterPlanPath(whenYAndNotX, makeTriplet("p11", EQUAL, "b"));
            SupportFilterPlanPath pathSeven = new SupportFilterPlanPath(whenXAndNotY, makeTriplet("p12", EQUAL, "c"));
            SupportFilterPlanPath pathEight = new SupportFilterPlanPath(whenXAndNotY, makeTriplet("p13", EQUAL, "d"));
            SupportFilterPlan plan = new SupportFilterPlan(confirm, null, pathOne, pathTwo, pathThree, pathFour, pathFive, pathSix, pathSeven, pathEight);
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", plan);
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p12", EQUAL)},
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p13", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p12", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p13", EQUAL)}
                });
            }
            sendS1Assert(env, 10, "a", "b", "-", "-", false);
            sendS1Assert(env, 11, "-", "-", "c", "d", false);
            sendS1Assert(env, 12, "a", "-", "c", "-", true);
            sendS1Assert(env, 13, "-", "b", "c", "-", true);
            sendS1Assert(env, 14, "a", "-", "-", "d", true);
            sendS1Assert(env, 15, "-", "b", "-", "d", true);
            env.sendEventBean(new SupportBean_S2(1));

            env.sendEventBean(new SupportBean_S0(2, "x"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p12", EQUAL)},
                    new FilterItem[]{new FilterItem("p13", EQUAL)}
                });
            }
            sendS1Assert(env, 20, "-", "-", "-", "-", false);
            sendS1Assert(env, 21, "-", "-", "c", "-", true);
            sendS1Assert(env, 22, "-", "-", "-", "d", true);
            env.sendEventBean(new SupportBean_S2(2));

            env.sendEventBean(new SupportBean_S0(3, "y"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL)}
                });
            }
            sendS1Assert(env, 30, "-", "-", "-", "-", false);
            sendS1Assert(env, 31, "a", "-", "-", "-", true);
            sendS1Assert(env, 32, "-", "b", "-", "-", true);
            env.sendEventBean(new SupportBean_S2(3));

            env.undeployAll();
        }
    }

    private static class ExprFilterSixPathAndLeftOrLLVRightOrLL implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or p11='b' or context.s0.p00='x') and (p12='c' or p13='d')");
            runAssertion(env, milestone, advanced, "(p13='d' or 'c'=p12) and (context.s0.p00='x' or p11='b' or p10='a')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S2;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");

            String pathWhenX = "context.s0.p00=\"x\"";
            String pathWhenNotX = "not " + pathWhenX;
            SupportFilterPlanPath pathOne = new SupportFilterPlanPath(pathWhenNotX, makeTriplet("p10", EQUAL, "a"), makeTriplet("p12", EQUAL, "c"));
            SupportFilterPlanPath pathTwo = new SupportFilterPlanPath(pathWhenNotX, makeTriplet("p10", EQUAL, "a"), makeTriplet("p13", EQUAL, "d"));
            SupportFilterPlanPath pathThree = new SupportFilterPlanPath(pathWhenNotX, makeTriplet("p11", EQUAL, "b"), makeTriplet("p12", EQUAL, "c"));
            SupportFilterPlanPath pathFour = new SupportFilterPlanPath(pathWhenNotX, makeTriplet("p11", EQUAL, "b"), makeTriplet("p13", EQUAL, "d"));
            SupportFilterPlanPath pathFive = new SupportFilterPlanPath(pathWhenX, makeTriplet("p12", EQUAL, "c"));
            SupportFilterPlanPath pathSix = new SupportFilterPlanPath(pathWhenX, makeTriplet("p13", EQUAL, "d"));
            SupportFilterPlan plan = new SupportFilterPlan(pathOne, pathTwo, pathThree, pathFour, pathFive, pathSix);
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", plan);
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p12", EQUAL)},
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p13", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p12", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p13", EQUAL)}
                });
            }
            sendS1Assert(env, 10, "a", "b", "-", "-", false);
            sendS1Assert(env, 11, "-", "-", "c", "d", false);
            sendS1Assert(env, 12, "a", "-", "c", "-", true);
            sendS1Assert(env, 13, "-", "b", "c", "-", true);
            sendS1Assert(env, 14, "a", "-", "-", "d", true);
            sendS1Assert(env, 15, "-", "b", "-", "d", true);
            env.sendEventBean(new SupportBean_S2(1));

            env.sendEventBean(new SupportBean_S0(2, "x"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p12", EQUAL)},
                    new FilterItem[]{new FilterItem("p13", EQUAL)}
                });
            }
            sendS1Assert(env, 20, "-", "-", "-", "-", false);
            sendS1Assert(env, 21, "-", "-", "c", "-", true);
            sendS1Assert(env, 22, "-", "-", "-", "d", true);
            env.sendEventBean(new SupportBean_S2(2));

            env.undeployAll();
        }
    }

    private static class ExprFilterTwoPathAndLeftOrLVRightOrLL implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or context.s0.p00='x') and (p11='c' or p12='d')");
            runAssertion(env, milestone, advanced, "(p12='d' or p11='c') and (context.s0.p00='x' or p10='a')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S2;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");

            SupportFilterPlanPath pathOne = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a", "context.s0.p00=\"x\""), makeTriplet("p11", EQUAL, "c"));
            SupportFilterPlanPath pathTwo = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a", "context.s0.p00=\"x\""), makeTriplet("p12", EQUAL, "d"));
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(pathOne, pathTwo));
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p11", EQUAL)},
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p12", EQUAL)}
                });
            }
            sendS1Assert(env, 10, "a", "c", "-", true);
            sendS1Assert(env, 11, "a", "-", "d", true);
            sendS1Assert(env, 12, "a", "c", "d", true);
            sendS1Assert(env, 13, "-", "c", "d", false);
            env.sendEventBean(new SupportBean_S2(1));

            env.sendEventBean(new SupportBean_S0(2, "x"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p11", EQUAL)},
                    new FilterItem[]{new FilterItem("p12", EQUAL)}
                });
            }
            sendS1Assert(env, 20, "-", "c", "-", true);
            sendS1Assert(env, 21, "-", "-", "d", true);
            sendS1Assert(env, 22, "-", "-", "-", false);
            env.sendEventBean(new SupportBean_S2(1));

            env.undeployAll();
        }
    }

    private static class ExprFilterFourPathAndWithOrLLOrLLOrVV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or p11='b') and (p12='c' or p13='d') and (s0.p00='x' or s0.p00='y')");
            runAssertion(env, milestone, advanced, "(s0.p00='x' or s0.p00='y') and ('d'=p13 or 'c'=p12) and ('b'=p11 or 'a'=p10)");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> SupportBean_S1(" + filter + ")];\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(null, "s0.p00=\"x\" or s0.p00=\"y\"", makeABCDCombinationPath()));
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "x"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", makeABCDCombinationFilterItems());
            }
            sendS1Assert(env, 10, "-", "-", "-", "-", false);
            sendS1Assert(env, 11, "a", "-", "c", "-", true);

            env.sendEventBean(new SupportBean_S0(2, "y"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", makeABCDCombinationFilterItems());
            }
            sendS1Assert(env, 20, "-", "b", "c", "-", true);

            env.sendEventBean(new SupportBean_S0(3, "-"));
            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 30, "a", "-", "c", "-", false);

            env.undeployAll();
        }
    }

    private static class ExprFilterFourPathAndWithOrLLOrLLWithV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or p11='b') and (p12='c' or p13='d') and s0.p00='x'");
            runAssertion(env, milestone, advanced, "s0.p00='x' and ('d'=p13 or 'c'=p12) and ('b'=p11 or 'a'=p10)");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> SupportBean_S1(" + filter + ")];\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(null, "s0.p00=\"x\"", makeABCDCombinationPath()));
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "x"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", makeABCDCombinationFilterItems());
            }
            sendS1Assert(env, 10, "-", "-", "-", "-", false);
            sendS1Assert(env, 11, "a", "-", "c", "-", true);

            env.sendEventBean(new SupportBean_S0(2, "-"));
            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 20, "a", "-", "c", "-", false);

            env.undeployAll();
        }
    }

    private static class ExprFilterFourPathAndWithOrLLOrLL implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or p11='b') and (p12='c' or p13='d')");
            runAssertion(env, milestone, advanced, "('d'=p13 or 'c'=p12) and ('b'=p11 or 'a'=p10)");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(makeABCDCombinationPath()));
            }
            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", makeABCDCombinationFilterItems());
            }
            sendS1Assert(env, 10, "-", "-", "-", "-", false);
            sendS1Assert(env, 11, "a", "-", "c", "-", true);
            sendS1Assert(env, 12, "a", "-", "-", "d", true);
            sendS1Assert(env, 13, "-", "b", "c", "-", true);
            sendS1Assert(env, 14, "-", "b", "-", "d", true);
            sendS1Assert(env, 15, "a", "b", "-", "-", false);
            sendS1Assert(env, 16, "-", "-", "c", "d", false);

            env.undeployAll();
        }
    }

    private static class ExprFilterThreePathOrWithAndLVAndLVAndLV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10 = 'a' and s0.p00 like '%1%') or (p10 = 'b' and s0.p00 like '%2%') or (p10 = 'c' and s0.p00 like '%3%')");
            runAssertion(env, milestone, advanced, "(s0.p00 like '%2%' and p10 = 'b') or (s0.p00 like '%3%' and 'c' = p10) or (p10 = 'a' and s0.p00 like '%1%')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> SupportBean_S1(" + filter + ")];\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanPath pathOne = new SupportFilterPlanPath("s0.p00 like \"%1%\"", makeTriplet("p10", EQUAL, "a"));
            SupportFilterPlanPath pathTwo = new SupportFilterPlanPath("s0.p00 like \"%2%\"", makeTriplet("p10", EQUAL, "b"));
            SupportFilterPlanPath pathThree = new SupportFilterPlanPath("s0.p00 like \"%3%\"", makeTriplet("p10", EQUAL, "c"));
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(pathOne, pathTwo, pathThree));
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 10, "a", false);
            sendS1Assert(env, 11, "b", false);
            sendS1Assert(env, 12, "c", false);

            env.sendEventBean(new SupportBean_S0(2, "1"));
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean_S1", new FilterItem("p10", EQUAL));
            }
            sendS1Assert(env, 20, "c", false);
            sendS1Assert(env, 21, "b", false);
            sendS1Assert(env, 22, "a", true);

            env.sendEventBean(new SupportBean_S0(3, "2"));
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean_S1", new FilterItem("p10", EQUAL));
            }
            sendS1Assert(env, 30, "a", false);
            sendS1Assert(env, 31, "c", false);
            sendS1Assert(env, 32, "b", true);

            env.sendEventBean(new SupportBean_S0(4, "3"));
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean_S1", new FilterItem("p10", EQUAL));
            }
            sendS1Assert(env, 40, "a", false);
            sendS1Assert(env, 41, "b", false);
            sendS1Assert(env, 42, "c", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathAndWithOrLVVOrLVOrLV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10 = 'a' or s0.p00 like '%1%' or s0.p00 like '%2%') and " +
                "(p11 = 'b' or s0.p00 like '%3%') and (p12 = 'c' or s0.p00 like '%4%')");
            runAssertion(env, milestone, advanced, "('c' = p12 or s0.p00 like '%4%') and" +
                "(s0.p00 like '%3%' or p11 = 'b') and" +
                "(s0.p00 like '%1%' or p10 = 'a' or s0.p00 like '%2%')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> SupportBean_S1(" + filter + ")];\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanTriplet tripletOne = makeTriplet("p10", EQUAL, "a", "s0.p00 like \"%1%\" or s0.p00 like \"%2%\"");
            SupportFilterPlanTriplet tripletTwo = makeTriplet("p11", EQUAL, "b", "s0.p00 like \"%3%\"");
            SupportFilterPlanTriplet tripletThree = makeTriplet("p12", EQUAL, "c", "s0.p00 like \"%4%\"");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(new SupportFilterPlanPath(tripletOne, tripletTwo, tripletThree)));
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p11", EQUAL), new FilterItem("p12", EQUAL)}
                });
            }
            sendS1Assert(env, 10, "a", "b", "-", false);
            sendS1Assert(env, 11, "-", "b", "c", false);
            sendS1Assert(env, 12, "a", "b", "c", true);

            env.sendEventBean(new SupportBean_S0(2, "1"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p12", EQUAL)}
                });
            }
            sendS1Assert(env, 20, "-", "b", "-", false);
            sendS1Assert(env, 21, "-", "-", "c", false);
            sendS1Assert(env, 22, "-", "b", "c", true);

            env.sendEventBean(new SupportBean_S0(3, "2"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p12", EQUAL)}
                });
            }
            sendS1Assert(env, 30, "-", "b", "-", false);
            sendS1Assert(env, 31, "-", "-", "c", false);
            sendS1Assert(env, 32, "-", "b", "c", true);

            env.sendEventBean(new SupportBean_S0(4, "3"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p12", EQUAL)}
                });
            }
            sendS1Assert(env, 40, "a", "-", "-", false);
            sendS1Assert(env, 41, "-", "-", "c", false);
            sendS1Assert(env, 42, "a", "-", "c", true);

            env.sendEventBean(new SupportBean_S0(5, "4"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p11", EQUAL)}
                });
            }
            sendS1Assert(env, 50, "a", "-", "-", false);
            sendS1Assert(env, 51, "-", "b", "-", false);
            sendS1Assert(env, 52, "a", "b", "-", true);

            env.sendEventBean(new SupportBean_S0(6, "1234"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 60, "-", "-", "-", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterTwoPathAndLeftOrLLRightV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10 = 'a' or p11 = 'b') and context.s0.p00 = 'x'");
            runAssertion(env, milestone, advanced, "context.s0.p00 = 'x' and (p10 = 'a' or p11 = 'b')");
            runAssertion(env, milestone, advanced, "(p10 = 'a' or p11 = 'b') and context.s0.p00 = 'x'");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S2;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanTriplet tripletOne = makeTriplet("p10", EQUAL, "a");
            SupportFilterPlanTriplet tripletTwo = makeTriplet("p11", EQUAL, "b");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(null, "context.s0.p00=\"x\"", new SupportFilterPlanPath(tripletOne), new SupportFilterPlanPath(tripletTwo)));
            }
            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 10, "a", "b", false);
            env.sendEventBean(new SupportBean_S2(1));

            env.sendEventBean(new SupportBean_S0(2, "x"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL)}
                });
            }
            sendS1Assert(env, 20, "-", "-", false);
            sendS1Assert(env, 21, "a", "-", true);
            sendS1Assert(env, 22, "-", "b", true);
            env.sendEventBean(new SupportBean_S2(2));

            env.undeployAll();
        }
    }

    private static class ExprFilterTwoPathOrLeftOrLVRightOrLV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10 regexp '.*a.*' or context.s0.p00 = 'x') or (p11 regexp '.*b.*' or context.s0.p01 = 'y')");
            runAssertion(env, milestone, advanced, "context.s0.p00 = 'x' or context.s0.p01 = 'y' or p10 regexp '.*a.*' or p11 regexp '.*b.*'");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S2;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean_S1(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanTriplet tripletOne = makeTripletRebool(".p10 regexp ?", "\".*a.*\"");
            SupportFilterPlanTriplet tripletTwo = makeTripletRebool(".p11 regexp ?", "\".*b.*\"");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan("context.s0.p00=\"x\" or context.s0.p01=\"y\"", null, new SupportFilterPlanPath(tripletOne), new SupportFilterPlanPath(tripletTwo)));
            }

            env.sendEventBean(new SupportBean_S0(1, "-", "-"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem(".p10 regexp ?", REBOOL)},
                    new FilterItem[]{new FilterItem(".p11 regexp ?", REBOOL)}
                });
            }
            env.milestoneInc(milestone);
            sendS1Assert(env, 10, "-", "-", false);
            sendS1Assert(env, 11, "-", "globe", true);
            sendS1Assert(env, 12, "garden", "-", true);
            sendS1Assert(env, 13, "globe", "garden", false);
            env.sendEventBean(new SupportBean_S2(1));

            env.sendEventBean(new SupportBean_S0(2, "x", "-"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 20, "-", "-", true);
            env.sendEventBean(new SupportBean_S2(2));

            env.sendEventBean(new SupportBean_S0(3, "-", "y"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 30, "-", "-", true);
            env.sendEventBean(new SupportBean_S2(2));

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathOrLeftVRightAndWithLL implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "s0.p00='x' or (p10 = 'a' and p11 regexp '.*b.*')");
            runAssertion(env, milestone, advanced, "(p10 = 'a' and p11 regexp '.*b.*') or (s0.p00='x')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> " +
                "SupportBean_S1(" + filter + ")]";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanTriplet tripletOne = makeTriplet("p10", EQUAL, "a");
            SupportFilterPlanTriplet tripletTwo = makeTripletRebool(".p11 regexp ?", "\".*b.*\"");
            SupportFilterPlanPath path = new SupportFilterPlanPath(tripletOne, tripletTwo);
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan("s0.p00=\"x\"", null, path));
            }

            env.sendEventBean(new SupportBean_S0(1, "-"));
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem(".p11 regexp ?", REBOOL)}
                });
            }
            sendS1Assert(env, 10, "-", "b", false);
            sendS1Assert(env, 11, "a", "-", false);
            env.milestoneInc(milestone);
            sendS1Assert(env, 12, "a", "globe", true);

            env.sendEventBean(new SupportBean_S0(2, "x"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 20, "-", "-", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterAndOrUnwinding implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "a=1 or (b=2 or c=3)", "a=1 or b=2 or c=3");
            runAssertion(env, "a=1 and (b=2 and c=3)", "a=1 and b=2 and c=3");

            runAssertion(env, "a=1 or (b=2 or (c=3 or d=4))", "a=1 or b=2 or c=3 or d=4");
            runAssertion(env, "a=1 or (b=2 or (c=3 or (d=4 or e=5)))", "a=1 or b=2 or c=3 or d=4 or e=5");
            runAssertion(env, "a=1 or (b=2 or (c=3 or (d=4 or (e=5 or f=6))))", "a=1 or b=2 or c=3 or d=4 or e=5 or f=6");
            runAssertion(env, "a=1 or (b=2 or (c=3 or (d=4 or (e=5 or f=6 or g=7))))", "a=1 or b=2 or c=3 or d=4 or e=5 or f=6 or g=7");
            runAssertion(env, "(((a=1 or b=2) or c=3) or d=4 or e=5) or f=6 or g=7", "a=1 or b=2 or c=3 or d=4 or e=5 or f=6 or g=7");

            runAssertion(env, "a=1 and (b=2 and (c=3 and d=4))", "a=1 and b=2 and c=3 and d=4");
            runAssertion(env, "a=1 and (b=2 and (c=3 and (d=4 and e=5)))", "a=1 and b=2 and c=3 and d=4 and e=5");
            runAssertion(env, "a=1 and (b=2 and (c=3 and (d=4 and (e=5 and f=6))))", "a=1 and b=2 and c=3 and d=4 and e=5 and f=6");
            runAssertion(env, "a=1 and (b=2 and (c=3 and (d=4 and (e=5 and f=6))))", "a=1 and b=2 and c=3 and d=4 and e=5 and f=6");
            runAssertion(env, "a=1 and (b=2 and (c=3 and (d=4 and (e=5 and f=6 and g=7))))", "a=1 and b=2 and c=3 and d=4 and e=5 and f=6 and g=7");
            runAssertion(env, "(((a=1 and b=2) and c=3) and d=4 and e=5) and f=6 and g=7", "a=1 and b=2 and c=3 and d=4 and e=5 and f=6 and g=7");

            runAssertion(env, "(a=1 and (b=2 and c=3)) or (d=4 or (e=5 or f=6))", "(a=1 and b=2 and c=3) or d=4 or e=5 or f=6");
            runAssertion(env, "((a=1 or b=2) or (c=3)) and (d=5 and e=6)", "(a=1 or b=2 or c=3) and d=5 and e=6");
            runAssertion(env, "a=1 or b=2 and c=3 or d=4 and e=5", "a=1 or (b=2 and c=3) or (d=4 and e=5)");
            runAssertion(env, "((a=1 and b=2 and c=3 and d=4) and e=5) or (f=6 or (g=7 or h=8 or i=9))", "(a=1 and b=2 and c=3 and d=4 and e=5) or f=6 or g=7 or h=8 or i=9");
        }

        private void runAssertion(RegressionEnvironment env, String filter, String expectedText) {
            String epl = HOOK + "@name('s0') select * from SupportBeanSimpleNumber(" + filter + ")";
            SupportFilterPlanHook.reset();
            env.compile(epl);
            SupportFilterPlanEntry plan = SupportFilterPlanHook.assertPlanSingleAndReset();
            ExprNode receivedNode = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(plan.getPlanNodes());

            EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured("SupportBeanSimpleNumber");
            EventType[] typesPerStream = new EventType[]{eventType};
            String[] typeAliases = new String[]{"sbsn"};
            ExprNode expectedNode = ((EPRuntimeSPI) env.runtime()).getReflectiveCompileSvc().reflectiveCompileExpression(expectedText, typesPerStream, typeAliases);

            assertTrue(ExprNodeUtilityCompare.deepEquals(expectedNode, receivedNode, true));
        }
    }

    private static class ExprFilterOnePathAndLeftLOrVRightLOrV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "(p10='a' or s0.p00='x') and (p11='b' or s0.p01='y')");
            runAssertion(env, milestone, advanced, "(s0.p01='y' or p11='b') and (s0.p00='x' or p10='a')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK  + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> " +
                "SupportBean_S1(" + filter + ")]";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanTriplet tripletOne = makeTriplet("p10", EQUAL, "a", "s0.p00=\"x\"");
            SupportFilterPlanTriplet tripletTwo = makeTriplet("p11", EQUAL, "b", "s0.p01=\"y\"");
            SupportFilterPlanPath path = new SupportFilterPlanPath(tripletOne, tripletTwo);
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(path));
            }

            env.sendEventBean(new SupportBean_S0(1, "-", "-"));
            env.milestoneInc(milestone);
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p11", EQUAL)}
                });
            }
            sendS1Assert(env, 10, "-", "b", false);
            sendS1Assert(env, 11, "a", "-", false);
            sendS1Assert(env, 12, "a", "b", true);

            env.sendEventBean(new SupportBean_S0(2, "x", "-"));
            env.milestoneInc(milestone);
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean_S1", new FilterItem("p11", EQUAL));
            }
            sendS1Assert(env, 21, "a", "-", false);
            sendS1Assert(env, 20, "-", "b", true);

            env.sendEventBean(new SupportBean_S0(2, "-", "y"));
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean_S1", new FilterItem("p10", EQUAL));
            }
            sendS1Assert(env, 30, "-", "b", false);
            sendS1Assert(env, 31, "a", "-", true);

            env.sendEventBean(new SupportBean_S0(2, "x", "y"));
            env.milestoneInc(milestone);
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 40, "-", "-", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterTwoPathOrLeftLRightAndLWithV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "p10='a' or (p11='b' and s0.p00='x')");
            runAssertion(env, milestone, advanced, "(s0.p00='x' and p11='b') or p10='a'");
            runAssertion(env, milestone, advanced, "(p11='b' and s0.p00='x') or p10='a'");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> " +
                "SupportBean_S1(" + filter + ")]";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanPath pathOne = makePathFromSingle("p10", EQUAL, "a");
            SupportFilterPlanPath pathTwo = new SupportFilterPlanPath("s0.p00=\"x\"", makeTriplet("p11", EQUAL, "b"));
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan(pathOne, pathTwo));
            }

            env.sendEventBean(new SupportBean_S0(1, "x"));
            env.milestoneInc(milestone);
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL)}
                });
            }
            sendS1Assert(env, 10, "-", "-", false);
            sendS1Assert(env, 11, "-", "b", true);

            env.sendEventBean(new SupportBean_S0(2, "x"));
            env.milestoneInc(milestone);
            sendS1Assert(env, 20, "-", "-", false);
            sendS1Assert(env, 21, "a", "-", true);

            env.sendEventBean(new SupportBean_S0(3, "-"));
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean_S1", new FilterItem("p10", EQUAL));
            }
            sendS1Assert(env, 30, "-", "b", false);
            sendS1Assert(env, 31, "a", "b", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterTwoPathOrWithLLV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "p10='a' or p11='b' or s0.p00='x'");
            runAssertion(env, milestone, advanced, "p11='b' or s0.p00='x' or p10='a'");
            runAssertion(env, milestone, advanced, "s0.p00='x' or p11='b' or p10='a'");
            runAssertion(env, milestone, advanced, "(s0.p00='x' or p11='b') or p10='a'");
            runAssertion(env, milestone, advanced, "s0.p00='x' or (p11='b' or p10='a')");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> " +
                "SupportBean_S1(" + filter + ")]";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanPath pathOne = makePathFromSingle("p10", EQUAL, "a");
            SupportFilterPlanPath pathTwo = makePathFromSingle("p11", EQUAL, "b");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan("s0.p00=\"x\"", null, pathOne, pathTwo));
            }

            env.sendEventBean(new SupportBean_S0(1, "x"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 10, "-", "-", true);

            env.sendEventBean(new SupportBean_S0(2, "y"));
            env.milestoneInc(milestone);
            if (advanced) {
                assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean_S1", new FilterItem[][]{
                    new FilterItem[]{new FilterItem("p10", EQUAL)},
                    new FilterItem[]{new FilterItem("p11", EQUAL)}
                });
            }
            sendS1Assert(env, 20, "-", "-", false);
            sendS1Assert(env, 21, "a", "-", true);

            env.sendEventBean(new SupportBean_S0(3, "y"));
            sendS1Assert(env, 30, "-", "-", false);
            sendS1Assert(env, 31, "-", "b", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathOrWithLVV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, advanced, "p10='a' or s0.p00='x' or s0.p01='y'");
            runAssertion(env, advanced, "s0.p00='x' or p10='a' or s0.p01='y'");
            runAssertion(env, advanced, "s0.p00='x' or (s0.p01='y' or p10='a')");
        }

        private void runAssertion(RegressionEnvironment env, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> " +
                "SupportBean_S1(" + filter + ")]";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            SupportFilterPlanPath path = makePathFromSingle("p10", EQUAL, "a");
            if (advanced) {
                assertPlanSingleByType("SupportBean_S1", new SupportFilterPlan("s0.p00=\"x\" or s0.p01=\"y\"", null, path));
            }

            env.sendEventBean(new SupportBean_S0(1, "x", "-"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 10, "-", true);

            env.sendEventBean(new SupportBean_S0(2, "-", "y"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean_S1");
            }
            sendS1Assert(env, 20, "-", true);

            env.sendEventBean(new SupportBean_S0(3, "-", "-"));
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean_S1", new FilterItem("p10", EQUAL));
            }
            sendS1Assert(env, 30, "-", false);
            sendS1Assert(env, 31, "a", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathNegate1Eq2WithContextFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select * from SupportBean_S0;\n" +
                "create context MyContext start SupportBean_S0(1=2);\n" +
                "@name('s0') context MyContext select * from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            sendSBAssert(env, "E1", false);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathNegate1Eq2WithStage implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = HOOK + "@name('s0') select * from SupportBean(1=2)";
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingle(new SupportFilterPlan(null, "1=2", makePathsFromEmpty()));
                assertFilterSvcNone(env.statement("s0"), "SupportBean");
            }
            String deploymentId = env.deploymentId("s0");

            sendSBAssert(env, "E1", false);

            env.stageService().getStage("P1");
            stageIt(env, "P1", deploymentId);

            env.stageService().getStage("P1").getEventService().sendEventBean(new SupportBean("E1", 1), "SupportBean");
            assertFalse(env.listenerStage("P1", "s0").getIsInvokedAndReset());

            unstageIt(env, "P1", deploymentId);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathNegate1Eq2WithDataflow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "EventBusSource -> ReceivedStream<SupportBean> { filter : 1 = 2 } " +
                "DefaultSupportCaptureOp(ReceivedStream) {}");

            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            df.start();

            env.sendEventBean(new SupportBean());

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            assertEquals(0, future.getCurrent().length);

            df.cancel();
            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathNegate1Eq2WithContextCategory implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContext group by theString='abc' and 1=2 as categoryOne from SupportBean;\n" +
                "@name('s0') context MyContext select * from SupportBean;\n";
            EPCompiled compiled = env.compile(epl);
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            if (advanced) {
                SupportMessageAssertUtil.tryInvalidDeploy(env, compiled, "Failed to deploy: Category context 'MyContext' for category 'categoryOne' has evaluated to a condition that cannot become true");
            }
        }
    }

    private static class ExprFilterOnePathOrLeftLRightVWithPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "theString = 'abc' or (s0.p00 || s1.p10 || s2[0].p20 || s2[1].p20 = 'QRST')");
            runAssertion(env, milestone, advanced, "(s0.p00 || s1.p10 || s2[0].p20 || s2[1].p20 = 'QRST') or theString = 'abc'");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> s1=SupportBean_S1 -> [2] s2=SupportBean_S2 -> " +
                "SupportBean(" + filter + ")]";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingleByType("SupportBean", new SupportFilterPlan("s0.p00||s1.p10||s2[0].p20||s2[1].p20=\"QRST\"", null, makePathsFromSingle("theString", EQUAL, "abc")));
            }

            env.sendEventBean(new SupportBean_S0(1, "Q"));
            env.sendEventBean(new SupportBean_S1(2, "R"));
            env.sendEventBean(new SupportBean_S2(3, "S"));
            env.sendEventBean(new SupportBean_S2(4, "T"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean");
            }

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean");
            }
            sendSBAssert(env, "x", true);

            env.sendEventBean(new SupportBean_S0(11, "Q"));
            env.sendEventBean(new SupportBean_S1(12, "-"));
            env.sendEventBean(new SupportBean_S2(13, "-"));
            env.sendEventBean(new SupportBean_S2(14, "-"));
            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean", new FilterItem("theString", EQUAL));
            }

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean", new FilterItem("theString", EQUAL));
            }
            sendSBAssert(env, "x", false);
            sendSBAssert(env, "abc", true);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathAndLeftLRightVWithPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "theString = 'abc' and s0.p00 = 'x'");
            runAssertion(env, milestone, advanced, "s0.p00 = 'x' and theString = 'abc'");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = HOOK + "@name('s0') select * from pattern[every s0=SupportBean_S0 -> SupportBean(" + filter + ")];\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingleByType("SupportBean", new SupportFilterPlan(null, "s0.p00=\"x\"", makePathsFromSingle("theString", EQUAL, "abc")));
            }

            env.sendEventBean(new SupportBean_S0(1, "x"));

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcByTypeSingle(env.statement("s0"), "SupportBean", new FilterItem("theString", EQUAL));
            }
            sendSBAssert(env, "def", false);
            sendSBAssert(env, "abc", true);

            env.sendEventBean(new SupportBean_S0(2, "-"));
            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean");
            }

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean");
            }
            sendSBAssert(env, "abc", false);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathAndLeftLRightV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, advanced, "theString = 'abc' and context.s0.p00 = 'x'");
            runAssertion(env, milestone, advanced, "context.s0.p00 = 'x' and theString = 'abc'");
            runAssertion(env, milestone, advanced, "context.s0.p00 = 'x' and theString = 'abc'");
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, boolean advanced, String filter) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S1;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingle(new SupportFilterPlan(null, "context.s0.p00=\"x\"", makePathsFromSingle("theString", EQUAL, "abc")));
            }

            env.sendEventBean(new SupportBean_S0(1, "x"));
            if (advanced) {
                assertFilterSvcSingle(env.statement("s0"), "theString", EQUAL);
            }
            sendSBAssert(env, "abc", true);
            sendSBAssert(env, "def", false);

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcSingle(env.statement("s0"), "theString", EQUAL);
            }
            sendSBAssert(env, "abc", true);
            sendSBAssert(env, "def", false);
            env.sendEventBean(new SupportBean_S1(1));
            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean");
            }

            env.sendEventBean(new SupportBean_S0(2, "-"));
            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean");
            }
            sendSBAssert(env, "abc", false);
            sendSBAssert(env, "def", false);

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcNone(env.statement("s0"), "SupportBean");
            }
            sendSBAssert(env, "abc", false);
            sendSBAssert(env, "def", false);

            env.undeployAll();
        }
    }

    private static class ExprFilterOnePathOrLeftLRightV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            boolean advanced = hasFilterIndexPlanAdvanced(env);
            runAssertion(env, milestone, "theString = 'abc' or context.s0.p00 = 'x'", advanced);
            runAssertion(env, milestone, "context.s0.p00 = 'x' or theString = 'abc'", advanced);
            runAssertion(env, milestone, "context.s0.p00 = 'x' or theString = 'abc'", advanced);
        }

        private void runAssertion(RegressionEnvironment env, AtomicInteger milestone, String filter, boolean advanced) {
            String epl = "create context MyContext start SupportBean_S0 as s0 end SupportBean_S1;\n" +
                HOOK + "@name('s0') context MyContext select * from SupportBean(" + filter + ");\n";
            SupportFilterPlanHook.reset();
            env.compileDeploy(epl).addListener("s0");
            if (advanced) {
                assertPlanSingle(new SupportFilterPlan("context.s0.p00=\"x\"", null, makePathsFromSingle("theString", EQUAL, "abc")));
            }

            env.sendEventBean(new SupportBean_S0(1, "x"));
            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean");
            }
            sendSBAssert(env, "abc", true);
            sendSBAssert(env, "def", true);

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcEmpty(env.statement("s0"), "SupportBean");
            }
            sendSBAssert(env, "abc", true);
            sendSBAssert(env, "def", true);
            env.sendEventBean(new SupportBean_S1(1));

            env.sendEventBean(new SupportBean_S0(2, "-"));
            if (advanced) {
                assertFilterSvcSingle(env.statement("s0"), "theString", EQUAL);
            }
            sendSBAssert(env, "abc", true);
            sendSBAssert(env, "def", false);

            env.milestoneInc(milestone);

            if (advanced) {
                assertFilterSvcSingle(env.statement("s0"), "theString", EQUAL);
            }
            sendSBAssert(env, "abc", true);
            sendSBAssert(env, "def", false);

            env.undeployAll();
        }
    }

    private static SupportFilterPlanPath[] makeABCDCombinationPath() {
        SupportFilterPlanPath pathOne = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a"), makeTriplet("p12", EQUAL, "c"));
        SupportFilterPlanPath pathTwo = new SupportFilterPlanPath(makeTriplet("p10", EQUAL, "a"), makeTriplet("p13", EQUAL, "d"));
        SupportFilterPlanPath pathThree = new SupportFilterPlanPath(makeTriplet("p11", EQUAL, "b"), makeTriplet("p12", EQUAL, "c"));
        SupportFilterPlanPath pathFour = new SupportFilterPlanPath(makeTriplet("p11", EQUAL, "b"), makeTriplet("p13", EQUAL, "d"));
        return new SupportFilterPlanPath[]{pathOne, pathTwo, pathThree, pathFour};
    }

    private static FilterItem[][] makeABCDCombinationFilterItems() {
        return new FilterItem[][]{
            new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p12", EQUAL)},
            new FilterItem[]{new FilterItem("p10", EQUAL), new FilterItem("p13", EQUAL)},
            new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p12", EQUAL)},
            new FilterItem[]{new FilterItem("p11", EQUAL), new FilterItem("p13", EQUAL)}
        };
    }

    private static void sendSBAssert(RegressionEnvironment env, String theString, boolean received) {
        env.sendEventBean(new SupportBean(theString, 0));
        assertEquals(received, env.listener("s0").getIsInvokedAndReset());
    }

    private static void sendS1Assert(RegressionEnvironment env, int id, String p10, String p11, String p12, String p13, boolean expected) {
        env.sendEventBean(new SupportBean_S1(id, p10, p11, p12, p13));
        assertEquals(expected, env.listener("s0").getIsInvokedAndReset());
    }

    private static void sendS1Assert(RegressionEnvironment env, int id, String p10, String p11, String p12, boolean expected) {
        sendS1Assert(env, id, p10, p11, p12, null, expected);
    }

    private static void sendS1Assert(RegressionEnvironment env, int id, String p10, String p11, boolean expected) {
        sendS1Assert(env, id, p10, p11, null, expected);
    }

    private static void sendS1Assert(RegressionEnvironment env, int id, String p10, boolean expected) {
        sendS1Assert(env, id, p10, null, expected);
    }
}
