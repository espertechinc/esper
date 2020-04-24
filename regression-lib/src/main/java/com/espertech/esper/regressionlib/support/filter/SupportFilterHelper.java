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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.support.events.SupportEventTypeHelper;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceSPI;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import java.util.*;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertTrue;
import static org.junit.Assert.*;

public class SupportFilterHelper {

    public static void assertFilterCount(RegressionEnvironment env, int count, String stmtName) {
        EPStatement statement = env.statement(stmtName);
        if (statement == null) {
            fail("Statement not found '" + stmtName + "'");
        }
        assertEquals(count, SupportFilterHelper.getFilterCountAnyType(statement));
    }

    public static String getFilterToString(RegressionEnvironment env, String statementName) {
        EPStatementSPI statementSPI = (EPStatementSPI) env.statement(statementName);
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> set = filterServiceSPI.get(Collections.singleton(statementSPI.getStatementId()));

        TreeSet<String> sorted = new TreeSet<>();
        for (Map.Entry<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> typeEntry : set.entrySet()) {
            for (List<FilterItem[]> filterItems : typeEntry.getValue().values()) {
                for (FilterItem[] itemArray : filterItems) {
                    EventType type = SupportEventTypeHelper.getEventTypeForTypeId(statementSPI.getStatementContext(), typeEntry.getKey());

                    StringBuilder builder = new StringBuilder();
                    builder.append(type.getName()).append("(");
                    String delimiter = "";
                    for (FilterItem item : itemArray) {
                        builder.append(delimiter);
                        builder.append(item.getName());
                        builder.append(item.getOp().getTextualOp());
                        builder.append(item.getOptionalValue());
                        delimiter = ",";
                    }
                    builder.append(")");
                    sorted.add(builder.toString());
                }
            }
        }
        return sorted.toString();
    }

    public static int getFilterCount(EPStatement statement, String eventTypeName) {
        EPStatementSPI statementSPI = (EPStatementSPI) statement;
        EventTypeIdPair typeId = SupportEventTypeHelper.getTypeIdForName(statementSPI.getStatementContext(), eventTypeName);
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> set = filterServiceSPI.get(Collections.singleton(statementSPI.getStatementId()));
        for (Map.Entry<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> entry : set.entrySet()) {
            if (entry.getKey().equals(typeId)) {
                List<FilterItem[]> list = entry.getValue().get(statementSPI.getStatementId());
                return list.size();
            }
        }
        return 0;
    }

    public static int getFilterCountAnyType(EPStatement statement) {
        EPStatementSPI statementSPI = (EPStatementSPI) statement;
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> set = filterServiceSPI.get(Collections.singleton(statementSPI.getStatementId()));
        int total = 0;
        for (Map.Entry<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> entry : set.entrySet()) {
            List<FilterItem[]> list = entry.getValue().get(statementSPI.getStatementId());
            if (list != null) {
                total += list.size();
            }
        }
        return total;
    }

    public static void assertFilterTwo(EPStatement statement, String expressionOne, FilterOperator opOne, String expressionTwo, FilterOperator opTwo) {
        EPStatementSPI statementSPI = (EPStatementSPI) statement;
        FilterItem[] multi = getFilterMulti(statementSPI);
        assertEquals(2, multi.length);
        assertEquals(opOne, multi[0].getOp());
        assertEquals(expressionOne, multi[0].getName());
        assertEquals(opTwo, multi[1].getOp());
        assertEquals(expressionTwo, multi[1].getName());
    }

    public static FilterItem getFilterSingle(EPStatement statement) {
        FilterItem[] params = getFilterMulti((EPStatementSPI) statement);
        assertEquals(1, params.length);
        return params[0];
    }

    public static void assertFilterSingle(EPStatement stmt, String epl, String expression, FilterOperator op) {
        EPStatementSPI statementSPI = (EPStatementSPI) stmt;
        FilterItem param = getFilterSingle(statementSPI);
        assertEquals("failed for '" + epl + "'", op, param.getOp());
        assertEquals(expression, param.getName());
    }

    public static FilterItem[] getFilterMulti(EPStatementSPI statementSPI) {
        int statementId = statementSPI.getStatementContext().getStatementId();
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> set = filterServiceSPI.get(Collections.singleton(statementId));
        assertEquals(1, set.size());
        Map<Integer, List<FilterItem[]>> filters = set.values().iterator().next();
        assertTrue(filters.containsKey(statementId));
        assertEquals(1, filters.size());
        List<FilterItem[]> paths = filters.get(statementId);
        assertEquals(1, paths.size());
        return paths.iterator().next();
    }

    public static void assertFilterByTypeSingle(EPStatement statement, String eventTypeName, FilterItem expected) {
        FilterItem[][] filtersAll = getFilterMulti(statement, eventTypeName);
        assertEquals(1, filtersAll.length);
        FilterItem[] filters = filtersAll[0];
        assertEquals(1, filters.length);
        assertEquals(expected, filters[0]);
    }

    public static void assertFilterByTypeMulti(EPStatement statement, String eventTypeName, FilterItem[][] expected) {
        Comparator<FilterItem> comparator = new Comparator<FilterItem>() {
            public int compare(FilterItem o1, FilterItem o2) {
                if (o1.getName().equals(o2.getName())) {
                    if (o1.getOp().ordinal() > o1.getOp().ordinal()) {
                        return 1;
                    }
                    if (o1.getOp().ordinal() < o1.getOp().ordinal()) {
                        return -1;
                    }
                    return 0;
                }
                return o1.getName().compareTo(o2.getName());
            }
        };

        FilterItem[][] found = getFilterMulti(statement, eventTypeName);

        for (int i = 0; i < found.length; i++) {
            Arrays.sort(found[i], comparator);
        }

        for (int i = 0; i < expected.length; i++) {
            Arrays.sort(expected[i], comparator);
        }

        EPAssertionUtil.assertEqualsAnyOrder(expected, found);
    }

    public static int getFilterCountApprox(RegressionEnvironment env) {
        FilterServiceSPI spi = ((EPRuntimeSPI) env.runtime()).getServicesContext().getFilterService();
        return spi.getFilterCountApprox();
    }

    public static Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> getFilterAllStmt(EPRuntime runtime) {
        String[] deployments = runtime.getDeploymentService().getDeployments();
        Set<Integer> statements = new HashSet<>();
        EPStatementSPI statementSPI = null;
        for (String deployment : deployments) {
            EPDeployment info = runtime.getDeploymentService().getDeployment(deployment);
            for (EPStatement statement : info.getStatements()) {
                statementSPI = (EPStatementSPI) statement;
                statements.add(statementSPI.getStatementId());
            }
        }
        if (statementSPI == null) {
            throw new IllegalStateException("Empty statements");
        }

        FilterServiceSPI filterService = ((EPRuntimeSPI) runtime).getServicesContext().getFilterService();
        return filterService.get(statements);
    }

    public static Map<Integer, List<FilterItem[]>> getFilterAllStmtForType(EPRuntime runtime, String eventTypeName) {
        Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> pairs = getFilterAllStmt(runtime);

        EventType eventType = runtime.getEventTypeService().getBusEventType(eventTypeName);
        EventTypeIdPair typeId = eventType.getMetadata().getEventTypeIdPair();
        return pairs.get(typeId);
    }

    public static FilterItem[][] getFilterMulti(EPStatement statement, String eventTypeName) {
        EPStatementSPI spi = (EPStatementSPI) statement;
        EventTypeIdPair typeId = SupportEventTypeHelper.getTypeIdForName(spi.getStatementContext(), eventTypeName);
        int statementId = spi.getStatementContext().getStatementId();
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) spi.getStatementContext().getFilterService();
        Map<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> set = filterServiceSPI.get(Collections.singleton(statementId));

        Map<Integer, List<FilterItem[]>> filters = null;
        for (Map.Entry<EventTypeIdPair, Map<Integer, List<FilterItem[]>>> entry : set.entrySet()) {
            if (entry.getKey().equals(typeId)) {
                filters = entry.getValue();
            }
        }
        assertNotNull(filters);
        assertFalse(filters.isEmpty());

        List<FilterItem[]> params = filters.get(statementId);
        assertFalse(params.isEmpty());

        return params.toArray(new FilterItem[params.size()][]);
    }

    public static void assertFilterMultiSameIndexDepthOne(EPStatement stmt, String eventType, int numEntries, String expression, FilterOperator operator) {
        FilterItem[][] items = getFilterMulti(stmt, eventType);
        assertEquals(numEntries, items.length);
        for (int i = 0; i < numEntries; i++) {
            FilterItem[] entries = items[i];
            assertEquals(1, entries.length);
            FilterItem item = entries[0];
            assertEquals(expression, item.getName());
            assertEquals(operator, item.getOp());
            assertSame(items[0][0].getIndex(), item.getIndex());
        }
    }

    public static void assertFilterMultiSameIndexDepthOne(Map<Integer, List<FilterItem[]>> filters, int numEntries, String expression, FilterOperator operator) {
        assertEquals(numEntries, filters.size());
        FilterItem first = null;
        for (Map.Entry<Integer, List<FilterItem[]>> stmtEntry : filters.entrySet()) {
            FilterItem[][] entriesStmt = stmtEntry.getValue().toArray(new FilterItem[0][]);
            assertEquals(1, entriesStmt.length);
            FilterItem[] entries = entriesStmt[0];
            assertEquals(1, entries.length);
            FilterItem item = entries[0];
            assertEquals(expression, item.getName());
            assertEquals(operator, item.getOp());
            if (first == null) {
                first = item;
            }
            assertSame(first.getIndex(), item.getIndex());
        }
    }
}
