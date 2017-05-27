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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecNWTableInfraCreateIndex implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);

        runAssertionMultiRangeAndKey(epService, true);
        runAssertionMultiRangeAndKey(epService, false);

        runAssertionHashBTreeWidening(epService, true);
        runAssertionHashBTreeWidening(epService, false);

        runAssertionWidening(epService, true);
        runAssertionWidening(epService, false);

        runAssertionCompositeIndex(epService, true);
        runAssertionCompositeIndex(epService, false);

        runAssertionIndexReferences(epService, true);
        runAssertionIndexReferences(epService, false);

        runAssertionIndexStaleness(epService, true);
        runAssertionIndexStaleness(epService, false);

        runAssertionLateCreate(epService, true);
        runAssertionLateCreate(epService, false);

        runAssertionMultipleColumnMultipleIndex(epService, true);
        runAssertionMultipleColumnMultipleIndex(epService, false);

        runAssertionDropCreate(epService, true);
        runAssertionDropCreate(epService, false);

        runAssertionOnSelectReUse(epService, true);
        runAssertionOnSelectReUse(epService, false);

        runAssertionInvalid(epService, true);
        runAssertionInvalid(epService, false);
    }

    private void runAssertionInvalid(EPServiceProvider epService, boolean namedWindow) {
        String eplCreate = namedWindow ?
                "create window MyInfraOne#keepall as (f1 string, f2 int)" :
                "create table MyInfraOne as (f1 string primary key, f2 int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("create index MyInfraIndex on MyInfraOne(f1)");

        epService.getEPAdministrator().createEPL("create context ContextOne initiated by SupportBean terminated after 5 sec");
        epService.getEPAdministrator().createEPL("create context ContextTwo initiated by SupportBean terminated after 5 sec");
        String eplCreateWContext = namedWindow ?
                "context ContextOne create window MyInfraCtx#keepall as (f1 string, f2 int)" :
                "context ContextOne create table MyInfraCtx as (f1 string primary key, f2 int primary key)";
        epService.getEPAdministrator().createEPL(eplCreateWContext);

        SupportMessageAssertUtil.tryInvalid(epService, "create index MyInfraIndex on MyInfraOne(f1)",
                "Error starting statement: An index by name 'MyInfraIndex' already exists [");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyInfraOne(fx)",
                "Error starting statement: Property named 'fx' not found");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyInfraOne(f1, f1)",
                "Error starting statement: Property named 'f1' has been declared more then once [create index IndexTwo on MyInfraOne(f1, f1)]");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyWindowX(f1, f1)",
                "Error starting statement: A named window or table by name 'MyWindowX' does not exist [create index IndexTwo on MyWindowX(f1, f1)]");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyInfraOne(f1 bubu, f2)",
                "Error starting statement: Unrecognized advanced-type index 'bubu'");

        SupportMessageAssertUtil.tryInvalid(epService, "create gugu index IndexTwo on MyInfraOne(f2)",
                "Invalid keyword 'gugu' in create-index encountered, expected 'unique' [create gugu index IndexTwo on MyInfraOne(f2)]");

        SupportMessageAssertUtil.tryInvalid(epService, "create unique index IndexTwo on MyInfraOne(f2 btree)",
                "Error starting statement: Combination of unique index with btree (range) is not supported [create unique index IndexTwo on MyInfraOne(f2 btree)]");

        // invalid context
        SupportMessageAssertUtil.tryInvalid(epService, "create unique index IndexTwo on MyInfraCtx(f1)",
                "Error starting statement: " + (namedWindow ? "Named window" : "Table") + " by name 'MyInfraCtx' has been declared for context 'ContextOne' and can only be used within the same context");
        SupportMessageAssertUtil.tryInvalid(epService, "context ContextTwo create unique index IndexTwo on MyInfraCtx(f1)",
                "Error starting statement: " + (namedWindow ? "Named window" : "Table") + " by name 'MyInfraCtx' has been declared for context 'ContextOne' and can only be used within the same context");

        // invalid insert-into unique index
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String eplCreateTwo = namedWindow ?
                "@Name('create') create window MyInfraTwo#keepall as SupportBean" :
                "@Name('create') create table MyInfraTwo(theString string primary key, intPrimitive int primary key)";
        epService.getEPAdministrator().createEPL(eplCreateTwo);
        epService.getEPAdministrator().createEPL("@Name('insert') insert into MyInfraTwo select theString, intPrimitive from SupportBean");
        epService.getEPAdministrator().createEPL("create unique index I1 on MyInfraTwo(theString)");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        try {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
            fail();
        } catch (Exception ex) {
            String text = namedWindow ?
                    "Unexpected exception in statement 'create': Unique index violation, index 'I1' is a unique index and key 'E1' already exists" :
                    "java.lang.RuntimeException: Unexpected exception in statement 'insert': Unique index violation, index 'I1' is a unique index and key 'E1' already exists";
            assertEquals(text, ex.getMessage());
        }

        if (!namedWindow) {
            epService.getEPAdministrator().createEPL("create table MyTable (p0 string, sumint sum(int))");
            SupportMessageAssertUtil.tryInvalid(epService, "create index MyIndex on MyTable(p0)",
                    "Error starting statement: Tables without primary key column(s) do not allow creating an index [");
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraOne", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraTwo", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraCtx", false);
    }

    private void runAssertionOnSelectReUse(EPServiceProvider epService, boolean namedWindow) {
        String stmtTextCreateOne = namedWindow ?
                "create window MyInfraONR#keepall as (f1 string, f2 int)" :
                "create table MyInfraONR as (f1 string primary key, f2 int primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfraONR(f1, f2) select theString, intPrimitive from SupportBean");
        EPStatement indexOne = epService.getEPAdministrator().createEPL("create index MyInfraONRIndex1 on MyInfraONR(f2)");
        String[] fields = "f1,f2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        EPStatement stmtOnSelect = epService.getEPAdministrator().createEPL("on SupportBean_S0 s0 select nw.f1 as f1, nw.f2 as f2 from MyInfraONR nw where nw.f2 = s0.id");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOnSelect.addListener(listener);
        assertEquals(namedWindow ? 1 : 2, getIndexCount(epService, namedWindow, "MyInfraONR"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        indexOne.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        // create second identical statement
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("on SupportBean_S0 s0 select nw.f1 as f1, nw.f2 as f2 from MyInfraONR nw where nw.f2 = s0.id");
        assertEquals(namedWindow ? 1 : 2, getIndexCount(epService, namedWindow, "MyInfraONR"));

        stmtOnSelect.destroy();
        assertEquals(namedWindow ? 1 : 2, getIndexCount(epService, namedWindow, "MyInfraONR"));

        stmtTwo.destroy();
        assertEquals(namedWindow ? 0 : 1, getIndexCount(epService, namedWindow, "MyInfraONR"));

        // two-key index order test
        epService.getEPAdministrator().createEPL("create window MyInfraFour#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("create index idx1 on MyInfraFour (theString, intPrimitive)");
        epService.getEPAdministrator().createEPL("on SupportBean sb select * from MyInfraFour w where w.theString = sb.theString and w.intPrimitive = sb.intPrimitive");
        epService.getEPAdministrator().createEPL("on SupportBean sb select * from MyInfraFour w where w.intPrimitive = sb.intPrimitive and w.theString = sb.theString");
        assertEquals(1, getNamedWindowMgmtService(epService).getNamedWindowIndexes("MyInfraFour").length);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraONR", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraFour", false);
    }

    private void runAssertionDropCreate(EPServiceProvider epService, boolean namedWindow) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtTextCreateOne = namedWindow ?
                "create window MyInfraDC#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfraDC as (f1 string primary key, f2 int primary key, f3 string primary key, f4 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfraDC(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");
        EPStatement indexOne = epService.getEPAdministrator().createEPL("create index MyInfraDCIndex1 on MyInfraDC(f1)");
        EPStatement indexTwo = epService.getEPAdministrator().createEPL("create index MyInfraDCIndex2 on MyInfraDC(f4)");
        String[] fields = "f1,f2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));

        indexOne.destroy();

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfraDC where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraDC where f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        indexTwo.destroy();

        result = epService.getEPRuntime().executeQuery("select * from MyInfraDC where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraDC where f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        indexTwo = epService.getEPAdministrator().createEPL("create index MyInfraDCIndex2 on MyInfraDC(f4)");

        result = epService.getEPRuntime().executeQuery("select * from MyInfraDC where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraDC where f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        indexTwo.destroy();
        assertEquals(namedWindow ? 0 : 1, getIndexCount(epService, namedWindow, "MyInfraDC"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraDC", false);
    }

    private int getIndexCount(EPServiceProvider epService, boolean namedWindow, String name) {
        EventTableIndexRepository repo = getIndexInstanceRepo(epService, namedWindow, name);
        return repo.getIndexDescriptors().length;
    }

    private void runAssertionMultipleColumnMultipleIndex(EPServiceProvider epService, boolean namedWindow) {
        String stmtTextCreateOne = namedWindow ?
                "create window MyInfraMCMI#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfraMCMI as (f1 string primary key, f2 int, f3 string, f4 string)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfraMCMI(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");
        epService.getEPAdministrator().createEPL("create index MyInfraMCMIIndex1 on MyInfraMCMI(f2, f3, f1)");
        epService.getEPAdministrator().createEPL("create index MyInfraMCMIIndex2 on MyInfraMCMI(f2, f3)");
        epService.getEPAdministrator().createEPL("create index MyInfraMCMIIndex3 on MyInfraMCMI(f2)");
        String[] fields = "f1,f2,f3,f4".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -4));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -3));

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfraMCMI where f3='>E1<'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraMCMI where f3='>E1<' and f2=-2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraMCMI where f3='>E1<' and f2=-2 and f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraMCMI where f2=-2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraMCMI where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraMCMI where f3='>E1<' and f2=-2 and f1='E1' and f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraMCMI", false);
    }

    private void runAssertionLateCreate(EPServiceProvider epService, boolean namedWindow) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtTextCreateOne = namedWindow ?
                "create window MyInfraLC#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfraLC as (f1 string primary key, f2 int primary key, f3 string primary key, f4 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfraLC(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -4));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -3));

        epService.getEPAdministrator().createEPL("create index MyInfraLCIndex on MyInfraLC(f2, f3, f1)");
        String[] fields = "f1,f2,f3,f4".split(",");

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfraLC where f3='>E1<' order by f2 asc");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{
                {"E1", -4, ">E1<", "?E1?"}, {"E1", -3, ">E1<", "?E1?"}, {"E1", -2, ">E1<", "?E1?"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraLC", false);
    }

    private void runAssertionIndexStaleness(EPServiceProvider epService, boolean isNamedWindow) {

        String eplCreate = isNamedWindow ?
                "@Hint('enable_window_subquery_indexshare') create window MyInfraIS#keepall (pkey string, col0 int, col1 long)" :
                "create table MyInfraIS (pkey string primary key, col0 int, col1 long)";
        epService.getEPAdministrator().createEPL(eplCreate);

        epService.getEPAdministrator().createEPL("@name('idx') create index MyIndex on MyInfraIS (pkey, col0)");
        epService.getEPAdministrator().createEPL("on SupportBean merge MyInfraIS where theString = pkey " +
                "when not matched then insert select theString as pkey, intPrimitive as col0, longPrimitive as col1");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("on SupportBean_S0 select col0,col1 from MyInfraIS where pkey=p00").addListener(listener);

        makeSendSupportBean(epService, "E1", 10, 100L);
        assertCols(epService, listener, "E1,E2", new Object[][]{{10, 100L}, null});

        makeSendSupportBean(epService, "E2", 11, 101L);
        assertCols(epService, listener, "E1,E2", new Object[][]{{10, 100L}, {11, 101L}});

        epService.getEPAdministrator().getStatement("idx").destroy();

        makeSendSupportBean(epService, "E3", 12, 102L);
        assertCols(epService, listener, "E1,E2,E3", new Object[][]{{10, 100L}, {11, 101L}, {12, 102L}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraIS", false);
    }

    private void runAssertionIndexReferences(EPServiceProvider epService, boolean isNamedWindow) {

        String eplCreate = isNamedWindow ?
                "@Hint('enable_window_subquery_indexshare') create window MyInfraIR#keepall (col0 string, pkey int)" :
                "create table MyInfraIR (col0 string, pkey int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraIR select theString as col0, intPrimitive as pkey from SupportBean");

        epService.getEPAdministrator().createEPL("@name('idx') create index MyIndex on MyInfraIR (col0)");
        epService.getEPAdministrator().createEPL("@name('merge') on SupportBean_S1 merge MyInfraIR where col0 = p10 when matched then delete");
        epService.getEPAdministrator().createEPL("@name('subq') select (select col0 from MyInfraIR where col0 = s1.p10) from SupportBean_S1 s1");
        epService.getEPAdministrator().createEPL("@name('join') select col0 from MyInfraIR, SupportBean_S1#lastevent where col0 = p10");
        assertIndexesRef(epService, isNamedWindow, "MyInfraIR", isNamedWindow ? "idx,merge,subq" : "idx,merge,subq,join");

        epService.getEPAdministrator().getStatement("idx").destroy();
        assertIndexesRef(epService, isNamedWindow, "MyInfraIR", isNamedWindow ? "merge,subq" : "merge,subq,join");

        epService.getEPAdministrator().getStatement("merge").destroy();
        assertIndexesRef(epService, isNamedWindow, "MyInfraIR", isNamedWindow ? "subq" : "subq,join");

        epService.getEPAdministrator().getStatement("subq").destroy();
        assertIndexesRef(epService, isNamedWindow, "MyInfraIR", isNamedWindow ? "" : "join");

        epService.getEPAdministrator().getStatement("join").destroy();
        epService.getEPRuntime().executeQuery("select * from MyInfraIR where col0 = 'a'");
        epService.getEPRuntime().executeQuery("select * from MyInfraIR mt1, MyInfraIR mt2 where mt1.col0 = 'a'");
        assertNull(getIndexEntry(epService, isNamedWindow, "MyInfraIR"));
        assertIndexCountInstance(epService, isNamedWindow, "MyInfraIR", isNamedWindow ? 0 : 1);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraIR", false);
    }

    private void assertIndexCountInstance(EPServiceProvider epService, boolean namedWindow, String name, int count) {
        EventTableIndexRepository repo = getIndexInstanceRepo(epService, namedWindow, name);
        assertEquals(count, repo.getTables().size());
    }

    private EventTableIndexRepository getIndexInstanceRepo(EPServiceProvider epService, boolean namedWindow, String name) {
        if (namedWindow) {
            NamedWindowProcessorInstance instance = getNamedWindowMgmtService(epService).getProcessor(name).getProcessorInstanceNoContext();
            return instance.getRootViewInstance().getIndexRepository();
        }
        TableMetadata metadata = getTableService(epService).getTableMetadata(name);
        return metadata.getState(-1).getIndexRepository();
    }

    private void assertIndexesRef(EPServiceProvider epService, boolean isNamedWindow, String name, String csvNames) {
        EventTableIndexMetadataEntry entry = getIndexEntry(epService, isNamedWindow, name);
        if (csvNames.isEmpty()) {
            assertNull(entry);
        } else {
            EPAssertionUtil.assertEqualsAnyOrder(csvNames.split(","), entry.getReferringStatements());
        }
    }

    private EventTableIndexMetadataEntry getIndexEntry(EPServiceProvider epService, boolean namedWindow, String name) {
        IndexedPropDesc descOne = new IndexedPropDesc("col0", String.class);
        IndexMultiKey index = new IndexMultiKey(false, Arrays.asList(descOne), Collections.<IndexedPropDesc>emptyList(), null);
        EventTableIndexMetadata meta = getIndexMetaRepo(epService, namedWindow, name);
        return meta.getIndexes().get(index);
    }

    private EventTableIndexMetadata getIndexMetaRepo(EPServiceProvider epService, boolean namedWindow, String name) {
        if (namedWindow) {
            NamedWindowProcessor processor = getNamedWindowMgmtService(epService).getProcessor(name);
            return processor.getEventTableIndexMetadataRepo();
        } else {
            TableMetadata metadata = getTableService(epService).getTableMetadata(name);
            return metadata.getEventTableIndexMetadataRepo();
        }
    }

    private TableService getTableService(EPServiceProvider epService) {
        return ((EPServiceProviderSPI) epService).getServicesContext().getTableService();
    }

    private NamedWindowMgmtService getNamedWindowMgmtService(EPServiceProvider epService) {
        return ((EPServiceProviderSPI) epService).getServicesContext().getNamedWindowMgmtService();
    }

    private void runAssertionCompositeIndex(EPServiceProvider epService, boolean isNamedWindow) {
        String stmtTextCreate = isNamedWindow ?
                "create window MyInfraCI#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfraCI as (f1 string primary key, f2 int, f3 string, f4 string)";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraCI(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");
        EPStatement indexOne = epService.getEPAdministrator().createEPL("create index MyInfraCIIndex on MyInfraCI(f2, f3, f1)");
        String[] fields = "f1,f2,f3,f4".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfraCI where f3='>E1<'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraCI where f3='>E1<' and f2=-2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfraCI where f3='>E1<' and f2=-2 and f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        indexOne.destroy();

        // test SODA
        String create = "create index MyInfraCIIndex on MyInfraCI(f2, f3, f1)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(create);
        assertEquals(create, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(create, stmt.getText());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraCI", false);
    }

    private void runAssertionWidening(EPServiceProvider epService, boolean isNamedWindow) {
        // widen to long
        String stmtTextCreate = isNamedWindow ?
                "create window MyInfraW#keepall as (f1 long, f2 string)" :
                "create table MyInfraW as (f1 long primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraW(f1, f2) select longPrimitive, theString from SupportBean");
        epService.getEPAdministrator().createEPL("create index MyInfraWIndex1 on MyInfraW(f1)");
        String[] fields = "f1,f2".split(",");

        sendEventLong(epService, "E1", 10L);

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfraW where f1=10");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10L, "E1"}});

        // coerce to short
        stmtTextCreate = isNamedWindow ?
                "create window MyInfraWTwo#keepall as (f1 short, f2 string)" :
                "create table MyInfraWTwo as (f1 short primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraWTwo(f1, f2) select shortPrimitive, theString from SupportBean");
        epService.getEPAdministrator().createEPL("create index MyInfraWTwoIndex1 on MyInfraWTwo(f1)");

        sendEventShort(epService, "E1", (short) 2);

        result = epService.getEPRuntime().executeQuery("select * from MyInfraWTwo where f1=2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{(short) 2, "E1"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraW", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraWTwo", false);
    }

    private void runAssertionHashBTreeWidening(EPServiceProvider epService, boolean isNamedWindow) {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        // widen to long
        String eplCreate = isNamedWindow ?
                "create window MyInfraHBTW#keepall as (f1 long, f2 string)" :
                "create table MyInfraHBTW as (f1 long primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);

        String eplInsert = "insert into MyInfraHBTW(f1, f2) select longPrimitive, theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplInsert);

        epService.getEPAdministrator().createEPL("create index MyInfraHBTWIndex1 on MyInfraHBTW(f1 btree)");
        String[] fields = "f1,f2".split(",");

        sendEventLong(epService, "E1", 10L);
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfraHBTW where f1>9");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10L, "E1"}});

        // SODA
        String epl = "create index IX1 on MyInfraHBTW(f1, f2 btree)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(model.toEPL(), epl);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        assertEquals(epl, stmt.getText());

        // SODA with unique
        String eplUnique = "create unique index IX2 on MyInfraHBTW(f1)";
        EPStatementObjectModel modelUnique = epService.getEPAdministrator().compileEPL(eplUnique);
        assertEquals(eplUnique, modelUnique.toEPL());
        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        assertEquals(eplUnique, stmtUnique.getText());

        // coerce to short
        String eplCreateTwo = isNamedWindow ?
                "create window MyInfraHBTWTwo#keepall as (f1 short, f2 string)" :
                "create table MyInfraHBTWTwo as (f1 short primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(eplCreateTwo);

        String eplInsertTwo = "insert into MyInfraHBTWTwo(f1, f2) select shortPrimitive, theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplInsertTwo);
        epService.getEPAdministrator().createEPL("create index MyInfraHBTWTwoIndex1 on MyInfraHBTWTwo(f1 btree)");

        sendEventShort(epService, "E1", (short) 2);

        result = epService.getEPRuntime().executeQuery("select * from MyInfraHBTWTwo where f1>=2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{(short) 2, "E1"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraHBTW", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraHBTWTwo", false);
    }

    private void runAssertionMultiRangeAndKey(EPServiceProvider epService, boolean isNamedWindow) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        String eplCreate = isNamedWindow ?
                "create window MyInfraMRAK#keepall as SupportBeanRange" :
                "create table MyInfraMRAK(id string primary key, key string, keyLong long, rangeStartLong long primary key, rangeEndLong long primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);

        String eplInsert = isNamedWindow ?
                "insert into MyInfraMRAK select * from SupportBeanRange" :
                "on SupportBeanRange t0 merge MyInfraMRAK t1 where t0.id = t1.id when not matched then insert select id, key, keyLong, rangeStartLong, rangeEndLong";
        epService.getEPAdministrator().createEPL(eplInsert);

        epService.getEPAdministrator().createEPL("create index idx1 on MyInfraMRAK(key hash, keyLong hash, rangeStartLong btree, rangeEndLong btree)");
        String[] fields = "id".split(",");

        String query1 = "select * from MyInfraMRAK where rangeStartLong > 1 and rangeEndLong > 2 and keyLong=1 and key='K1' order by id asc";
        runQueryAssertion(epService, query1, fields, null);

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("E1", "K1", 1L, 2L, 3L));
        runQueryAssertion(epService, query1, fields, new Object[][]{{"E1"}});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("E2", "K1", 1L, 2L, 4L));
        runQueryAssertion(epService, query1, fields, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("E3", "K1", 1L, 3L, 3L));
        runQueryAssertion(epService, query1, fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        String query2 = "select * from MyInfraMRAK where rangeStartLong > 1 and rangeEndLong > 2 and keyLong=1 order by id asc";
        runQueryAssertion(epService, query2, fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        assertEquals(isNamedWindow ? 1 : 2, getIndexCount(epService, isNamedWindow, "MyInfraMRAK"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraMRAK", false);
    }

    private void runQueryAssertion(EPServiceProvider epService, String epl, String[] fields, Object[][] expected) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(epl);
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, expected);
    }

    private void sendEventLong(EPServiceProvider epService, String theString, long longPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        theEvent.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventShort(EPServiceProvider epService, String theString, short shortPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        theEvent.setShortPrimitive(shortPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void makeSendSupportBean(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(b);
    }

    private void assertCols(EPServiceProvider epService, SupportUpdateListener listener, String listOfP00, Object[][] expected) {
        String[] p00s = listOfP00.split(",");
        assertEquals(p00s.length, expected.length);
        for (int i = 0; i < p00s.length; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00s[i]));
            if (expected[i] == null) {
                assertFalse(listener.isInvoked());
            } else {
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col0,col1".split(","), expected[i]);
            }
        }
    }
}
