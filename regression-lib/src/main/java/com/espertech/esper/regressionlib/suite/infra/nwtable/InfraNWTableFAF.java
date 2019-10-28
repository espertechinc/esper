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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.context.SupportHashCodeFuncGranularCRC32;
import com.espertech.esper.regressionlib.support.context.SupportSelectorByHashCode;
import com.espertech.esper.regressionlib.support.context.SupportSelectorCategory;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import com.espertech.esper.runtime.client.EPStatement;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidFAFCompile;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class InfraNWTableFAF implements IndexBackingTableInfo {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraSelectWildcard(true));
        execs.add(new InfraSelectWildcard(false));

        execs.add(new InfraSelectWildcardSceneTwo(true));
        execs.add(new InfraSelectWildcardSceneTwo(false));

        execs.add(new InfraInsert(true));
        execs.add(new InfraInsert(false));

        execs.add(new InfraUpdate(true));
        execs.add(new InfraUpdate(false));

        execs.add(new InfraDelete(true));
        execs.add(new InfraDelete(false));

        execs.add(new InfraDeleteContextPartitioned(true));
        execs.add(new InfraDeleteContextPartitioned(false));

        execs.add(new InfraSelectCountStar(true));
        execs.add(new InfraSelectCountStar(false));

        execs.add(new InfraAggUngroupedRowForAll(true));
        execs.add(new InfraAggUngroupedRowForAll(false));

        execs.add(new InfraInClause(true));
        execs.add(new InfraInClause(false));

        execs.add(new InfraAggUngroupedRowForGroup(true));
        execs.add(new InfraAggUngroupedRowForGroup(false));

        execs.add(new InfraJoin(true, true));
        execs.add(new InfraJoin(false, false));
        execs.add(new InfraJoin(true, false));
        execs.add(new InfraJoin(false, true));

        execs.add(new InfraAggUngroupedRowForEvent(true));
        execs.add(new InfraAggUngroupedRowForEvent(false));

        execs.add(new InfraJoinWhere(true));
        execs.add(new InfraJoinWhere(false));

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            execs.add(new Infra3StreamInnerJoin(rep, true));
            execs.add(new Infra3StreamInnerJoin(rep, false));
        }

        execs.add(new InfraExecuteFilter(true));
        execs.add(new InfraExecuteFilter(false));

        execs.add(new InfraInvalid(true));
        execs.add(new InfraInvalid(false));

        execs.add(new InfraSelectDistinct(true));
        execs.add(new InfraSelectDistinct(false));

        return execs;
    }

    private static class InfraSelectDistinct implements RegressionExecution {
        private final boolean namedWindow;

        private InfraSelectDistinct(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path;
            String query;
            String[] fields;
            EPFireAndForgetQueryResult result;

            // Non-join
            path = setupInfra(env, namedWindow);

            env.sendEventBean(makeBean("E1", 0, 10));
            env.sendEventBean(makeBean("E2", 0, 10));
            env.sendEventBean(makeBean("E3", 0, 11));

            query = "select distinct longPrimitive from MyInfra order by longPrimitive asc";
            fields = "longPrimitive".split(",");
            result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10L}, {11L}});

            env.undeployAll();

            // Join
            path = setupInfraJoin(env, namedWindow);
            insertInfra1Event(env, path, "A", "X");
            insertInfra1Event(env, path, "B", "Y");
            insertInfra2Event(env, path, "X", 10);
            insertInfra2Event(env, path, "Y", 10);

            query = "select distinct value from Infra1 as i1, Infra2 as i2 where i1.keyJoin = i2.keyJoin";
            fields = "value".split(",");
            result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{10d}});

            env.undeployAll();
        }
    }

    private static class InfraExecuteFilter implements RegressionExecution {
        private final boolean namedWindow;

        private InfraExecuteFilter(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, namedWindow);

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 11));
            env.sendEventBean(new SupportBean("E3", 5));

            String query = "select * from MyInfra(intPrimitive > 1, intPrimitive < 10)";
            runAssertionFilter(env, path, query);

            query = "select * from MyInfra(intPrimitive > 1) where intPrimitive < 10";
            runAssertionFilter(env, path, query);

            query = "select * from MyInfra where intPrimitive < 10 and intPrimitive > 1";
            runAssertionFilter(env, path, query);

            env.undeployAll();
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        private final boolean namedWindow;

        private InfraInvalid(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, namedWindow);
            String epl;

            epl = "insert into MyInfra select 1";
            tryInvalidFAFCompile(env, path, epl, "Column '1' could not be assigned to any of the properties of the underlying type (missing column names, event property, setter method or constructor?) [insert into MyInfra select 1]");

            epl = "selectoo man";
            tryInvalidFAFCompile(env, path, epl, "Incorrect syntax near 'selectoo' [selectoo man]");
            
            epl = "select * from MyInfra output every 10 seconds";
            tryInvalidFAFCompile(env, path, epl, "Output rate limiting is not a supported feature of on-demand queries [select * from MyInfra output every 10 seconds]");

            epl = "select prev(1, theString) from MyInfra";
            tryInvalidFAFCompile(env, path, epl, "Failed to validate select-clause expression 'prev(1,theString)': Previous function cannot be used in this context [select prev(1, theString) from MyInfra]");

            epl = "insert into MyInfra(intPrimitive) select 'a'";
            if (namedWindow) {
                tryInvalidFAFCompile(env, path, epl, "Invalid assignment of column 'intPrimitive' of type 'java.lang.String' to event property 'intPrimitive' typed as 'int', column and parameter types mismatch [insert into MyInfra(intPrimitive) select 'a']");
            } else {
                tryInvalidFAFCompile(env, path, epl, "Invalid assignment of column 'intPrimitive' of type 'java.lang.String' to event property 'intPrimitive' typed as 'java.lang.Integer', column and parameter types mismatch [insert into MyInfra(intPrimitive) select 'a']");
            }

            epl = "insert into MyInfra(intPrimitive, theString) select 1";
            tryInvalidFAFCompile(env, path, epl, "Number of supplied values in the select or values clause does not match insert-into clause [insert into MyInfra(intPrimitive, theString) select 1]");

            epl = "insert into MyInfra select 1 as intPrimitive from MyInfra";
            tryInvalidFAFCompile(env, path, epl, "Insert-into fire-and-forget query can only consist of an insert-into clause and a select-clause [insert into MyInfra select 1 as intPrimitive from MyInfra]");

            epl = "insert into MyInfra(intPrimitive, theString) values (1, 'a', 1)";
            tryInvalidFAFCompile(env, path, epl, "Number of supplied values in the select or values clause does not match insert-into clause [insert into MyInfra(intPrimitive, theString) values (1, 'a', 1)]");

            if (namedWindow) {
                epl = "select * from pattern [every MyInfra]";
                tryInvalidFAFCompile(env, path, epl, "On-demand queries require tables or named windows and do not allow event streams or patterns [select * from pattern [every MyInfra]]");

                epl = "select * from MyInfra#uni(intPrimitive)";
                tryInvalidFAFCompile(env, path, epl, "Views are not a supported feature of on-demand queries [select * from MyInfra#uni(intPrimitive)]");
            }

            epl = "on MyInfra select * from MyInfra";
            tryInvalidFAFCompile(env, path, epl, "Provided EPL expression is a continuous query expression (not an on-demand query)");

            env.undeployAll();
        }
    }

    private static class Infra3StreamInnerJoin implements RegressionExecution {
        private final EventRepresentationChoice eventRepresentationEnum;
        private final boolean namedWindow;

        private Infra3StreamInnerJoin(EventRepresentationChoice eventRepresentationEnum, boolean namedWindow) {
            this.eventRepresentationEnum = eventRepresentationEnum;
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String eplEvents = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedProduct.class) + " create schema Product (productId string, categoryId string);" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedCategory.class) + " create schema Category (categoryId string, owner string);" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedProductOwnerDetails.class) + " create schema ProductOwnerDetails (productId string, owner string);";
            String epl;
            if (namedWindow) {
                epl = eplEvents +
                    "create window WinProduct#keepall as select * from Product;" +
                    "create window WinCategory#keepall as select * from Category;" +
                    "create window WinProductOwnerDetails#keepall as select * from ProductOwnerDetails;" +
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

            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType(epl, path);

            sendEvent(eventRepresentationEnum, env, "Product", new String[]{"productId=Product1", "categoryId=Category1"});
            sendEvent(eventRepresentationEnum, env, "Product", new String[]{"productId=Product2", "categoryId=Category1"});
            sendEvent(eventRepresentationEnum, env, "Product", new String[]{"productId=Product3", "categoryId=Category1"});
            sendEvent(eventRepresentationEnum, env, "Category", new String[]{"categoryId=Category1", "owner=Petar"});
            sendEvent(eventRepresentationEnum, env, "ProductOwnerDetails", new String[]{"productId=Product1", "owner=Petar"});

            String[] fields = "WinProduct.productId".split(",");
            EventBean[] queryResults;
            queryResults = env.compileExecuteFAF("" +
                "select WinProduct.productId " +
                " from WinProduct" +
                " inner join WinCategory on WinProduct.categoryId=WinCategory.categoryId" +
                " inner join WinProductOwnerDetails on WinProduct.productId=WinProductOwnerDetails.productId", path
            ).getArray();
            EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

            queryResults = env.compileExecuteFAF("" +
                "select WinProduct.productId " +
                " from WinProduct" +
                " inner join WinCategory on WinProduct.categoryId=WinCategory.categoryId" +
                " inner join WinProductOwnerDetails on WinProduct.productId=WinProductOwnerDetails.productId" +
                " where WinCategory.owner=WinProductOwnerDetails.owner", path
            ).getArray();
            EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

            queryResults = env.compileExecuteFAF("" +
                "select WinProduct.productId " +
                " from WinProduct, WinCategory, WinProductOwnerDetails" +
                " where WinCategory.owner=WinProductOwnerDetails.owner" +
                " and WinProduct.categoryId=WinCategory.categoryId" +
                " and WinProduct.productId=WinProductOwnerDetails.productId", path
            ).getArray();
            EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

            String eplQuery = "select WinProduct.productId " +
                " from WinProduct" +
                " inner join WinCategory on WinProduct.categoryId=WinCategory.categoryId" +
                " inner join WinProductOwnerDetails on WinProduct.productId=WinProductOwnerDetails.productId" +
                " having WinCategory.owner=WinProductOwnerDetails.owner";
            queryResults = env.compileExecuteFAF(eplQuery, path).getArray();
            EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

            EPStatementObjectModel model = env.eplToModel(eplQuery);
            queryResults = env.compileExecuteFAF(model, path).getArray();
            EPAssertionUtil.assertPropsPerRow(queryResults, fields, new Object[][]{{"Product1"}});

            env.undeployAll();
        }
    }

    private static class InfraJoinWhere implements RegressionExecution {
        private final boolean namedWindow;

        private InfraJoinWhere(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfraJoin(env, namedWindow);

            String queryAgg = "select w1.key, sum(value) from Infra1 w1, Infra2 w2 WHERE w1.keyJoin = w2.keyJoin GROUP BY w1.key order by w1.key";
            String[] fieldsAgg = "w1.key,sum(value)".split(",");
            String queryNoagg = "select w1.key, w2.value from Infra1 w1, Infra2 w2 where w1.keyJoin = w2.keyJoin and value = 1 order by w1.key";
            String[] fieldsNoagg = "w1.key,w2.value".split(",");

            EventBean[] result = env.compileExecuteFAF(queryAgg, path).getArray();
            assertEquals(0, result.length);
            result = env.compileExecuteFAF(queryNoagg, path).getArray();
            assertNull(result);

            insertInfra1Event(env, path, "key1", "keyJoin1");

            result = env.compileExecuteFAF(queryAgg, path).getArray();
            assertEquals(0, result.length);
            result = env.compileExecuteFAF(queryNoagg, path).getArray();
            assertNull(result);

            insertInfra2Event(env, path, "keyJoin1", 1d);

            result = env.compileExecuteFAF(queryAgg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}});
            result = env.compileExecuteFAF(queryNoagg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}});

            env.milestone(0);

            insertInfra2Event(env, path, "keyJoin2", 2d);

            result = env.compileExecuteFAF(queryAgg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}});
            result = env.compileExecuteFAF(queryNoagg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}});

            insertInfra1Event(env, path, "key2", "keyJoin2");

            result = env.compileExecuteFAF(queryAgg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}, {"key2", 2d}});
            result = env.compileExecuteFAF(queryNoagg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}});

            insertInfra2Event(env, path, "keyJoin2", 1d);

            result = env.compileExecuteFAF(queryAgg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsAgg, new Object[][]{{"key1", 1d}, {"key2", 3d}});
            result = env.compileExecuteFAF(queryNoagg, path).getArray();
            EPAssertionUtil.assertPropsPerRow(result, fieldsNoagg, new Object[][]{{"key1", 1d}, {"key2", 1d}});

            env.undeployAll();
        }
    }

    private static class InfraAggUngroupedRowForEvent implements RegressionExecution {
        private final boolean namedWindow;

        private InfraAggUngroupedRowForEvent(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, namedWindow);

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 11));
            env.sendEventBean(new SupportBean("E3", 5));
            String[] fields = new String[]{"theString", "total"};

            String query = "select theString, sum(intPrimitive) as total from MyInfra";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E1", 16}, {"E2", 16}, {"E3", 16}});

            env.sendEventBean(new SupportBean("E4", -2));
            result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E1", 14}, {"E2", 14}, {"E3", 14}, {"E4", 14}});

            env.undeployAll();
        }
    }

    private static class InfraJoin implements RegressionExecution {
        private final boolean isFirstNW;
        private final boolean isSecondNW;

        private InfraJoin(boolean isFirstNW, boolean isSecondNW) {
            this.isFirstNW = isFirstNW;
            this.isSecondNW = isSecondNW;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, isFirstNW);

            String eplSecondCreate = isSecondNW ?
                "create window MySecondInfra#keepall as select * from SupportBean_A" :
                "create table MySecondInfra as (id string primary key)";
            env.compileDeploy(eplSecondCreate, path);
            String eplSecondFill = isSecondNW ?
                "insert into MySecondInfra select * from SupportBean_A " :
                "on SupportBean_A sba merge MySecondInfra msi where msi.id = sba.id when not matched then insert select id";
            env.compileDeploy(eplSecondFill, path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 11));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 5));
            env.sendEventBean(new SupportBean_A("E2"));
            String[] fields = new String[]{"theString", "intPrimitive", "id"};

            String query = "select theString, intPrimitive, id from MyInfra nw1, " +
                "MySecondInfra nw2 where nw1.theString = nw2.id";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E2", 11, "E2"}});

            env.sendEventBean(new SupportBean("E3", 1));
            env.sendEventBean(new SupportBean("E3", 2));
            env.sendEventBean(new SupportBean_A("E3"));

            result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E2", 11, "E2"}, {"E3", 1, "E3"}, {"E3", 2, "E3"}});

            env.undeployAll();
        }
    }

    private static class InfraAggUngroupedRowForGroup implements RegressionExecution {
        private final boolean namedWindow;

        private InfraAggUngroupedRowForGroup(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, namedWindow);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 11));
            env.sendEventBean(new SupportBean("E1", 5));
            String[] fields = new String[]{"theString", "total"};

            String query = "select theString, sum(intPrimitive) as total from MyInfra group by theString order by theString asc";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E1", 6}, {"E2", 11}});

            env.sendEventBean(new SupportBean("E2", -2));
            env.sendEventBean(new SupportBean("E3", 3));
            result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E1", 6}, {"E2", 9}, {"E3", 3}});

            env.undeployAll();
        }
    }

    private static class InfraInClause implements RegressionExecution {
        private final boolean namedWindow;

        private InfraInClause(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = setupInfra(env, namedWindow);

            env.sendEventBean(makeBean("E1", 10, 100L));
            env.sendEventBean(makeBean("E2", 20, 200L));
            env.sendEventBean(makeBean("E3", 30, 300L));
            env.sendEventBean(makeBean("E4", 40, 400L));

            // try no index
            runAssertionIn(env, path);

            // try suitable index
            env.compileDeploy("@name('stmtIdx1') create index Idx1 on MyInfra(theString, intPrimitive)", path);
            runAssertionIn(env, path);
            env.undeployModuleContaining("stmtIdx1");

            // backwards index
            env.compileDeploy("@name('stmtIdx2') create index Idx2 on MyInfra(intPrimitive, theString)", path);
            runAssertionIn(env, path);
            env.undeployModuleContaining("stmtIdx2");

            // partial index
            env.compileDeploy("@name('stmtIdx3') create index Idx3 on MyInfra(intPrimitive)", path);
            runAssertionIn(env, path);
            env.undeployModuleContaining("stmtIdx3");

            env.undeployAll();
        }
    }

    private static void runAssertionIn(RegressionEnvironment env, RegressionPath path) {
        tryAssertionIn(env, path, "theString in ('E2', 'E3') and intPrimitive in (10, 20)", new Long[]{200L});
        tryAssertionIn(env, path, "intPrimitive in (30, 20) and theString in ('E4', 'E1')", new Long[]{});
        tryAssertionIn(env, path, "intPrimitive in (30, 20) and theString in ('E2', 'E1')", new Long[]{200L});
        tryAssertionIn(env, path, "theString in ('E2', 'E3') and intPrimitive in (20, 30)", new Long[]{200L, 300L});
        tryAssertionIn(env, path, "theString in ('E2', 'E3') and intPrimitive in (30, 20)", new Long[]{200L, 300L});
        tryAssertionIn(env, path, "theString in ('E1', 'E2', 'E3', 'E4') and intPrimitive in (10, 20, 30)", new Long[]{100L, 200L, 300L});
    }

    private static void tryAssertionIn(RegressionEnvironment env, RegressionPath path, String filter, Long[] expected) {
        EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfra where " + filter, path);
        assertEquals(result.getArray().length, expected.length);
        List<Long> values = new ArrayList<>();
        for (EventBean event : result.getArray()) {
            values.add((Long) event.get("longPrimitive"));
        }
        EPAssertionUtil.assertEqualsAnyOrder(expected, values.toArray());
    }

    private static class InfraAggUngroupedRowForAll implements RegressionExecution {
        private final boolean namedWindow;

        private InfraAggUngroupedRowForAll(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, namedWindow);

            env.sendEventBean(new SupportBean("E1", 0));
            env.sendEventBean(new SupportBean("E2", 11));
            env.sendEventBean(new SupportBean("E3", 5));
            String[] fields = new String[]{"total"};

            String query = "select sum(intPrimitive) as total from MyInfra";
            EPFireAndForgetQueryResult result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{16}});

            env.sendEventBean(new SupportBean("E4", -2));
            result = env.compileExecuteFAF(query, path);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{14}});

            env.undeployAll();
        }
    }

    private static class InfraSelectCountStar implements RegressionExecution {
        private final boolean namedWindow;

        private InfraSelectCountStar(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, namedWindow);

            String[] fields = new String[]{"cnt"};
            String epl = "select count(*) as cnt from MyInfra";
            EPCompiled query = env.compileFAF(epl, path);
            EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(query);

            EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(query);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{0L}});
            EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{0L}});

            env.sendEventBean(new SupportBean("E1", 1));
            result = env.runtime().getFireAndForgetService().executeQuery(query);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{1L}});
            EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{1L}});
            result = env.runtime().getFireAndForgetService().executeQuery(query);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{1L}});
            EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{1L}});

            env.sendEventBean(new SupportBean("E2", 2));
            result = env.runtime().getFireAndForgetService().executeQuery(query);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{2L}});
            EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{2L}});

            EPStatementObjectModel model = env.eplToModel(epl);
            EPCompiled compiledFromModel = env.compileFAF(model, path);
            result = env.compileExecuteFAF(model, path);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{2L}});
            EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{2L}});

            EPFireAndForgetPreparedQuery preparedFromModel = env.runtime().getFireAndForgetService().prepareQuery(compiledFromModel);
            EPAssertionUtil.assertPropsPerRow(preparedFromModel.execute().iterator(), fields, new Object[][]{{2L}});

            env.undeployAll();
        }
    }

    private static class InfraSelectWildcard implements RegressionExecution {
        private final boolean namedWindow;

        private InfraSelectWildcard(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, namedWindow);

            String query = "select * from MyInfra";
            EPCompiled compiled = env.compileFAF(query, path);
            EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled);
            final String[] fields = new String[]{"theString", "intPrimitive"};
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, null);

            EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(compiled);
            EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, null);

            env.sendEventBean(new SupportBean("E1", 1));
            result = env.runtime().getFireAndForgetService().executeQuery(compiled);
            EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E1", 1}});
            EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{"E1", 1}});

            env.sendEventBean(new SupportBean("E2", 2));
            result = env.runtime().getFireAndForgetService().executeQuery(compiled);
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(prepared.execute().iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

            env.undeployAll();
        }
    }

    public static class InfraSelectWildcardSceneTwo implements RegressionExecution {
        private final boolean namedWindow;

        private InfraSelectWildcardSceneTwo(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"key", "value"};
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = namedWindow ?
                "@name('create') create window MyInfra.win:keepall() as select theString as key, intBoxed as value from SupportBean" :
                "@name('create') create table MyInfra (key string primary key, value int)";
            env.compileDeploy(stmtTextCreate, path).addListener("create");

            String stmtTextInsert = "insert into MyInfra(key, value) select irstream theString, intBoxed from SupportBean";
            env.compileDeploy(stmtTextInsert, path);

            env.milestone(0);

            // send event
            sendBeanInt(env, "G1", 10);
            sendBeanInt(env, "G2", -1);
            sendBeanInt(env, "G3", -2);
            sendBeanInt(env, "G4", 21);

            env.milestone(1);

            // perform query
            EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyInfra where value> 0", path);
            assertEquals(2, result.getArray().length);
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"G1", 10}, {"G4", 21}});
            } else {
                EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, new Object[][]{{"G1", 10}, {"G4", 21}});
            }

            env.milestone(2);

            // perform query
            result = env.compileExecuteFAF("select * from MyInfra where value < 0", path);
            assertEquals(2, result.getArray().length);
            EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields, new Object[][]{{"G2", -1}, {"G3", -2}});

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
    }

    private static class InfraDeleteContextPartitioned implements RegressionExecution {
        private final boolean namedWindow;

        private InfraDeleteContextPartitioned(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // test hash-segmented context
            String eplCtx = "create context MyCtx coalesce consistent_hash_crc32(theString) from SupportBean granularity 4 preallocate";
            env.compileDeploy(eplCtx, path);

            String eplCreate = namedWindow ?
                "context MyCtx create window CtxInfra#keepall as SupportBean" :
                "context MyCtx create table CtxInfra (theString string primary key, intPrimitive int primary key)";
            env.compileDeploy(eplCreate, path);
            String eplPopulate = namedWindow ?
                "context MyCtx insert into CtxInfra select * from SupportBean" :
                "context MyCtx on SupportBean sb merge CtxInfra ci where sb.theString = ci.theString and sb.intPrimitive = ci.intPrimitive when not matched then insert select theString, intPrimitive";
            env.compileDeploy(eplPopulate, path);

            SupportHashCodeFuncGranularCRC32 codeFunc = new SupportHashCodeFuncGranularCRC32(4);
            int[] codes = new int[5];
            for (int i = 0; i < 5; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
                codes[i] = codeFunc.codeFor("E" + i);
            }
            EPAssertionUtil.assertEqualsExactOrder(new int[]{3, 1, 3, 1, 2}, codes);   // just to make sure CRC32 didn't change

            // assert counts individually per context partition
            assertCtxInfraCountPerCode(env, path, new long[]{0, 2, 1, 2});

            // delete per context partition (E0 ended up in '3')
            compileExecuteFAF(env, path, "delete from CtxInfra where theString = 'E0'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(1)});
            assertCtxInfraCountPerCode(env, path, new long[]{0, 2, 1, 2});

            EPFireAndForgetQueryResult result = compileExecuteFAF(env, path, "delete from CtxInfra where theString = 'E0'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(3)});
            assertCtxInfraCountPerCode(env, path, new long[]{0, 2, 1, 1});
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString".split(","), new Object[][]{{"E0"}});
            }

            // delete per context partition (E1 ended up in '1')
            compileExecuteFAF(env, path, "delete from CtxInfra where theString = 'E1'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(0)});
            assertCtxInfraCountPerCode(env, path, new long[]{0, 2, 1, 1});

            compileExecuteFAF(env, path, "delete from CtxInfra where theString = 'E1'", new ContextPartitionSelector[]{new SupportSelectorByHashCode(1)});
            assertCtxInfraCountPerCode(env, path, new long[]{0, 1, 1, 1});
            env.undeployAll();

            // test category-segmented context
            String eplCtxCategory = "create context MyCtxCat group by intPrimitive < 0 as negative, group by intPrimitive > 0 as positive from SupportBean";
            env.compileDeploy(eplCtxCategory, path);
            env.compileDeploy("context MyCtxCat create window CtxInfraCat#keepall as SupportBean", path);
            env.compileDeploy("context MyCtxCat insert into CtxInfraCat select * from SupportBean", path);

            env.sendEventBean(new SupportBean("E1", -2));
            env.sendEventBean(new SupportBean("E2", 1));
            env.sendEventBean(new SupportBean("E3", -3));
            env.sendEventBean(new SupportBean("E4", 2));
            assertEquals(2L, getCtxInfraCatCount(env, path, "positive"));
            assertEquals(2L, getCtxInfraCatCount(env, path, "negative"));

            result = env.compileExecuteFAF("context MyCtxCat delete from CtxInfraCat where context.label = 'negative'", path);
            assertEquals(2L, getCtxInfraCatCount(env, path, "positive"));
            assertEquals(0L, getCtxInfraCatCount(env, path, "negative"));
            EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString".split(","), new Object[][]{{"E1"}, {"E3"}});

            env.undeployAll();
        }

        private EPFireAndForgetQueryResult compileExecuteFAF(RegressionEnvironment env, RegressionPath path, String epl, ContextPartitionSelector[] selectors) {
            EPCompiled compiled = env.compileFAF(epl, path);
            return env.runtime().getFireAndForgetService().executeQuery(compiled, selectors);
        }
    }

    private static class InfraDelete implements RegressionExecution {
        private final boolean namedWindow;

        private InfraDelete(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = setupInfra(env, namedWindow);

            // test delete-all
            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }
            assertEquals(10L, getMyInfraCount(env, path));
            EPFireAndForgetQueryResult result = env.compileExecuteFAF("delete from MyInfra", path);
            assertEquals(0L, getMyInfraCount(env, path));
            if (namedWindow) {
                assertEquals(env.statement("TheInfra").getEventType(), result.getEventType());
                assertEquals(10, result.getArray().length);
                assertEquals("E0", result.getArray()[0].get("theString"));
            } else {
                assertEquals(0, result.getArray().length);
            }

            // test SODA + where-clause
            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }
            assertEquals(10L, getMyInfraCount(env, path));
            String eplWithWhere = "delete from MyInfra where theString=\"E1\"";
            EPStatementObjectModel modelWithWhere = env.eplToModel(eplWithWhere);
            assertEquals(eplWithWhere, modelWithWhere.toEPL());
            result = env.compileExecuteFAF(modelWithWhere, path);
            assertEquals(9L, getMyInfraCount(env, path));
            if (namedWindow) {
                assertEquals(env.statement("TheInfra").getEventType(), result.getEventType());
                EPAssertionUtil.assertPropsPerRow(result.getArray(), "theString".split(","), new Object[][]{{"E1"}});
            }

            // test SODA delete-all
            String eplDelete = "delete from MyInfra";
            EPStatementObjectModel modelDeleteOnly = env.eplToModel(eplDelete);
            assertEquals(eplDelete, modelDeleteOnly.toEPL());
            env.compileExecuteFAF(modelDeleteOnly, path);
            assertEquals(0L, getMyInfraCount(env, path));

            for (int i = 0; i < 5; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }

            // test with index
            if (namedWindow) {
                env.compileDeploy("create unique index Idx1 on MyInfra (theString)", path);
            }
            runQueryAssertCount(env, path, INDEX_CALLBACK_HOOK + "delete from MyInfra where theString = 'E1' and intPrimitive = 0", 5, namedWindow ? "Idx1" : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
            runQueryAssertCount(env, path, INDEX_CALLBACK_HOOK + "delete from MyInfra where theString = 'E1' and intPrimitive = 1", 4, namedWindow ? "Idx1" : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
            runQueryAssertCount(env, path, INDEX_CALLBACK_HOOK + "delete from MyInfra where theString = 'E2'", 3, namedWindow ? "Idx1" : null, namedWindow ? BACKING_SINGLE_UNIQUE : null);
            runQueryAssertCount(env, path, INDEX_CALLBACK_HOOK + "delete from MyInfra where intPrimitive = 4", 2, null, null);

            // test with alias
            runQueryAssertCount(env, path, INDEX_CALLBACK_HOOK + "delete from MyInfra as w1 where w1.theString = 'E3'", 1, namedWindow ? "Idx1" : null, namedWindow ? BACKING_SINGLE_UNIQUE : null);

            // test consumption
            env.compileDeploy("@name('s0') select rstream * from MyInfra", path).addListener("s0");
            env.compileExecuteFAF("delete from MyInfra", path);
            final String[] fields = new String[]{"theString", "intPrimitive"};
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E0", 0});
            } else {
                assertFalse(env.listener("s0").isInvoked());
            }

            env.undeployAll();
        }
    }

    private static class InfraUpdate implements RegressionExecution {

        private final boolean namedWindow;

        private InfraUpdate(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = setupInfra(env, namedWindow);
            final String[] fields = new String[]{"theString", "intPrimitive"};

            // test update-all
            for (int i = 0; i < 2; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }
            EPFireAndForgetQueryResult result = compileExecute("update MyInfra set theString = 'ABC'", path, env);
            EPAssertionUtil.assertPropsPerRow(env.iterator("TheInfra"), fields, new Object[][]{{"ABC", 0}, {"ABC", 1}});
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"ABC", 0}, {"ABC", 1}});
            }

            // test update with where-clause
            env.compileExecuteFAF("delete from MyInfra", path);
            for (int i = 0; i < 3; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }
            result = env.compileExecuteFAF("update MyInfra set theString = 'X', intPrimitive=-1 where theString = 'E1' and intPrimitive = 1", path);
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"X", -1}});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("TheInfra"), fields, new Object[][]{{"E0", 0}, {"E2", 2}, {"X", -1}});

            // test update with SODA
            String epl = "update MyInfra set intPrimitive=intPrimitive+10 where theString=\"E2\"";
            EPStatementObjectModel model = env.eplToModel(epl);
            assertEquals(epl, model.toEPL());
            result = env.compileExecuteFAF(model, path);
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E2", 12}});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("TheInfra"), fields, new Object[][]{{"E0", 0}, {"X", -1}, {"E2", 12}});

            // test update with initial value
            result = env.compileExecuteFAF("update MyInfra set intPrimitive=5, theString='x', theString = initial.theString || 'y', intPrimitive=initial.intPrimitive+100 where theString = 'E0'", path);
            if (namedWindow) {
                EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E0y", 100}});
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("TheInfra"), fields, new Object[][]{{"X", -1}, {"E2", 12}, {"E0y", 100}});

            env.compileExecuteFAF("delete from MyInfra", path);
            for (int i = 0; i < 5; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }

            // test with index
            if (namedWindow) {
                env.compileDeploy("create unique index Idx1 on MyInfra (theString)", path);
            }
            runQueryAssertCountNonNegative(env, path, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where theString = 'E1' and intPrimitive = 0", 5, namedWindow ? "Idx1" : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
            runQueryAssertCountNonNegative(env, path, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where theString = 'E1' and intPrimitive = 1", 4, namedWindow ? "Idx1" : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : BACKING_MULTI_UNIQUE);
            runQueryAssertCountNonNegative(env, path, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where theString = 'E2'", 3, namedWindow ? "Idx1" : null, namedWindow ? BACKING_SINGLE_UNIQUE : null);
            runQueryAssertCountNonNegative(env, path, INDEX_CALLBACK_HOOK + "update MyInfra set intPrimitive=-1 where intPrimitive = 4", 2, null, null);

            // test with alias
            runQueryAssertCountNonNegative(env, path, INDEX_CALLBACK_HOOK + "update MyInfra as w1 set intPrimitive=-1 where w1.theString = 'E3'", 1, namedWindow ? "Idx1" : null, namedWindow ? BACKING_SINGLE_UNIQUE : null);

            // test consumption
            env.compileDeploy("@name('s0') select irstream * from MyInfra", path).addListener("s0");
            env.compileExecuteFAF("update MyInfra set intPrimitive=1000 where theString = 'E0'", path);
            if (namedWindow) {
                EPAssertionUtil.assertProps(env.listener("s0").assertPairGetIRAndReset(), fields, new Object[]{"E0", 1000}, new Object[]{"E0", 0});
            }

            // test update via UDF and setter
            if (namedWindow) {
                env.compileExecuteFAF("delete from MyInfra", path);
                env.sendEventBean(new SupportBean("A", 10));
                env.compileExecuteFAF("update MyInfra mw set mw.setTheString('XYZ'), doubleInt(mw)", path);
                EPAssertionUtil.assertPropsPerRow(env.iterator("TheInfra"),
                    "theString,intPrimitive".split(","), new Object[][]{{"XYZ", 20}});
            }

            env.undeployAll();
        }
    }

    private static class InfraInsert implements RegressionExecution {
        private final boolean namedWindow;

        private InfraInsert(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = setupInfra(env, namedWindow);

            Supplier<EPStatement> stmt = () -> env.statement("TheInfra");
            String[] propertyNames = "theString,intPrimitive".split(",");

            // try column name provided with insert-into
            String eplSelect = "insert into MyInfra (theString, intPrimitive) select 'a', 1";
            EPFireAndForgetQueryResult resultOne = env.compileExecuteFAF(eplSelect, path);
            assertFAFInsertResult(resultOne, new Object[]{"a", 1}, propertyNames, stmt.get(), namedWindow);
            EPAssertionUtil.assertPropsPerRow(stmt.get().iterator(), propertyNames, new Object[][]{{"a", 1}});

            // try SODA and column name not provided with insert-into
            String eplTwo = "insert into MyInfra select \"b\" as theString, 2 as intPrimitive";
            EPStatementObjectModel modelWSelect = env.eplToModel(eplTwo);
            assertEquals(eplTwo, modelWSelect.toEPL());
            EPFireAndForgetQueryResult resultTwo = env.compileExecuteFAF(modelWSelect, path);
            assertFAFInsertResult(resultTwo, new Object[]{"b", 2}, propertyNames, stmt.get(), namedWindow);
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.get().iterator(), propertyNames, new Object[][]{{"a", 1}, {"b", 2}});

            // create unique index, insert duplicate row
            env.compileDeploy("create unique index I1 on MyInfra (theString)", path);
            try {
                String eplThree = "insert into MyInfra (theString) select 'a' as theString";
                env.compileExecuteFAF(eplThree, path);
            } catch (EPException ex) {
                assertEquals("Unique index violation, index 'I1' is a unique index and key 'a' already exists", ex.getMessage());
            }
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("TheInfra"), propertyNames, new Object[][]{{"a", 1}, {"b", 2}});

            // try second no-column-provided version
            String eplMyInfraThree = namedWindow ?
                "@name('InfraThree') create window MyInfraThree#keepall as (p0 string, p1 int)" :
                "@name('InfraThree') create table MyInfraThree as (p0 string, p1 int)";
            env.compileDeploy(eplMyInfraThree, path);
            env.compileExecuteFAF("insert into MyInfraThree select 'a' as p0, 1 as p1", path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("InfraThree"), "p0,p1".split(","), new Object[][]{{"a", 1}});

            // try enum-value insert
            String epl = "create schema MyMode (mode " + SupportEnum.class.getName() + ");\n" +
                (namedWindow ?
                    "@name('enumwin') create window MyInfraTwo#unique(mode) as MyMode" :
                    "@name('enumwin') create table MyInfraTwo as (mode " + SupportEnum.class.getName() + ");\n");
            env.compileDeploy(epl, path);
            env.compileExecuteFAF("insert into MyInfraTwo select " + SupportEnum.class.getName() + "." + SupportEnum.ENUM_VALUE_2.name() + " as mode", path);
            EPAssertionUtil.assertProps(env.iterator("enumwin").next(), "mode".split(","), new Object[]{SupportEnum.ENUM_VALUE_2});

            // try insert-into with values-keyword and explicit column names
            env.compileExecuteFAF("delete from MyInfra", path);
            String eplValuesKW = "insert into MyInfra(theString, intPrimitive) values (\"a\", 1)";
            EPFireAndForgetQueryResult resultValuesKW = env.compileExecuteFAF(eplValuesKW, path);
            assertFAFInsertResult(resultValuesKW, new Object[]{"a", 1}, propertyNames, stmt.get(), namedWindow);
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.get().iterator(), propertyNames, new Object[][]{{"a", 1}});

            // try insert-into with values model
            env.compileExecuteFAF("delete from MyInfra", path);
            EPStatementObjectModel modelWValuesKW = env.eplToModel(eplValuesKW);
            assertEquals(eplValuesKW, modelWValuesKW.toEPL());
            env.compileExecuteFAF(modelWValuesKW, path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.get().iterator(), propertyNames, new Object[][]{{"a", 1}});

            // try insert-into with values-keyword and as-names
            env.compileExecuteFAF("delete from MyInfraThree", path);
            String eplValuesWithoutCols = "insert into MyInfraThree values ('b', 2)";
            env.compileExecuteFAF(eplValuesWithoutCols, path);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("InfraThree"), "p0,p1".split(","), new Object[][]{{"b", 2}});

            env.undeployAll();
        }
    }

    private static void assertFAFInsertResult(EPFireAndForgetQueryResult resultOne, Object[] objects, String[] propertyNames, EPStatement stmt, boolean namedWindow) {
        assertSame(resultOne.getEventType(), stmt.getEventType());
        if (namedWindow) {
            assertEquals(1, resultOne.getArray().length);
            EPAssertionUtil.assertPropsPerRow(resultOne.getArray(), propertyNames, new Object[][]{objects});
        } else {
            assertEquals(0, resultOne.getArray().length);
        }
    }

    private static RegressionPath setupInfra(RegressionEnvironment env, boolean namedWindow) {
        RegressionPath path = new RegressionPath();
        String eplCreate = namedWindow ?
            "@Name('TheInfra') create window MyInfra#keepall as select * from SupportBean" :
            "@Name('TheInfra') create table MyInfra as (theString string primary key, intPrimitive int primary key, longPrimitive long)";
        env.compileDeploy(eplCreate, path);
        String eplInsert = namedWindow ?
            "@Name('Insert') insert into MyInfra select * from SupportBean" :
            "@Name('Insert') on SupportBean sb merge MyInfra mi where mi.theString = sb.theString and mi.intPrimitive=sb.intPrimitive" +
                " when not matched then insert select theString, intPrimitive, longPrimitive";
        env.compileDeploy(eplInsert, path);
        return path;
    }

    private static SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static void doubleInt(SupportBean bean) {
        bean.setIntPrimitive(bean.getIntPrimitive() * 2);
    }

    private static void assertCtxInfraCountPerCode(RegressionEnvironment env, RegressionPath path, long[] expectedCountPerCode) {
        for (int i = 0; i < expectedCountPerCode.length; i++) {
            assertEquals("for code " + i, expectedCountPerCode[i], getCtxInfraCount(env, path, i));
        }
    }

    private static void sendEvent(EventRepresentationChoice eventRepresentationEnum, RegressionEnvironment env, String eventName, String[] attributes) {

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            List<Object> eventObjectArray = new ArrayList<Object>();
            for (String attribute : attributes) {
                String value = attribute.split("=")[1];
                eventObjectArray.add(value);
            }
            env.sendEventObjectArray(eventObjectArray.toArray(), eventName);
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> eventMap = new HashMap<>();
            for (String attribute : attributes) {
                String key = attribute.split("=")[0];
                String value = attribute.split("=")[1];
                eventMap.put(key, value);
            }
            env.sendEventMap(eventMap, eventName);
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(eventName)));
            for (String attribute : attributes) {
                String key = attribute.split("=")[0];
                String value = attribute.split("=")[1];
                record.put(key, value);
            }
            env.eventService().sendEventAvro(record, eventName);
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject event = new JsonObject();
            for (String attribute : attributes) {
                String key = attribute.split("=")[0];
                String value = attribute.split("=")[1];
                event.add(key, value);
            }
            env.eventService().sendEventJson(event.toString(), eventName);
        } else {
            fail();
        }
    }

    private static RegressionPath setupInfraJoin(RegressionEnvironment env, boolean namedWindow) {

        String eplCreateOne = namedWindow ?
            (EventRepresentationChoice.MAP.getAnnotationText() + " create window Infra1#keepall (key String, keyJoin String)") :
            "create table Infra1 (key String primary key, keyJoin String)";
        String eplCreateTwo = namedWindow ?
            (EventRepresentationChoice.MAP.getAnnotationText() + " create window Infra2#keepall (keyJoin String, value double)") :
            "create table Infra2 (keyJoin String primary key, value double primary key)";
        RegressionPath path = new RegressionPath();
        env.compileDeploy(eplCreateOne, path);
        env.compileDeploy(eplCreateTwo, path);

        return path;
    }

    private static long getCtxInfraCount(RegressionEnvironment env, RegressionPath path, int hashCode) {
        EPCompiled compiled = env.compileFAF("select count(*) as c0 from CtxInfra", path);
        EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled, new ContextPartitionSelector[]{new SupportSelectorByHashCode(hashCode)});
        return (Long) result.getArray()[0].get("c0");
    }

    private static long getCtxInfraCatCount(RegressionEnvironment env, RegressionPath path, String categoryName) {
        EPCompiled compiled = env.compileFAF("select count(*) as c0 from CtxInfraCat", path);
        EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(compiled, new ContextPartitionSelector[]{new SupportSelectorCategory(categoryName)});
        return (Long) result.getArray()[0].get("c0");
    }

    private static void runAssertionFilter(RegressionEnvironment env, RegressionPath path, String query) {
        String[] fields = "theString,intPrimitive".split(",");
        EPFireAndForgetQueryResult result = env.compileExecuteFAF(query, path);
        EPAssertionUtil.assertPropsPerRow(result.iterator(), fields, new Object[][]{{"E3", 5}});

        EPCompiled compiled = env.compileFAF(query, path);
        EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(compiled);
        EPAssertionUtil.assertPropsPerRow(prepared.execute().iterator(), fields, new Object[][]{{"E3", 5}});
    }

    private static void runQueryAssertCountNonNegative(RegressionEnvironment env, RegressionPath path, String epl, int count, String indexName, String backingClass) {
        SupportQueryPlanIndexHook.reset();
        env.compileExecuteFAF(epl, path);
        long actual = (Long) env.compileExecuteFAF("select count(*) as c0 from MyInfra where intPrimitive >= 0", path).getArray()[0].get("c0");
        assertEquals(count, actual);
        SupportQueryPlanIndexHook.assertFAFAndReset(indexName, backingClass);
    }

    private static void runQueryAssertCount(RegressionEnvironment env, RegressionPath path, String epl, int count, String indexName, String backingClass) {
        env.compileExecuteFAF(epl, path);
        assertEquals(count, getMyInfraCount(env, path));
        SupportQueryPlanIndexHook.assertFAFAndReset(indexName, backingClass);
    }

    private static void insertInfra1Event(RegressionEnvironment env, RegressionPath path, String key, String keyJoin) {
        env.compileExecuteFAF("insert into Infra1 values ('" + key + "', '" + keyJoin + "')", path);
    }

    private static void insertInfra2Event(RegressionEnvironment env, RegressionPath path, String keyJoin, double value) {
        env.compileExecuteFAF("insert into Infra2 values ('" + keyJoin + "', " + value + ")", path);
    }

    private static long getMyInfraCount(RegressionEnvironment env, RegressionPath path) {
        return (Long) env.compileExecuteFAF("select count(*) as c0 from MyInfra", path).getArray()[0].get("c0");
    }

    private static EPFireAndForgetQueryResult compileExecute(String faf, RegressionPath path, RegressionEnvironment env) {
        EPCompiled compiled = env.compileFAF(faf, path);
        return env.runtime().getFireAndForgetService().executeQuery(compiled);
    }

    public static class MyLocalJsonProvidedProduct implements Serializable {
        public String productId;
        public String categoryId;
    }

    public static class MyLocalJsonProvidedCategory implements Serializable {
        public String categoryId;
        public String owner;
    }

    public static class MyLocalJsonProvidedProductOwnerDetails implements Serializable {
        public String productId;
        public String owner;
    }
}
