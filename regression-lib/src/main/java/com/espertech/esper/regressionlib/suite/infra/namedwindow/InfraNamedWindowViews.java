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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.bean.SupportOverrideOneA;
import com.espertech.esper.regressionlib.support.bean.SupportVariableSetEvent;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidDeploy;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidFAFCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static junit.framework.TestCase.assertTrue;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowViews {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraKeepAllSimple());
        execs.add(new InfraKeepAllSceneTwo());
        execs.add(new InfraBeanBacked());
        execs.add(new InfraTimeWindow());
        execs.add(new InfraTimeWindowSceneTwo());
        execs.add(new InfraTimeFirstWindow());
        execs.add(new InfraExtTimeWindow());
        execs.add(new InfraExtTimeWindowSceneTwo());
        execs.add(new InfraExtTimeWindowSceneThree());
        execs.add(new InfraTimeOrderWindow());
        execs.add(new InfraTimeOrderSceneTwo());
        execs.add(new InfraLengthWindow());
        execs.add(new InfraLengthWindowSceneTwo());
        execs.add(new InfraLengthFirstWindow());
        execs.add(new InfraTimeAccum());
        execs.add(new InfraTimeAccumSceneTwo());
        execs.add(new InfraTimeBatch());
        execs.add(new InfraTimeBatchSceneTwo());
        execs.add(new InfraTimeBatchLateConsumer());
        execs.add(new InfraLengthBatch());
        execs.add(new InfraLengthBatchSceneTwo());
        execs.add(new InfraSortWindow());
        execs.add(new InfraSortWindowSceneTwo());
        execs.add(new InfraTimeLengthBatch());
        execs.add(new InfraTimeLengthBatchSceneTwo());
        execs.add(new InfraLengthWindowSceneThree());
        execs.add(new InfraLengthWindowPerGroup());
        execs.add(new InfraTimeBatchPerGroup());
        execs.add(new InfraDoubleInsertSameWindow());
        execs.add(new InfraLastEvent());
        execs.add(new InfraLastEventSceneTwo());
        execs.add(new InfraFirstEvent());
        execs.add(new InfraUnique());
        execs.add(new InfraUniqueSceneTwo());
        execs.add(new InfraFirstUnique());
        execs.add(new InfraBeanContained());
        execs.add(new InfraIntersection());
        execs.add(new InfraBeanSchemaBacked());
        execs.add(new InfraDeepSupertypeInsert());
        execs.add(new InfraWithDeleteUseAs());
        execs.add(new InfraWithDeleteFirstAs());
        execs.add(new InfraWithDeleteSecondAs());
        execs.add(new InfraWithDeleteNoAs());
        execs.add(new InfraFilteringConsumer());
        execs.add(new InfraSelectGroupedViewLateStart());
        execs.add(new InfraFilteringConsumerLateStart());
        execs.add(new InfraInvalid());
        execs.add(new InfraNamedWindowInvalidAlreadyExists());
        execs.add(new InfraNamedWindowInvalidConsumerDataWindow());
        execs.add(new InfraPriorStats());
        execs.add(new InfraLateConsumer());
        execs.add(new InfraLateConsumerJoin());
        execs.add(new InfraPattern());
        execs.add(new InfraExternallyTimedBatch());
        execs.add(new InfraSelectStreamDotStarInsert());
        execs.add(new InfraSelectGroupedViewLateStartVariableIterate());
        execs.add(new InfraOnInsertPremptiveTwoWindow());
        execs.add(new InfraNamedWindowTimeToLiveDelete());
        return execs;
    }

    private static class InfraNamedWindowTimeToLiveDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('win') create window MyWindow#timetolive(current_timestamp() + longPrimitive) as SupportBean;\n" +
                    "on SupportBean merge MyWindow insert select *;\n" +
                    "on SupportBean_S0 delete from MyWindow where theString = p00;\n";
            env.advanceTime(0);
            env.compileDeploy(epl);

            sendSupportBeanLongPrim(env, "E1", 2000);
            sendSupportBeanLongPrim(env, "E2", 3000);
            sendSupportBeanLongPrim(env, "E3", 1000);
            sendSupportBeanLongPrim(env, "E4", 2000);
            assertIterate(env, "E1", "E2", "E3", "E4");

            env.advanceTime(500);

            sendS0(env, "E2");
            assertIterate(env, "E1", "E3", "E4");

            env.milestone(0);

            sendS0(env, "E1");
            assertIterate(env, "E3", "E4");

            env.milestone(1);

            env.advanceTime(1000);
            assertIterate(env, "E4");

            env.milestone(1);

            env.advanceTime(2000);
            assertIterate(env);

            env.undeployAll();
        }

        private void sendS0(RegressionEnvironment env, String p00) {
            env.sendEventBean(new SupportBean_S0(0, p00));
        }

        private void assertIterate(RegressionEnvironment env, String... values) {
            List<String> strings = new ArrayList<>();
            env.assertIterator("win", iterator -> {
                iterator.forEachRemaining(event -> strings.add(event.get("theString").toString()));
                EPAssertionUtil.assertEqualsAnyOrder(values, strings.toArray());
            });
        }
    }

    public static class InfraKeepAllSimple implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String[] fields = "theString".split(",");

            RegressionPath path = new RegressionPath();
            String eplCreate = "@Name('create') @public create window MyWindow.win:keepall() as SupportBean";
            env.compileDeploy(eplCreate, path).addListener("create");

            String eplInsert = "@Name('insert') insert into MyWindow select * from SupportBean";
            env.compileDeploy(eplInsert, path);

            env.milestone(0);

            sendSupportBean(env, "E1");
            env.assertPropsNew("create", fields, new Object[]{"E1"});

            env.milestone(1);

            sendSupportBean(env, "E2");
            env.assertPropsNew("create", fields, new Object[]{"E2"});

            env.undeployModuleContaining("insert");
            env.undeployModuleContaining("create");

            env.milestone(2);
        }
    }

    public static class InfraKeepAllSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.win:keepall() as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "@name('insert') insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // send event G1
            sendBeanInt(env, "G1", 10);
            env.assertPropsNew("create", fields, new Object[]{"G1", 10});

            env.milestone(0);

            // send event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G2", 20);
            env.assertPropsNew("create", fields, new Object[]{"G2", 20});

            env.milestone(1);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}, {"G2", 20}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 20});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});

            env.milestone(2);

            // send event G3
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G3", 30);
            env.assertPropsNew("create", fields, new Object[]{"G3", 30});

            env.milestone(3);

            // delete event G1
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}, {"G3", 30}});
            sendMarketBean(env, "G1");
            env.assertPropsOld("create", fields, new Object[]{"G1", 10});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}});

            env.milestone(4);

            // send event G4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}});
            sendBeanInt(env, "G4", 40);
            env.assertPropsNew("create", fields, new Object[]{"G4", 40});

            env.milestone(5);

            // send event G5
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}});
            sendBeanInt(env, "G5", 50);
            env.assertPropsNew("create", fields, new Object[]{"G5", 50});

            env.milestone(6);

            // send event G6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}, {"G5", 50}});
            sendBeanInt(env, "G6", 60);
            env.assertPropsNew("create", fields, new Object[]{"G6", 60});

            env.milestone(7);

            // delete event G6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}, {"G5", 50}, {"G6", 60}});
            sendMarketBean(env, "G6");
            env.assertPropsOld("create", fields, new Object[]{"G6", 60});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}, {"G5", 50}});

            // destroy all
            env.undeployModuleContaining("insert");
            env.undeployModuleContaining("delete");
            env.undeployModuleContaining("create");
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraSelectStreamDotStarInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(EventRepresentationChoice.OBJECTARRAY.getAnnotationText() + " create window MyNWWindowObjectArray#keepall (p0 int)", path);
            env.compileDeploy("insert into MyNWWindowObjectArray select intPrimitive as p0, sb.* as c0 from SupportBean as sb", path);
            env.undeployAll();
        }
    }

    private static class InfraBeanBacked implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionBeanBacked(env, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionBeanBacked(env, EventRepresentationChoice.MAP);
            tryAssertionBeanBacked(env, EventRepresentationChoice.DEFAULT);
            tryAssertionBeanBacked(env, EventRepresentationChoice.AVRO);
        }
    }

    private static class InfraBeanContained implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                if (!rep.isAvroOrJsonEvent()) {
                    tryAssertionBeanContained(env, rep);
                }
            }

            String epl = EventRepresentationChoice.AVRO.getAnnotationText() + " @name('create') create window MyWindowBC#keepall as (bean SupportBean_S0)";
            env.tryInvalidCompile(epl, "Property 'bean' type '" + SupportBean_S0.class.getName() + "' does not have a mapping to an Avro type ");
        }
    }

    private static class InfraIntersection implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("create window MyWindowINT#length(2)#unique(intPrimitive) as SupportBean;\n" +
                    "insert into MyWindowINT select * from SupportBean;\n" +
                    "@name('s0') select irstream * from MyWindowINT");

            String[] fields = "theString".split(",");
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E1"}}, null);

            env.sendEventBean(new SupportBean("E2", 2));
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{"E2"}}, null);

            env.sendEventBean(new SupportBean("E3", 2));
            env.assertListener("s0", listener -> EPAssertionUtil.assertPropsPerRowAnyOrder(listener.assertInvokedAndReset(), fields, new Object[][]{{"E3"}}, new Object[][]{{"E1"}, {"E2"}}));

            env.undeployAll();
        }
    }

    private static class InfraBeanSchemaBacked implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // Test create from schema
            String epl = "@public create schema ABC as " + SupportBean.class.getName() + ";\n" +
                    "@public create window MyWindowBSB#keepall as ABC;\n" +
                    "insert into MyWindowBSB select * from SupportBean;\n";
            env.compileDeploy(epl, path);

            env.sendEventBean(new SupportBean());
            assertEvent(env.compileExecuteFAF("select * from MyWindowBSB", path).getArray()[0], "MyWindowBSB");

            env.compileDeploy("@name('s0') select * from ABC", path).addListener("s0");

            env.sendEventBean(new SupportBean());
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class InfraDeepSupertypeInsert implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowDSI#keepall as select * from SupportOverrideBase;\n" +
                    "insert into MyWindowDSI select * from SupportOverrideOneA;\n";
            env.compileDeploy(epl);
            env.sendEventBean(new SupportOverrideOneA("1a", "1", "base"));
            env.assertIterator("create", iterator -> assertEquals("1a", iterator.next().get("val")));
            env.undeployAll();
        }
    }

    private static class InfraOnInsertPremptiveTwoWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema TypeOne(col1 int);\n";
            epl += "@public @buseventtype create schema TypeTwo(col2 int);\n";
            epl += "@public @buseventtype create schema TypeTrigger(trigger int);\n";
            epl += "create window WinOne#keepall as TypeOne;\n";
            epl += "create window WinTwo#keepall as TypeTwo;\n";

            epl += "@name('insert-window-one') insert into WinOne(col1) select intPrimitive from SupportBean;\n";

            epl += "@name('insert-otherstream') on TypeTrigger insert into OtherStream select col1 from WinOne;\n";
            epl += "@name('insert-window-two') on TypeTrigger insert into WinTwo(col2) select col1 from WinOne;\n";
            epl += "@name('s0') on OtherStream select col2 from WinTwo;\n";

            env.compileDeploy(epl, new RegressionPath()).addListener("s0");

            // populate WinOne
            env.sendEventBean(new SupportBean("E1", 9));

            // fire trigger
            if (EventRepresentationChoice.getEngineDefault(env.getConfiguration()).isObjectArrayEvent()) {
                env.eventService().getEventSender("TypeTrigger").sendEvent(new Object[0]);
            } else {
                env.eventService().getEventSender("TypeTrigger").sendEvent(new HashMap());
            }

            env.assertEqualsNew("s0", "col2", 9);

            env.undeployAll();
        }
    }

    private static class InfraWithDeleteUseAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryCreateWindow(env, "create window MyWindow#keepall as MySimpleKeyValueMap",
                    "on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key");
        }
    }

    private static class InfraWithDeleteFirstAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryCreateWindow(env, "create window MyWindow#keepall as select key, value from MySimpleKeyValueMap",
                    "on SupportMarketDataBean delete from MyWindow as s1 where symbol = s1.key");
        }
    }

    private static class InfraWithDeleteSecondAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryCreateWindow(env, "create window MyWindow#keepall as MySimpleKeyValueMap",
                    "on SupportMarketDataBean as s0 delete from MyWindow where s0.symbol = key");
        }
    }

    private static class InfraWithDeleteNoAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryCreateWindow(env, "create window MyWindow#keepall as select key as key, value as value from MySimpleKeyValueMap",
                    "on SupportMarketDataBean delete from MyWindow where symbol = key");
        }
    }

    private static class InfraTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindowTW#time(10 sec) as MySimpleKeyValueMap";
            env.compileDeploy(stmtTextCreate, path).addListener("create");
            env.assertPropsPerRowIterator("create", fields, null);

            // create insert into
            String stmtTextInsert = "insert into MyWindowTW select theString as key, longBoxed as value from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create consumer
            String stmtTextSelectOne = "@name('s0') select irstream key, value as value from MyWindowTW";
            env.compileDeploy(stmtTextSelectOne, path).addListener("s0");
            SupportUpdateListener listenerStmtOne = new SupportUpdateListener();

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowTW as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            sendTimer(env, 1000);
            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            sendTimer(env, 5000);
            sendSupportBean(env, "E2", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

            sendTimer(env, 10000);
            sendSupportBean(env, "E3", 3L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 3L});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

            // Should push out the window
            sendTimer(env, 10999);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 11000);
            env.assertPropsOld("create", fields, new Object[]{"E1", 1L});
            env.assertPropsOld("s0", fields, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E2", 2L}, {"E3", 3L}});

            sendSupportBean(env, "E4", 4L);
            env.assertPropsNew("create", fields, new Object[]{"E4", 4L});
            env.assertPropsNew("s0", fields, new Object[]{"E4", 4L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E2", 2L}, {"E3", 3L}, {"E4", 4L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 2L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 3L}, {"E4", 4L}});

            // nothing pushed
            sendTimer(env, 15000);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            // push last event
            sendTimer(env, 19999);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 20000);
            env.assertPropsOld("create", fields, new Object[]{"E3", 3L});
            env.assertPropsOld("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E4", 4L}});

            // delete E4
            sendMarketBean(env, "E4");
            env.assertPropsOld("create", fields, new Object[]{"E4", 4L});
            env.assertPropsOld("s0", fields, new Object[]{"E4", 4L});
            env.assertPropsPerRowIterator("create", fields, null);

            sendTimer(env, 100000);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class InfraTimeWindowSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow#time(10 sec) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            env.milestone(0);

            // create insert into
            String stmtTextInsert = "@name('insert') insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            env.milestone(1);

            // create consumer
            String stmtTextSelectOne = "@name('consume') select irstream key, value as value from MyWindow";
            env.compileDeploy(stmtTextSelectOne, path).addListener("consume");

            env.milestone(2);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            env.milestone(3);

            // send event
            env.advanceTime(0);
            sendBeanInt(env, "G1", 10);
            env.assertPropsNew("create", fields, new Object[]{"G1", 10});

            env.milestone(4);

            // send event
            env.advanceTime(5000);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G2", 20);
            env.assertPropsNew("create", fields, new Object[]{"G2", 20});

            env.milestone(5);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}, {"G2", 20}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 20});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});

            env.milestone(6);

            // move time window
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            env.advanceTime(10000);
            env.assertPropsOld("create", fields, new Object[]{"G1", 10});

            env.milestone(7);

            env.advanceTime(25000);
            env.assertListenerNotInvoked("create");

            env.milestone(8);

            // send events
            env.advanceTime(25000);
            sendBeanInt(env, "G3", 30);
            env.advanceTime(26000);
            sendBeanInt(env, "G4", 40);
            env.advanceTime(27000);
            sendBeanInt(env, "G5", 50);
            env.listenerReset("create");

            env.milestone(9);

            // delete g3
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}, {"G5", 50}});
            sendMarketBean(env, "G3");
            env.assertPropsOld("create", fields, new Object[]{"G3", 30});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 40}, {"G5", 50}});

            env.milestone(10);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 40}, {"G5", 50}});
            env.advanceTime(35999);
            env.assertListenerNotInvoked("create");
            env.advanceTime(36000);
            env.assertPropsOld("create", fields, new Object[]{"G4", 40});

            env.milestone(11);

            // delete g5
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G5", 50}});
            sendMarketBean(env, "G5");
            env.assertPropsOld("create", fields, new Object[]{"G5", 50});
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(12);

            env.assertPropsPerRowIterator("create", fields, null);

            // destroy all
            env.undeployModuleContaining("insert");
            env.undeployModuleContaining("delete");
            env.undeployModuleContaining("consume");
            env.undeployModuleContaining("create");
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraTimeFirstWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            sendTimer(env, 1000);

            String epl = "@name('create') create window MyWindowTFW#firsttime(10 sec) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowTFW select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowTFW;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowTFW as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("create").addListener("s0").addListener("delete");
            env.assertPropsPerRowIterator("create", fields, null);

            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            sendTimer(env, 5000);
            sendSupportBean(env, "E2", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

            sendTimer(env, 10000);
            sendSupportBean(env, "E3", 3L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 3L});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

            // Should not push out the window
            sendTimer(env, 12000);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

            sendSupportBean(env, "E4", 4L);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 2L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

            // nothing pushed
            sendTimer(env, 100000);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class InfraExtTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowETW#ext_timed(value, 10 sec) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowETW select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowETW;\n" +
                    "@name('delete') on SupportMarketDataBean delete from MyWindowETW where symbol = key";
            env.compileDeploy(epl).addListener("s0").addListener("create").addListener("delete");

            sendSupportBean(env, "E1", 1000L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 1000L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1000L}});

            sendSupportBean(env, "E2", 5000L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 5000L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 5000L});

            sendSupportBean(env, "E3", 10000L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 10000L});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 10000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1000L}, {"E2", 5000L}, {"E3", 10000L}});

            // Should push out the window
            sendSupportBean(env, "E4", 11000L);
            env.assertListener("create", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E4", 11000L});
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 1000L});
                listener.reset();
            });
            env.assertPropsIRPair("s0", fields, new Object[]{"E4", 11000L}, new Object[]{"E1", 1000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E2", 5000L}, {"E3", 10000L}, {"E4", 11000L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 5000L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 5000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 10000L}, {"E4", 11000L}});

            // nothing pushed other then E5 (E2 is deleted)
            sendSupportBean(env, "E5", 15000L);
            env.assertPropsNew("s0", fields, new Object[]{"E5", 15000L});
            env.assertPropsNew("create", fields, new Object[]{"E5", 15000L});

            env.undeployAll();
        }
    }

    public static class InfraExtTimeWindowSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.win:ext_timed(value, 10 sec) as select theString as key, longBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            env.milestone(0);

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, longBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            env.milestone(1);

            // create consumer
            String stmtTextSelectOne = "@name('consume') select irstream key, value as value from MyWindow";
            env.compileDeploy(stmtTextSelectOne, path).addListener("consume");

            env.milestone(2);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            env.milestone(3);

            // send event
            sendBeanLong(env, "G1", 0L);
            env.assertPropsNew("create", fields, new Object[]{"G1", 0L});

            env.milestone(4);

            // send event
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 0L}});
            sendBeanLong(env, "G2", 5000L);
            env.assertPropsNew("create", fields, new Object[]{"G2", 5000L});

            env.milestone(5);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 0L}, {"G2", 5000L}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 5000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 0L}});

            env.milestone(6);

            // move time window
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 0L}});
            sendBeanLong(env, "G3", 10000L);
            env.assertListener("create", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G3", 10000L});
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"G1", 0L});
                listener.reset();
            });
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 10000L}});

            env.milestone(7);

            // move time window
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 10000L}});
            sendBeanLong(env, "G4", 15000L);
            env.assertPropsNew("create", fields, new Object[]{"G4", 15000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 10000L}, {"G4", 15000L}});

            env.milestone(8);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 10000L}, {"G4", 15000L}});
            sendMarketBean(env, "G3");
            env.assertPropsOld("create", fields, new Object[]{"G3", 10000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 15000L}});

            env.milestone(9);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 15000L}});
            sendBeanLong(env, "G5", 21000L);
            env.assertPropsNew("create", fields, new Object[]{"G5", 21000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 15000L}, {"G5", 21000L}});

            env.milestone(10);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 15000L}, {"G5", 21000L}});

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanLong(RegressionEnvironment env, String string, long longBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setLongBoxed(longBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    public static class InfraExtTimeWindowSceneThree implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            RegressionPath path = new RegressionPath();

            env.advanceTime(0);
            env.compileDeploy("@public create window ABCWin.win:time(10 sec) as SupportBean", path);
            env.compileDeploy("insert into ABCWin select * from SupportBean", path);
            env.compileDeploy("@Name('s0') select irstream * from ABCWin", path);
            env.compileDeploy("on SupportBean_A delete from ABCWin where theString = id", path);
            env.addListener("s0");

            env.milestone(0);
            env.assertPropsPerRowIterator("s0", fields, null);

            env.advanceTime(1000);
            sendSupportBean(env, "E1");
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(1);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1"}});

            env.advanceTime(2000);
            sendSupportBean_A(env, "E1");    // delete E1
            env.assertPropsOld("s0", fields, new Object[]{"E1"});

            env.milestone(2);
            env.assertPropsPerRowIterator("s0", fields, new Object[0][]);

            env.advanceTime(3000);
            sendSupportBean(env, "E2");
            env.assertPropsNew("s0", fields, new Object[]{"E2"});

            env.advanceTime(3000);
            sendSupportBean(env, "E3");
            env.assertPropsNew("s0", fields, new Object[]{"E3"});

            env.milestone(3);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E2"}, {"E3"}});
            sendSupportBean_A(env, "E3");    // delete E3
            env.assertPropsOld("s0", fields, new Object[]{"E3"});

            env.milestone(4);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E2"}});
            env.advanceTime(12999);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(13000);
            env.assertPropsOld("s0", fields, new Object[]{"E2"});

            env.milestone(5);

            env.assertPropsPerRowIterator("s0", fields, new Object[0][]);

            env.undeployAll();
        }
    }

    private static class InfraTimeOrderWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') @public create window MyWindowTOW#time_order(value, 10 sec) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowTOW select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowTOW;\n" +
                    "@name('delete') on SupportMarketDataBean delete from MyWindowTOW where symbol = key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendTimer(env, 5000);
            sendSupportBean(env, "E1", 3000L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 3000L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 3000L});

            sendTimer(env, 6000);
            sendSupportBean(env, "E2", 2000L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 2000L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2000L});

            sendTimer(env, 10000);
            sendSupportBean(env, "E3", 1000L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 1000L});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 1000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 1000L}, {"E2", 2000L}, {"E1", 3000L}});

            // Should push out the window
            sendTimer(env, 11000);
            env.assertPropsOld("create", fields, new Object[]{"E3", 1000L});
            env.assertPropsOld("s0", fields, new Object[]{"E3", 1000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E2", 2000L}, {"E1", 3000L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 2000L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 2000L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 3000L}});

            sendTimer(env, 12999);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 13000);
            env.assertPropsOld("create", fields, new Object[]{"E1", 3000L});
            env.assertPropsOld("s0", fields, new Object[]{"E1", 3000L});
            env.assertPropsPerRowIterator("create", fields, null);

            sendTimer(env, 100000);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class InfraTimeOrderSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.ext:time_order(value, 10) as select theString as key, longBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, longBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // send event G1
            env.advanceTime(20000);
            sendBeanLong(env, "G1", 23000);
            env.assertPropsNew("create", fields, new Object[]{"G1"});

            env.milestone(0);

            // send event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1"}});
            env.advanceTime(20000);
            sendBeanLong(env, "G2", 19000);
            env.assertPropsNew("create", fields, new Object[]{"G2"});

            env.milestone(1);

            // send event G3, pass through
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G2"}, {"G1"}});
            env.advanceTime(21000);
            sendBeanLong(env, "G3", 10000);
            env.assertListener("create", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G3"});
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"G3"});
                listener.reset();
            });

            env.milestone(2);

            // delete G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G2"}, {"G1"}});
            env.advanceTime(21000);
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2"});

            env.milestone(3);

            // send event G4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1"}});
            env.advanceTime(22000);
            sendBeanLong(env, "G4", 18000);
            env.assertPropsNew("create", fields, new Object[]{"G4"});

            env.milestone(4);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4"}, {"G1"}});
            env.advanceTime(23000);
            sendBeanLong(env, "G5", 22000);
            env.assertPropsNew("create", fields, new Object[]{"G5"});

            env.milestone(5);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4"}, {"G5"}, {"G1"}});
            env.advanceTime(27999);
            env.assertListenerNotInvoked("create");
            env.advanceTime(28000);
            env.assertPropsOld("create", fields, new Object[]{"G4"});

            env.milestone(6);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G5"}, {"G1"}});
            env.advanceTime(31999);
            env.assertListenerNotInvoked("create");
            env.advanceTime(32000);
            env.assertPropsOld("create", fields, new Object[]{"G5"});

            env.milestone(7);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1"}});
            env.advanceTime(32000);
            sendBeanLong(env, "G6", 25000);
            env.assertPropsNew("create", fields, new Object[]{"G6"});

            env.milestone(8);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1"}, {"G6"}});
            env.advanceTime(32000);
            sendMarketBean(env, "G1");
            env.assertPropsOld("create", fields, new Object[]{"G1"});

            env.milestone(9);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G6"}});
            env.advanceTime(34999);
            env.assertListenerNotInvoked("create");
            env.advanceTime(35000);
            env.assertPropsOld("create", fields, new Object[]{"G6"});

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanLong(RegressionEnvironment env, String string, long longBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setLongBoxed(longBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraLengthWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowLW#length(3) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowLW select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowLW;\n" +
                    "@name('delete') on SupportMarketDataBean delete from MyWindowLW where symbol = key";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});

            sendSupportBean(env, "E2", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2L});

            sendSupportBean(env, "E3", 3L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 3L});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 2L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

            sendSupportBean(env, "E4", 4L);
            env.assertPropsNew("create", fields, new Object[]{"E4", 4L});
            env.assertPropsNew("s0", fields, new Object[]{"E4", 4L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 3L}, {"E4", 4L}});

            sendSupportBean(env, "E5", 5L);
            env.assertPropsIRPair("create", fields, new Object[]{"E5", 5L}, new Object[]{"E1", 1L});
            env.assertPropsIRPair("s0", fields, new Object[]{"E5", 5L}, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 3L}, {"E4", 4L}, {"E5", 5L}});

            sendSupportBean(env, "E6", 6L);
            env.assertPropsIRPair("create", fields, new Object[]{"E6", 6L}, new Object[]{"E3", 3L});
            env.assertPropsIRPair("s0", fields, new Object[]{"E6", 6L}, new Object[]{"E3", 3L});

            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class InfraLengthWindowSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.win:length(3) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // send event G1
            sendBeanInt(env, "G1", 10);
            env.assertPropsNew("create", fields, new Object[]{"G1", 10});

            env.milestone(0);

            // send event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G2", 20);
            env.assertPropsNew("create", fields, new Object[]{"G2", 20});

            env.milestone(1);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}, {"G2", 20}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 20});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});

            env.milestone(2);

            // send event G3
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G3", 30);
            env.assertPropsNew("create", fields, new Object[]{"G3", 30});

            env.milestone(3);

            // delete event G1
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}, {"G3", 30}});
            sendMarketBean(env, "G1");
            env.assertPropsOld("create", fields, new Object[]{"G1", 10});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}});

            env.milestone(4);

            // send event G4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}});
            sendBeanInt(env, "G4", 40);
            env.assertPropsNew("create", fields, new Object[]{"G4", 40});

            env.milestone(5);

            // send event G5
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}});
            sendBeanInt(env, "G5", 50);
            env.assertPropsNew("create", fields, new Object[]{"G5", 50});

            env.milestone(6);

            // send event G6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}, {"G5", 50}});
            sendBeanInt(env, "G6", 60);
            env.assertPropsIRPair("create", fields, new Object[]{"G6", 60}, new Object[]{"G3", 30});

            env.milestone(7);

            // delete event G6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 40}, {"G5", 50}, {"G6", 60}});
            sendMarketBean(env, "G6");
            env.assertPropsOld("create", fields, new Object[]{"G6", 60});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 40}, {"G5", 50}});

            env.milestone(8);

            // send event G7
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 40}, {"G5", 50}});
            sendBeanInt(env, "G7", 70);
            env.assertPropsNew("create", fields, new Object[]{"G7", 70});

            env.milestone(9);

            // send event G8
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 40}, {"G5", 50}, {"G7", 70}});
            sendBeanInt(env, "G8", 80);
            env.assertPropsIRPair("create", fields, new Object[]{"G8", 80}, new Object[]{"G4", 40});

            env.milestone(10);

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraLengthFirstWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowLFW#firstlength(2) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowLFW select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowLFW;\n" +
                    "@name('delete') on SupportMarketDataBean delete from MyWindowLFW where symbol = key";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});

            sendSupportBean(env, "E2", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2L});

            sendSupportBean(env, "E3", 3L);
            env.assertListenerNotInvoked("create");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 2L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            sendSupportBean(env, "E4", 4L);
            env.assertPropsNew("create", fields, new Object[]{"E4", 4L});
            env.assertPropsNew("s0", fields, new Object[]{"E4", 4L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E4", 4L}});

            sendSupportBean(env, "E5", 5L);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E4", 4L}});

            env.undeployAll();
        }
    }

    private static class InfraTimeAccum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            // create window
            String epl = "@name('create') create window MyWindowTA#time_accum(10 sec) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowTA select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowTA;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowTA as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendTimer(env, 1000);
            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});

            sendTimer(env, 5000);
            sendSupportBean(env, "E2", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 2L});

            sendTimer(env, 10000);
            sendSupportBean(env, "E3", 3L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 3L});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 3L});

            sendTimer(env, 15000);
            sendSupportBean(env, "E4", 4L);
            env.assertPropsNew("create", fields, new Object[]{"E4", 4L});
            env.assertPropsNew("s0", fields, new Object[]{"E4", 4L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}, {"E4", 4L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 2L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 3L}, {"E4", 4L}});

            // nothing pushed
            sendTimer(env, 24999);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 25000);
            env.assertListener("create", listener -> {
                EventBean[] oldData = listener.getLastOldData();
                assertEquals(3, oldData.length);
                EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
                EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
                EPAssertionUtil.assertProps(oldData[2], fields, new Object[]{"E4", 4L});
                listener.reset();
            });
            env.listenerReset("s0");
            env.assertPropsPerRowIterator("create", fields, null);

            // delete E4
            sendMarketBean(env, "E4");
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 30000);
            sendSupportBean(env, "E5", 5L);
            env.assertPropsNew("create", fields, new Object[]{"E5", 5L});
            env.assertPropsNew("s0", fields, new Object[]{"E5", 5L});

            sendTimer(env, 31000);
            sendSupportBean(env, "E6", 6L);
            env.assertPropsNew("create", fields, new Object[]{"E6", 6L});
            env.assertPropsNew("s0", fields, new Object[]{"E6", 6L});

            sendTimer(env, 38000);
            sendSupportBean(env, "E7", 7L);
            env.assertPropsNew("create", fields, new Object[]{"E7", 7L});
            env.assertPropsNew("s0", fields, new Object[]{"E7", 7L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E5", 5L}, {"E6", 6L}, {"E7", 7L}});

            // delete E7 - deleting the last should spit out the first 2 timely
            sendMarketBean(env, "E7");
            env.assertPropsOld("create", fields, new Object[]{"E7", 7L});
            env.assertPropsOld("s0", fields, new Object[]{"E7", 7L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E5", 5L}, {"E6", 6L}});

            sendTimer(env, 40999);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 41000);
            env.assertListener("s0", listener -> {
                assertNull(listener.getLastNewData());
                EventBean[] oldData = listener.getLastOldData();
                assertEquals(2, oldData.length);
                EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E5", 5L});
                EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E6", 6L});
                listener.reset();
            });
            env.listenerReset("create");
            env.assertPropsPerRowIterator("create", fields, null);

            sendTimer(env, 50000);
            sendSupportBean(env, "E8", 8L);
            env.assertPropsNew("create", fields, new Object[]{"E8", 8L});
            env.assertPropsNew("s0", fields, new Object[]{"E8", 8L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E8", 8L}});

            sendTimer(env, 55000);
            sendMarketBean(env, "E8");
            env.assertPropsOld("create", fields, new Object[]{"E8", 8L});
            env.assertPropsOld("s0", fields, new Object[]{"E8", 8L});
            env.assertPropsPerRowIterator("create", fields, null);

            sendTimer(env, 100000);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class InfraTimeAccumSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.win:time_accum(10 sec) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "@name('insert') insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create consumer
            String stmtTextSelectOne = "@name('consume') select irstream key, value as value from MyWindow";
            env.compileDeploy(stmtTextSelectOne, path).addListener("consume");

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            env.milestone(0);

            // send event
            env.advanceTime(1000);
            sendBeanInt(env, "G1", 1);
            env.assertPropsNew("create", fields, new Object[]{"G1", 1});

            env.milestone(1);

            // send event
            env.advanceTime(5000);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});
            sendBeanInt(env, "G2", 2);
            env.assertPropsNew("create", fields, new Object[]{"G2", 2});

            env.milestone(2);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}, {"G2", 2}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 2});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});

            env.milestone(3);

            // move time window
            env.advanceTime(10999);
            env.assertListenerNotInvoked("create");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});
            env.advanceTime(11000);
            env.assertPropsOld("create", fields, new Object[]{"G1", 1});
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(4);

            // Send G3
            env.assertPropsPerRowIterator("create", fields, null);
            env.advanceTime(20000);
            sendBeanInt(env, "G3", 3);
            env.assertPropsNew("create", fields, new Object[]{"G3", 3});

            env.milestone(5);

            // Send G4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}});
            env.advanceTime(29999);
            sendBeanInt(env, "G4", 4);
            env.assertPropsNew("create", fields, new Object[]{"G4", 4});

            env.milestone(6);

            // Delete G3
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G4", 4}});
            sendMarketBean(env, "G3");
            env.assertPropsOld("create", fields, new Object[]{"G3", 3});

            env.milestone(7);

            // Delete G4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G4", 4}});
            sendMarketBean(env, "G4");
            env.assertPropsOld("create", fields, new Object[]{"G4", 4});

            env.milestone(8);

            // Send timer, no events
            env.assertPropsPerRowIterator("create", fields, null);
            env.advanceTime(40000);
            env.assertListenerNotInvoked("create");

            env.milestone(9);

            // Send G5
            env.advanceTime(41000);
            sendBeanInt(env, "G5", 5);
            env.assertPropsNew("create", fields, new Object[]{"G5", 5});

            env.milestone(10);

            // Send G6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G5", 5}});
            env.advanceTime(42000);
            sendBeanInt(env, "G6", 6);
            env.assertPropsNew("create", fields, new Object[]{"G6", 6});

            env.milestone(11);

            // Send G7
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G5", 5}, {"G6", 6}});
            env.advanceTime(43000);
            sendBeanInt(env, "G7", 7);
            env.assertPropsNew("create", fields, new Object[]{"G7", 7});

            env.milestone(12);

            // Send G8
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G5", 5}, {"G6", 6}, {"G7", 7}});
            env.advanceTime(44000);
            sendBeanInt(env, "G8", 8);
            env.assertPropsNew("create", fields, new Object[]{"G8", 8});

            env.milestone(13);

            // Delete G6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G5", 5}, {"G6", 6}, {"G7", 7}, {"G8", 8}});
            sendMarketBean(env, "G6");
            env.assertPropsOld("create", fields, new Object[]{"G6", 6});

            env.milestone(14);

            // Delete G8
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G5", 5}, {"G7", 7}, {"G8", 8}});
            sendMarketBean(env, "G8");
            env.assertPropsOld("create", fields, new Object[]{"G8", 8});

            env.milestone(15);

            env.advanceTime(52999);
            env.assertListenerNotInvoked("create");
            env.advanceTime(53000);
            env.assertListener("create", listener -> {
                assertEquals(2, listener.getLastOldData().length);
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"G5", 5});
                EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"G7", 7});
                assertNull(listener.getLastNewData());
                listener.reset();
            });

            // destroy all
            env.undeployModuleContaining("insert");
            env.undeployModuleContaining("delete");
            env.undeployModuleContaining("consume");
            env.undeployModuleContaining("create");
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraTimeBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowTB#time_batch(10 sec) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowTB select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select key, value as value from MyWindowTB;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowTB as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendTimer(env, 1000);
            sendSupportBean(env, "E1", 1L);

            sendTimer(env, 5000);
            sendSupportBean(env, "E2", 2L);

            sendTimer(env, 10000);
            sendSupportBean(env, "E3", 3L);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}, {"E3", 3L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

            // nothing pushed
            sendTimer(env, 10999);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 11000);
            env.assertListener("create", listener -> {
                assertNull(listener.getLastOldData());
                EventBean[] newData = listener.getLastNewData();
                assertEquals(2, newData.length);
                EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
                EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
                listener.reset();
            });
            env.listenerReset("s0");
            env.assertPropsPerRowIterator("create", fields, null);

            sendTimer(env, 21000);
            env.assertListener("create", listener -> {
                assertNull(listener.getLastNewData());
                EventBean[] oldData = listener.getLastOldData();
                assertEquals(2, oldData.length);
                EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
                EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
                listener.reset();
            });
            env.listenerReset("s0");

            // send and delete E4, leaving an empty batch
            sendSupportBean(env, "E4", 4L);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E4", 4L}});

            sendMarketBean(env, "E4");
            env.assertPropsPerRowIterator("create", fields, null);

            sendTimer(env, 31000);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class InfraTimeBatchSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.win:time_batch(10) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // send event
            env.advanceTime(1000);
            sendBeanInt(env, "G1", 1);
            env.assertListenerNotInvoked("create");

            env.milestone(0);

            // send event
            env.advanceTime(5000);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});
            sendBeanInt(env, "G2", 2);
            env.assertListenerNotInvoked("create");

            env.milestone(1);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}, {"G2", 2}});
            sendMarketBean(env, "G2");
            env.assertListenerNotInvoked("create");

            env.milestone(2);

            // delete event G1
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});
            sendMarketBean(env, "G1");
            env.assertListenerNotInvoked("create");

            env.milestone(3);

            env.assertPropsPerRowIterator("create", fields, null);
            env.advanceTime(11000);
            env.assertListenerNotInvoked("create");

            env.milestone(4);

            // Send g3, g4 and g5
            env.advanceTime(15000);
            sendBeanInt(env, "G3", 3);
            sendBeanInt(env, "G4", 4);
            sendBeanInt(env, "G5", 5);

            env.milestone(5);

            // Delete g5
            sendMarketBean(env, "G5");
            env.assertListenerNotInvoked("create");

            env.milestone(6);

            // send g6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G4", 4}});
            env.advanceTime(18000);
            sendBeanInt(env, "G6", 6);
            env.assertListenerNotInvoked("create");

            env.milestone(7);

            // flush batch
            env.advanceTime(21000);
            env.assertListener("create", listener -> {
                assertEquals(3, listener.getLastNewData().length);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G3", 3});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"G4", 4});
                EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"G6", 6});
                assertNull(listener.getLastOldData());
                env.listenerReset("create");
            });
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(8);

            // send g7, g8 and g9
            env.advanceTime(22000);
            sendBeanInt(env, "G7", 7);
            sendBeanInt(env, "G8", 8);
            sendBeanInt(env, "G9", 9);

            env.milestone(9);

            // delete g7 and g9
            sendMarketBean(env, "G7");
            sendMarketBean(env, "G9");
            env.assertListenerNotInvoked("create");

            env.milestone(10);

            // flush
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G8", 8}});
            env.advanceTime(31000);
            env.assertListener("create", listener -> {
                assertEquals(1, listener.getLastNewData().length);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G8", 8});
                assertEquals(3, listener.getLastOldData().length);
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"G3", 3});
                EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"G4", 4});
                EPAssertionUtil.assertProps(listener.getLastOldData()[2], fields, new Object[]{"G6", 6});
                listener.reset();
            });

            env.assertPropsPerRowIterator("create", fields, null);

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraTimeBatchLateConsumer implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            sendTimer(env, 0);

            String epl = "@name('create') @public create window MyWindowTBLC#time_batch(10 sec) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowTBLC select theString as key, longBoxed as value from SupportBean;\n";
            env.compileDeploy(epl, path);

            sendTimer(env, 0);
            sendSupportBean(env, "E1", 1L);

            sendTimer(env, 5000);
            sendSupportBean(env, "E2", 2L);

            // create consumer
            String stmtTextSelectOne = "@name('s0') select sum(value) as value from MyWindowTBLC";
            env.compileDeploy(stmtTextSelectOne, path).addListener("s0");

            sendTimer(env, 8000);
            sendSupportBean(env, "E3", 3L);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 10000);
            env.assertPropsNew("s0", new String[]{"value"}, new Object[]{6L});
            env.assertPropsPerRowIterator("create", new String[]{"value"}, null);

            env.undeployAll();
        }
    }

    private static class InfraLengthBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowLB#length_batch(3) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowLB select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select key, value as value from MyWindowLB;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowLB as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendSupportBean(env, "E1", 1L);
            sendSupportBean(env, "E2", 2L);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            sendSupportBean(env, "E3", 3L);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

            sendSupportBean(env, "E4", 4L);
            env.assertListener("create", listener -> {
                assertNull(listener.getLastOldData());
                EventBean[] newData = listener.getLastNewData();
                assertEquals(3, newData.length);
                EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
                EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
                EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E4", 4L});
                listener.reset();
            });
            env.listenerReset("s0");
            env.assertPropsPerRowIterator("create", fields, null);

            sendSupportBean(env, "E5", 5L);
            sendSupportBean(env, "E6", 6L);
            sendMarketBean(env, "E5");
            sendMarketBean(env, "E6");
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, null);

            sendSupportBean(env, "E7", 7L);
            sendSupportBean(env, "E8", 8L);
            sendSupportBean(env, "E9", 9L);
            env.assertListener("create", listener -> {
                EventBean[] oldData = listener.getLastOldData();
                EventBean[] newData = listener.getLastNewData();
                assertEquals(3, newData.length);
                assertEquals(3, oldData.length);
                EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E7", 7L});
                EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E8", 8L});
                EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E9", 9L});
                EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
                EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
                EPAssertionUtil.assertProps(oldData[2], fields, new Object[]{"E4", 4L});
                listener.reset();
            });
            env.listenerReset("s0");

            sendSupportBean(env, "E10", 10L);
            sendSupportBean(env, "E10", 11L);
            sendMarketBean(env, "E10");

            sendSupportBean(env, "E21", 21L);
            sendSupportBean(env, "E22", 22L);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            sendSupportBean(env, "E23", 23L);
            env.assertListener("create", listener -> {
                assertEquals(3, listener.getLastNewData().length);
                assertEquals(3, listener.getLastOldData().length);
            });

            env.undeployAll();
        }
    }

    public static class InfraLengthBatchSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.win:length_batch(3) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            env.milestone(0);

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            env.milestone(1);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            env.milestone(2);

            // send event
            sendBeanInt(env, "G1", 10);
            env.assertListenerNotInvoked("create");

            env.milestone(3);

            // send event
            sendBeanInt(env, "G2", 20);
            env.assertListenerNotInvoked("create");

            env.milestone(4);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}, {"G2", 20}});
            sendMarketBean(env, "G2");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});

            env.milestone(5);

            // delete event G1
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendMarketBean(env, "G1");
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(6);

            // send event G3
            env.assertPropsPerRowIterator("create", fields, null);
            sendBeanInt(env, "G3", 30);
            env.assertListenerNotInvoked("create");

            env.milestone(7);

            // send event g4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}});
            sendBeanInt(env, "G4", 40);
            env.assertListenerNotInvoked("create");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}});

            env.milestone(8);

            // delete event G4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G4", 40}});
            sendMarketBean(env, "G4");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}});

            env.milestone(9);

            // send G5
            sendBeanInt(env, "G5", 50);
            env.assertListenerNotInvoked("create");

            env.milestone(10);

            // send G6, batch fires
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 30}, {"G5", 50}});
            sendBeanInt(env, "G6", 60);
            env.assertListener("create", listener -> {
                assertEquals(3, listener.getLastNewData().length);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G3", 30});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"G5", 50});
                EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"G6", 60});
                listener.reset();
            });
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(11);

            // send G8
            env.assertPropsPerRowIterator("create", fields, null);
            sendBeanInt(env, "G7", 70);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G7", 70}});

            env.milestone(12);

            // Send G8
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G7", 70}});
            sendBeanInt(env, "G8", 80);
            env.assertListenerNotInvoked("create");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G7", 70}, {"G8", 80}});

            env.milestone(13);

            // Delete G7
            sendMarketBean(env, "G7");
            env.assertListenerNotInvoked("create");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G8", 80}});

            env.milestone(14);

            // Send G9
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G8", 80}});
            sendBeanInt(env, "G9", 90);
            env.assertListenerNotInvoked("create");

            env.milestone(15);

            // Send G10
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G8", 80}, {"G9", 90}});
            sendBeanInt(env, "G10", 100);
            env.assertListener("create", listener -> {
                assertEquals(3, listener.getLastNewData().length);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G8", 80});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"G9", 90});
                EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"G10", 100});
                assertEquals(3, listener.getLastOldData().length);
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"G3", 30});
                EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"G5", 50});
                EPAssertionUtil.assertProps(listener.getLastOldData()[2], fields, new Object[]{"G6", 60});
                listener.reset();
            });
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(16);

            // send g11
            env.assertPropsPerRowIterator("create", fields, null);
            sendBeanInt(env, "G11", 110);
            sendBeanInt(env, "G12", 120);
            env.assertListenerNotInvoked("create");

            env.milestone(17);

            // delete g12
            sendMarketBean(env, "G12");
            env.assertListenerNotInvoked("create");

            env.milestone(18);

            // send g13
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G11", 110}});
            sendBeanInt(env, "G13", 130);
            env.assertListenerNotInvoked("create");

            // Send G14
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G11", 110}, {"G13", 130}});
            sendBeanInt(env, "G14", 140);
            env.assertListener("create", listener -> {
                assertEquals(3, listener.getLastNewData().length);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G11", 110});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"G13", 130});
                EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"G14", 140});
                assertEquals(3, listener.getLastOldData().length);
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"G8", 80});
                EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"G9", 90});
                EPAssertionUtil.assertProps(listener.getLastOldData()[2], fields, new Object[]{"G10", 100});
                listener.reset();
            });
            env.assertPropsPerRowIterator("create", fields, null);

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraSortWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowSW#sort(3, value asc) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowSW select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select key, value as value from MyWindowSW;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowSW as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("create").addListener("s0");

            sendSupportBean(env, "E1", 10L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 10L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 10L});

            sendSupportBean(env, "E2", 20L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 20L});

            sendSupportBean(env, "E3", 15L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 15L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E2", 20L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 20L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 10L}, {"E3", 15L}});

            sendSupportBean(env, "E4", 18L);
            env.assertPropsNew("create", fields, new Object[]{"E4", 18L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E4", 18L}});

            sendSupportBean(env, "E5", 17L);
            env.assertPropsIRPair("create", fields, new Object[]{"E5", 17L}, new Object[]{"E4", 18L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 10L}, {"E3", 15L}, {"E5", 17L}});

            // delete E1
            sendMarketBean(env, "E1");
            env.assertPropsOld("create", fields, new Object[]{"E1", 10L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 15L}, {"E5", 17L}});

            sendSupportBean(env, "E6", 16L);
            env.assertPropsNew("create", fields, new Object[]{"E6", 16L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 15L}, {"E6", 16L}, {"E5", 17L}});

            sendSupportBean(env, "E7", 16L);
            env.assertPropsIRPair("create", fields, new Object[]{"E7", 16L}, new Object[]{"E5", 17L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 15L}, {"E7", 16L}, {"E6", 16L}});

            // delete E7 has no effect
            sendMarketBean(env, "E7");
            env.assertPropsOld("create", fields, new Object[]{"E7", 16L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 15L}, {"E6", 16L}});

            sendSupportBean(env, "E8", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E8", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E8", 1L}, {"E3", 15L}, {"E6", 16L}});

            sendSupportBean(env, "E9", 1L);
            env.assertPropsIRPair("create", fields, new Object[]{"E9", 1L}, new Object[]{"E6", 16L});

            env.undeployAll();
        }
    }

    public static class InfraSortWindowSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.ext:sort(3, value) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // send event G1
            sendBeanInt(env, "G1", 10);
            env.assertPropsNew("create", fields, new Object[]{"G1", 10});

            env.milestone(0);

            // send event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G2", 9);
            env.assertPropsNew("create", fields, new Object[]{"G2", 9});
            env.listenerReset("create");

            env.milestone(1);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G2", 9}, {"G1", 10}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 9});

            env.milestone(2);

            // send g3
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G3", 3);
            env.assertPropsNew("create", fields, new Object[]{"G3", 3});

            env.milestone(3);

            // send g4
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G1", 10}});
            sendBeanInt(env, "G4", 4);
            env.assertPropsNew("create", fields, new Object[]{"G4", 4});

            env.milestone(4);

            // send g5
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G4", 4}, {"G1", 10}});
            sendBeanInt(env, "G5", 5);
            env.assertPropsIRPair("create", fields, new Object[]{"G5", 5}, new Object[]{"G1", 10});

            env.milestone(5);

            // send g6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G4", 4}, {"G5", 5}});
            sendBeanInt(env, "G6", 6);
            env.assertPropsIRPair("create", fields, new Object[]{"G6", 6}, new Object[]{"G6", 6});

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraTimeLengthBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowTLB#time_length_batch(10 sec, 3) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowTLB select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select key, value as value from MyWindowTLB;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowTLB as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("s0").addListener("delete").addListener("create");

            sendTimer(env, 1000);
            sendSupportBean(env, "E1", 1L);
            sendSupportBean(env, "E2", 2L);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 2L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            sendSupportBean(env, "E3", 3L);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 3L}});

            sendSupportBean(env, "E4", 4L);
            env.assertListener("create", listener -> {
                assertNull(listener.getLastOldData());
                EventBean[] newData = listener.getLastNewData();
                assertEquals(3, newData.length);
                EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E1", 1L});
                EPAssertionUtil.assertProps(newData[1], fields, new Object[]{"E3", 3L});
                EPAssertionUtil.assertProps(newData[2], fields, new Object[]{"E4", 4L});
                listener.reset();
            });
            env.listenerReset("s0");
            env.assertPropsPerRowIterator("create", fields, null);

            sendTimer(env, 5000);
            sendSupportBean(env, "E5", 5L);
            sendSupportBean(env, "E6", 6L);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E5", 5L}, {"E6", 6L}});

            sendMarketBean(env, "E5");   // deleting E5
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E6", 6L}});

            sendTimer(env, 10999);
            env.assertListenerNotInvoked("create");
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 11000);
            env.assertListener("create", listener -> {
                EventBean[] newData = listener.getLastNewData();
                assertEquals(1, newData.length);
                EPAssertionUtil.assertProps(newData[0], fields, new Object[]{"E6", 6L});
                EventBean[] oldData = listener.getLastOldData();
                assertEquals(3, oldData.length);
                EPAssertionUtil.assertProps(oldData[0], fields, new Object[]{"E1", 1L});
                EPAssertionUtil.assertProps(oldData[1], fields, new Object[]{"E3", 3L});
                EPAssertionUtil.assertProps(oldData[2], fields, new Object[]{"E4", 4L});
                env.listenerReset("create");
            });
            env.listenerReset("s0");

            env.undeployAll();
        }
    }

    public static class InfraTimeLengthBatchSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.win:time_length_batch(10 sec, 4) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // send event
            env.advanceTime(1000);
            sendBeanInt(env, "G1", 1);
            env.assertListenerNotInvoked("create");

            env.milestone(0);

            // send event
            env.advanceTime(5000);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});
            sendBeanInt(env, "G2", 2);
            env.assertListenerNotInvoked("create");

            env.milestone(1);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}, {"G2", 2}});
            sendMarketBean(env, "G2");
            env.assertListenerNotInvoked("create");

            env.milestone(2);

            // delete event G1
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});
            sendMarketBean(env, "G1");
            env.assertListenerNotInvoked("create");

            env.milestone(3);

            env.assertPropsPerRowIterator("create", fields, null);
            env.advanceTime(11000);
            env.assertListenerNotInvoked("create");

            env.milestone(4);

            // Send g3, g4 and g5
            env.advanceTime(15000);
            sendBeanInt(env, "G3", 3);
            sendBeanInt(env, "G4", 4);

            env.milestone(5);

            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G4", 4}});
            env.advanceTime(16000);
            sendBeanInt(env, "G5", 5);

            env.milestone(6);

            // Delete g5
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G4", 4}, {"G5", 5}});
            sendMarketBean(env, "G5");
            env.assertListenerNotInvoked("create");

            env.milestone(7);

            // send g6
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G3", 3}, {"G4", 4}});
            env.advanceTime(18000);
            sendBeanInt(env, "G6", 6);
            env.assertListenerNotInvoked("create");

            env.milestone(8);

            // flush batch
            env.advanceTime(24999);
            env.assertListenerNotInvoked("create");
            env.advanceTime(25000);
            env.assertListener("create", listener -> {
                assertEquals(3, listener.getLastNewData().length);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G3", 3});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"G4", 4});
                EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"G6", 6});
                assertNull(listener.getLastOldData());
                listener.reset();
            });
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(9);

            // send g7, g8 and g9
            env.advanceTime(28000);
            sendBeanInt(env, "G7", 7);
            sendBeanInt(env, "G8", 8);
            sendBeanInt(env, "G9", 9);

            env.milestone(10);

            // delete g7 and g9
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G7", 7}, {"G8", 8}, {"G9", 9}});
            sendMarketBean(env, "G7");
            sendMarketBean(env, "G9");
            env.assertListenerNotInvoked("create");

            env.milestone(11);

            // flush
            env.advanceTime(34999);
            env.assertListenerNotInvoked("create");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G8", 8}});
            env.advanceTime(35000);
            env.assertListener("create", listener -> {
                assertEquals(1, listener.getLastNewData().length);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"G8", 8});
                assertEquals(3, listener.getLastOldData().length);
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"G3", 3});
                EPAssertionUtil.assertProps(listener.getLastOldData()[1], fields, new Object[]{"G4", 4});
                EPAssertionUtil.assertProps(listener.getLastOldData()[2], fields, new Object[]{"G6", 6});
            });
            env.assertPropsPerRowIterator("create", fields, null);

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    public static class InfraLengthWindowSceneThree implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window ABCWin#length(2) as SupportBean", path);
            env.compileDeploy("insert into ABCWin select * from SupportBean", path);
            env.compileDeploy("on SupportBean_A delete from ABCWin where theString = id", path);
            env.compileDeploy("@Name('s0') select irstream * from ABCWin", path).addListener("s0");

            env.milestone(0);
            env.assertPropsPerRowIterator("s0", fields, null);

            sendSupportBean(env, "E1");
            env.assertPropsNew("s0", fields, new Object[]{"E1"});

            env.milestone(1);

            sendSupportBean_A(env, "E1");
            env.assertPropsOld("s0", fields, new Object[]{"E1"});

            env.milestone(2);

            env.assertPropsPerRowIterator("s0", fields, new Object[0][]);
            sendSupportBean(env, "E2");
            env.assertPropsNew("s0", fields, new Object[]{"E2"});
            sendSupportBean(env, "E3");
            env.assertPropsNew("s0", fields, new Object[]{"E3"});

            env.milestone(3);
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E2"}, {"E3"}});

            sendSupportBean_A(env, "E3");
            env.assertPropsOld("s0", fields, new Object[]{"E3"});

            env.milestone(4);

            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E2"}});

            sendSupportBean(env, "E4");
            env.assertPropsNew("s0", fields, new Object[]{"E4"});
            sendSupportBean(env, "E5");
            env.assertPropsIRPair("s0", fields, new Object[]{"E5"}, new Object[]{"E2"});

            env.undeployAll();
        }
    }

    private static class InfraLengthWindowPerGroup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowWPG#groupwin(value)#length(2) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowWPG select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowWPG;\n" +
                    "@name('delete') on SupportMarketDataBean delete from MyWindowWPG where symbol = key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E1", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});

            sendSupportBean(env, "E2", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E2", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E2", 1L});

            sendSupportBean(env, "E3", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E3", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E3", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E2", 1L}, {"E3", 2L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("create", fields, new Object[]{"E2", 1L});
            env.assertPropsOld("s0", fields, new Object[]{"E2", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}, {"E3", 2L}});

            sendSupportBean(env, "E4", 1L);
            env.assertPropsNew("create", fields, new Object[]{"E4", 1L});
            env.assertPropsNew("s0", fields, new Object[]{"E4", 1L});

            sendSupportBean(env, "E5", 1L);
            env.assertListener("create", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E5", 1L});
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 1L});
                listener.reset();
            });
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E5", 1L});
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 1L});
                listener.reset();
            });


            sendSupportBean(env, "E6", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E6", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E6", 2L});

            // delete E6
            sendMarketBean(env, "E6");
            env.assertPropsOld("create", fields, new Object[]{"E6", 2L});
            env.assertPropsOld("s0", fields, new Object[]{"E6", 2L});

            sendSupportBean(env, "E7", 2L);
            env.assertPropsNew("create", fields, new Object[]{"E7", 2L});
            env.assertPropsNew("s0", fields, new Object[]{"E7", 2L});

            sendSupportBean(env, "E8", 2L);
            env.assertListener("create", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E8", 2L});
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E3", 2L});
                listener.reset();
            });
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E8", 2L});
                EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E3", 2L});
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static class InfraTimeBatchPerGroup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            sendTimer(env, 0);
            String epl = "@name('create') create window MyWindowTBPG#groupwin(value)#time_batch(10 sec) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowTBPG select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select key, value as value from MyWindowTBPG;\n";
            env.compileDeploy(epl).addListener("s0").addListener("create");

            sendTimer(env, 1000);
            sendSupportBean(env, "E1", 10L);
            sendSupportBean(env, "E2", 20L);
            sendSupportBean(env, "E3", 20L);
            sendSupportBean(env, "E4", 10L);

            sendTimer(env, 11000);
            env.assertListener("create", listener -> {
                assertEquals(listener.getLastNewData().length, 4);
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E1", 10L});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"E4", 10L});
                EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"E2", 20L});
                EPAssertionUtil.assertProps(listener.getLastNewData()[3], fields, new Object[]{"E3", 20L});
                listener.reset();
            });
            env.assertListener("s0", listener -> {
                EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E1", 10L});
                EPAssertionUtil.assertProps(listener.getLastNewData()[1], fields, new Object[]{"E4", 10L});
                EPAssertionUtil.assertProps(listener.getLastNewData()[2], fields, new Object[]{"E2", 20L});
                EPAssertionUtil.assertProps(listener.getLastNewData()[3], fields, new Object[]{"E3", 20L});
                listener.reset();
            });

            env.undeployAll();
        }
    }

    private static class InfraDoubleInsertSameWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowDISM#keepall as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowDISM select theString as key, longBoxed+1 as value from SupportBean;\n" +
                    "insert into MyWindowDISM select theString as key, longBoxed+2 as value from SupportBean;\n" +
                    "@name('s0') select key, value as value from MyWindowDISM";
            env.compileDeploy(epl).addListener("create").addListener("s0");

            sendSupportBean(env, "E1", 10L);
            env.assertListener("create", listener -> {
                assertEquals(2, listener.getNewDataList().size());    // listener to window gets 2 individual events
                assertEquals(2, listener.getNewDataList().size());   // listener to statement gets 1 individual event
                assertEquals(2, listener.getNewDataListFlattened().length);
                assertEquals(2, listener.getNewDataListFlattened().length);
            });
            env.assertPropsPerRowNewFlattened("s0",  fields, new Object[][]{{"E1", 11L}, {"E1", 12L}});

            env.undeployAll();
        }
    }

    private static class InfraLastEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowLE#lastevent as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowLE select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowLE;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowLE as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            sendSupportBean(env, "E2", 2L);
            env.assertPropsIRPair("s0", fields, new Object[]{"E2", 2L}, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E2", 2L}});

            // delete E2
            sendMarketBean(env, "E2");
            env.assertPropsOld("s0", fields, new Object[]{"E2", 2L});
            env.assertPropsPerRowIterator("create", fields, null);

            sendSupportBean(env, "E3", 3L);
            env.assertPropsNew("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 3L}});

            // delete E3
            sendMarketBean(env, "E3");
            env.assertPropsOld("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, null);

            sendSupportBean(env, "E4", 4L);
            env.assertPropsNew("s0", fields, new Object[]{"E4", 4L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E4", 4L}});

            // delete other event
            sendMarketBean(env, "E1");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    public static class InfraLastEventSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow.std:lastevent() as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            // create insert into
            String stmtTextInsert = "insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path).addListener("delete");

            // send event
            sendBeanInt(env, "G1", 1);
            env.assertPropsNew("create", fields, new Object[]{"G1", 1});

            env.milestone(0);

            // send event
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1}});
            sendBeanInt(env, "G2", 2);
            env.assertPropsIRPair("create", fields, new Object[]{"G2", 2}, new Object[]{"G1", 1});

            env.milestone(1);

            // delete event G2
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G2", 2}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 2});

            env.milestone(2);

            env.assertIterator("create", iterator -> assertEquals(0, EPAssertionUtil.iteratorCount(iterator)));
            sendBeanInt(env, "G3", 3);
            env.assertPropsNew("create", fields, new Object[]{"G3", 3});

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraFirstEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowFE#firstevent as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowFE select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowFE;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowFE as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            sendSupportBean(env, "E2", 2L);
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1L}});

            // delete E2
            sendMarketBean(env, "E1");
            env.assertPropsOld("s0", fields, new Object[]{"E1", 1L});
            env.assertPropsPerRowIterator("create", fields, null);

            sendSupportBean(env, "E3", 3L);
            env.assertPropsNew("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 3L}});

            // delete E3
            sendMarketBean(env, "E2");   // no effect
            sendMarketBean(env, "E3");
            env.assertPropsOld("s0", fields, new Object[]{"E3", 3L});
            env.assertPropsPerRowIterator("create", fields, null);

            sendSupportBean(env, "E4", 4L);
            env.assertPropsNew("s0", fields, new Object[]{"E4", 4L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E4", 4L}});

            // delete other event
            sendMarketBean(env, "E1");
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class InfraUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowUN#unique(key) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowUN select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowUN;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowUN as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("s0").addListener("create");

            sendSupportBean(env, "G1", 1L);
            env.assertPropsNew("s0", fields, new Object[]{"G1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1L}});

            sendSupportBean(env, "G2", 20L);
            env.assertPropsNew("s0", fields, new Object[]{"G2", 20L});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 1L}, {"G2", 20L}});

            // delete G2
            sendMarketBean(env, "G2");
            env.assertPropsOld("s0", fields, new Object[]{"G2", 20L});

            sendSupportBean(env, "G1", 2L);
            env.assertPropsIRPair("s0", fields, new Object[]{"G1", 2L}, new Object[]{"G1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 2L}});

            sendSupportBean(env, "G2", 21L);
            env.assertPropsNew("s0", fields, new Object[]{"G2", 21L});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 2L}, {"G2", 21L}});

            sendSupportBean(env, "G2", 22L);
            env.assertPropsIRPair("s0", fields, new Object[]{"G2", 22L}, new Object[]{"G2", 21L});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 2L}, {"G2", 22L}});

            sendMarketBean(env, "G1");
            env.assertPropsOld("s0", fields, new Object[]{"G1", 2L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G2", 22L}});

            env.undeployAll();
        }
    }

    public static class InfraUniqueSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindow#unique(key) as select theString as key, intBoxed as value from SupportBean";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            env.milestone(0);

            // create insert into
            String stmtTextInsert = "@name('insert') insert into MyWindow(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            env.milestone(1);

            // create consumer
            String stmtTextSelectOne = "@name('consume') select irstream key, value as value from MyWindow";
            env.compileDeploy(stmtTextSelectOne, path);

            env.milestone(2);

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindow as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path);

            env.milestone(3);

            // send event
            sendBeanInt(env, "G1", 10);
            env.assertPropsNew("create", fields, new Object[]{"G1", 10});

            env.milestone(4);

            // send event
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 10}});
            sendBeanInt(env, "G2", 20);
            env.assertPropsNew("create", fields, new Object[]{"G2", 20});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 10}, {"G2", 20}});

            env.milestone(5);

            // delete event G2
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 10}, {"G2", 20}});
            sendMarketBean(env, "G1");
            env.assertPropsOld("create", fields, new Object[]{"G1", 10});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G2", 20}});

            env.milestone(6);

            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G2", 20}});
            sendMarketBean(env, "G2");
            env.assertPropsOld("create", fields, new Object[]{"G2", 20});

            // destroy all
            env.undeployAll();
        }

        private SupportBean sendBeanInt(RegressionEnvironment env, String string, int intBoxed) {
            SupportBean bean = new SupportBean();
            bean.setTheString(string);
            bean.setIntBoxed(intBoxed);
            env.sendEventBean(bean);
            return bean;
        }

        private void sendMarketBean(RegressionEnvironment env, String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            env.sendEventBean(bean);
        }
    }

    private static class InfraFirstUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowFU#firstunique(key) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowFU select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowFU;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowFU as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("create").addListener("s0").addListener("delete");

            sendSupportBean(env, "G1", 1L);
            env.assertPropsNew("s0", fields, new Object[]{"G1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1L}});

            sendSupportBean(env, "G2", 20L);
            env.assertPropsNew("s0", fields, new Object[]{"G2", 20L});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 1L}, {"G2", 20L}});

            // delete G2
            sendMarketBean(env, "G2");
            env.assertPropsOld("s0", fields, new Object[]{"G2", 20L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1L}});

            sendSupportBean(env, "G1", 2L);  // ignored
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G1", 1L}});

            sendSupportBean(env, "G2", 21L);
            env.assertPropsNew("s0", fields, new Object[]{"G2", 21L});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 1L}, {"G2", 21L}});

            sendSupportBean(env, "G2", 22L); // ignored
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 1L}, {"G2", 21L}});

            sendMarketBean(env, "G1");
            env.assertPropsOld("s0", fields, new Object[]{"G1", 1L});
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"G2", 21L}});

            env.undeployAll();
        }
    }

    private static class InfraFilteringConsumer implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowFC#unique(key) as select theString as key, intPrimitive as value from SupportBean;\n" +
                    "insert into MyWindowFC select theString as key, intPrimitive as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowFC(value > 0, value < 10);\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowFC as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("create").addListener("s0").addListener("delete");

            sendSupportBeanInt(env, "G1", 5);
            env.assertPropsNew("s0", fields, new Object[]{"G1", 5});
            env.assertPropsNew("create", fields, new Object[]{"G1", 5});

            sendSupportBeanInt(env, "G1", 15);
            env.assertPropsOld("s0", fields, new Object[]{"G1", 5});
            env.assertPropsIRPair("create", fields, new Object[]{"G1", 15}, new Object[]{"G1", 5});

            // send G2
            sendSupportBeanInt(env, "G2", 8);
            env.assertPropsNew("s0", fields, new Object[]{"G2", 8});
            env.assertPropsNew("create", fields, new Object[]{"G2", 8});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 15}, {"G2", 8}});
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"G2", 8}});

            // delete G2
            sendMarketBean(env, "G2");
            env.assertPropsOld("s0", fields, new Object[]{"G2", 8});
            env.assertPropsOld("create", fields, new Object[]{"G2", 8});

            // send G3
            sendSupportBeanInt(env, "G3", -1);
            env.assertListenerNotInvoked("s0");
            env.assertPropsNew("create", fields, new Object[]{"G3", -1});
            env.assertPropsPerRowIteratorAnyOrder("create", fields, new Object[][]{{"G1", 15}, {"G3", -1}});
            env.assertPropsPerRowIterator("s0", fields, null);

            // delete G2
            sendMarketBean(env, "G3");
            env.assertListenerNotInvoked("s0");
            env.assertPropsOld("create", fields, new Object[]{"G3", -1});

            sendSupportBeanInt(env, "G1", 6);
            sendSupportBeanInt(env, "G2", 7);
            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"G1", 6}, {"G2", 7}});

            env.undeployAll();
        }
    }

    private static class InfraSelectGroupedViewLateStart implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String epl = "@name('create') @public create window MyWindowSGVS#groupwin(theString, intPrimitive)#length(9) as select theString, intPrimitive from SupportBean;\n" +
                    "@name('insert') insert into MyWindowSGVS select theString, intPrimitive from SupportBean;\n";
            env.compileDeploy(epl, path);

            // fill window
            String[] stringValues = new String[]{"c0", "c1", "c2"};
            for (int i = 0; i < stringValues.length; i++) {
                for (int j = 0; j < 3; j++) {
                    env.sendEventBean(new SupportBean(stringValues[i], j));
                }
            }
            env.sendEventBean(new SupportBean("c0", 1));
            env.sendEventBean(new SupportBean("c1", 2));
            env.sendEventBean(new SupportBean("c3", 3));
            env.assertIterator("create", iterator -> {
                EventBean[] received = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals(12, received.length);
            });

            // create select stmt
            String stmtTextSelect = "@name('s0') select theString, intPrimitive, count(*) from MyWindowSGVS group by theString, intPrimitive order by theString, intPrimitive";
            env.compileDeploy(stmtTextSelect, path);
            env.assertIterator("s0", iterator -> {
                EventBean[] received = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals(10, received.length);
                EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive,count(*)".split(","),
                    new Object[][]{
                        {"c0", 0, 1L},
                        {"c0", 1, 2L},
                        {"c0", 2, 1L},
                        {"c1", 0, 1L},
                        {"c1", 1, 1L},
                        {"c1", 2, 2L},
                        {"c2", 0, 1L},
                        {"c2", 1, 1L},
                        {"c2", 2, 1L},
                        {"c3", 3, 1L},
                    });
            });

            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("create");
        }
    }

    private static class InfraSelectGroupedViewLateStartVariableIterate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') @public create window MyWindowSGVLS#groupwin(theString, intPrimitive)#length(9) as select theString, intPrimitive, longPrimitive, boolPrimitive from SupportBean";
            env.compileDeploy(stmtTextCreate, path);

            // create insert into
            String stmtTextInsert = "insert into MyWindowSGVLS select theString, intPrimitive, longPrimitive, boolPrimitive from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // create variable
            env.compileDeploy("@public create variable string var_1_1_1", path);
            env.compileDeploy("on SupportVariableSetEvent(variableName='var_1_1_1') set var_1_1_1 = value", path);

            // fill window
            String[] stringValues = new String[]{"c0", "c1", "c2"};
            for (int i = 0; i < stringValues.length; i++) {
                for (int j = 0; j < 3; j++) {
                    SupportBean bean = new SupportBean(stringValues[i], j);
                    bean.setLongPrimitive(j);
                    bean.setBoolPrimitive(true);
                    env.sendEventBean(bean);
                }
            }
            // extra record to create non-uniform data
            SupportBean bean = new SupportBean("c1", 1);
            bean.setLongPrimitive(10);
            bean.setBoolPrimitive(true);
            env.sendEventBean(bean);
            env.assertIterator("create", iterator -> {
                EventBean[] received = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals(10, received.length);
            });

            // create select stmt
            String stmtTextSelect = "@name('s0') select theString, intPrimitive, avg(longPrimitive) as avgLong, count(boolPrimitive) as cntBool" +
                    " from MyWindowSGVLS group by theString, intPrimitive having theString = var_1_1_1 order by theString, intPrimitive";
            env.compileDeploy(stmtTextSelect, path);

            // set variable to C0
            env.sendEventBean(new SupportVariableSetEvent("var_1_1_1", "c0"));

            // get iterator results
            env.assertIterator("s0", iterator -> {
                EventBean[] received = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals(3, received.length);
                EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive,avgLong,cntBool".split(","),
                    new Object[][]{
                        {"c0", 0, 0.0, 1L},
                        {"c0", 1, 1.0, 1L},
                        {"c0", 2, 2.0, 1L},
                    });
            });

            // set variable to C1
            env.sendEventBean(new SupportVariableSetEvent("var_1_1_1", "c1"));

            env.assertIterator("s0", iterator -> {
                EventBean[] received = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals(3, received.length);
                EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive,avgLong,cntBool".split(","),
                    new Object[][]{
                        {"c1", 0, 0.0, 1L},
                        {"c1", 1, 5.5, 2L},
                        {"c1", 2, 2.0, 1L},
                    });
            });

            env.undeployAll();
        }
    }

    private static class InfraFilteringConsumerLateStart implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"sumvalue"};
            RegressionPath path = new RegressionPath();

            String epl = "@name('create') @public create window MyWindowFCLS#keepall as select theString as key, intPrimitive as value from SupportBean;\n" +
                    "insert into MyWindowFCLS select theString as key, intPrimitive as value from SupportBean;\n";
            env.compileDeploy(epl, path);

            sendSupportBeanInt(env, "G1", 5);
            sendSupportBeanInt(env, "G2", 15);
            sendSupportBeanInt(env, "G3", 2);

            // create consumer
            String stmtTextSelectOne = "@name('s0') select irstream sum(value) as sumvalue from MyWindowFCLS(value > 0, value < 10)";
            env.compileDeploy(stmtTextSelectOne, path).addListener("s0");
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{7}});

            sendSupportBeanInt(env, "G4", 1);
            env.assertPropsIRPair("s0", fields, new Object[]{8}, new Object[]{7});
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{8}});

            sendSupportBeanInt(env, "G5", 20);
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{8}});

            sendSupportBeanInt(env, "G6", 9);
            env.assertPropsIRPair("s0", fields, new Object[]{17}, new Object[]{8});
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{17}});

            // create delete stmt
            String stmtTextDelete = "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowFCLS as s1 where s0.symbol = s1.key";
            env.compileDeploy(stmtTextDelete, path);

            sendMarketBean(env, "G4");
            env.assertPropsIRPair("s0", fields, new Object[]{16}, new Object[]{17});
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{16}});

            sendMarketBean(env, "G5");
            env.assertListenerNotInvoked("s0");
            env.assertPropsPerRowIterator("s0", fields, new Object[][]{{16}});

            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("delete");
            env.undeployModuleContaining("create");
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("create window MyWindowI1#groupwin(value)#uni(value) as MySimpleKeyValueMap",
                    "Named windows require one or more child views that are data window views [create window MyWindowI1#groupwin(value)#uni(value) as MySimpleKeyValueMap]");

            env.tryInvalidCompile("create window MyWindowI2 as MySimpleKeyValueMap",
                    "Named windows require one or more child views that are data window views [create window MyWindowI2 as MySimpleKeyValueMap]");

            env.tryInvalidCompile("on MySimpleKeyValueMap delete from dummy",
                    "A named window or table 'dummy' has not been declared [on MySimpleKeyValueMap delete from dummy]");

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window SomeWindow#keepall as (a int)", path);
            env.tryInvalidCompile(path, "update SomeWindow set a = 'a' where a = 'b'",
                    "Provided EPL expression is an on-demand query expression (not a continuous query)");
            tryInvalidFAFCompile(env, path, "update istream SomeWindow set a = 'a' where a = 'b'",
                    "Provided EPL expression is a continuous query expression (not an on-demand query)");

            // test model-after with no field
            env.tryInvalidCompile("create window MyWindowI3#keepall as select innermap.abc from OuterMap",
                    "Failed to validate select-clause expression 'innermap.abc': Failed to resolve property 'innermap.abc' to a stream or nested property in a stream");

            env.undeployAll();
        }
    }

    private static class InfraNamedWindowInvalidAlreadyExists implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("create window MyWindowAE#keepall as MySimpleKeyValueMap", compilerOptions -> compilerOptions.setAccessModifierNamedWindow(ctx -> NameAccessModifier.PUBLIC));
            env.deploy(compiled);
            tryInvalidDeploy(env, compiled, "A precondition is not satisfied: A named window by name 'MyWindowAE' has already been created for module '(unnamed)'");
            env.undeployAll();
        }
    }

    private static class InfraNamedWindowInvalidConsumerDataWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create window MyWindowCDW#keepall as MySimpleKeyValueMap", path);
            env.tryInvalidCompile(path, "select key, value as value from MyWindowCDW#time(10 sec)",
                    "Consuming statements to a named window cannot declare a data window view onto the named window [select key, value as value from MyWindowCDW#time(10 sec)]");
            env.undeployAll();
        }
    }

    private static class InfraPriorStats implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldsPrior = new String[]{"priorKeyOne", "priorKeyTwo"};
            String[] fieldsStat = new String[]{"average"};

            String epl = "@name('create') create window MyWindowPS#keepall as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowPS select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select prior(1, key) as priorKeyOne, prior(2, key) as priorKeyTwo from MyWindowPS;\n" +
                    "@name('s3') select average from MyWindowPS#uni(value);\n";
            env.compileDeploy(epl).addListener("create").addListener("s0").addListener("s3");

            env.assertStatement("create", statement -> {
                assertEquals(String.class, statement.getEventType().getPropertyType("key"));
                assertEquals(Long.class, statement.getEventType().getPropertyType("value"));
            });

            // send events
            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("s0", fieldsPrior, new Object[]{null, null});
            env.assertPropsNew("s3", fieldsStat, new Object[]{1d});
            env.assertPropsPerRowIterator("s3", fieldsStat, new Object[][]{{1d}});

            sendSupportBean(env, "E2", 2L);
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"E1", null});
            env.assertPropsNew("s3", fieldsStat, new Object[]{1.5d});
            env.assertPropsPerRowIterator("s3", fieldsStat, new Object[][]{{1.5d}});

            sendSupportBean(env, "E3", 2L);
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"E2", "E1"});
            env.assertPropsNew("s3", fieldsStat, new Object[]{5 / 3d});
            env.assertPropsPerRowIterator("s3", fieldsStat, new Object[][]{{5 / 3d}});

            sendSupportBean(env, "E4", 2L);
            env.assertPropsNew("s0", fieldsPrior, new Object[]{"E3", "E2"});
            env.assertPropsNew("s3", fieldsStat, new Object[]{1.75});
            env.assertPropsPerRowIterator("s3", fieldsStat, new Object[][]{{1.75d}});

            env.undeployAll();
        }
    }

    private static class InfraLateConsumer implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldsWin = new String[]{"key", "value"};
            String[] fieldsStat = new String[]{"average"};
            String[] fieldsCnt = new String[]{"cnt"};
            RegressionPath path = new RegressionPath();

            String stmtTextCreate = "@name('create') @public create window MyWindowLCL#keepall as MySimpleKeyValueMap";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            env.assertStatement("create", statement -> {
                assertEquals(String.class, statement.getEventType().getPropertyType("key"));
                assertEquals(Long.class, statement.getEventType().getPropertyType("value"));
            });

            String stmtTextInsert = "insert into MyWindowLCL select theString as key, longBoxed as value from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // send events
            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fieldsWin, new Object[]{"E1", 1L});

            sendSupportBean(env, "E2", 2L);
            env.assertPropsNew("create", fieldsWin, new Object[]{"E2", 2L});

            String stmtTextSelectOne = "@name('s0') select irstream average from MyWindowLCL#uni(value)";
            env.compileDeploy(stmtTextSelectOne, path).addListener("s0");
            env.assertPropsPerRowIterator("s0", fieldsStat, new Object[][]{{1.5d}});

            sendSupportBean(env, "E3", 2L);
            env.assertPropsIRPair("s0", fieldsStat, new Object[]{5 / 3d}, new Object[]{3 / 2d});
            env.assertPropsPerRowIterator("s0", fieldsStat, new Object[][]{{5 / 3d}});

            sendSupportBean(env, "E4", 2L);
            env.assertPropsPerRowLastNew("s0", fieldsStat, new Object[][]{{7 / 4d}});
            env.assertPropsPerRowIterator("s0", fieldsStat, new Object[][]{{7 / 4d}});

            String stmtTextSelectTwo = "@name('s2') select count(*) as cnt from MyWindowLCL";
            env.compileDeploy(stmtTextSelectTwo, path);
            env.assertPropsPerRowIterator("s2", fieldsCnt, new Object[][]{{4L}});
            env.assertPropsPerRowIterator("s0", fieldsStat, new Object[][]{{7 / 4d}});

            sendSupportBean(env, "E5", 3L);
            env.assertPropsIRPair("s0", fieldsStat, new Object[]{10 / 5d}, new Object[]{7 / 4d});
            env.assertPropsPerRowIterator("s0", fieldsStat, new Object[][]{{10 / 5d}});
            env.assertPropsPerRowIterator("s2", fieldsCnt, new Object[][]{{5L}});

            env.undeployAll();
        }
    }

    private static class InfraLateConsumerJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fieldsWin = new String[]{"key", "value"};
            String[] fieldsJoin = new String[]{"key", "value", "symbol"};
            RegressionPath path = new RegressionPath();

            String stmtTextCreate = "@name('create') @public create window MyWindowLCJ#keepall as MySimpleKeyValueMap";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            env.assertStatement("create", statement -> {
                assertEquals(String.class, statement.getEventType().getPropertyType("key"));
                assertEquals(Long.class, statement.getEventType().getPropertyType("value"));
            });

            String stmtTextInsert = "insert into MyWindowLCJ select theString as key, longBoxed as value from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            // send events
            sendSupportBean(env, "E1", 1L);
            env.assertPropsNew("create", fieldsWin, new Object[]{"E1", 1L});

            sendSupportBean(env, "E2", 1L);
            env.assertPropsNew("create", fieldsWin, new Object[]{"E2", 1L});

            // This replays into MyWindow
            String stmtTextSelectTwo = "@name('s2') select key, value, symbol from MyWindowLCJ as s0" +
                    " left outer join SupportMarketDataBean#keepall as s1" +
                    " on s0.value = s1.volume";
            env.compileDeploy(stmtTextSelectTwo, path).addListener("s2");
            env.assertListenerNotInvoked("s2");
            env.assertPropsPerRowIterator("s2", fieldsJoin, new Object[][]{{"E1", 1L, null}, {"E2", 1L, null}});

            sendMarketBean(env, "S1", 1);    // join on long
            env.assertListener("s2", listener -> {
                assertEquals(2, listener.getLastNewData().length);
                if (listener.getLastNewData()[0].get("key").equals("E1")) {
                    EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsJoin, new Object[]{"E1", 1L, "S1"});
                    EPAssertionUtil.assertProps(listener.getLastNewData()[1], fieldsJoin, new Object[]{"E2", 1L, "S1"});
                } else {
                    EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsJoin, new Object[]{"E2", 1L, "S1"});
                    EPAssertionUtil.assertProps(listener.getLastNewData()[1], fieldsJoin, new Object[]{"E1", 1L, "S1"});
                }
                listener.reset();
            });
            env.assertPropsPerRowIterator("s2", fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}});

            sendMarketBean(env, "S2", 2);    // join on long
            env.assertListenerNotInvoked("s2");
            env.assertPropsPerRowIterator("s2", fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}});

            sendSupportBean(env, "E3", 2L);
            env.assertPropsNew("create", fieldsWin, new Object[]{"E3", 2L});
            env.assertPropsNew("s2", fieldsJoin, new Object[]{"E3", 2L, "S2"});
            env.assertPropsPerRowIterator("s2", fieldsJoin, new Object[][]{{"E1", 1L, "S1"}, {"E2", 1L, "S1"}, {"E3", 2L, "S2"}});

            env.undeployAll();
        }
    }

    private static class InfraPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            String epl = "@name('create') create window MyWindowPAT#keepall as MySimpleKeyValueMap;\n" +
                    "@name('s0') select a.key as key, a.value as value from pattern [every a=MyWindowPAT(key='S1') or a=MyWindowPAT(key='S2')];\n" +
                    "insert into MyWindowPAT select theString as key, longBoxed as value from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            sendSupportBean(env, "E1", 1L);
            env.assertListenerNotInvoked("s0");

            sendSupportBean(env, "S1", 2L);
            env.assertPropsNew("s0", fields, new Object[]{"S1", 2L});

            sendSupportBean(env, "S1", 3L);
            env.assertPropsNew("s0", fields, new Object[]{"S1", 3L});

            sendSupportBean(env, "S2", 4L);
            env.assertPropsNew("s0", fields, new Object[]{"S2", 4L});

            sendSupportBean(env, "S1", 1L);
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class InfraExternallyTimedBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};

            String epl = "@name('create') create window MyWindowETB#ext_timed_batch(value, 10 sec, 0L) as MySimpleKeyValueMap;\n" +
                    "insert into MyWindowETB select theString as key, longBoxed as value from SupportBean;\n" +
                    "@name('s0') select irstream key, value as value from MyWindowETB;\n" +
                    "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowETB as s1 where s0.symbol = s1.key;\n";
            env.compileDeploy(epl).addListener("delete").addListener("create").addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 1000L);
            sendSupportBean(env, "E2", 8000L);

            env.milestone(1);

            sendSupportBean(env, "E3", 9999L);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1000L}, {"E2", 8000L}, {"E3", 9999L}});

            env.milestone(2);

            // delete E2
            sendMarketBean(env, "E2");
            env.assertListener("create", listener -> EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, null, new Object[][]{{"E2", 8000L}}));
            env.assertListener("s0", listener -> EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, null, new Object[][]{{"E2", 8000L}}));
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 1000L}, {"E3", 9999L}});

            env.milestone(3);

            sendSupportBean(env, "E4", 10000L);
            env.assertListener("create", listener -> EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                    new Object[][]{{"E1", 1000L}, {"E3", 9999L}}, null));
            env.assertListener("s0", listener -> EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                    new Object[][]{{"E1", 1000L}, {"E3", 9999L}}, null));
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E4", 10000L}});

            env.milestone(4);

            // delete E4
            sendMarketBean(env, "E4");
            env.assertListener("create", listener -> EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, null, new Object[][]{{"E4", 10000L}}));
            env.assertListener("s0", listener -> EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields, null, new Object[][]{{"E4", 10000L}}));
            env.assertPropsPerRowIterator("create", fields, null);

            env.milestone(5);

            sendSupportBean(env, "E5", 14000L);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E5", 14000L}});

            env.milestone(6);

            sendSupportBean(env, "E6", 21000L);
            env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E6", 21000L}});
            env.assertPropsPerRowIRPair("create", fields, new Object[][]{{"E5", 14000L}}, new Object[][]{{"E1", 1000L}, {"E3", 9999L}});

            env.undeployAll();
        }
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongBoxed(longBoxed);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String id) {
        env.sendEventBean(new SupportBean_A(id));
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportBean sendSupportBeanLongPrim(RegressionEnvironment env, String theString, long longPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendSupportBeanInt(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        env.sendEventBean(bean);
    }

    private static void tryAssertionBeanContained(RegressionEnvironment env, EventRepresentationChoice rep) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy(rep.getAnnotationText() + " @name('create') @public create window MyWindowBC#keepall as (bean SupportBean_S0)", path);
        env.addListener("create");
        env.assertStatement("create", statement -> assertTrue(rep.matchesClass(statement.getEventType().getUnderlyingType())));
        env.compileDeploy("insert into MyWindowBC select bean.* as bean from SupportBean_S0 as bean", path);

        env.sendEventBean(new SupportBean_S0(1, "E1"));
        env.assertPropsNew("create", "bean.p00".split(","), new Object[]{"E1"});

        env.undeployAll();
    }

    private static void tryCreateWindow(RegressionEnvironment env, String createWindowStatement, String deleteStatement) {
        String[] fields = new String[]{"key", "value"};
        RegressionPath path = new RegressionPath();

        String epl = "@name('create') @public " + createWindowStatement + ";\n" +
                "@name('insert') insert into MyWindow select theString as key, longBoxed as value from SupportBean;\n" +
                "@name('s0') select irstream key, value*2 as value from MyWindow;\n" +
                "@name('s2') select irstream key, sum(value) as value from MyWindow group by key;\n" +
                "@name('s3') select irstream key, value from MyWindow where value >= 10;\n";
        env.compileDeploy(epl, path).addListener("create").addListener("s0").addListener("s2").addListener("s3");

        env.assertStatement("create", statement -> {
            assertEquals(String.class, statement.getEventType().getPropertyType("key"));
            assertEquals(Long.class, statement.getEventType().getPropertyType("value"));
        });

        // send events
        sendSupportBean(env, "E1", 10L);
        env.assertPropsNew("s0", fields, new Object[]{"E1", 20L});
        env.assertPropsIRPair("s2", fields, new Object[]{"E1", 10L}, new Object[]{"E1", null});
        env.assertPropsNew("s3", fields, new Object[]{"E1", 10L});
        env.assertPropsNew("create", fields, new Object[]{"E1", 10L});
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 10L}});
        env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1", 20L}});

        sendSupportBean(env, "E2", 20L);
        env.assertPropsNew("s0", fields, new Object[]{"E2", 40L});
        env.assertPropsIRPair("s2", fields, new Object[]{"E2", 20L}, new Object[]{"E2", null});
        env.assertPropsNew("s3", fields, new Object[]{"E2", 20L});
        env.assertPropsNew("create", fields, new Object[]{"E2", 20L});
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 10L}, {"E2", 20L}});
        env.assertPropsPerRowIterator("s0", fields, new Object[][]{{"E1", 20L}, {"E2", 40L}});

        sendSupportBean(env, "E3", 5L);
        env.assertPropsNew("s0", fields, new Object[]{"E3", 10L});
        env.assertPropsIRPair("s2", fields, new Object[]{"E3", 5L}, new Object[]{"E3", null});
        env.assertListenerNotInvoked("s3");
        env.assertPropsNew("create", fields, new Object[]{"E3", 5L});
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E1", 10L}, {"E2", 20L}, {"E3", 5L}});

        // create delete stmt
        env.compileDeploy("@name('delete') " + deleteStatement, path).addListener("delete");

        // send delete event
        sendMarketBean(env, "E1");
        env.assertPropsOld("s0", fields, new Object[]{"E1", 20L});
        env.assertPropsIRPair("s2", fields, new Object[]{"E1", null}, new Object[]{"E1", 10L});
        env.assertPropsOld("s3", fields, new Object[]{"E1", 10L});
        env.assertPropsOld("create", fields, new Object[]{"E1", 10L});
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E2", 20L}, {"E3", 5L}});

        // send delete event again, none deleted now
        sendMarketBean(env, "E1");
        env.assertListenerNotInvoked("s0");
        env.assertListenerNotInvoked("s2");
        env.assertListenerNotInvoked("create");
        env.assertListenerInvoked("delete");
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E2", 20L}, {"E3", 5L}});

        // send delete event
        sendMarketBean(env, "E2");
        env.assertPropsOld("s0", fields, new Object[]{"E2", 40L});
        env.assertPropsIRPair("s2", fields, new Object[]{"E2", null}, new Object[]{"E2", 20L});
        env.assertPropsOld("s3", fields, new Object[]{"E2", 20L});
        env.assertPropsOld("create", fields, new Object[]{"E2", 20L});
        env.assertPropsPerRowIterator("create", fields, new Object[][]{{"E3", 5L}});

        // send delete event
        sendMarketBean(env, "E3");
        env.assertPropsOld("s0", fields, new Object[]{"E3", 10L});
        env.assertPropsIRPair("s2", fields, new Object[]{"E3", null}, new Object[]{"E3", 5L});
        env.assertListenerNotInvoked("s3");
        env.assertPropsOld("create", fields, new Object[]{"E3", 5L});
        env.assertListenerInvoked("delete");
        env.assertPropsPerRowIterator("create", fields, null);

        env.undeployModuleContaining("delete");
        env.undeployModuleContaining("s0");
    }

    private static void tryAssertionBeanBacked(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        // Test create from class
        env.compileDeploy(eventRepresentationEnum.getAnnotationText() + " @name('create') @public create window MyWindowBB#keepall as SupportBean", path);
        env.addListener("create");
        env.compileDeploy("@public insert into MyWindowBB select * from SupportBean", path);

        env.compileDeploy("@name('s0') select * from MyWindowBB", path).addListener("s0");
        assertStatelessStmt(env, "s0", true);

        env.sendEventBean(new SupportBean());
        env.assertEventNew("create", event -> assertEvent(event, "MyWindowBB"));
        env.assertListener("s0", listener -> assertEvent(listener.assertOneGetNewAndReset(), "MyWindowBB"));

        env.compileDeploy("@name('update') on SupportBean_A update MyWindowBB set theString='s'", path).addListener("update");
        env.sendEventBean(new SupportBean_A("A1"));
        env.assertListener("update", listener -> assertEvent(listener.getLastNewData()[0], "MyWindowBB"));

        // test bean-property
        env.undeployAll();
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        env.sendEventBean(bean);
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void assertEvent(EventBean theEvent, String name) {
        assertTrue(theEvent.getEventType() instanceof BeanEventType);
        assertTrue(theEvent.getUnderlying() instanceof SupportBean);
        assertEquals(EventTypeTypeClass.NAMED_WINDOW, theEvent.getEventType().getMetadata().getTypeClass());
        assertEquals(name, theEvent.getEventType().getName());
    }

    public static Schema getSupportBeanS0Schema() {
        return record("SupportBean_S0").fields().requiredString("p00").endRecord();
    }
}
