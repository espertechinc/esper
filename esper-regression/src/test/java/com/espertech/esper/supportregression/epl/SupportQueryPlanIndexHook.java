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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.epl.join.plan.QueryPlan;
import com.espertech.esper.epl.join.plan.QueryPlanIndex;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.join.plan.TableLookupIndexReqKey;
import com.espertech.esper.epl.join.util.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class SupportQueryPlanIndexHook implements QueryPlanIndexHook {

    private static final List<QueryPlanIndexDescSubquery> SUBQUERIES = new ArrayList<QueryPlanIndexDescSubquery>();
    private static final List<QueryPlanIndexDescOnExpr> ONEXPRS = new ArrayList<QueryPlanIndexDescOnExpr>();
    private static final List<QueryPlanIndexDescFAF> FAFSNAPSHOTS = new ArrayList<QueryPlanIndexDescFAF>();
    private static final List<QueryPlan> JOINS = new ArrayList<QueryPlan>();
    private static final List<QueryPlanIndexDescHistorical> HISTORICALS = new ArrayList<QueryPlanIndexDescHistorical>();

    public static String resetGetClassName() {
        reset();
        return SupportQueryPlanIndexHook.class.getName();
    }

    public static void reset() {
        SUBQUERIES.clear();
        ONEXPRS.clear();
        FAFSNAPSHOTS.clear();
        JOINS.clear();
        HISTORICALS.clear();
    }

    public void historical(QueryPlanIndexDescHistorical historicalPlan) {
        HISTORICALS.add(historicalPlan);
    }

    public void subquery(QueryPlanIndexDescSubquery subquery) {
        SUBQUERIES.add(subquery);
    }

    public void infraOnExpr(QueryPlanIndexDescOnExpr onexprdesc) {
        ONEXPRS.add(onexprdesc);
    }

    public void fireAndForget(QueryPlanIndexDescFAF fafdesc) {
        FAFSNAPSHOTS.add(fafdesc);
    }

    public void join(QueryPlan join) {
        JOINS.add(join);
    }

    public static List<QueryPlanIndexDescSubquery> getAndResetSubqueries() {
        List<QueryPlanIndexDescSubquery> copy = new ArrayList<QueryPlanIndexDescSubquery>(SUBQUERIES);
        reset();
        return copy;
    }

    public static QueryPlanIndexDescOnExpr getAndResetOnExpr() {
        QueryPlanIndexDescOnExpr onexpr = ONEXPRS.get(0);
        reset();
        return onexpr;
    }

    public static void assertSubquery(QueryPlanIndexDescSubquery subquery, int subqueryNum, String tableName, String indexBackingClass) {
        if (indexBackingClass == null) {
            Assert.assertEquals(0, subquery.getTables().length);
            return;
        }
        Assert.assertEquals(tableName, subquery.getTables()[0].getIndexName());
        Assert.assertEquals(subqueryNum, subquery.getSubqueryNum());
        Assert.assertEquals(indexBackingClass, subquery.getTables()[0].getIndexDesc());
    }

    public static void assertSubqueryBackingAndReset(int subqueryNum, String tableName, String indexBackingClass) {
        Assert.assertTrue(SUBQUERIES.size() == 1);
        QueryPlanIndexDescSubquery subquery = SUBQUERIES.get(0);
        assertSubquery(subquery, subqueryNum, tableName, indexBackingClass);
        reset();
    }

    public static QueryPlanIndexDescSubquery assertSubqueryAndReset() {
        Assert.assertTrue(SUBQUERIES.size() == 1);
        QueryPlanIndexDescSubquery subquery = SUBQUERIES.get(0);
        reset();
        return subquery;
    }

    public static void assertOnExprTableAndReset(String indexName, String indexDescription) {
        Assert.assertTrue(ONEXPRS.size() == 1);
        QueryPlanIndexDescOnExpr onexp = ONEXPRS.get(0);
        if (indexDescription != null) {
            Assert.assertEquals(indexDescription, onexp.getTables()[0].getIndexDesc());
            Assert.assertEquals(indexName, onexp.getTables()[0].getIndexName()); // can be null
        } else {
            Assert.assertNull(onexp.getTables());
            Assert.assertNull(onexp.getTableLookupStrategy());
        }
        reset();
    }

    public static QueryPlanIndexDescOnExpr assertOnExprAndReset() {
        Assert.assertTrue(ONEXPRS.size() == 1);
        QueryPlanIndexDescOnExpr onexp = ONEXPRS.get(0);
        reset();
        return onexp;
    }

    public static void assertFAFAndReset(String tableName, String indexBackingClass) {
        Assert.assertTrue(FAFSNAPSHOTS.size() == 1);
        QueryPlanIndexDescFAF fafdesc = FAFSNAPSHOTS.get(0);
        Assert.assertEquals(tableName, fafdesc.getTables()[0].getIndexName());
        Assert.assertEquals(indexBackingClass, fafdesc.getTables()[0].getIndexDesc());
        reset();
    }

    public static void assertJoinOneStreamAndReset(boolean unique) {
        Assert.assertTrue(JOINS.size() == 1);
        QueryPlan join = JOINS.get(0);
        QueryPlanIndex first = join.getIndexSpecs()[1];
        TableLookupIndexReqKey firstName = first.getItems().keySet().iterator().next();
        QueryPlanIndexItem index = first.getItems().get(firstName);
        Assert.assertEquals(unique, index.isUnique());
        reset();
    }

    public static QueryPlan assertJoinAndReset() {
        Assert.assertTrue(JOINS.size() == 1);
        QueryPlan join = JOINS.get(0);
        reset();
        return join;
    }

    public static void assertJoinAllStreamsAndReset(boolean unique) {
        Assert.assertTrue(JOINS.size() == 1);
        QueryPlan join = JOINS.get(0);
        for (QueryPlanIndex index : join.getIndexSpecs()) {
            TableLookupIndexReqKey firstName = index.getItems().keySet().iterator().next();
            QueryPlanIndexItem indexDesc = index.getItems().get(firstName);
            Assert.assertEquals(unique, indexDesc.isUnique());
        }
        reset();
    }

    public static QueryPlanIndexDescHistorical assertHistoricalAndReset() {
        QueryPlanIndexDescHistorical item = HISTORICALS.get(0);
        reset();
        return item;
    }
}
