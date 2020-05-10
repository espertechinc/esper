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
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompileHook;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanForge;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanPathForge;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanPathTripletForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamConstantForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.filterspec.FilterOperator.*;
import static org.junit.Assert.*;

public class SupportFilterPlanHook implements FilterSpecCompileHook {
    private static List<SupportFilterPlanEntry> entries;

    static {
        reset();
    }

    public static void reset() {
        entries = new ArrayList<>();
    }

    public static List<SupportFilterPlanEntry> getEntries() {
        return entries;
    }

    public static void assertPlanSingle(SupportFilterPlan expected) {
        if (entries.size() != 1) {
            fail("Zero or multiple entries");
        }
        assertPlan(expected, entries.get(0).getPlan());
    }

    public static void assertPlanSingleByType(String eventTypeName, SupportFilterPlan expected) {
        SupportFilterPlanEntry found = null;
        for (SupportFilterPlanEntry entry : entries) {
            if (entry.getEventType().getName().equals(eventTypeName)) {
                if (found != null) {
                    fail("found multiple for type " + eventTypeName);
                }
                found = entry;
            }
        }
        if (found == null) {
            fail("No entry for type " + eventTypeName);
        }
        assertPlan(expected, found.getPlan());
    }

    public void filterIndexPlan(EventType eventType, List<ExprNode> validatedNodes, FilterSpecPlanForge plan) {
        entries.add(new SupportFilterPlanEntry(eventType, plan, validatedNodes));
    }

    public static FilterSpecParamForge assertPlanSingleForTypeAndReset(String typeName) {
        SupportFilterPlanEntry found = null;
        for (SupportFilterPlanEntry entry : entries) {
            if (!entry.getEventType().getName().equals(typeName)) {
                continue;
            }
            if (found != null) {
                fail("Found multiple");
            }
            found = entry;
        }
        assertNotNull(found);
        reset();
        return found.getAssertSingle(typeName);
    }

    public static FilterSpecParamForge assertPlanSingleTripletAndReset(String typeName) {
        SupportFilterPlanEntry entry = assertPlanSingleAndReset();
        return entry.getAssertSingle(typeName);
    }

    public static SupportFilterPlanEntry assertPlanSingleAndReset() {
        assertEquals(1, entries.size());
        SupportFilterPlanEntry entry = entries.get(0);
        reset();
        return entry;
    }

    public static void assertPlan(SupportFilterPlan expected, FilterSpecPlanForge received) {
        assertEquals(expected.getPaths().length, received.getPaths().length);
        assertExpressionOpt(expected.getControlConfirm(), received.getFilterConfirm());
        assertExpressionOpt(expected.getControlNegate(), received.getFilterNegate());

        List<FilterSpecPlanPathForge> pathsReceived = new ArrayList<>(Arrays.asList(received.getPaths()));
        for (int i = 0; i < expected.getPaths().length; i++) {
            SupportFilterPlanPath pathExpected = expected.getPaths()[i];

            FilterSpecPlanPathForge path = findPath(pathExpected, pathsReceived);
            if (path == null) {
                fail("Failed to find path: " + pathExpected);
            }
            pathsReceived.remove(path);
            assertPlanPath(pathExpected, path);
        }
    }

    private static FilterSpecPlanPathForge findPath(SupportFilterPlanPath pathExpected, List<FilterSpecPlanPathForge> pathsReceived) {
        SupportFilterPlanTriplet[] tripletsExpected = sortTriplets(pathExpected.getTriplets());
        FilterSpecPlanPathForge found = null;

        for (FilterSpecPlanPathForge pathReceived : pathsReceived) {
            if (pathExpected.getTriplets().length != pathReceived.getTriplets().length) {
                continue;
            }
            FilterSpecPlanPathTripletForge[] tripletsReceived = sortTriplets(pathReceived.getTriplets());
            boolean matches = true;
            for (int i = 0; i < tripletsReceived.length; i++) {
                SupportFilterPlanTriplet expected = tripletsExpected[i];
                FilterSpecPlanPathTripletForge received = tripletsReceived[i];
                if (!expected.getLookupable().equals(received.getParam().getLookupable().getExpression()) ||
                    !expected.getOp().equals(received.getParam().getFilterOperator())) {
                    matches = false;
                    break;
                }
                StringBuilder builder = new StringBuilder();
                received.getParam().valueExprToString(builder, 0);
                String value = builder.toString();
                if (expected.getOp() == EQUAL) {
                    String textExpected = FilterSpecParamConstantForge.valueExprToString(expected.getValue());
                    if (!textExpected.equals(value)) {
                        matches = false;
                        break;
                    }
                } else if (expected.getOp() == BOOLEAN_EXPRESSION || expected.getOp() == REBOOL) {
                    String textExpected = FilterSpecParamExprNodeForge.valueExprToString(expected.getValue());
                    if (!textExpected.equals(value)) {
                        matches = false;
                        break;
                    }
                } else {
                    throw new IllegalStateException("Filter op " + expected.getOp() + " not handled");
                }
            }
            if (matches) {
                if (found != null) {
                    throw new IllegalStateException("Multiple matches");
                }
                found = pathReceived;
            }
        }
        return found;
    }

    private static void assertExpressionOpt(String expected, ExprNode expression) {
        if (expected == null) {
            assertNull(expression);
        } else {
            assertEquals(expected, ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expression));
        }
    }

    private static void assertPlanPath(SupportFilterPlanPath pathExpected, FilterSpecPlanPathForge pathReceived) {
        assertExpressionOpt(pathExpected.getControlNegate(), pathReceived.getPathNegate());
        assertEquals(pathExpected.getTriplets().length, pathReceived.getTriplets().length);

        FilterSpecPlanPathTripletForge[] tripletsReceived = sortTriplets(pathReceived.getTriplets());
        SupportFilterPlanTriplet[] tripletsExpected = sortTriplets(pathExpected.getTriplets());

        for (int i = 0; i < tripletsExpected.length; i++) {
            assertPlanPathTriplet(tripletsExpected[i], tripletsReceived[i]);
        }
    }

    private static SupportFilterPlanTriplet[] sortTriplets(SupportFilterPlanTriplet[] triplets) {
        SupportFilterPlanTriplet[] sorted = new SupportFilterPlanTriplet[triplets.length];
        System.arraycopy(triplets, 0, sorted, 0, triplets.length);
        Arrays.sort(sorted, (o1, o2) -> {
            int comparedLookupable = o1.getLookupable().compareTo(o2.getLookupable());
            if (comparedLookupable != 0) {
                return comparedLookupable;
            }
            int comparedOperator = Integer.compare(o1.getOp().ordinal(), o2.getOp().ordinal());
            if (comparedOperator != 0) {
                return comparedOperator;
            }
            throw new IllegalStateException("Comparator does not support value comparison");
        });
        return sorted;
    }

    private static FilterSpecPlanPathTripletForge[] sortTriplets(FilterSpecPlanPathTripletForge[] triplets) {
        FilterSpecPlanPathTripletForge[] sorted = new FilterSpecPlanPathTripletForge[triplets.length];
        System.arraycopy(triplets, 0, sorted, 0, triplets.length);
        Arrays.sort(sorted, (o1, o2) -> {
            int comparedLookupable = o1.getParam().getLookupable().getExpression().compareTo(o2.getParam().getLookupable().getExpression());
            if (comparedLookupable != 0) {
                return comparedLookupable;
            }
            int comparedOperator = Integer.compare(o1.getParam().getFilterOperator().ordinal(), o2.getParam().getFilterOperator().ordinal());
            if (comparedOperator != 0) {
                return comparedOperator;
            }
            throw new IllegalStateException("Comparator does not support value comparison");
        });
        return sorted;
    }

    private static void assertPlanPathTriplet(SupportFilterPlanTriplet tripletExpected, FilterSpecPlanPathTripletForge tripletReceived) {
        assertExpressionOpt(tripletExpected.getControlConfirm(), tripletReceived.getTripletConfirm());
        assertEquals(tripletExpected.getLookupable(), tripletReceived.getParam().getLookupable().getExpression());
        assertEquals(tripletExpected.getOp(), tripletReceived.getParam().getFilterOperator());
        StringBuilder out = new StringBuilder();
        tripletReceived.getParam().valueExprToString(out, 0);
        if (tripletExpected.getOp() == FilterOperator.EQUAL) {
            String expected = FilterSpecParamConstantForge.valueExprToString(tripletExpected.getValue());
            assertEquals(expected, out.toString());
        } else if (tripletExpected.getOp() == BOOLEAN_EXPRESSION || tripletExpected.getOp() == REBOOL) {
            String expected = FilterSpecParamExprNodeForge.valueExprToString(tripletExpected.getValue());
            assertEquals(expected, out.toString());
        } else {
            fail("operator value to-string not supported yet");
        }
    }

    public static SupportFilterPlanTriplet makeTripletRebool(String lookupable, String value) {
        return makeTriplet(lookupable, REBOOL, value);
    }

    public static SupportFilterPlanTriplet makeTriplet(String lookupable, FilterOperator op, String value) {
        return makeTriplet(lookupable, op, value, null);
    }

    public static SupportFilterPlanTriplet makeTriplet(String lookupable, FilterOperator op, String value, String controlConfirm) {
        return new SupportFilterPlanTriplet(lookupable, op, value, controlConfirm);
    }

    public static SupportFilterPlanPath makePathFromSingle(String lookupable, FilterOperator op, String value) {
        SupportFilterPlanTriplet triplet = makeTriplet(lookupable, op, value);
        return new SupportFilterPlanPath(new SupportFilterPlanTriplet[]{triplet});
    }

    public static SupportFilterPlanPath[] makePathsFromSingle(String lookupable, FilterOperator op, String value) {
        return new SupportFilterPlanPath[]{makePathFromSingle(lookupable, op, value)};
    }

    public static SupportFilterPlanPath[] makePathsFromEmpty() {
        SupportFilterPlanPath path = new SupportFilterPlanPath(new SupportFilterPlanTriplet[0]);
        return new SupportFilterPlanPath[]{path};
    }
}
