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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.support.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SupportQueryPlanIndexHook implements QueryPlanIndexHook {

    private static final List<QueryPlanIndexDescSubquery> SUBQUERIES = new ArrayList<QueryPlanIndexDescSubquery>();
    private static final List<QueryPlanIndexDescOnExpr> ONEXPRS = new ArrayList<QueryPlanIndexDescOnExpr>();
    private static final List<QueryPlanIndexDescFAF> FAFSNAPSHOTS = new ArrayList<QueryPlanIndexDescFAF>();
    private static final List<QueryPlanForge> JOINS = new ArrayList<QueryPlanForge>();
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

    public void join(QueryPlanForge join) {
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
            assertEquals(0, subquery.getTables().length);
            return;
        }

        String actualIndexDescription = subquery.getTables()[0].getIndexName();

        if (!tableName.equals(actualIndexDescription)) {
            if ((tableName.equals("MyInfra")) && (actualIndexDescription.equals("One"))) {
                actualIndexDescription = actualIndexDescription.replace("One", "MyInfra");
            } else if ((tableName.equals("One")) && (actualIndexDescription.equals("MyInfra"))) {
                actualIndexDescription = actualIndexDescription.replace("MyInfra", "One");
            }
        }

        assertEquals(tableName, subquery.getTables()[0].getIndexName());
        assertEquals(subqueryNum, subquery.getSubqueryNum());
        assertEquals(indexBackingClass, subquery.getTables()[0].getIndexDesc());
    }

    public static void assertSubqueryBackingAndReset(int subqueryNum, String tableName, String indexBackingClass) {
        assertEquals(1, SUBQUERIES.size());
        QueryPlanIndexDescSubquery subquery = SUBQUERIES.get(0);
        assertSubquery(subquery, subqueryNum, tableName, indexBackingClass);
        reset();
    }

    public static QueryPlanIndexDescSubquery assertSubqueryAndReset() {
        assertEquals(1, SUBQUERIES.size());
        QueryPlanIndexDescSubquery subquery = SUBQUERIES.get(0);
        reset();
        return subquery;
    }

    public static void assertOnExprTableAndReset(String indexName, String indexDescription) {
        assertEquals(1, ONEXPRS.size());
        QueryPlanIndexDescOnExpr onexp = ONEXPRS.get(0);
        if (indexDescription != null) {
            String actualIndexDescription = onexp.getTables()[0].getIndexDesc();

            if (!indexDescription.equals(actualIndexDescription)) {
                if ((indexDescription.equals("unique")) && (actualIndexDescription.equals("non-unique"))) {
                    actualIndexDescription = actualIndexDescription.replace("non-unique", "unique");
                } else if ((indexDescription.equals("non-unique")) && (actualIndexDescription.equals("unique"))) {
                    actualIndexDescription = actualIndexDescription.replace("unique", "non-unique");
                }
            }

            assertEquals(indexDescription, actualIndexDescription);
            assertEquals(indexName, onexp.getTables()[0].getIndexName()); // can be null
        } else {
            assertNull(onexp.getTables());
            assertNull(onexp.getTableLookupStrategy());
        }
        reset();
    }

    public static QueryPlanIndexDescOnExpr assertOnExprAndReset() {
        assertTrue(ONEXPRS.size() == 1);
        QueryPlanIndexDescOnExpr onexp = ONEXPRS.get(0);
        reset();
        return onexp;
    }

    public static void assertFAFAndReset(String tableName, String indexBackingClassStartsWith) {
        assertTrue(FAFSNAPSHOTS.size() == 1);
        QueryPlanIndexDescFAF fafdesc = FAFSNAPSHOTS.get(0);
        assertEquals(tableName, fafdesc.getTables()[0].getIndexName());
        String name = fafdesc.getTables()[0].getIndexDesc();
        if (indexBackingClassStartsWith != null) {
            assertTrue(name.startsWith(indexBackingClassStartsWith));
        }
        reset();
    }

    public static void assertJoinOneStreamAndReset(boolean unique) {
        assertTrue(JOINS.size() == 1);
        QueryPlanForge join = JOINS.get(0);
        QueryPlanIndexForge first = join.getIndexSpecs()[1];
        TableLookupIndexReqKey firstName = first.getItems().keySet().iterator().next();
        QueryPlanIndexItemForge index = first.getItems().get(firstName);
        assertEquals(unique, index.isUnique());
        reset();
    }

    public static QueryPlanForge assertJoinAndReset() {
        assertTrue(JOINS.size() == 1);
        QueryPlanForge join = JOINS.get(0);
        reset();
        return join;
    }

    public static void assertJoinAllStreamsAndReset(boolean unique) {
        assertTrue(JOINS.size() == 1);
        QueryPlanForge join = JOINS.get(0);
        for (QueryPlanIndexForge index : join.getIndexSpecs()) {
            TableLookupIndexReqKey firstName = index.getItems().keySet().iterator().next();
            QueryPlanIndexItemForge indexDesc = index.getItems().get(firstName);
            assertEquals(unique, indexDesc.isUnique());
        }
        reset();
    }

    public static QueryPlanIndexDescHistorical assertHistoricalAndReset() {
        QueryPlanIndexDescHistorical item = HISTORICALS.get(0);
        reset();
        return item;
    }
}
