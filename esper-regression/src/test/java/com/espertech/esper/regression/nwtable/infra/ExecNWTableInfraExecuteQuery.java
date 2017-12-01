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

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.deploy.DeploymentOptions;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.context.SupportHashCodeFuncGranularCRC32;
import com.espertech.esper.supportregression.context.SupportSelectorByHashCode;
import com.espertech.esper.supportregression.context.SupportSelectorCategory;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.generic.GenericData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalidFAF;
import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalidFAFSyntax;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecNWTableInfraExecuteQuery implements RegressionExecution, IndexBackingTableInfo {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_A.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInsert(epService, true);
        runAssertionInsert(epService, false);

        runAssertionParameterizedQuery(epService, true);
        runAssertionParameterizedQuery(epService, false);

        runAssertionUpdate(epService, true);
        runAssertionUpdate(epService, false);

        runAssertionDelete(epService, true);
        runAssertionDelete(epService, false);

        runAssertionDeleteContextPartitioned(epService, true);
        runAssertionDeleteContextPartitioned(epService, false);

        runAssertionSelectWildcard(epService, true);
        runAssertionSelectWildcard(epService, false);

        runAssertionSelectCountStar(epService, true);
        runAssertionSelectCountStar(epService, false);

        runAssertionAggUngroupedRowForAll(epService, true);
        runAssertionAggUngroupedRowForAll(epService, false);

        runAssertionInClause(epService, true);
        runAssertionInClause(epService, false);

        runAssertionAggUngroupedRowForGroup(epService, true);
        runAssertionAggUngroupedRowForGroup(epService, false);

        runAssertionJoin(epService, true, true);
        runAssertionJoin(epService, false, false);
        runAssertionJoin(epService, true, false);
        runAssertionJoin(epService, false, true);

        runAssertionAggUngroupedRowForEvent(epService, true);
        runAssertionAggUngroupedRowForEvent(epService, false);

        runAssertionJoinWhere(epService, true);
        runAssertionJoinWhere(epService, false);

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertion3StreamInnerJoin(epService, rep, true);
            runAssertion3StreamInnerJoin(epService, rep, false);
        }

        runAssertionExecuteFilter(epService, true);
        runAssertionExecuteFilter(epService, false);

        runAssertionInvalid(epService, true);
        runAssertionInvalid(epService, false);
    }

    private void runAssertionExecuteFilter(EPServiceProvider epService, boolean isNamedWindow) throws Exception {
        setupInfra(epService, isNamedWindow);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));

        String query = "select * from MyInfra(intPrimitive > 1, intPrimitive < 10)";
        runAssertionFilter(epService, query);

        query = "select * from MyInfra(intPrimitive > 1) where intPrimitive < 10";
        runAssertionFilter(epService, query);

        query = "select * from MyInfra where intPrimitive < 10 and intPrimitive > 1";
        runAssertionFilter(epService, query);

        destroyInfra(epService);
    }

    private void runAssertionFilter(EPServiceProvider epService, String query) {
        String[] fields = "theString,intPrimitive".split(",");
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E3", 5}});

        EPOnDemandPreparedQuery prepared = epService.getEPRuntime().prepareQuery(query);
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{"E3", 5}});
    }

    private void runAssertionInvalid(EPServiceProvider epService, boolean isNamedWindow) {
        setupInfra(epService, isNamedWindow);
        String epl;

        epl = "insert into MyInfra select 1";
        tryInvalidFAF(epService, epl, "Error executing statement: Column '1' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?) [insert into MyInfra select 1]");

        epl = "selectoo man";
        tryInvalidFAFSyntax(epService, epl, "Incorrect syntax near 'selectoo' [selectoo man]");

        epl = "select (select * from MyInfra) from MyInfra";
        tryInvalidFAF(epService, epl, "Subqueries are not a supported feature of on-demand queries [select (select * from MyInfra) from MyInfra]");

        epl = "select * from MyInfra output every 10 seconds";
        tryInvalidFAF(epService, epl, "Error executing statement: Output rate limiting is not a supported feature of on-demand queries [select * from MyInfra output every 10 seconds]");

        epl = "select prev(1, theString) from MyInfra";
        tryInvalidFAF(epService, epl, "Error executing statement: Failed to validate select-clause expression 'prev(1,theString)': Previous function cannot be used in this context [select prev(1, theString) from MyInfra]");

        epl = "insert into MyInfra(intPrimitive) select 'a'";
        if (isNamedWindow) {
            tryInvalidFAF(epService, epl, "Error executing statement: Invalid assignment of column 'intPrimitive' of type 'java.lang.String' to event property 'intPrimitive' typed as 'int', column and parameter types mismatch [insert into MyInfra(intPrimitive) select 'a']");
        } else {
            tryInvalidFAF(epService, epl, "Error executing statement: Invalid assignment of column 'intPrimitive' of type 'java.lang.String' to event property 'intPrimitive' typed as 'java.lang.Integer', column and parameter types mismatch [insert into MyInfra(intPrimitive) select 'a']");
        }

        epl = "insert into MyInfra(intPrimitive, theString) select 1";
        tryInvalidFAF(epService, epl, "Error executing statement: Number of supplied values in the select or values clause does not match insert-into clause [insert into MyInfra(intPrimitive, theString) select 1]");

        epl = "insert into MyInfra select 1 as intPrimitive from MyInfra";
        tryInvalidFAF(epService, epl, "Error executing statement: Insert-into fire-and-forget query can only consist of an insert-into clause and a select-clause [insert into MyInfra select 1 as intPrimitive from MyInfra]");

        epl = "insert into MyInfra(intPrimitive, theString) values (1, 'a', 1)";
        tryInvalidFAF(epService, epl, "Error executing statement: Number of supplied values in the select or values clause does not match insert-into clause [insert into MyInfra(intPrimitive, theString) values (1, 'a', 1)]");

        if (isNamedWindow) {
            epl = "select * from pattern [every MyInfra]";
            tryInvalidFAF(epService, epl, "Error executing statement: On-demand queries require tables or named windows and do not allow event streams or patterns [select * from pattern [every MyInfra]]");

            epl = "select * from MyInfra#uni(intPrimitive)";
            tryInvalidFAF(epService, epl, "Error executing statement: Views are not a supported feature of on-demand queries [select * from MyInfra#uni(intPrimitive)]");
        }

        destroyInfra(epService);
    }

    private void runAssertion3StreamInnerJoin(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, boolean isNamedWindow) throws Exception {
        String eplEvents = eventRepresentationEnum.getAnnotationText() + " create schema Product (productId string, categoryId string);" +
                eventRepresentationEnum.getAnnotationText() + " create schema Category (categoryId string, owner string);" +
                eventRepresentationEnum.getAnnotationText() + " create schema ProductOwnerDetails (productId string, owner string);";
        String epl;
        if (isNamedWindow) {
            epl = eplEvents +
                    eventRepresentationEnum.getAnnotationText() + " create window WinProduct#keepall as select * from Product;" +
                    eventRepresentationEnum.getAnnotationText() + " create window WinCategory#keepall as select * from Category;" +
                    eventRepresentationEnum.getAnnotationText() + " create window WinProductOwnerDetails#keepall as select * from ProductOwnerDetails;" +
                    "insert into WinProduct select * from Product;" +
                    "insert into WinCategory select * from Category;" +
                    "insert into WinProductOwnerDetails select * from ProductOwnerDetails;";
        } else {
            epl = eplEvents +
                    "create table WinProduct (productId string primary key, categoryId string primary key);" +
                    "create table WinCategory (categoryId string primary key, owner string primary key);" +
                    "create table WinProductOwnerDetails (productId string primary key, owner string);" +
                    "on Product t1 merge WinProduct t2 where t1.productId = t2.productId and t1.categoryId = t2.categoryId when not matched then insert select productId, categoryId;" +
                    "on Category t1 merge WinCategory t2 where t1.categoryId = t2.categoryId when not matched then insert select categoryId, owner;" +
                    "on ProductOwnerDetails t1 merge WinProductOwnerDetails t2 where t1.productId = t2.productId when not matched then insert select productId, owner;";
        }
        EPDeploymentAdmin dAdmin = epService.getEPAdministrator().getDeploymentAdmin();
        dAdmin.deploy(dAdmin.parse(epl), new DeploymentOptions());

        sendEvent(eventRepresentationEnum, epService, "Product", new String[]{"productId=Product1", "categoryId=Category1"});
        sendEvent(eventRepresentationEnum, epService, "Product", new String[]{"productId=Product2", "categoryId=Category1"});
        sendEvent(eventRepresentationEnum, epService, "Product", new String[]{"productId=Product3", "categoryId=Category1"});
        sendEvent(eventRepresentationEnum, epService, "Category", new String[]{"categoryId=Category1", "owner=Petar"});
        sendEvent(eventRepresentationEnum, epService, "ProductOwnerDetails", new String[]{"productId=Product1", "owner=Petar"});

        String[] fields = "WinProduct.productId".split(",");
        EventBean[] queryResults;
        queryResults = epService.getEPRuntime().executeQuery("" +
                "select WinProduct.productId " +
                " from WinProduct" +
                " inner join WinCategory on WinProduct.categoryId=WinCategory.categoryId" +
                " inner join WinProductOwnerDetails on WinProduct.productId=WinProductOwnerDetails.productId"
        ).getArray();
        EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

        queryResults = epService.getEPRuntime().executeQuery("" +
                "select WinProduct.productId " +
                " from WinProduct" +
                " inner join WinCategory on WinProduct.categoryId=WinCategory.categoryId" +
                " inner join WinProductOwnerDetails on WinProduct.productId=WinProductOwnerDetails.productId" +
                " where WinCategory.owner=WinProductOwnerDetails.owner"
        ).getArray();
        EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

        queryResults = epService.getEPRuntime().executeQuery("" +
                "select WinProduct.productId " +
                " from WinProduct, WinCategory, WinProductOwnerDetails" +
                " where WinCategory.owner=WinProductOwnerDetails.owner" +
                " and WinProduct.categoryId=WinCategory.categoryId" +
                " and WinProduct.productId=WinProductOwnerDetails.productId"
        ).getArray();
        EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

        String eplQuery = "select WinProduct.productId " +
                " from WinProduct" +
                " inner join WinCategory on WinProduct.categoryId=WinCategory.categoryId" +
                " inner join WinProductOwnerDetails on WinProduct.productId=WinProductOwnerDetails.productId" +
                " having WinCategory.owner=WinProductOwnerDetails.owner";
        queryResults = epService.getEPRuntime().executeQuery(eplQuery).getArray();
        EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplQuery);
        queryResults = epService.getEPRuntime().executeQuery(model).getArray();
        EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("Product", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("Category", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("ProductOwnerDetails", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("WinProduct", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("WinCategory", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("WinProductOwnerDetails", false);
    }

    private void runAssertionJoinWhere(EPServiceProvider epService, boolean isNamedWindow) throws Exception {
        String eplCreateOne = isNamedWindow ?
                (EventRepresentationChoice.MAP.getAnnotationText() + " create window Infra1#keepall (key String, keyJoin String)") :
                "create table Infra1 (key String primary key, keyJoin String)";
        String eplCreateTwo = isNamedWindow ?
                (EventRepresentationChoice.MAP.getAnnotationText() + " create window Infra2#keepall (keyJoin String, value double)") :
                "create table Infra2 (keyJoin String primary key, value double primary key)";
        epService.getEPAdministrator().createEPL(eplCreateOne);
        epService.getEPAdministrator().createEPL(eplCreateTwo);

        String queryAgg = "select w1.key, sum(value) from Infra1 w1, Infra2 w2 WHERE w1.keyJoin = w2.keyJoin GROUP BY w1.key order by w1.key";
        String[] fieldsAgg = "w1.key,sum(value)".split(",");
        String queryNoagg = "select w1.key, w2.value from Infra1 w1, Infra2 w2 where w1.keyJoin = w2.keyJoin and value = 1 order by w1.key";
        String[] fieldsNoagg = "w1.key,w2.value".split(",");

        EventBean[] result = epService.getEPRuntime().executeQuery(queryAgg).getArray();
        assertEquals(0, result.length);
        result = epService.getEPRuntime().executeQuery(queryNoagg).getArray();
        assertNull(result);

        insertInfra1Event(epService, "key1", "keyJoin1");

        result = epService.getEPRuntime().executeQuery(queryAgg).getArray();
        assertEquals(0, result.length);
        result = epService.getEPRuntime().executeQuery(queryNoagg).getArray();
        assertNull(result);

        insertInfra2Event(epService, "keyJoin1", 1d);

        result = epService.getEPRuntime().executeQuery(queryAgg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}});
        result = epService.getEPRuntime().executeQuery(queryNoagg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}});

        insertInfra2Event(epService, "keyJoin2", 2d);

        result = epService.getEPRuntime().executeQuery(queryAgg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}});
        result = epService.getEPRuntime().executeQuery(queryNoagg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}});

        insertInfra1Event(epService, "key2", "keyJoin2");

        result = epService.getEPRuntime().executeQuery(queryAgg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}, {"key2", 2d}});
        result = epService.getEPRuntime().executeQuery(queryNoagg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}});

        insertInfra2Event(epService, "keyJoin2", 1d);

        result = epService.getEPRuntime().executeQuery(queryAgg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}, {"key2", 3d}});
        result = epService.getEPRuntime().executeQuery(queryNoagg).getArray();
        EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}, {"key2", 1d}});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("Infra1", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("Infra2", false);
    }

    private void insertInfra1Event(EPServiceProvider epService, String key, String keyJoin) {
        epService.getEPRuntime().executeQuery("insert into Infra1 values ('" + key + "', '" + keyJoin + "')");
    }

    private void insertInfra2Event(EPServiceProvider epService, String keyJoin, double value) {
        epService.getEPRuntime().executeQuery("insert into Infra2 values ('" + keyJoin + "', " + value + ")");
    }

    private void runAssertionAggUngroupedRowForEvent(EPServiceProvider epService, boolean isNamedWindow) throws Exception {
        setupInfra(epService, isNamedWindow);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        String[] fields = new String[]{"theString", "total"};

        String query = "select theString, sum(intPrimitive) as total from MyInfra";
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E1", 16}, {"E2", 16}, {"E3", 16}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -2));
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E1", 14}, {"E2", 14}, {"E3", 14}, {"E4", 14}});

        destroyInfra(epService);
    }

    private void runAssertionJoin(EPServiceProvider epService, boolean isFirstNW, boolean isSecondNW) throws Exception {
        setupInfra(epService, isFirstNW);

        String eplSecondCreate = isSecondNW ?
                "create window MySecondInfra#keepall as select * from SupportBean_A" :
                "create table MySecondInfra as (id string primary key)";
        epService.getEPAdministrator().createEPL(eplSecondCreate);
        String eplSecondFill = isSecondNW ?
                "insert into MySecondInfra select * from SupportBean_A " :
                "on SupportBean_A sba merge MySecondInfra msi where msi.id = sba.id when not matched then insert select id";
        epService.getEPAdministrator().createEPL(eplSecondFill);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        String[] fields = new String[]{"theString", "intPrimitive", "id"};

        String query = "select theString, intPrimitive, id from MyInfra nw1, " +
                "MySecondInfra nw2 where nw1.theString = nw2.id";
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E2", 11, "E2"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));

        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E2", 11, "E2"}, {"E3", 1, "E3"}, {"E3", 2, "E3"}});

        destroyInfra(epService);
        epService.getEPAdministrator().getConfiguration().removeEventType("MySecondInfra", false);
    }

    private void runAssertionAggUngroupedRowForGroup(EPServiceProvider epService, boolean isNamedWindow) throws Exception {
        setupInfra(epService, isNamedWindow);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        String[] fields = new String[]{"theString", "total"};

        String query = "select theString, sum(intPrimitive) as total from MyInfra group by theString order by theString asc";
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E1", 6}, {"E2", 11}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", -2));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E1", 6}, {"E2", 9}, {"E3", 3}});

        destroyInfra(epService);
    }

    private void runAssertionInClause(EPServiceProvider epService, boolean isNamedWindow) {

        setupInfra(epService, isNamedWindow);

        epService.getEPRuntime().sendEvent(makeBean("E1", 10, 100L));
        epService.getEPRuntime().sendEvent(makeBean("E2", 20, 200L));
        epService.getEPRuntime().sendEvent(makeBean("E3", 30, 300L));
        epService.getEPRuntime().sendEvent(makeBean("E4", 40, 400L));

        // try no index
        runAssertionIn(epService);

        // try suitable index
        EPStatement stmtIdx1 = epService.getEPAdministrator().createEPL("create index Idx1 on MyInfra(theString, intPrimitive)");
        runAssertionIn(epService);
        stmtIdx1.destroy();

        // backwards index
        EPStatement stmtIdx2 = epService.getEPAdministrator().createEPL("create index Idx2 on MyInfra(intPrimitive, theString)");
        runAssertionIn(epService);
        stmtIdx2.destroy();

        // partial index
        EPStatement stmtIdx3 = epService.getEPAdministrator().createEPL("create index Idx3 on MyInfra(intPrimitive)");
        runAssertionIn(epService);
        stmtIdx3.destroy();

        destroyInfra(epService);
    }

    private void runAssertionIn(EPServiceProvider epService) {
        tryAssertionIn(epService, "theString in ('E2', 'E3') and intPrimitive in (10, 20)", new Long[]{200L});
        tryAssertionIn(epService, "intPrimitive in (30, 20) and theString in ('E4', 'E1')", new Long[]{});
        tryAssertionIn(epService, "intPrimitive in (30, 20) and theString in ('E2', 'E1')", new Long[]{200L});
        tryAssertionIn(epService, "theString in ('E2', 'E3') and intPrimitive in (20, 30)", new Long[]{200L, 300L});
        tryAssertionIn(epService, "theString in ('E2', 'E3') and intPrimitive in (30, 20)", new Long[]{200L, 300L});
        tryAssertionIn(epService, "theString in ('E1', 'E2', 'E3', 'E4') and intPrimitive in (10, 20, 30)", new Long[]{100L, 200L, 300L});
    }

    private void tryAssertionIn(EPServiceProvider epService, String filter, Long[] expected) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select * from MyInfra where " + filter);
        assertEquals(result.getArray().length, expected.length);
        List<Long> values = new ArrayList<>();
        for (EventBean event : result.getArray()) {
            values.add((Long) event.get("longPrimitive"));
        }
        EPAssertionUtil.assertEqualsAnyOrder(expected, values.toArray());
    }

    private void runAssertionAggUngroupedRowForAll(EPServiceProvider epService, boolean isNamedWindow) throws Exception {
        setupInfra(epService, isNamedWindow);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 5));
        String[] fields = new String[]{"total"};

        String query = "select sum(intPrimitive) as total from MyInfra";
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{16}});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", -2));
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{14}});

        destroyInfra(epService);
    }

    private void runAssertionSelectCountStar(EPServiceProvider epService, boolean isNamedWindow) throws Exception {
        setupInfra(epService, isNamedWindow);

        String[] fields = new String[]{"cnt"};
        String query = "select count(*) as cnt from MyInfra";
        EPOnDemandPreparedQuery prepared = epService.getEPRuntime().prepareQuery(query);

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{0L}});
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{0L}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{1L}});
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{1L}});
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{1L}});
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{1L}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{2L}});
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{2L}});

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(query);
        result = epService.getEPRuntime().executeQuery(model);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{2L}});
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{2L}});

        EPOnDemandPreparedQuery preparedFromModel = epService.getEPRuntime().prepareQuery(model);
        EPAssertionUtil.assertPropsPerRow(preparedFromModel.execute().iterator(), fields, new Object[][]{{2L}});

        destroyInfra(epService);
    }

    private void runAssertionSelectWildcard(EPServiceProvider epService, boolean isNamedWindow) throws Exception {
        setupInfra(epService, isNamedWindow);

        String query = "select * from MyInfra";
        EPOnDemandPreparedQuery prepared = epService.getEPRuntime().prepareQuery(query);
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(query);
        final String[] fields = new String[]{"theString", "intPrimitive"};
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, null);
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{"E1", 1}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        result = epService.getEPRuntime().executeQuery(query);
        EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(prepared.execute().iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        destroyInfra(epService);
    }

    private void runAssertionDeleteContextPartitioned(EPServiceProvider epService, boolean isNamedWindow) {

        // test hash-segmented context
        String eplCtx = "create context MyCtx coalesce consistent_hash_crc32(theString) from SupportBean granularity 4 preallocate";
        epService.getEPAdministrator().createEPL(eplCtx);

        String eplCreate = isNamedWindow ?
                "context MyCtx create window CtxInfra#keepall as SupportBean" :
                "context MyCtx create table CtxInfra (theString string primary key, intPrimitive int primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        String eplPopulate = isNamedWindow ?
                "context MyCtx insert into CtxInfra select * from SupportBean" :
                "context MyCtx on SupportBean sb merge CtxInfra ci where sb.theString = ci.theString and sb.intPrimitive = ci.intPrimitive when not matched then insert select theString, intPrimitive";
        epService.getEPAdministrator().createEPL(eplPopulate);

        SupportHashCodeFuncGranularCRC32 codeFunc = new SupportHashCodeFuncGranularCRC32(4);
        int[] codes = new int[5];
        for (int i = 0; i < 5; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
            codes[i] = codeFunc.codeFor("E" + i);
        }
        EPAssertionUtil.assertEqualsExactOrder(new int[]{3, 1, 3, 1, 2}, codes);   // just to make sure CRC32 didn't change

        // assert counts individually per context partition
        assertCtxInfraCountPerCode(epService, new long[]{0, 2, 1, 2});

        // delete per context partition (E0 ended up in '3')
        epService.getEPRuntime().executeQuery("delete from CtxInfra where theString = 'E0'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(1)});
        assertCtxInfraCountPerCode(epService, new long[]{0, 2, 1, 2});

        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("delete from CtxInfra where theString = 'E0'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(3)});
        assertCtxInfraCountPerCode(epService, new long[]{0, 2, 1, 1});
        if (isNamedWindow) {
            EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString".split(","), new Object[][]{{"E0"}});
        }

        // delete per context partition (E1 ended up in '1')
        epService.getEPRuntime().executeQuery("delete from CtxInfra where theString = 'E1'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(0)});
        assertCtxInfraCountPerCode(epService, new long[]{0, 2, 1, 1});

        epService.getEPRuntime().executeQuery("delete from CtxInfra where theString = 'E1'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(1)});
        assertCtxInfraCountPerCode(epService, new long[]{0, 1, 1, 1});
        epService.getEPAdministrator().destroyAllStatements();

        // test category-segmented context
        String eplCtxCategory = "create context MyCtxCat group by intPrimitive < 0 as negative, group by intPrimitive > 0 as positive from SupportBean";
        epService.getEPAdministrator().createEPL(eplCtxCategory);
        epService.getEPAdministrator().createEPL("context MyCtxCat create window CtxInfraCat#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("context MyCtxCat insert into CtxInfraCat select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", -3));
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        assertEquals(2L, getCtxInfraCatCount(epService, "positive"));
        assertEquals(2L, getCtxInfraCatCount(epService, "negative"));

        result = epService.getEPRuntime().executeQuery("context MyCtxCat delete from CtxInfraCat where context.label = 'negative'");
        assertEquals(2L, getCtxInfraCatCount(epService, "positive"));
        assertEquals(0L, getCtxInfraCatCount(epService, "negative"));
        EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString".split(","), new Object[][]{{"E1"}, {"E3"}});

        destroyInfra(epService);
        epService.getEPAdministrator().getConfiguration().removeEventType("CtxInfra", false);
    }

    private void runAssertionDelete(EPServiceProvider epService, boolean isNamedWindow) {

        setupInfra(epService, isNamedWindow);

        // test delete-all
        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        assertEquals(10L, getMyInfraCount(epService));
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("delete from MyInfra");
        assertEquals(0L, getMyInfraCount(epService));
        if (isNamedWindow) {
            assertEquals(epService.getEPAdministrator().getConfiguration().getEventType("MyInfra"), result.getEventType());
            assertEquals(10, result.getArray().length);
            assertEquals("E0", result.getArray()[0].get("theString"));
        } else {
            assertEquals(0, result.getArray().length);
        }

        // test SODA + where-clause
        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        assertEquals(10L, getMyInfraCount(epService));
        String eplWithWhere = "delete from MyInfra where theString=\"E1\"";
        EPStatementObjectModel modelWithWhere = epService.getEPAdministrator().compileEPL(eplWithWhere);
        assertEquals(eplWithWhere, modelWithWhere.toEPL());
        result = epService.getEPRuntime().executeQuery(modelWithWhere);
        assertEquals(9L, getMyInfraCount(epService));
        if (isNamedWindow) {
            assertEquals(epService.getEPAdministrator().getConfiguration().getEventType("MyInfra"), result.getEventType());
            EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString".split(","), new Object[][]{{"E1"}});
        }

        // test SODA delete-all
        String eplDelete = "delete from MyInfra";
        EPStatementObjectModel modelDeleteOnly = epService.getEPAdministrator().compileEPL(eplDelete);
        assertEquals(eplDelete, modelDeleteOnly.toEPL());
        epService.getEPRuntime().executeQuery(modelDeleteOnly);
        assertEquals(0L, getMyInfraCount(epService));

        // test with index
        if (isNamedWindow) {
            epService.getEPAdministrator().createEPL("create unique index Idx1 on MyInfra (theString)");
        }
        for (int i = 0; i < 5; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        runQueryAssertCount(epService, INDEX_CALLBACK_HOOK + "delete from MyInfra where theString = 'E1' and intPrimitive = 0", 5, isNamedWindow ? "Idx1" : "primary-MyInfra", isNamedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
        runQueryAssertCount(epService, INDEX_CALLBACK_HOOK + "delete from MyInfra where theString = 'E1' and intPrimitive = 1", 4, isNamedWindow ? "Idx1" : "primary-MyInfra", isNamedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
        runQueryAssertCount(epService, INDEX_CALLBACK_HOOK + "delete from MyInfra where theString = 'E2'", 3, isNamedWindow ? "Idx1" : null, isNamedWindow ? BACKING_SINGLE_UNIQUE : null);
        runQueryAssertCount(epService, INDEX_CALLBACK_HOOK + "delete from MyInfra where intPrimitive = 4", 2, null, null);

        // test with alias
        runQueryAssertCount(epService, INDEX_CALLBACK_HOOK + "delete from MyInfra as w1 where w1.theString = 'E3'", 1, isNamedWindow ? "Idx1" : null, isNamedWindow ? BACKING_SINGLE_UNIQUE : null);

        // test consumption
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rstream * from MyInfra");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().executeQuery("delete from MyInfra");
        final String[] fields = new String[]{"theString", "intPrimitive"};
        if (isNamedWindow) {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E0", 0});
        } else {
            assertFalse(listener.isInvoked());
        }

        destroyInfra(epService);
    }

    private void runQueryAssertCount(EPServiceProvider epService, String epl, int count, String indexName, String backingClass) {
        epService.getEPRuntime().executeQuery(epl);
        assertEquals(count, getMyInfraCount(epService));
        SupportQueryPlanIndexHook.assertFAFAndReset(indexName, backingClass);
    }

    private long getMyInfraCount(EPServiceProvider epService) {
        return (Long) epService.getEPRuntime().executeQuery("select count(*) as c0 from MyInfra").getArray()[0].get("c0");
    }

    private void runAssertionUpdate(EPServiceProvider epService, boolean isNamedWindow) {

        setupInfra(epService, isNamedWindow);
        final String[] fields = new String[]{"theString", "intPrimitive"};

        // test update-all
        for (int i = 0; i < 2; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("update MyInfra set theString = 'ABC'");
        EPAssertionUtil.assertPropsPerRow(epService.getEPAdministrator().getStatement("TheInfra").iterator(), fields, new Object[][]{{"ABC", 0}, {"ABC", 1}});
        if (isNamedWindow) {
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"ABC", 0}, {"ABC", 1}});
        }

        // test update with where-clause
        epService.getEPRuntime().executeQuery("delete from MyInfra");
        for (int i = 0; i < 3; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        result = epService.getEPRuntime().executeQuery("update MyInfra set theString = 'X', intPrimitive=-1 where theString = 'E1' and intPrimitive = 1");
        if (isNamedWindow) {
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"X", -1}});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(epService.getEPAdministrator().getStatement("TheInfra").iterator(), fields, new Object[][]{{"E0", 0}, {"E2", 2}, {"X", -1}});

        // test update with SODA
        String epl = "update MyInfra set intPrimitive=intPrimitive+10 where theString=\"E2\"";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        result = epService.getEPRuntime().executeQuery(model);
        if (isNamedWindow) {
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E2", 12}});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(epService.getEPAdministrator().getStatement("TheInfra").iterator(), fields, new Object[][]{{"E0", 0}, {"X", -1}, {"E2", 12}});

        // test update with initial value
        result = epService.getEPRuntime().executeQuery("update MyInfra set intPrimitive=5, theString='x', theString = initial.theString || 'y', intPrimitive=initial.intPrimitive+100 where theString = 'E0'");
        if (isNamedWindow) {
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E0y", 100}});
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(epService.getEPAdministrator().getStatement("TheInfra").iterator(), fields, new Object[][]{{"X", -1}, {"E2", 12}, {"E0y", 100}});
        epService.getEPRuntime().executeQuery("delete from MyInfra");

        // test with index
        if (isNamedWindow) {
            epService.getEPAdministrator().createEPL("create unique index Idx1 on MyInfra (theString)");
        }
        for (int i = 0; i < 5; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E" + i, i));
        }
        runQueryAssertCountNonNegative(epService, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where theString = 'E1' and intPrimitive = 0", 5, isNamedWindow ? "Idx1" : "primary-MyInfra", isNamedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
        runQueryAssertCountNonNegative(epService, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where theString = 'E1' and intPrimitive = 1", 4, isNamedWindow ? "Idx1" : "primary-MyInfra", isNamedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
        runQueryAssertCountNonNegative(epService, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where theString = 'E2'", 3, isNamedWindow ? "Idx1" : null, isNamedWindow ? BACKING_SINGLE_UNIQUE : null);
        runQueryAssertCountNonNegative(epService, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where intPrimitive = 4", 2, null, null);

        // test with alias
        runQueryAssertCountNonNegative(epService, INDEX_CALLBACK_HOOK + "update MyInfra as w1 set intPrimitive=-1 where w1.theString = 'E3'", 1, isNamedWindow ? "Idx1" : null, isNamedWindow ? BACKING_SINGLE_UNIQUE : null);

        // test consumption
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from MyInfra");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().executeQuery("update MyInfra set intPrimitive=1000 where theString = 'E0'");
        if (isNamedWindow) {
            EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E0", 1000}, new Object[]{"E0", 0});
        }

        // test update via UDF and setter
        if (isNamedWindow) {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("doubleInt", this.getClass().getName(), "doubleInt");
            epService.getEPRuntime().executeQuery("delete from MyInfra");
            epService.getEPRuntime().sendEvent(new SupportBean("A", 10));
            epService.getEPRuntime().executeQuery("update MyInfra mw set mw.setTheString('XYZ'), doubleInt(mw)");
            EPAssertionUtil.assertPropsPerRow(epService.getEPAdministrator().getStatement("TheInfra").iterator(),
                    "theString,intPrimitive".split(","), new Object[][]{{"XYZ", 20}});
        }

        destroyInfra(epService);
    }

    private void runQueryAssertCountNonNegative(EPServiceProvider epService, String epl, int count, String indexName, String backingClass) {
        SupportQueryPlanIndexHook.reset();
        epService.getEPRuntime().executeQuery(epl);
        long actual = (Long) epService.getEPRuntime().executeQuery("select count(*) as c0 from MyInfra where intPrimitive >= 0").getArray()[0].get("c0");
        assertEquals(count, actual);
        SupportQueryPlanIndexHook.assertFAFAndReset(indexName, backingClass);
    }

    private void runAssertionParameterizedQuery(EPServiceProvider epService, boolean isNamedWindow) {

        setupInfra(epService, isNamedWindow);

        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(makeBean("E" + i, i, i * 1000));
        }

        // test one parameter
        String eplOneParam = "select * from MyInfra where intPrimitive = ?";
        EPOnDemandPreparedQueryParameterized pqOneParam = epService.getEPRuntime().prepareQueryWithParameters(eplOneParam);
        for (int i = 0; i < 10; i++) {
            runParameterizedQuery(epService, pqOneParam, new Object[]{i}, new String[]{"E" + i});
        }
        runParameterizedQuery(epService, pqOneParam, new Object[]{-1}, null); // not found

        // test two parameter
        String eplTwoParam = "select * from MyInfra where intPrimitive = ? and longPrimitive = ?";
        EPOnDemandPreparedQueryParameterized pqTwoParam = epService.getEPRuntime().prepareQueryWithParameters(eplTwoParam);
        for (int i = 0; i < 10; i++) {
            runParameterizedQuery(epService, pqTwoParam, new Object[]{i, (long) i * 1000}, new String[]{"E" + i});
        }
        runParameterizedQuery(epService, pqTwoParam, new Object[]{-1, 1000}, null); // not found

        // test in-clause with string objects
        String eplInSimple = "select * from MyInfra where theString in (?, ?, ?)";
        EPOnDemandPreparedQueryParameterized pqInSimple = epService.getEPRuntime().prepareQueryWithParameters(eplInSimple);
        runParameterizedQuery(epService, pqInSimple, new Object[]{"A", "A", "A"}, null); // not found
        runParameterizedQuery(epService, pqInSimple, new Object[]{"A", "E3", "A"}, new String[]{"E3"});

        // test in-clause with string array
        String eplInArray = "select * from MyInfra where theString in (?)";
        EPOnDemandPreparedQueryParameterized pqInArray = epService.getEPRuntime().prepareQueryWithParameters(eplInArray);
        runParameterizedQuery(epService, pqInArray, new Object[]{new String[]{"E3", "E6", "E8"}}, new String[]{"E3", "E6", "E8"});

        // various combinations
        runParameterizedQuery(epService, epService.getEPRuntime().prepareQueryWithParameters("select * from MyInfra where theString in (?) and longPrimitive = 4000"),
                new Object[]{new String[]{"E3", "E4", "E8"}}, new String[]{"E4"});
        runParameterizedQuery(epService, epService.getEPRuntime().prepareQueryWithParameters("select * from MyInfra where longPrimitive > 8000"),
                new Object[]{}, new String[]{"E9"});
        runParameterizedQuery(epService, epService.getEPRuntime().prepareQueryWithParameters("select * from MyInfra where longPrimitive < ?"),
                new Object[]{2000}, new String[]{"E0", "E1"});
        runParameterizedQuery(epService, epService.getEPRuntime().prepareQueryWithParameters("select * from MyInfra where longPrimitive between ? and ?"),
                new Object[]{2000, 4000}, new String[]{"E2", "E3", "E4"});

        destroyInfra(epService);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runParameterizedQuery(EPServiceProvider epService, EPOnDemandPreparedQueryParameterized parameterizedQuery, Object[] parameters, String[] expected) {

        for (int i = 0; i < parameters.length; i++) {
            parameterizedQuery.setObject(i + 1, parameters[i]);
        }
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery(parameterizedQuery);
        if (expected == null) {
            assertEquals(0, result.getArray().length);
        } else {
            assertEquals(expected.length, result.getArray().length);
            String[] resultStrings = new String[result.getArray().length];
            for (int i = 0; i < resultStrings.length; i++) {
                resultStrings[i] = (String) result.getArray()[i].get("theString");
            }
            EPAssertionUtil.assertEqualsAnyOrder(expected, resultStrings);
        }
    }

    private void runAssertionInsert(EPServiceProvider epService, boolean isNamedWindow) {

        setupInfra(epService, isNamedWindow);

        EPStatement stmt = epService.getEPAdministrator().getStatement("TheInfra");
        String[] propertyNames = "theString,intPrimitive".split(",");

        // try column name provided with insert-into
        String eplSelect = "insert into MyInfra (theString, intPrimitive) select 'a', 1";
        EPOnDemandQueryResult resultOne = epService.getEPRuntime().executeQuery(eplSelect);
        assertFAFInsertResult(resultOne, new Object[]{"a", 1}, propertyNames, stmt, isNamedWindow);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), propertyNames, new Object[][]{{"a", 1}});

        // try SODA and column name not provided with insert-into
        String eplTwo = "insert into MyInfra select \"b\" as theString, 2 as intPrimitive";
        EPStatementObjectModel modelWSelect = epService.getEPAdministrator().compileEPL(eplTwo);
        assertEquals(eplTwo, modelWSelect.toEPL());
        EPOnDemandQueryResult resultTwo = epService.getEPRuntime().executeQuery(modelWSelect);
        assertFAFInsertResult(resultTwo, new Object[]{"b", 2}, propertyNames, stmt, isNamedWindow);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), propertyNames, new Object[][]{{"a", 1}, {"b", 2}});

        // create unique index, insert duplicate row
        epService.getEPAdministrator().createEPL("create unique index I1 on MyInfra (theString)");
        try {
            String eplThree = "insert into MyInfra (theString) select 'a' as theString";
            epService.getEPRuntime().executeQuery(eplThree);
        } catch (EPException ex) {
            assertEquals("Error executing statement: Unique index violation, index 'I1' is a unique index and key 'a' already exists [insert into MyInfra (theString) select 'a' as theString]", ex.getMessage());
        }
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), propertyNames, new Object[][]{{"a", 1}, {"b", 2}});

        // try second no-column-provided version
        String eplMyInfraThree = isNamedWindow ?
                "create window MyInfraThree#keepall as (p0 string, p1 int)" :
                "create table MyInfraThree as (p0 string, p1 int)";
        EPStatement stmtMyInfraThree = epService.getEPAdministrator().createEPL(eplMyInfraThree);
        epService.getEPRuntime().executeQuery("insert into MyInfraThree select 'a' as p0, 1 as p1");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtMyInfraThree.iterator(), "p0,p1".split(","), new Object[][]{{"a", 1}});

        // try enum-value insert
        epService.getEPAdministrator().createEPL("create schema MyMode (mode " + SupportEnum.class.getName() + ")");
        String eplInfraMode = isNamedWindow ?
                "create window MyInfraTwo#unique(mode) as MyMode" :
                "create table MyInfraTwo as (mode " + SupportEnum.class.getName() + ")";
        EPStatement stmtEnumWin = epService.getEPAdministrator().createEPL(eplInfraMode);
        epService.getEPRuntime().executeQuery("insert into MyInfraTwo select " + SupportEnum.class.getName() + "." + SupportEnum.ENUM_VALUE_2.name() + " as mode");
        EPAssertionUtil.assertProps(stmtEnumWin.iterator().next(), "mode".split(","), new Object[]{SupportEnum.ENUM_VALUE_2});

        // try insert-into with values-keyword and explicit column names
        epService.getEPRuntime().executeQuery("delete from MyInfra");
        String eplValuesKW = "insert into MyInfra(theString, intPrimitive) values (\"a\", 1)";
        EPOnDemandQueryResult resultValuesKW = epService.getEPRuntime().executeQuery(eplValuesKW);
        assertFAFInsertResult(resultValuesKW, new Object[]{"a", 1}, propertyNames, stmt, isNamedWindow);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), propertyNames, new Object[][]{{"a", 1}});

        // try insert-into with values model
        epService.getEPRuntime().executeQuery("delete from MyInfra");
        EPStatementObjectModel modelWValuesKW = epService.getEPAdministrator().compileEPL(eplValuesKW);
        assertEquals(eplValuesKW, modelWValuesKW.toEPL());
        epService.getEPRuntime().executeQuery(modelWValuesKW);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), propertyNames, new Object[][]{{"a", 1}});

        // try insert-into with values-keyword and as-names
        epService.getEPRuntime().executeQuery("delete from MyInfraThree");
        String eplValuesWithoutCols = "insert into MyInfraThree values ('b', 2)";
        epService.getEPRuntime().executeQuery(eplValuesWithoutCols);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmtMyInfraThree.iterator(), "p0,p1".split(","), new Object[][]{{"b", 2}});

        destroyInfra(epService);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraTwo", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraThree", false);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertFAFInsertResult(EPOnDemandQueryResult resultOne, Object[] objects, String[] propertyNames, EPStatement stmt, boolean isNamedWindow) {
        assertSame(resultOne.getEventType(), stmt.getEventType());
        if (isNamedWindow) {
            assertEquals(1, resultOne.getArray().length);
            EPAssertionUtil.assertPropsPerRow(resultOne.getArray(), propertyNames, new Object[][]{objects});
        } else {
            assertEquals(0, resultOne.getArray().length);
        }
    }

    private void setupInfra(EPServiceProvider epService, boolean isNamedWindow) {
        String eplCreate = isNamedWindow ?
                "@Name('TheInfra') create window MyInfra#keepall as select * from SupportBean" :
                "@Name('TheInfra') create table MyInfra as (theString string primary key, intPrimitive int primary key, longPrimitive long)";
        epService.getEPAdministrator().createEPL(eplCreate);
        String eplInsert = isNamedWindow ?
                "@Name('Insert') insert into MyInfra select * from SupportBean" :
                "@Name('Insert') on SupportBean sb merge MyInfra mi where mi.theString = sb.theString and mi.intPrimitive=sb.intPrimitive" +
                        " when not matched then insert select theString, intPrimitive, longPrimitive";
        epService.getEPAdministrator().createEPL(eplInsert);
    }

    private void destroyInfra(EPServiceProvider epService) {
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static void doubleInt(SupportBean bean) {
        bean.setIntPrimitive(bean.getIntPrimitive() * 2);
    }

    private void assertCtxInfraCountPerCode(EPServiceProvider epService, long[] expectedCountPerCode) {
        for (int i = 0; i < expectedCountPerCode.length; i++) {
            assertEquals("for code " + i, expectedCountPerCode[i], getCtxInfraCount(epService, i));
        }
    }

    private void sendEvent(EventRepresentationChoice eventRepresentationEnum, EPServiceProvider epService, String eventName, String[] attributes) {

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            List<Object> eventObjectArray = new ArrayList<Object>();
            for (String attribute : attributes) {
                String value = attribute.split("=")[1];
                eventObjectArray.add(value);
            }
            epService.getEPRuntime().sendEvent(eventObjectArray.toArray(), eventName);
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> eventMap = new HashMap<String, Object>();
            for (String attribute : attributes) {
                String key = attribute.split("=")[0];
                String value = attribute.split("=")[1];
                eventMap.put(key, value);
            }
            epService.getEPRuntime().sendEvent(eventMap, eventName);
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, eventName));
            for (String attribute : attributes) {
                String key = attribute.split("=")[0];
                String value = attribute.split("=")[1];
                record.put(key, value);
            }
            epService.getEPRuntime().sendEventAvro(record, eventName);
        } else {
            fail();
        }
    }

    private long getCtxInfraCount(EPServiceProvider epService, int hashCode) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select count(*) as c0 from CtxInfra", new ContextPartitionSelector[]{new SupportSelectorByHashCode(hashCode)});
        return (Long) result.getArray()[0].get("c0");
    }

    private long getCtxInfraCatCount(EPServiceProvider epService, String categoryName) {
        EPOnDemandQueryResult result = epService.getEPRuntime().executeQuery("select count(*) as c0 from CtxInfraCat", new ContextPartitionSelector[]{new SupportSelectorCategory(categoryName)});
        return (Long) result.getArray()[0].get("c0");
    }
}
