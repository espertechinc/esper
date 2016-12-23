/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;

public class TestInfraCreateIndex extends TestCase
{
    private EPServiceProviderSPI epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testMultiRangeAndKey() {
        runAssertionMultiRangeAndKey(true);
        runAssertionMultiRangeAndKey(false);
    }

    public void testHashBTreeWidening() {
        runAssertionHashBTreeWidening(true);
        runAssertionHashBTreeWidening(false);
    }

    public void testWidening() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        runAssertionWidening(true);
        runAssertionWidening(false);
    }

    public void testCompositeIndex() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        runAssertionCompositeIndex(true);
        runAssertionCompositeIndex(false);
    }

    public void testIndexReferences() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);
        runAssertionIndexReferences(true);
        runAssertionIndexReferences(false);
    }

    public void testIndexStaleness() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        runAssertionIndexStaleness(true);
        runAssertionIndexStaleness(false);
    }

    public void testLateCreate() {
        runAssertionLateCreate(true);
        runAssertionLateCreate(false);
    }

    public void testMultipleColumnMultipleIndex() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        runAssertionMultipleColumnMultipleIndex(true);
        runAssertionMultipleColumnMultipleIndex(false);
    }

    public void testDropCreate() {
        runAssertionDropCreate(true);
        runAssertionDropCreate(false);
    }

    public void testOnSelectReUse() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);

        runAssertionOnSelectReUse(true);
        runAssertionOnSelectReUse(false);
    }

    public void testInvalid() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        runAssertionInvalid(true);
        runAssertionInvalid(false);
    }

    private void runAssertionInvalid(boolean namedWindow) {
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as (f1 string, f2 int)" :
                "create table MyInfra as (f1 string primary key, f2 int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("create index MyInfraIndex on MyInfra(f1)");

        epService.getEPAdministrator().createEPL("create context ContextOne initiated by SupportBean terminated after 5 sec");
        epService.getEPAdministrator().createEPL("create context ContextTwo initiated by SupportBean terminated after 5 sec");
        String eplCreateWContext = namedWindow ?
                "context ContextOne create window MyInfraCtx#keepall as (f1 string, f2 int)" :
                "context ContextOne create table MyInfraCtx as (f1 string primary key, f2 int primary key)";
        epService.getEPAdministrator().createEPL(eplCreateWContext);

        SupportMessageAssertUtil.tryInvalid(epService, "create index MyInfraIndex on MyInfra(f1)",
                "Error starting statement: An index by name 'MyInfraIndex' already exists [");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyInfra(fx)",
                "Error starting statement: Property named 'fx' not found");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyInfra(f1, f1)",
                "Error starting statement: Property named 'f1' has been declared more then once [create index IndexTwo on MyInfra(f1, f1)]");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyWindowX(f1, f1)",
                "Error starting statement: A named window or table by name 'MyWindowX' does not exist [create index IndexTwo on MyWindowX(f1, f1)]");

        SupportMessageAssertUtil.tryInvalid(epService, "create index IndexTwo on MyWindowX(f1 bubu, f2)",
                "Invalid column index type 'bubu' encountered, please use any of the following index type names [BTREE, HASH] [create index IndexTwo on MyWindowX(f1 bubu, f2)]");

        SupportMessageAssertUtil.tryInvalid(epService, "create gugu index IndexTwo on MyInfra(f2)",
                "Invalid keyword 'gugu' in create-index encountered, expected 'unique' [create gugu index IndexTwo on MyInfra(f2)]");

        SupportMessageAssertUtil.tryInvalid(epService, "create unique index IndexTwo on MyInfra(f2 btree)",
                "Error starting statement: Combination of unique index with btree (range) is not supported [create unique index IndexTwo on MyInfra(f2 btree)]");

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
        }
        catch (Exception ex) {
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
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraTwo", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraCtx", false);
    }

    private void runAssertionOnSelectReUse(boolean namedWindow)
    {
        String stmtTextCreateOne = namedWindow ?
                "create window MyInfra#keepall as (f1 string, f2 int)" :
                "create table MyInfra as (f1 string primary key, f2 int primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfra(f1, f2) select theString, intPrimitive from SupportBean");
        EPStatement indexOne = epService.getEPAdministrator().createEPL("create index MyInfraIndex1 on MyInfra(f2)");
        String fields[] = "f1,f2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        EPStatement stmtOnSelect = epService.getEPAdministrator().createEPL("on SupportBean_S0 s0 select nw.f1 as f1, nw.f2 as f2 from MyInfra nw where nw.f2 = s0.id");
        stmtOnSelect.addListener(listener);
        assertEquals(namedWindow ? 1 : 2, getIndexCount(namedWindow));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        indexOne.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        // create second identical statement
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("on SupportBean_S0 s0 select nw.f1 as f1, nw.f2 as f2 from MyInfra nw where nw.f2 = s0.id");
        assertEquals(namedWindow ? 1 : 2, getIndexCount(namedWindow));

        stmtOnSelect.destroy();
        assertEquals(namedWindow ? 1 : 2, getIndexCount(namedWindow));

        stmtTwo.destroy();
        assertEquals(namedWindow ? 0 : 1, getIndexCount(namedWindow));

        // two-key index order test
        epService.getEPAdministrator().createEPL("create window MyInfraTwo#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("create index idx1 on MyInfraTwo (theString, intPrimitive)");
        epService.getEPAdministrator().createEPL("on SupportBean sb select * from MyInfraTwo w where w.theString = sb.theString and w.intPrimitive = sb.intPrimitive");
        epService.getEPAdministrator().createEPL("on SupportBean sb select * from MyInfraTwo w where w.intPrimitive = sb.intPrimitive and w.theString = sb.theString");
        assertEquals(1, epService.getNamedWindowMgmtService().getNamedWindowIndexes("MyInfraTwo").length);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionDropCreate(boolean namedWindow)
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtTextCreateOne = namedWindow ?
                "create window MyInfra#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfra as (f1 string primary key, f2 int primary key, f3 string primary key, f4 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfra(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");
        EPStatement indexOne = epService.getEPAdministrator().createEPL("create index MyInfraIndex1 on MyInfra(f1)");
        EPStatement indexTwo = epService.getEPAdministrator().createEPL("create index MyInfraIndex2 on MyInfra(f4)");
        String fields[] = "f1,f2".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));

        indexOne.destroy();

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        indexTwo.destroy();

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        indexTwo = epService.getEPAdministrator().createEPL("create index MyInfraIndex2 on MyInfra(f4)");

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2}});

        indexTwo.destroy();
        assertEquals(namedWindow ? 0 : 1, getIndexCount(namedWindow));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private int getIndexCount(boolean namedWindow) {
        EventTableIndexRepository repo = getIndexInstanceRepo(namedWindow);
        return repo.getIndexDescriptors().length;
    }

    private void runAssertionMultipleColumnMultipleIndex(boolean namedWindow)
    {
        String stmtTextCreateOne = namedWindow ?
                "create window MyInfra#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfra as (f1 string primary key, f2 int, f3 string, f4 string)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfra(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");
        epService.getEPAdministrator().createEPL("create index MyInfraIndex1 on MyInfra(f2, f3, f1)");
        epService.getEPAdministrator().createEPL("create index MyInfraIndex2 on MyInfra(f2, f3)");
        epService.getEPAdministrator().createEPL("create index MyInfraIndex3 on MyInfra(f2)");
        String fields[] = "f1,f2,f3,f4".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", -4));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -3));

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<' and f2=-2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<' and f2=-2 and f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f2=-2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<' and f2=-2 and f1='E1' and f4='?E1?'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionLateCreate(boolean namedWindow)
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String stmtTextCreateOne = namedWindow ?
                "create window MyInfra#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfra as (f1 string primary key, f2 int primary key, f3 string primary key, f4 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyInfra(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -4));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -3));

        epService.getEPAdministrator().createEPL("create index MyInfraIndex on MyInfra(f2, f3, f1)");
        String fields[] = "f1,f2,f3,f4".split(",");

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<' order by f2 asc");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{
                {"E1", -4, ">E1<", "?E1?"}, {"E1", -3, ">E1<", "?E1?"}, {"E1", -2, ">E1<", "?E1?"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionIndexStaleness(boolean isNamedWindow) {

        String eplCreate = isNamedWindow ?
                "@Hint('enable_window_subquery_indexshare') create window MyInfra#keepall (pkey string, col0 int, col1 long)" :
                "create table MyInfra (pkey string primary key, col0 int, col1 long)";
        epService.getEPAdministrator().createEPL(eplCreate);

        epService.getEPAdministrator().createEPL("@name('idx') create index MyIndex on MyInfra (pkey, col0)");
        epService.getEPAdministrator().createEPL("on SupportBean merge MyInfra where theString = pkey " +
                "when not matched then insert select theString as pkey, intPrimitive as col0, longPrimitive as col1");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 select col0,col1 from MyInfra where pkey=p00").addListener(listener);

        makeSendSupportBean("E1", 10, 100L);
        assertCols("E1,E2", new Object[][] {{10, 100L}, null});

        makeSendSupportBean("E2", 11, 101L);
        assertCols("E1,E2", new Object[][] {{10, 100L}, {11, 101L}});

        epService.getEPAdministrator().getStatement("idx").destroy();

        makeSendSupportBean("E3", 12, 102L);
        assertCols("E1,E2,E3", new Object[][] {{10, 100L}, {11, 101L}, {12, 102L}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionIndexReferences(boolean isNamedWindow) {
        
        String eplCreate = isNamedWindow ?
                "@Hint('enable_window_subquery_indexshare') create window MyInfra#keepall (col0 string, pkey int)" :
                "create table MyInfra (col0 string, pkey int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString as col0, intPrimitive as pkey from SupportBean");

        epService.getEPAdministrator().createEPL("@name('idx') create index MyIndex on MyInfra (col0)");
        epService.getEPAdministrator().createEPL("@name('merge') on SupportBean_S1 merge MyInfra where col0 = p10 when matched then delete");
        epService.getEPAdministrator().createEPL("@name('subq') select (select col0 from MyInfra where col0 = s1.p10) from SupportBean_S1 s1");
        epService.getEPAdministrator().createEPL("@name('join') select col0 from MyInfra, SupportBean_S1#lastevent where col0 = p10");
        assertIndexesRef(isNamedWindow, isNamedWindow ? "idx,merge,subq" : "idx,merge,subq,join");

        epService.getEPAdministrator().getStatement("idx").destroy();
        assertIndexesRef(isNamedWindow, isNamedWindow ? "merge,subq" : "merge,subq,join");

        epService.getEPAdministrator().getStatement("merge").destroy();
        assertIndexesRef(isNamedWindow, isNamedWindow ? "subq" : "subq,join");

        epService.getEPAdministrator().getStatement("subq").destroy();
        assertIndexesRef(isNamedWindow, isNamedWindow ? "" : "join");

        epService.getEPAdministrator().getStatement("join").destroy();
        epService.getEPRuntime().executeQuery("select * from MyInfra where col0 = 'a'");
        epService.getEPRuntime().executeQuery("select * from MyInfra mt1, MyInfra mt2 where mt1.col0 = 'a'");
        assertNull(getIndexEntry(isNamedWindow));
        assertIndexCountInstance(isNamedWindow, isNamedWindow ? 0 : 1);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void assertIndexCountInstance(boolean namedWindow, int count) {
        EventTableIndexRepository repo = getIndexInstanceRepo(namedWindow);
        assertEquals(count, repo.getTables().size());
    }

    private EventTableIndexRepository getIndexInstanceRepo(boolean namedWindow) {
        if (namedWindow) {
            NamedWindowProcessorInstance instance = getNamedWindowMgmtService().getProcessor("MyInfra").getProcessorInstanceNoContext();
            return instance.getRootViewInstance().getIndexRepository();
        }
        TableMetadata metadata = getTableService().getTableMetadata("MyInfra");
        return metadata.getState(-1).getIndexRepository();
    }

    private void assertIndexesRef(boolean isNamedWindow, String csvNames) {
        EventTableIndexMetadataEntry entry = getIndexEntry(isNamedWindow);
        if (csvNames.isEmpty()) {
            assertNull(entry);
        }
        else {
            EPAssertionUtil.assertEqualsAnyOrder(csvNames.split(","), entry.getReferringStatements());
        }
    }

    private EventTableIndexMetadataEntry getIndexEntry(boolean namedWindow) {
        IndexedPropDesc descOne = new IndexedPropDesc("col0", String.class);
        IndexMultiKey index = new IndexMultiKey(false, Arrays.asList(descOne), Collections.<IndexedPropDesc>emptyList());
        EventTableIndexMetadata meta = getIndexMetaRepo(namedWindow);
        return meta.getIndexes().get(index);
    }

    private EventTableIndexMetadata getIndexMetaRepo(boolean namedWindow) {
        if (namedWindow) {
            NamedWindowProcessor processor = getNamedWindowMgmtService().getProcessor("MyInfra");
            return processor.getEventTableIndexMetadataRepo();
        }
        else {
            TableMetadata metadata = getTableService().getTableMetadata("MyInfra");
            return metadata.getEventTableIndexMetadataRepo();
        }
    }

    private TableService getTableService() {
        return epService.getServicesContext().getTableService();
    }

    private NamedWindowMgmtService getNamedWindowMgmtService() {
        return epService.getServicesContext().getNamedWindowMgmtService();
    }

    private void runAssertionCompositeIndex(boolean isNamedWindow)
    {
        String stmtTextCreate = isNamedWindow ?
                "create window MyInfra#keepall as (f1 string, f2 int, f3 string, f4 string)" :
                "create table MyInfra as (f1 string primary key, f2 int, f3 string, f4 string)";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra(f1, f2, f3, f4) select theString, intPrimitive, '>'||theString||'<', '?'||theString||'?' from SupportBean");
        EPStatement indexOne = epService.getEPAdministrator().createEPL("create index MyInfraIndex on MyInfra(f2, f3, f1)");
        String fields[] = "f1,f2,f3,f4".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<' and f2=-2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        result = epService.getEPRuntime().executeQuery("select * from MyInfra where f3='>E1<' and f2=-2 and f1='E1'");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E1", -2, ">E1<", "?E1?"}});

        indexOne.destroy();

        // test SODA
        String create = "create index MyInfraIndex on MyInfra(f2, f3, f1)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(create);
        assertEquals(create, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(create, stmt.getText());

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionWidening(boolean isNamedWindow)
    {
        // widen to long
        String stmtTextCreate = isNamedWindow ?
                "create window MyInfra#keepall as (f1 long, f2 string)" :
                "create table MyInfra as (f1 long primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra(f1, f2) select longPrimitive, theString from SupportBean");
        epService.getEPAdministrator().createEPL("create index MyInfraIndex1 on MyInfra(f1)");
        String fields[] = "f1,f2".split(",");

        sendEventLong("E1", 10L);

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where f1=10");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10L, "E1"}});

        // coerce to short
        stmtTextCreate = isNamedWindow ?
                "create window MyInfraTwo#keepall as (f1 short, f2 string)" :
                "create table MyInfraTwo as (f1 short primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfraTwo(f1, f2) select shortPrimitive, theString from SupportBean");
        epService.getEPAdministrator().createEPL("create index MyInfraTwoIndex1 on MyInfraTwo(f1)");

        sendEventShort("E1", (short) 2);

        result = epService.getEPRuntime().executeQuery("select * from MyInfraTwo where f1=2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{(short) 2, "E1"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraTwo", false);
    }

    public void runAssertionHashBTreeWidening(boolean isNamedWindow) {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        // widen to long
        String eplCreate = isNamedWindow ?
                "create window MyInfra#keepall as (f1 long, f2 string)" :
                "create table MyInfra as (f1 long primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);

        String eplInsert = "insert into MyInfra(f1, f2) select longPrimitive, theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplInsert);

        epService.getEPAdministrator().createEPL("create index MyInfraIndex1 on MyInfra(f1 btree)");
        String fields[] = "f1,f2".split(",");

        sendEventLong("E1", 10L);
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where f1>9");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10L, "E1"}});

        // SODA
        String epl = "create index IX1 on MyInfra(f1, f2 btree)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(model.toEPL(), epl);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        assertEquals(epl, stmt.getText());

        // SODA with unique
        String eplUnique = "create unique index IX2 on MyInfra(f1)";
        EPStatementObjectModel modelUnique = epService.getEPAdministrator().compileEPL(eplUnique);
        assertEquals(eplUnique, modelUnique.toEPL());
        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        assertEquals(eplUnique, stmtUnique.getText());

        // coerce to short
        String eplCreateTwo = isNamedWindow ?
                "create window MyInfraTwo#keepall as (f1 short, f2 string)" :
                "create table MyInfraTwo as (f1 short primary key, f2 string primary key)";
        epService.getEPAdministrator().createEPL(eplCreateTwo);

        String eplInsertTwo = "insert into MyInfraTwo(f1, f2) select shortPrimitive, theString from SupportBean";
        epService.getEPAdministrator().createEPL(eplInsertTwo);
        epService.getEPAdministrator().createEPL("create index MyInfraTwoIndex1 on MyInfraTwo(f1 btree)");

        sendEventShort("E1", (short) 2);

        result = epService.getEPRuntime().executeQuery("select * from MyInfraTwo where f1>=2");
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{(short) 2, "E1"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraTwo", false);
    }

    private void runAssertionMultiRangeAndKey(boolean isNamedWindow) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanRange", SupportBeanRange.class);

        String eplCreate = isNamedWindow ?
                "create window MyInfra#keepall as SupportBeanRange" :
                "create table MyInfra(id string primary key, key string, keyLong long, rangeStartLong long primary key, rangeEndLong long primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);

        String eplInsert = isNamedWindow ?
                "insert into MyInfra select * from SupportBeanRange" :
                "on SupportBeanRange t0 merge MyInfra t1 where t0.id = t1.id when not matched then insert select id, key, keyLong, rangeStartLong, rangeEndLong";
        epService.getEPAdministrator().createEPL(eplInsert);

        epService.getEPAdministrator().createEPL("create index idx1 on MyInfra(key hash, keyLong hash, rangeStartLong btree, rangeEndLong btree)");
        String fields[] = "id".split(",");

        String query1 = "select * from MyInfra where rangeStartLong > 1 and rangeEndLong > 2 and keyLong=1 and key='K1' order by id asc";
        runQueryAssertion(query1, fields, null);
        
        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("E1", "K1", 1L, 2L, 3L));
        runQueryAssertion(query1, fields, new Object[][] {{"E1"}});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("E2", "K1", 1L, 2L, 4L));
        runQueryAssertion(query1, fields, new Object[][] {{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(SupportBeanRange.makeLong("E3", "K1", 1L, 3L, 3L));
        runQueryAssertion(query1, fields, new Object[][] {{"E1"}, {"E2"}, {"E3"}});

        String query2 = "select * from MyInfra where rangeStartLong > 1 and rangeEndLong > 2 and keyLong=1 order by id asc";
        runQueryAssertion(query2, fields, new Object[][] {{"E1"}, {"E2"}, {"E3"}});

        assertEquals(isNamedWindow ? 1 : 2, getIndexCount(isNamedWindow));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runQueryAssertion(String epl, String[] fields, Object[][] expected) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(epl);
        EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, expected);
    }

    private void sendEventLong(String theString, long longPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        theEvent.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventShort(String theString, short shortPrimitive) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        theEvent.setShortPrimitive(shortPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void makeSendSupportBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        b.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(b);
    }

    private void assertCols(String listOfP00, Object[][] expected) {
        String[] p00s = listOfP00.split(",");
        assertEquals(p00s.length, expected.length);
        for (int i = 0; i < p00s.length; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00s[i]));
            if (expected[i] == null) {
                assertFalse(listener.isInvoked());
            }
            else {
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col0,col1".split(","), expected[i]);
            }
        }
    }
}
