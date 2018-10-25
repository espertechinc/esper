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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.hook.vdw.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDataWindowLookupContextSPI;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDW;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDWForge;
import com.espertech.esper.regressionlib.support.util.IndexAssertionEventSend;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import org.junit.Assert;

import javax.naming.NamingException;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class ClientExtendVirtualDataWindow implements RegressionExecution, IndexBackingTableInfo {

    public void run(RegressionEnvironment env) {
        runAssertionInsertConsume(env);
        runAssertionOnMerge(env);
        runAssertionLimitation(env);
        runAssertionJoinAndLifecyle(env);
        runAssertionContextWJoin(env);
        runAssertionFireAndForget(env);
        runAssertionOnDelete(env);
        runAssertionInvalid(env);
        runAssertionManagementEvents(env);
        runAssertionIndexChoicesJoinUniqueVirtualDW(env);
        runAssertionLateConsume(env);
        runAssertionContextWSubquery(env);
        runAssertionSubquery(env);
        runAssertionLookupSPI(env);
    }

    private void runAssertionLateConsume(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyVDW.test:vdwwithparam() as SupportBean", path);
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        window.setData(Collections.singleton(supportBean));
        env.compileDeploy("insert into MyVDW select * from SupportBean", path);

        // test aggregated consumer - wherein the virtual data window does not return an iterator that prefills the aggregation state
        String[] fields = "val0".split(",");
        env.compileDeploy("@Name('s0') select sum(intPrimitive) as val0 from MyVDW", path).addListener("s0");
        EPAssertionUtil.assertProps(env.statement("s0").iterator().next(), fields, new Object[]{100});

        env.sendEventBean(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{110});

        env.sendEventBean(new SupportBean("E1", 20));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{130});

        // assert events received for add-consumer and remove-consumer
        env.undeployModuleContaining("s0");

        if (env.isHA()) {
            env.undeployAll();
            return;
        }

        VirtualDataWindowEventConsumerAdd addConsumerEvent = (VirtualDataWindowEventConsumerAdd) window.getEvents().get(0);
        VirtualDataWindowEventConsumerRemove removeConsumerEvent = (VirtualDataWindowEventConsumerRemove) window.getEvents().get(1);

        for (VirtualDataWindowEventConsumerBase base : new VirtualDataWindowEventConsumerBase[]{addConsumerEvent, removeConsumerEvent}) {
            Assert.assertEquals(-1, base.getAgentInstanceId());
            Assert.assertEquals("MyVDW", base.getNamedWindowName());
            Assert.assertEquals("s0", base.getStatementName());
        }
        assertSame(removeConsumerEvent.getConsumerObject(), addConsumerEvent.getConsumerObject());
        window.getEvents().clear();

        // test filter criteria passed to event
        env.compileDeploy("@Name('ABC') select sum(intPrimitive) as val0 from MyVDW(theString = 'A')", path);
        VirtualDataWindowEventConsumerAdd eventWithFilter = (VirtualDataWindowEventConsumerAdd) window.getEvents().get(0);
        assertNotNull(eventWithFilter.getFilter());
        assertNotNull(eventWithFilter.getExprEvaluatorContext());

        env.undeployAll();
    }

    private void runAssertionLookupSPI(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyVDW.test:vdwnoparam() as SupportBean", path);

        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("E1", 100);
        window.setData(Collections.singleton(supportBean));

        env.compileDeploy("@name('s0') select (select sum(intPrimitive) from MyVDW vdw where vdw.theString = s0.p00) from SupportBean_S0 s0", path);
        env.addListener("s0");
        VirtualDataWindowLookupContextSPI spiContext = (VirtualDataWindowLookupContextSPI) window.getLastRequestedLookup();
        assertNotNull(spiContext);

        env.undeployAll();
    }

    private void runAssertionInsertConsume(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        String[] fields;
        env.compileDeploy("create window MyVDW.test:vdw() as SupportBean", path);
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        window.setData(Collections.singleton(supportBean));
        env.compileDeploy("insert into MyVDW select * from SupportBean", path);

        // test straight consume
        fields = "theString,intPrimitive".split(",");
        env.compileDeploy("@name('s0') select irstream * from MyVDW", path).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 200));
        assertNull(env.listener("s0").getLastOldData());
        EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[0], fields, new Object[]{"E1", 200});
        env.undeployModuleContaining("s0");

        // test aggregated consumer - wherein the virtual data window does not return an iterator that prefills the aggregation state
        fields = "val0".split(",");
        env.compileDeploy("@name('s0') select sum(intPrimitive) as val0 from MyVDW", path).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 100));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{200});

        env.sendEventBean(new SupportBean("E1", 50));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{250});

        env.undeployAll();
    }

    private void runAssertionOnMerge(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyVDW.test:vdw() as MapType", path);

        // define some test data to return, via lookup
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("col1", "key1");
        mapData.put("col2", "key2");
        window.setData(Collections.singleton(mapData));

        String[] fieldsMerge = "col1,col2".split(",");
        env.compileDeploy("@name('s0') on SupportBean sb merge MyVDW vdw " +
            "where col1 = theString " +
            "when matched then update set col2 = 'xxx'" +
            "when not matched then insert select theString as col1, 'abc' as col2, 1 as col3", path).addListener("s0");
        env.compileDeploy("@name('consume') select * from MyVDW", path).addListener("consume");

        // try yes-matched case
        env.sendEventBean(new SupportBean("key1", 2));
        EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fieldsMerge, new Object[]{"key1", "key2"});
        EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[0], fieldsMerge, new Object[]{"key1", "xxx"});
        EPAssertionUtil.assertProps(window.getLastUpdateOld()[0], fieldsMerge, new Object[]{"key1", "key2"});
        EPAssertionUtil.assertProps(window.getLastUpdateNew()[0], fieldsMerge, new Object[]{"key1", "xxx"});
        EPAssertionUtil.assertProps(env.listener("consume").assertOneGetNewAndReset(), fieldsMerge, new Object[]{"key1", "xxx"});

        // try not-matched case
        env.sendEventBean(new SupportBean("key2", 3));
        assertNull(env.listener("s0").getLastOldData());
        EPAssertionUtil.assertProps(env.listener("s0").getAndResetLastNewData()[0], fieldsMerge, new Object[]{"key2", "abc"});
        EPAssertionUtil.assertProps(env.listener("consume").assertOneGetNewAndReset(), fieldsMerge, new Object[]{"key2", "abc"});
        assertNull(window.getLastUpdateOld());
        EPAssertionUtil.assertProps(window.getLastUpdateNew()[0], fieldsMerge, new Object[]{"key2", "abc"});

        env.undeployAll();
    }

    private void runAssertionLimitation(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('window') create window MyVDW.test:vdw() as SupportBean", path);
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        window.setData(Collections.singleton(supportBean));
        env.compileDeploy("insert into MyVDW select * from SupportBean", path);

        // cannot iterate named window
        assertFalse(env.iterator("window").hasNext());

        // test data window aggregation (rows not included in aggregation)
        env.compileDeploy("@name('s0') select window(theString) as val0 from MyVDW", path).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 100));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", "E1"}, (String[]) env.listener("s0").assertOneGetNewAndReset().get("val0"));

        env.undeployAll();
    }

    private void runAssertionJoinAndLifecyle(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyVDW.test:vdw(1, 'abc') as SupportBean", path);
        String[] fields = "st0.id,vdw.theString,vdw.intPrimitive".split(",");

        // define some test data to return, via lookup
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        supportBean.setLongPrimitive(50);
        window.setData(Collections.singleton(supportBean));

        assertNotNull(window.getContext().getEventFactory());
        Assert.assertEquals("MyVDW", window.getContext().getEventType().getName());
        assertNotNull(window.getContext().getStatementContext());
        Assert.assertEquals(2, window.getContext().getParameters().length);
        Assert.assertEquals(1, window.getContext().getParameters()[0]);
        Assert.assertEquals("abc", window.getContext().getParameters()[1]);
        Assert.assertEquals("MyVDW", window.getContext().getNamedWindowName());

        // test no-criteria join
        env.compileDeploy("@name('s0') select * from MyVDW vdw, SupportBean_ST0#lastevent st0", path).addListener("s0");
        assertIndexSpec(window.getLastRequestedLookup(), "", "");

        env.sendEventBean(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "S1", 100});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{}, window.getLastAccessKeys());
        env.undeployModuleContaining("s0");

        // test single-criteria join
        env.compileDeploy("@name('s0') select * from MyVDW vdw, SupportBean_ST0#lastevent st0 where vdw.theString = st0.id", path).addListener("s0");
        assertIndexSpec(window.getRequestedLookups().get(1), "theString=(String)", "");

        env.sendEventBean(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, window.getLastAccessKeys());
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportBean_ST0("S1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S1", "S1", 100});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1"}, window.getLastAccessKeys());
        env.undeployModuleContaining("s0");

        // test multi-criteria join
        env.compileDeploy("@name('s0') select vdw.theString from MyVDW vdw, SupportBeanRange#lastevent st0 " +
            "where vdw.theString = st0.id and longPrimitive = keyLong and intPrimitive between rangeStart and rangeEnd", path);
        env.addListener("s0");
        assertIndexSpec(window.getRequestedLookups().get(1), "theString=(String)|longPrimitive=(Long)", "intPrimitive[,](Integer)");

        env.sendEventBean(SupportBeanRange.makeKeyLong("S1", 50L, 80, 120));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "vdw.theString".split(","), new Object[]{"S1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", 50L, new VirtualDataWindowKeyRange(80, 120)}, window.getLastAccessKeys());

        // destroy
        env.undeployAll();
        assertNull(getFromContext(env, "/virtualdw/MyVDW"));
        assertTrue(window.isDestroyed());

        env.undeployAll();
    }

    private void runAssertionSubquery(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        SupportVirtualDW window = registerTypeSetMapData(env, path);

        // test no-criteria subquery
        env.compileDeploy("@name('s0') select (select col1 from MyVDW vdw) from SupportBean_ST0", path).addListener("s0");
        assertIndexSpec(window.getLastRequestedLookup(), "", "");

        env.sendEventBean(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{}, window.getLastAccessKeys());
        env.undeployModuleContaining("s0");

        // test single-criteria subquery
        env.compileDeploy("@name('s0') select (select col1 from MyVDW vdw where col1=st0.id) as val0 from SupportBean_ST0 st0", path).addListener("s0");
        assertIndexSpec(window.getLastRequestedLookup(), "col1=(String)", "");

        env.sendEventBean(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{null});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, window.getLastAccessKeys());
        env.sendEventBean(new SupportBean_ST0("key1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1"}, window.getLastAccessKeys());
        env.undeployModuleContaining("s0");

        // test multi-criteria subquery
        env.compileDeploy("@name('s0') select " +
            "(select col1 from MyVDW vdw where col1=r.id and col2=r.key and col3 between r.rangeStart and r.rangeEnd) as val0 " +
            "from SupportBeanRange r", path).addListener("s0");
        assertIndexSpec(window.getLastRequestedLookup(), "col1=(String)|col2=(String)", "col3[,](Integer)");

        env.sendEventBean(new SupportBeanRange("key1", "key2", 5, 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1", "key2", new VirtualDataWindowKeyRange(5, 10)}, window.getLastAccessKeys());
        env.undeployModuleContaining("s0");

        // test aggregation
        env.compileDeploy("create schema SampleEvent as (id string)", path);
        env.compileDeploy("create window MySampleWindow.test:vdw() as SampleEvent", path);
        env.compileDeploy("@name('s0') select (select count(*) as cnt from MySampleWindow) as c0 "
            + "from SupportBean ste", path).addListener("s0");

        SupportVirtualDW thewindow = (SupportVirtualDW) getFromContext(env, "/virtualdw/MySampleWindow");
        Map<String, Object> row1 = Collections.singletonMap("id", "V1");
        thewindow.setData(Collections.singleton(row1));

        env.sendEventBean(new SupportBean("E1", 1));
        Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("c0"));

        Set rows = new HashSet();
        rows.add(row1);
        rows.add(Collections.<String, Object>singletonMap("id", "V2"));
        thewindow.setData(rows);

        env.sendEventBean(new SupportBean("E2", 2));
        Assert.assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("c0"));

        env.undeployAll();
    }

    private void runAssertionContextWJoin(RegressionEnvironment env) {
        SupportVirtualDW.setInitializationData(Collections.singleton(new SupportBean("E1", 1)));
        RegressionPath path = new RegressionPath();

        // prepare
        env.compileDeploy("create context MyContext coalesce by " +
            "consistent_hash_crc32(theString) from SupportBean, " +
            "consistent_hash_crc32(p00) from SupportBean_S0 " +
            "granularity 4 preallocate", path);
        env.compileDeploy("context MyContext create window MyWindow.test:vdw() as SupportBean", path);

        // join
        String eplSubquerySameCtx = "@name('s0') context MyContext "
            + "select * from SupportBean_S0 as s0 unidirectional, MyWindow as mw where mw.theString = s0.p00";
        env.compileDeploy(eplSubquerySameCtx, path).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1, "E1"));
        assertTrue(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private void runAssertionContextWSubquery(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        SupportVirtualDW.setInitializationData(Collections.singleton(new SupportBean("E1", 1)));

        env.compileDeploy("create context MyContext coalesce by " +
            "consistent_hash_crc32(theString) from SupportBean, " +
            "consistent_hash_crc32(p00) from SupportBean_S0 " +
            "granularity 4 preallocate", path);
        env.compileDeploy("context MyContext create window MyWindow.test:vdw() as SupportBean", path);

        // subquery - same context
        String eplSubquerySameCtx = "context MyContext "
            + "select (select intPrimitive from MyWindow mw where mw.theString = s0.p00) as c0 "
            + "from SupportBean_S0 s0";
        env.compileDeploy("@name('s0') " + eplSubquerySameCtx, path).addListener("s0");
        env.compileDeploy("@Hint('disable_window_subquery_indexshare') @name('s1') " + eplSubquerySameCtx, path);

        env.sendEventBean(new SupportBean_S0(0, "E1"));
        Assert.assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        env.undeployModuleContaining("s0");

        // subquery - no context
        String eplSubqueryNoCtx = "select (select intPrimitive from MyWindow mw where mw.theString = s0.p00) as c0 "
            + "from SupportBean_S0 s0";
        tryInvalidCompile(env, path, eplSubqueryNoCtx,
            "Failed to plan subquery number 1 querying MyWindow: Mismatch in context specification, the context for the named window 'MyWindow' is 'MyContext' and the query specifies no context  [select (select intPrimitive from MyWindow mw where mw.theString = s0.p00) as c0 from SupportBean_S0 s0]");

        SupportVirtualDW.setInitializationData(null);
        env.undeployAll();
    }

    private void runAssertionFireAndForget(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        SupportVirtualDW window = registerTypeSetMapData(env, path);

        // test no-criteria FAF
        EPFireAndForgetQueryResult result = env.compileExecuteFAF("select col1 from MyVDW vdw", path);
        assertIndexSpec(window.getLastRequestedLookup(), "", "");
        Assert.assertEquals("MyVDW", window.getLastRequestedLookup().getNamedWindowName());
        Assert.assertEquals(-1, window.getLastRequestedLookup().getStatementId());
        assertNull(window.getLastRequestedLookup().getStatementName());
        assertNotNull(window.getLastRequestedLookup().getStatementAnnotations());
        assertTrue(window.getLastRequestedLookup().isFireAndForget());
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[0], window.getLastAccessKeys());

        // test single-criteria FAF
        result = env.compileExecuteFAF("select col1 from MyVDW vdw where col1='key1'", path);
        assertIndexSpec(window.getLastRequestedLookup(), "col1=(String)", "");
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1"}, window.getLastAccessKeys());

        // test multi-criteria subquery
        result = env.compileExecuteFAF("select col1 from MyVDW vdw where col1='key1' and col2='key2' and col3 between 5 and 15", path);
        assertIndexSpec(window.getLastRequestedLookup(), "col1=(String)|col2=(String)", "col3[,](Double)");
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"key1", "key2", new VirtualDataWindowKeyRange(5d, 15d)}, window.getLastAccessKeys());

        // test multi-criteria subquery
        result = env.compileExecuteFAF("select col1 from MyVDW vdw where col1='key1' and col2>'key0' and col3 between 5 and 15", path);
        assertIndexSpec(window.getLastRequestedLookup(), "col1=(String)", "col3[,](Double)|col2>(String)");
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"key1", new VirtualDataWindowKeyRange(5d, 15d), "key0"}, window.getLastAccessKeys());

        env.undeployAll();
    }

    private void runAssertionOnDelete(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        SupportVirtualDW window = registerTypeSetMapData(env, path);

        // test no-criteria on-delete
        env.compileDeploy("@name('s0') on SupportBean_ST0 delete from MyVDW vdw", path).addListener("s0");
        assertIndexSpec(window.getLastRequestedLookup(), "", "");

        env.sendEventBean(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{}, window.getLastAccessKeys());
        env.undeployModuleContaining("s0");

        // test single-criteria on-delete
        env.compileDeploy("@name('s0') on SupportBean_ST0 st0 delete from MyVDW vdw where col1=st0.id", path).addListener("s0");
        assertIndexSpec(window.getLastRequestedLookup(), "col1=(String)", "");

        env.sendEventBean(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, window.getLastAccessKeys());
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportBean_ST0("key1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1"}, window.getLastAccessKeys());
        env.undeployModuleContaining("s0");

        // test multie-criteria on-delete
        env.compileDeploy("@Name('s0') on SupportBeanRange r delete " +
            "from MyVDW vdw where col1=r.id and col2=r.key and col3 between r.rangeStart and r.rangeEnd", path).addListener("s0");
        assertIndexSpec(window.getLastRequestedLookup(), "col1=(String)|col2=(String)", "col3[,](Integer)");
        Assert.assertEquals("MyVDW", window.getLastRequestedLookup().getNamedWindowName());
        assertNotNull(window.getLastRequestedLookup().getStatementId());
        Assert.assertEquals("s0", window.getLastRequestedLookup().getStatementName());
        Assert.assertEquals(1, window.getLastRequestedLookup().getStatementAnnotations().length);
        assertFalse(window.getLastRequestedLookup().isFireAndForget());

        env.sendEventBean(new SupportBeanRange("key1", "key2", 5, 10));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1", "key2", new VirtualDataWindowKeyRange(5, 10)}, window.getLastAccessKeys());

        env.undeployAll();
    }

    private void runAssertionInvalid(RegressionEnvironment env) {
        String epl;

        epl = "create window ABC.invalid:invalid() as SupportBean";
        tryInvalidCompile(env, epl, "Failed to validate data window declaration: Virtual data window forge class " + SupportBean.class.getName() + " does not implement the interface " + VirtualDataWindowForge.class.getName());

        epl = "select * from SupportBean.test:vdw()";
        tryInvalidCompile(env, epl, "Failed to validate data window declaration: Virtual data window requires use with a named window in the create-window syntax [select * from SupportBean.test:vdw()]");

        tryInvalidCompile(env, "create window ABC.test:exceptionvdw() as SupportBean",
            "Failed to validate data window declaration: Validation exception initializing virtual data window 'ABC': This is a test exception [create window ABC.test:exceptionvdw() as SupportBean]");
    }

    private void runAssertionManagementEvents(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        SupportVirtualDW vdw = registerTypeSetMapData(env, path);

        // create-index event
        vdw.getEvents().clear();
        env.compileDeploy("@name('idx') create index IndexOne on MyVDW (col3, col2 btree)", path);
        VirtualDataWindowEventStartIndex startEvent = (VirtualDataWindowEventStartIndex) vdw.getEvents().get(0);
        Assert.assertEquals("MyVDW", startEvent.getNamedWindowName());
        Assert.assertEquals("IndexOne", startEvent.getIndexName());
        Assert.assertEquals(2, startEvent.getFields().size());
        Assert.assertEquals("col3", startEvent.getFields().get(0).getName());
        Assert.assertEquals("hash", startEvent.getFields().get(0).getType());
        Assert.assertEquals("col2", startEvent.getFields().get(1).getName());
        Assert.assertEquals("btree", startEvent.getFields().get(1).getType());
        assertFalse(startEvent.isUnique());

        // stop-index event
        vdw.getEvents().clear();
        env.undeployModuleContaining("idx");
        VirtualDataWindowEventStopIndex stopEvent = (VirtualDataWindowEventStopIndex) vdw.getEvents().get(0);
        Assert.assertEquals("MyVDW", stopEvent.getNamedWindowName());
        Assert.assertEquals("IndexOne", stopEvent.getIndexName());

        // stop named window
        vdw.getEvents().clear();
        env.undeployAll();
        VirtualDataWindowEventStopWindow stopWindow = (VirtualDataWindowEventStopWindow) vdw.getEvents().get(0);
        Assert.assertEquals("MyVDW", stopWindow.getNamedWindowName());
    }

    private void runAssertionIndexChoicesJoinUniqueVirtualDW(RegressionEnvironment env) {

        // test no where clause with unique on multiple props, exact specification of where-clause
        IndexAssertionEventSend assertSendEvents = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "vdw.theString,vdw.intPrimitive,ssb1.i1".split(",");
                env.sendEventBean(new SupportSimpleBeanOne("S1", 1, 102, 103));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S1", 101, 1});
            }
        };

        CaseEnum[] testCases = CaseEnum.values();
        for (CaseEnum caseEnum : testCases) {
            tryAssertionVirtualDW(env, caseEnum, "theString", "where vdw.theString = ssb1.s1", true, assertSendEvents);
            tryAssertionVirtualDW(env, caseEnum, "i1", "where vdw.theString = ssb1.s1", false, assertSendEvents);
            tryAssertionVirtualDW(env, caseEnum, "intPrimitive", "where vdw.theString = ssb1.s1", false, assertSendEvents);
            tryAssertionVirtualDW(env, caseEnum, "longPrimitive", "where vdw.longPrimitive = ssb1.l1", true, assertSendEvents);
            tryAssertionVirtualDW(env, caseEnum, "longPrimitive,theString", "where vdw.theString = ssb1.s1 and vdw.longPrimitive = ssb1.l1", true, assertSendEvents);
        }
    }

    private void tryAssertionVirtualDW(RegressionEnvironment env, CaseEnum caseEnum, String uniqueFields, String whereClause, boolean unique, IndexAssertionEventSend assertion) {
        SupportQueryPlanIndexHook.reset();
        SupportVirtualDWForge.setUniqueKeys(new HashSet<>(Arrays.asList(uniqueFields.split(","))));

        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyVDW.test:vdw() as SupportBean", path);
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 101);
        supportBean.setDoublePrimitive(102);
        supportBean.setLongPrimitive(103);
        window.setData(Collections.singleton(supportBean));

        String eplUnique = IndexBackingTableInfo.INDEX_CALLBACK_HOOK +
            "@name('s0') select * from ";

        if (caseEnum == CaseEnum.UNIDIRECTIONAL) {
            eplUnique += "SupportSimpleBeanOne as ssb1 unidirectional ";
        } else {
            eplUnique += "SupportSimpleBeanOne#lastevent as ssb1 ";
        }
        eplUnique += ", MyVDW as vdw ";
        eplUnique += whereClause;

        env.compileDeploy(eplUnique, path).addListener("s0");

        // assert query plan
        SupportQueryPlanIndexHook.assertJoinOneStreamAndReset(unique);

        // run assertion
        assertion.run();

        env.undeployAll();
        SupportVirtualDWForge.setUniqueKeys(null);
    }

    private static enum CaseEnum {
        UNIDIRECTIONAL,
        MULTIDIRECTIONAL,
    }

    private SupportVirtualDW registerTypeSetMapData(RegressionEnvironment env, RegressionPath path) {
        SupportVirtualDWForge.getInitializations().clear();
        env.compileDeploy("@Name('create-nw') create window MyVDW.test:vdw() as MapType", path);

        Assert.assertEquals(1, SupportVirtualDWForge.getInitializations().size());
        VirtualDataWindowForgeContext forgeContext = SupportVirtualDWForge.getInitializations().get(0);
        Assert.assertEquals("MyVDW", forgeContext.getEventType().getName());
        assertNotNull("MyVDW", forgeContext.getNamedWindowName());
        Assert.assertEquals(0, forgeContext.getParameters().length);
        Assert.assertEquals(0, forgeContext.getParameterExpressions().length);
        assertNotNull(forgeContext.getViewForgeEnv());

        // define some test data to return, via lookup
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(env, "/virtualdw/MyVDW");
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("col1", "key1");
        mapData.put("col2", "key2");
        mapData.put("col3", 10);
        window.setData(Collections.singleton(mapData));

        return window;
    }

    private void assertIndexSpec(VirtualDataWindowLookupContext indexSpec, String hashfields, String btreefields) {
        assertIndexFields(hashfields, indexSpec.getHashFields());
        assertIndexFields(btreefields, indexSpec.getBtreeFields());
    }

    private void assertIndexFields(String hashfields, List<VirtualDataWindowLookupFieldDesc> fields) {
        if (hashfields.isEmpty() && fields.isEmpty()) {
            return;
        }
        String[] split = hashfields.split("\\|");
        List<String> found = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            VirtualDataWindowLookupFieldDesc field = fields.get(i);
            String result = field.getPropertyName() + field.getOperator().getOp() + "(" + field.getLookupValueType().getSimpleName() + ")";
            found.add(result);
        }
        EPAssertionUtil.assertEqualsAnyOrder(split, found.toArray());
    }

    private VirtualDataWindow getFromContext(RegressionEnvironment env, String name) {
        try {
            return (VirtualDataWindow) env.runtime().getContext().lookup(name);
        } catch (NamingException e) {
            throw new RuntimeException("Name '" + name + "' could not be looked up");
        }
    }
}
