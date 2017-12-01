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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexAssertionEventSend;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDW;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDWExceptionFactory;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDWFactory;
import com.espertech.esper.supportregression.virtualdw.SupportVirtualDWInvalidFactory;
import junit.framework.TestCase;

import javax.naming.NamingException;
import java.util.*;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecClientVirtualDataWindow implements RegressionExecution, IndexBackingTableInfo {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWFactory.class.getName());
        configuration.addPlugInVirtualDataWindow("invalid", "invalid", TestCase.class.getName());
        configuration.addPlugInVirtualDataWindow("test", "testnoindex", SupportVirtualDWInvalidFactory.class.getName());
        configuration.addPlugInVirtualDataWindow("test", "exceptionvdw", SupportVirtualDWExceptionFactory.class.getName());
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        configuration.addEventType("SupportBeanRange", SupportBeanRange.class);
        SupportQueryPlanIndexHook.reset();
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInsertConsume(epService);
        runAssertionOnMerge(epService);
        runAssertionLimitation(epService);
        runAssertionJoinAndLifecyle(epService);
        runAssertionSubquery(epService);
        runAssertionContextWJoin(epService);
        runAssertionContextWSubquery(epService);
        runAssertionFireAndForget(epService);
        runAssertionOnDelete(epService);
        runAssertionInvalid(epService);
        runAssertionManagementEvents(epService);
        runAssertionIndexChoicesJoinUniqueVirtualDW(epService);
    }

    private void runAssertionInsertConsume(EPServiceProvider epService) {

        epService.getEPAdministrator().createEPL("create window MyVDW.test:vdw() as SupportBean");
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(epService, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        window.setData(Collections.singleton(supportBean));
        epService.getEPAdministrator().createEPL("insert into MyVDW select * from SupportBean");

        // test straight consume
        String[] fields = "theString,intPrimitive".split(",");
        EPStatement stmtConsume = epService.getEPAdministrator().createEPL("select irstream * from MyVDW");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtConsume.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 200));
        assertNull(listener.getLastOldData());
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], fields, new Object[]{"E1", 200});
        stmtConsume.destroy();

        // test aggregated consumer - wherein the virtual data window does not return an iterator that prefills the aggregation state
        fields = "val0".split(",");
        EPStatement stmtAggregate = epService.getEPAdministrator().createEPL("select sum(intPrimitive) as val0 from MyVDW");
        stmtAggregate.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{150});

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionOnMerge(EPServiceProvider epService) {
        // defined test type
        Map<String, Object> mapType = new HashMap<>();
        mapType.put("col1", "string");
        mapType.put("col2", "string");
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", mapType);

        epService.getEPAdministrator().createEPL("create window MyVDW.test:vdw() as MapType");

        // define some test data to return, via lookup
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(epService, "/virtualdw/MyVDW");
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("col1", "key1");
        mapData.put("col2", "key2");
        window.setData(Collections.singleton(mapData));

        String[] fieldsMerge = "col1,col2".split(",");
        EPStatement stmtMerge = epService.getEPAdministrator().createEPL("on SupportBean sb merge MyVDW vdw " +
                "where col1 = theString " +
                "when matched then update set col2 = 'xxx'" +
                "when not matched then insert select theString as col1, 'abc' as col2");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtMerge.addListener(listener);
        SupportUpdateListener listenerConsume = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from MyVDW").addListener(listenerConsume);

        // try yes-matched case
        epService.getEPRuntime().sendEvent(new SupportBean("key1", 2));
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fieldsMerge, new Object[]{"key1", "key2"});
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], fieldsMerge, new Object[]{"key1", "xxx"});
        EPAssertionUtil.assertProps(window.getLastUpdateOld()[0], fieldsMerge, new Object[]{"key1", "key2"});
        EPAssertionUtil.assertProps(window.getLastUpdateNew()[0], fieldsMerge, new Object[]{"key1", "xxx"});
        EPAssertionUtil.assertProps(listenerConsume.assertOneGetNewAndReset(), fieldsMerge, new Object[]{"key1", "xxx"});

        // try not-matched case
        epService.getEPRuntime().sendEvent(new SupportBean("key2", 3));
        assertNull(listener.getLastOldData());
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], fieldsMerge, new Object[]{"key2", "abc"});
        EPAssertionUtil.assertProps(listenerConsume.assertOneGetNewAndReset(), fieldsMerge, new Object[]{"key2", "abc"});
        assertNull(window.getLastUpdateOld());
        EPAssertionUtil.assertProps(window.getLastUpdateNew()[0], fieldsMerge, new Object[]{"key2", "abc"});

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionLimitation(EPServiceProvider epService) {
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyVDW.test:vdw() as SupportBean");
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(epService, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        window.setData(Collections.singleton(supportBean));
        epService.getEPAdministrator().createEPL("insert into MyVDW select * from SupportBean");

        // cannot iterate named window
        assertFalse(stmtWindow.iterator().hasNext());

        // test data window aggregation (rows not included in aggregation)
        EPStatement stmtAggregate = epService.getEPAdministrator().createEPL("select window(theString) as val0 from MyVDW");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtAggregate.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, (String[]) listener.assertOneGetNewAndReset().get("val0"));

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionJoinAndLifecyle(EPServiceProvider epService) {

        EPStatement stmt = epService.getEPAdministrator().createEPL("create window MyVDW.test:vdw(1, 'abc') as SupportBean");

        // define some test data to return, via lookup
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(epService, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 100);
        supportBean.setLongPrimitive(50);
        window.setData(Collections.singleton(supportBean));

        assertNotNull(window.getContext().getEventFactory());
        assertEquals("MyVDW", window.getContext().getEventType().getName());
        assertNotNull(window.getContext().getStatementContext());
        assertEquals(2, window.getContext().getParameters().length);
        assertEquals(1, window.getContext().getParameters()[0]);
        assertEquals("abc", window.getContext().getParameters()[1]);
        assertEquals("MyVDW", window.getContext().getNamedWindowName());

        // test no-criteria join
        String[] fields = "st0.id,vdw.theString,vdw.intPrimitive".split(",");
        EPStatement stmtJoinAll = epService.getEPAdministrator().createEPL("select * from MyVDW vdw, SupportBean_ST0#lastevent st0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtJoinAll.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "", "");

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "S1", 100});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{}, window.getLastAccessKeys());
        stmtJoinAll.destroy();

        // test single-criteria join
        EPStatement stmtJoinSingle = epService.getEPAdministrator().createEPL("select * from MyVDW vdw, SupportBean_ST0#lastevent st0 where vdw.theString = st0.id");
        stmtJoinSingle.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "theString=(String)", "");

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, window.getLastAccessKeys());
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("S1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S1", "S1", 100});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1"}, window.getLastAccessKeys());
        stmtJoinSingle.destroy();

        // test multi-criteria join
        EPStatement stmtJoinMulti = epService.getEPAdministrator().createEPL("select vdw.theString from MyVDW vdw, SupportBeanRange#lastevent st0 " +
                "where vdw.theString = st0.id and longPrimitive = keyLong and intPrimitive between rangeStart and rangeEnd");
        stmtJoinMulti.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "theString=(String)|longPrimitive=(Long)", "intPrimitive[,](Integer)");

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeKeyLong("S1", 50L, 80, 120));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "vdw.theString".split(","), new Object[]{"S1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"S1", 50L, new VirtualDataWindowKeyRange(80, 120)}, window.getLastAccessKeys());

        // destroy
        stmt.destroy();
        assertNull(getFromContext(epService, "/virtualdw/MyVDW"));
        assertTrue(window.isDestroyed());

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionSubquery(EPServiceProvider epService) {

        SupportVirtualDW window = registerTypeSetMapData(epService);

        // test no-criteria subquery
        EPStatement stmtSubqueryAll = epService.getEPAdministrator().createEPL("select (select col1 from MyVDW vdw) from SupportBean_ST0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSubqueryAll.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "", "");

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{}, window.getLastAccessKeys());
        stmtSubqueryAll.destroy();

        // test single-criteria subquery
        EPStatement stmtSubqSingleKey = epService.getEPAdministrator().createEPL("select (select col1 from MyVDW vdw where col1=st0.id) as val0 from SupportBean_ST0 st0");
        stmtSubqSingleKey.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "col1=(String)", "");

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{null});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, window.getLastAccessKeys());
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("key1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1"}, window.getLastAccessKeys());
        stmtSubqSingleKey.destroy();

        // test multi-criteria subquery
        EPStatement stmtSubqMultiKey = epService.getEPAdministrator().createEPL("select " +
                "(select col1 from MyVDW vdw where col1=r.id and col2=r.key and col3 between r.rangeStart and r.rangeEnd) as val0 " +
                "from SupportBeanRange r");
        stmtSubqMultiKey.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "col1=(String)|col2=(String)", "col3[,](Integer)");

        epService.getEPRuntime().sendEvent(new SupportBeanRange("key1", "key2", 5, 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1", "key2", new VirtualDataWindowKeyRange(5, 10)}, window.getLastAccessKeys());
        stmtSubqMultiKey.destroy();

        // test aggregation
        epService.getEPAdministrator().createEPL("create schema SampleEvent as (id string)");
        epService.getEPAdministrator().createEPL("create window MySampleWindow.test:vdw() as SampleEvent");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select (select count(*) as cnt from MySampleWindow) as c0 "
                + "from SupportBean ste");
        stmt.addListener(listener);

        SupportVirtualDW thewindow = (SupportVirtualDW) getFromContext(epService, "/virtualdw/MySampleWindow");
        Map<String, Object> row1 = Collections.singletonMap("id", "V1");
        thewindow.setData(Collections.singleton(row1));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(1L, listener.assertOneGetNewAndReset().get("c0"));

        Set rows = new HashSet();
        rows.add(row1);
        rows.add(Collections.<String, Object>singletonMap("id", "V2"));
        thewindow.setData(rows);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals(2L, listener.assertOneGetNewAndReset().get("c0"));

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionContextWJoin(EPServiceProvider epService) {
        SupportVirtualDW.setInitializationData(Collections.singleton(new SupportBean("E1", 1)));

        // prepare
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create context MyContext coalesce by " +
                "consistent_hash_crc32(theString) from SupportBean, " +
                "consistent_hash_crc32(p00) from SupportBean_S0 " +
                "granularity 4 preallocate");
        epService.getEPAdministrator().createEPL("context MyContext create window MyWindow.test:vdw() as SupportBean");

        // join
        String eplSubquerySameCtx = "context MyContext "
                + "select * from SupportBean_S0 as s0 unidirectional, MyWindow as mw where mw.theString = s0.p00";
        EPStatement stmtSameCtx = epService.getEPAdministrator().createEPL(eplSubquerySameCtx);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSameCtx.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertTrue(listener.isInvoked());

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionContextWSubquery(EPServiceProvider epService) {
        SupportVirtualDW.setInitializationData(Collections.singleton(new SupportBean("E1", 1)));

        // prepare
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().createEPL("create context MyContext coalesce by " +
                "consistent_hash_crc32(theString) from SupportBean, " +
                "consistent_hash_crc32(p00) from SupportBean_S0 " +
                "granularity 4 preallocate");
        epService.getEPAdministrator().createEPL("context MyContext create window MyWindow.test:vdw() as SupportBean");

        // subquery - same context
        String eplSubquerySameCtx = "context MyContext "
                + "select (select intPrimitive from MyWindow mw where mw.theString = s0.p00) as c0 "
                + "from SupportBean_S0 s0";
        EPStatement stmtSameCtx = epService.getEPAdministrator().createEPL(eplSubquerySameCtx);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSameCtx.addListener(listener);
        epService.getEPAdministrator().createEPL("@Hint('disable_window_subquery_indexshare') " + eplSubquerySameCtx);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        assertEquals(1, listener.assertOneGetNewAndReset().get("c0"));
        stmtSameCtx.destroy();

        // subquery - no context
        String eplSubqueryNoCtx = "select (select intPrimitive from MyWindow mw where mw.theString = s0.p00) as c0 "
                + "from SupportBean_S0 s0";
        try {
            epService.getEPAdministrator().createEPL(eplSubqueryNoCtx);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to plan subquery number 1 querying MyWindow: Mismatch in context specification, the context for the named window 'MyWindow' is 'MyContext' and the query specifies no context  [select (select intPrimitive from MyWindow mw where mw.theString = s0.p00) as c0 from SupportBean_S0 s0]", ex.getMessage());
        }

        SupportVirtualDW.setInitializationData(null);
        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionFireAndForget(EPServiceProvider epService) {

        SupportVirtualDW window = registerTypeSetMapData(epService);

        // test no-criteria FAF
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select col1 from MyVDW vdw");
        assertIndexSpec(window.getLastRequestedIndex(), "", "");
        assertEquals("MyVDW", window.getLastRequestedIndex().getNamedWindowName());
        assertEquals(-1, window.getLastRequestedIndex().getStatementId());
        assertNull(window.getLastRequestedIndex().getStatementName());
        assertNotNull(window.getLastRequestedIndex().getStatementAnnotations());
        assertTrue(window.getLastRequestedIndex().isFireAndForget());
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[0], window.getLastAccessKeys());

        // test single-criteria FAF
        result = epService.getEPRuntime().executeQuery("select col1 from MyVDW vdw where col1='key1'");
        assertIndexSpec(window.getLastRequestedIndex(), "col1=(String)", "");
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1"}, window.getLastAccessKeys());

        // test multi-criteria subquery
        result = epService.getEPRuntime().executeQuery("select col1 from MyVDW vdw where col1='key1' and col2='key2' and col3 between 5 and 15");
        assertIndexSpec(window.getLastRequestedIndex(), "col1=(String)|col2=(String)", "col3[,](Double)");
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"key1", "key2", new VirtualDataWindowKeyRange(5d, 15d)}, window.getLastAccessKeys());

        // test multi-criteria subquery
        result = epService.getEPRuntime().executeQuery("select col1 from MyVDW vdw where col1='key1' and col2>'key0' and col3 between 5 and 15");
        assertIndexSpec(window.getLastRequestedIndex(), "col1=(String)", "col3[,](Double)|col2>(String)");
        EPAssertionUtil.assertProps(result.getArray()[0], "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"key1", new VirtualDataWindowKeyRange(5d, 15d), "key0"}, window.getLastAccessKeys());

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionOnDelete(EPServiceProvider epService) {
        SupportVirtualDW window = registerTypeSetMapData(epService);

        // test no-criteria on-delete
        EPStatement stmtOnDeleteAll = epService.getEPAdministrator().createEPL("on SupportBean_ST0 delete from MyVDW vdw");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOnDeleteAll.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "", "");

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{}, window.getLastAccessKeys());
        stmtOnDeleteAll.destroy();

        // test single-criteria on-delete
        EPStatement stmtOnDeleteSingleKey = epService.getEPAdministrator().createEPL("on SupportBean_ST0 st0 delete from MyVDW vdw where col1=st0.id");
        stmtOnDeleteSingleKey.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "col1=(String)", "");

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 0));
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"E1"}, window.getLastAccessKeys());
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("key1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1"}, window.getLastAccessKeys());
        stmtOnDeleteSingleKey.destroy();

        // test multie-criteria on-delete
        EPStatement stmtOnDeleteMultiKey = epService.getEPAdministrator().createEPL("@Name('ABC') on SupportBeanRange r delete " +
                "from MyVDW vdw where col1=r.id and col2=r.key and col3 between r.rangeStart and r.rangeEnd");
        stmtOnDeleteMultiKey.addListener(listener);
        assertIndexSpec(window.getLastRequestedIndex(), "col1=(String)|col2=(String)", "col3[,](Integer)");
        assertEquals("MyVDW", window.getLastRequestedIndex().getNamedWindowName());
        assertNotNull(window.getLastRequestedIndex().getStatementId());
        assertEquals("ABC", window.getLastRequestedIndex().getStatementName());
        assertEquals(1, window.getLastRequestedIndex().getStatementAnnotations().length);
        assertFalse(window.getLastRequestedIndex().isFireAndForget());

        epService.getEPRuntime().sendEvent(new SupportBeanRange("key1", "key2", 5, 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col1".split(","), new Object[]{"key1"});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{"key1", "key2", new VirtualDataWindowKeyRange(5, 10)}, window.getLastAccessKeys());

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epl = "create window ABC.invalid:invalid() as SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Virtual data window factory class junit.framework.TestCase does not implement the interface com.espertech.esper.client.hook.VirtualDataWindowFactory [create window ABC.invalid:invalid() as SupportBean]");

        epl = "select * from SupportBean.test:vdw()";
        tryInvalid(epService, epl, "Error starting statement: Virtual data window requires use with a named window in the create-window syntax [select * from SupportBean.test:vdw()]");

        epService.getEPAdministrator().createEPL("create window ABC.test:testnoindex() as SupportBean");
        epl = "select (select * from ABC) from SupportBean";
        tryInvalid(epService, epl, "Unexpected exception starting statement: Exception obtaining index lookup from virtual data window, the implementation has returned a null index [select (select * from ABC) from SupportBean]");

        try {
            epService.getEPAdministrator().createEPL("create window ABC.test:exceptionvdw() as SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Error attaching view to event stream: Validation exception initializing virtual data window 'ABC': This is a test exception [create window ABC.test:exceptionvdw() as SupportBean]", ex.getMessage());
        }
    }

    private void runAssertionManagementEvents(EPServiceProvider epService) {
        SupportVirtualDW vdw = registerTypeSetMapData(epService);

        // create-index event
        vdw.getEvents().clear();
        EPStatement stmtIndex = epService.getEPAdministrator().createEPL("create index IndexOne on MyVDW (col3, col2 btree)");
        VirtualDataWindowEventStartIndex startEvent = (VirtualDataWindowEventStartIndex) vdw.getEvents().get(0);
        assertEquals("MyVDW", startEvent.getNamedWindowName());
        assertEquals("IndexOne", startEvent.getIndexName());
        assertEquals(2, startEvent.getFields().size());
        assertEquals("col3", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(startEvent.getFields().get(0).getExpressions().get(0)));
        assertEquals("hash", startEvent.getFields().get(0).getType());
        assertEquals("col2", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(startEvent.getFields().get(1).getExpressions().get(0)));
        assertEquals("btree", startEvent.getFields().get(1).getType());
        assertFalse(startEvent.isUnique());

        // stop-index event
        vdw.getEvents().clear();
        stmtIndex.stop();
        VirtualDataWindowEventStopIndex stopEvent = (VirtualDataWindowEventStopIndex) vdw.getEvents().get(0);
        assertEquals("MyVDW", stopEvent.getNamedWindowName());
        assertEquals("IndexOne", stopEvent.getIndexName());

        // stop named window
        vdw.getEvents().clear();
        epService.getEPAdministrator().getStatement("create-nw").stop();
        VirtualDataWindowEventStopWindow stopWindow = (VirtualDataWindowEventStopWindow) vdw.getEvents().get(0);
        assertEquals("MyVDW", stopWindow.getNamedWindowName());

        // start named window (not an event but a new factory call)
        SupportVirtualDWFactory.getWindows().clear();
        SupportVirtualDWFactory.getInitializations().clear();
        epService.getEPAdministrator().getStatement("create-nw").start();
        assertEquals(1, SupportVirtualDWFactory.getWindows().size());
        assertEquals(1, SupportVirtualDWFactory.getInitializations().size());

        destroyStmtsRemoveTypes(epService);
    }

    private void runAssertionIndexChoicesJoinUniqueVirtualDW(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        // test no where clause with unique on multiple props, exact specification of where-clause
        IndexAssertionEventSend assertSendEvents = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "vdw.theString,vdw.intPrimitive,ssb1.i1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("S1", 1, 102, 103));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"S1", 101, 1});
            }
        };

        CaseEnum[] testCases = CaseEnum.values();
        for (CaseEnum caseEnum : testCases) {
            tryAssertionVirtualDW(epService, listener, caseEnum, "theString", "where vdw.theString = ssb1.s1", true, assertSendEvents);
            tryAssertionVirtualDW(epService, listener, caseEnum, "i1", "where vdw.theString = ssb1.s1", false, assertSendEvents);
            tryAssertionVirtualDW(epService, listener, caseEnum, "intPrimitive", "where vdw.theString = ssb1.s1", false, assertSendEvents);
            tryAssertionVirtualDW(epService, listener, caseEnum, "longPrimitive", "where vdw.longPrimitive = ssb1.l1", true, assertSendEvents);
            tryAssertionVirtualDW(epService, listener, caseEnum, "longPrimitive,theString", "where vdw.theString = ssb1.s1 and vdw.longPrimitive = ssb1.l1", true, assertSendEvents);
        }
    }

    private void tryAssertionVirtualDW(EPServiceProvider epService, SupportUpdateListener listener, CaseEnum caseEnum, String uniqueFields, String whereClause, boolean unique, IndexAssertionEventSend assertion) {
        SupportQueryPlanIndexHook.reset();
        SupportVirtualDWFactory.setUniqueKeys(new HashSet<>(Arrays.asList(uniqueFields.split(","))));
        epService.getEPAdministrator().createEPL("create window MyVDW.test:vdw() as SupportBean");
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(epService, "/virtualdw/MyVDW");
        SupportBean supportBean = new SupportBean("S1", 101);
        supportBean.setDoublePrimitive(102);
        supportBean.setLongPrimitive(103);
        window.setData(Collections.singleton(supportBean));

        String eplUnique = INDEX_CALLBACK_HOOK +
                "select * from ";

        if (caseEnum == CaseEnum.UNIDIRECTIONAL) {
            eplUnique += "SSB1 as ssb1 unidirectional ";
        } else {
            eplUnique += "SSB1#lastevent as ssb1 ";
        }
        eplUnique += ", MyVDW as vdw ";
        eplUnique += whereClause;

        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        stmtUnique.addListener(listener);

        // assert query plan
        SupportQueryPlanIndexHook.assertJoinOneStreamAndReset(unique);

        // run assertion
        assertion.run();

        destroyStmtsRemoveTypes(epService);
        SupportVirtualDWFactory.setUniqueKeys(null);
    }

    private static enum CaseEnum {
        UNIDIRECTIONAL,
        MULTIDIRECTIONAL,
    }

    private SupportVirtualDW registerTypeSetMapData(EPServiceProvider epService) {
        Map<String, Object> mapType = new HashMap<>();
        mapType.put("col1", "string");
        mapType.put("col2", "string");
        mapType.put("col3", "int");
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", mapType);

        SupportVirtualDWFactory.getInitializations().clear();
        epService.getEPAdministrator().createEPL("@Name('create-nw') create window MyVDW.test:vdw() as MapType");

        assertEquals(1, SupportVirtualDWFactory.getInitializations().size());
        VirtualDataWindowFactoryContext factoryContext = SupportVirtualDWFactory.getInitializations().get(0);
        assertNotNull(factoryContext.getEventFactory());
        assertEquals("MyVDW", factoryContext.getEventType().getName());
        assertNotNull("MyVDW", factoryContext.getNamedWindowName());
        assertEquals(0, factoryContext.getParameters().length);
        assertEquals(0, factoryContext.getParameterExpressions().length);
        assertNotNull(factoryContext.getViewFactoryContext());

        // define some test data to return, via lookup
        SupportVirtualDW window = (SupportVirtualDW) getFromContext(epService, "/virtualdw/MyVDW");
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

    private void destroyStmtsRemoveTypes(EPServiceProvider epService) {
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyVDW", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MapType", true);
    }

    private VirtualDataWindow getFromContext(EPServiceProvider epService, String name) {
        try {
            return (VirtualDataWindow) epService.getContext().lookup(name);
        } catch (NamingException e) {
            throw new RuntimeException("Name '" + name + "' could not be looked up");
        }
    }
}
