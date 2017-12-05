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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.epl.spec.ContextDetailPartitionItem;
import com.espertech.esper.epl.spec.ContextDetailPartitioned;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.util.StatementSpecCompiledAnalyzerResult;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.filterspec.*;
import com.espertech.esper.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

public class ContextControllerPartitionedUtil {

    protected static Class[] validateContextDesc(String contextName, ContextDetailPartitioned segmentedSpec) throws ExprValidationException {

        if (segmentedSpec.getItems().isEmpty()) {
            throw new ExprValidationException("Empty list of partition items");
        }

        // verify properties exist
        for (ContextDetailPartitionItem item : segmentedSpec.getItems()) {
            EventType type = item.getFilterSpecCompiled().getFilterForEventType();
            for (String property : item.getPropertyNames()) {
                EventPropertyGetter getter = type.getGetter(property);
                if (getter == null) {
                    throw new ExprValidationException("For context '" + contextName + "' property name '" + property + "' not found on type " + type.getName());
                }
            }
        }

        // verify property number and types compatible
        ContextDetailPartitionItem firstItem = segmentedSpec.getItems().get(0);
        if (segmentedSpec.getItems().size() > 1) {
            // verify the same filter event type is only listed once

            for (int i = 0; i < segmentedSpec.getItems().size(); i++) {
                EventType compareTo = segmentedSpec.getItems().get(i).getFilterSpecCompiled().getFilterForEventType();

                for (int j = 0; j < segmentedSpec.getItems().size(); j++) {
                    if (i == j) {
                        continue;
                    }

                    EventType compareFrom = segmentedSpec.getItems().get(j).getFilterSpecCompiled().getFilterForEventType();
                    if (compareFrom == compareTo) {
                        throw new ExprValidationException("For context '" + contextName + "' the event type '" + compareFrom.getName() + "' is listed twice");
                    }
                    if (EventTypeUtility.isTypeOrSubTypeOf(compareFrom, compareTo) || EventTypeUtility.isTypeOrSubTypeOf(compareTo, compareFrom)) {
                        throw new ExprValidationException("For context '" + contextName + "' the event type '" + compareFrom.getName() + "' is listed twice: Event type '" +
                                compareFrom.getName() + "' is a subtype or supertype of event type '" + compareTo.getName() + "'");
                    }

                }
            }

            // build property type information
            String[] names = new String[firstItem.getPropertyNames().size()];
            Class[] types = new Class[firstItem.getPropertyNames().size()];
            Class[] typesBoxed = new Class[firstItem.getPropertyNames().size()];
            for (int i = 0; i < firstItem.getPropertyNames().size(); i++) {
                String property = firstItem.getPropertyNames().get(i);
                names[i] = property;
                types[i] = firstItem.getFilterSpecCompiled().getFilterForEventType().getPropertyType(property);
                typesBoxed[i] = JavaClassHelper.getBoxedType(types[i]);
            }

            // compare property types and numbers
            for (int item = 1; item < segmentedSpec.getItems().size(); item++) {
                ContextDetailPartitionItem nextItem = segmentedSpec.getItems().get(item);

                // compare number of properties
                if (nextItem.getPropertyNames().size() != types.length) {
                    throw new ExprValidationException("For context '" + contextName + "' expected the same number of property names for each event type, found " +
                            types.length + " properties for event type '" + firstItem.getFilterSpecCompiled().getFilterForEventType().getName() +
                            "' and " + nextItem.getPropertyNames().size() + " properties for event type '" + nextItem.getFilterSpecCompiled().getFilterForEventType().getName() + "'");
                }

                // compare property types
                for (int i = 0; i < nextItem.getPropertyNames().size(); i++) {
                    String property = nextItem.getPropertyNames().get(i);
                    Class type = JavaClassHelper.getBoxedType(nextItem.getFilterSpecCompiled().getFilterForEventType().getPropertyType(property));
                    Class typeBoxed = JavaClassHelper.getBoxedType(type);
                    boolean left = JavaClassHelper.isSubclassOrImplementsInterface(typeBoxed, typesBoxed[i]);
                    boolean right = JavaClassHelper.isSubclassOrImplementsInterface(typesBoxed[i], typeBoxed);
                    if (typeBoxed != typesBoxed[i] && !left && !right) {
                        throw new ExprValidationException("For context '" + contextName + "' for context '" + contextName + "' found mismatch of property types, property '" + names[i] +
                                "' of type '" + JavaClassHelper.getClassNameFullyQualPretty(types[i]) +
                                "' compared to property '" + property +
                                "' of type '" + JavaClassHelper.getClassNameFullyQualPretty(typeBoxed) + "'");
                    }
                }
            }
        }

        Class[] propertyTypes = new Class[firstItem.getPropertyNames().size()];
        for (int i = 0; i < firstItem.getPropertyNames().size(); i++) {
            String property = firstItem.getPropertyNames().get(i);
            propertyTypes[i] = firstItem.getFilterSpecCompiled().getFilterForEventType().getPropertyType(property);
        }
        return propertyTypes;
    }

    protected static void validateStatementForContext(String contextName, ContextControllerStatementBase statement, StatementSpecCompiledAnalyzerResult streamAnalysis, Collection<EventType> itemEventTypes, NamedWindowMgmtService namedWindowMgmtService)
            throws ExprValidationException {
        List<FilterSpecCompiled> filters = streamAnalysis.getFilters();

        boolean isCreateWindow = statement.getStatementSpec().getCreateWindowDesc() != null;

        // if no create-window: at least one of the filters must match one of the filters specified by the context
        if (!isCreateWindow) {
            for (FilterSpecCompiled filter : filters) {
                for (EventType itemEventType : itemEventTypes) {
                    EventType stmtFilterType = filter.getFilterForEventType();
                    if (stmtFilterType == itemEventType) {
                        return;
                    }
                    if (EventTypeUtility.isTypeOrSubTypeOf(stmtFilterType, itemEventType)) {
                        return;
                    }

                    NamedWindowProcessor processor = namedWindowMgmtService.getProcessor(stmtFilterType.getName());
                    if (processor != null && processor.getContextName() != null && processor.getContextName().equals(contextName)) {
                        return;
                    }
                }
            }

            if (!filters.isEmpty()) {
                throw new ExprValidationException(getTypeValidationMessage(contextName, filters.get(0).getFilterForEventType().getName()));
            }
            return;
        }

        // validate create-window with column definition: not allowed, requires typed
        if (statement.getStatementSpec().getCreateWindowDesc().getColumns() != null &&
                statement.getStatementSpec().getCreateWindowDesc().getColumns().size() > 0) {
            throw new ExprValidationException("Segmented context '" + contextName +
                    "' requires that named windows are associated to an existing event type and that the event type is listed among the partitions defined by the create-context statement");
        }

        // validate create-window declared type
        String declaredAsName = statement.getStatementSpec().getCreateWindowDesc().getAsEventTypeName();
        if (declaredAsName != null) {
            for (EventType itemEventType : itemEventTypes) {
                if (itemEventType.getName().equals(declaredAsName)) {
                    return;
                }
            }

            throw new ExprValidationException(getTypeValidationMessage(contextName, declaredAsName));
        }
    }

    // Compare filters in statement with filters in segmented context, addendum filter compilation
    public static void populateAddendumFilters(Object keyValue, List<FilterSpecCompiled> filtersSpecs, ContextDetailPartitioned segmentedSpec, StatementSpecCompiled optionalStatementSpecCompiled, IdentityHashMap<FilterSpecCompiled, FilterValueSetParam[][]> addendums) {
        for (FilterSpecCompiled filtersSpec : filtersSpecs) {
            FilterValueSetParam[][] addendum = getAddendumFilters(keyValue, filtersSpec, segmentedSpec, true, optionalStatementSpecCompiled);
            if (addendum == null) {
                continue;
            }

            FilterValueSetParam[][] existing = addendums.get(filtersSpec);
            if (existing != null) {
                addendum = FilterAddendumUtil.multiplyAddendum(existing, addendum);
            }
            addendums.put(filtersSpec, addendum);
        }
    }

    public static FilterValueSetParam[][] getAddendumFilters(Object keyValue, FilterSpecCompiled filtersSpec, ContextDetailPartitioned segmentedSpec, boolean includePartition, StatementSpecCompiled optionalStatementSpecCompiled) {

        // determine whether create-named-window
        boolean isCreateWindow = optionalStatementSpecCompiled != null && optionalStatementSpecCompiled.getCreateWindowDesc() != null;
        ContextDetailPartitionItem foundPartition = null;

        if (!isCreateWindow) {
            for (ContextDetailPartitionItem partitionItem : segmentedSpec.getItems()) {
                boolean typeOrSubtype = EventTypeUtility.isTypeOrSubTypeOf(filtersSpec.getFilterForEventType(), partitionItem.getFilterSpecCompiled().getFilterForEventType());
                if (typeOrSubtype) {
                    foundPartition = partitionItem;
                    break;
                }
            }
        } else {
            String declaredAsName = optionalStatementSpecCompiled.getCreateWindowDesc().getAsEventTypeName();
            if (declaredAsName == null) {
                return null;
            }
            for (ContextDetailPartitionItem partitionItem : segmentedSpec.getItems()) {
                if (partitionItem.getFilterSpecCompiled().getFilterForEventType().getName().equals(declaredAsName)) {
                    foundPartition = partitionItem;
                    break;
                }
            }
        }

        if (foundPartition == null) {
            return null;
        }

        List<FilterValueSetParam> addendumFilters = new ArrayList<FilterValueSetParam>(foundPartition.getPropertyNames().size());
        if (foundPartition.getPropertyNames().size() == 1) {
            String propertyName = foundPartition.getPropertyNames().get(0);
            EventPropertyGetter getter = foundPartition.getFilterSpecCompiled().getFilterForEventType().getGetter(propertyName);
            Class resultType = foundPartition.getFilterSpecCompiled().getFilterForEventType().getPropertyType(propertyName);
            ExprFilterSpecLookupable lookupable = new ExprFilterSpecLookupable(propertyName, getter, resultType, false);
            FilterValueSetParam filter = getFilterMayEqualOrNull(lookupable, keyValue);
            addendumFilters.add(filter);
        } else {
            Object[] keys = keyValue instanceof MultiKeyUntyped ? ((MultiKeyUntyped) keyValue).getKeys() : (Object[]) keyValue;
            for (int i = 0; i < foundPartition.getPropertyNames().size(); i++) {
                String partitionPropertyName = foundPartition.getPropertyNames().get(i);
                EventPropertyGetter getter = foundPartition.getFilterSpecCompiled().getFilterForEventType().getGetter(partitionPropertyName);
                Class resultType = foundPartition.getFilterSpecCompiled().getFilterForEventType().getPropertyType(partitionPropertyName);
                ExprFilterSpecLookupable lookupable = new ExprFilterSpecLookupable(partitionPropertyName, getter, resultType, false);
                FilterValueSetParam filter = getFilterMayEqualOrNull(lookupable, keys[i]);
                addendumFilters.add(filter);
            }
        }

        FilterValueSetParam[][] addendum = new FilterValueSetParam[1][];
        addendum[0] = addendumFilters.toArray(new FilterValueSetParam[addendumFilters.size()]);

        FilterValueSetParam[][] partitionFilters = foundPartition.getParametersCompiled();
        if (partitionFilters != null && includePartition) {
            addendum = FilterAddendumUtil.addAddendum(partitionFilters, addendum[0]);
        }

        return addendum;
    }

    private static FilterValueSetParam getFilterMayEqualOrNull(ExprFilterSpecLookupable lookupable, Object keyValue) {
        return new FilterValueSetParamImpl(lookupable, FilterOperator.IS, keyValue);
    }

    private static String getTypeValidationMessage(String contextName, String typeNameEx) {
        return "Segmented context '" + contextName + "' requires that any of the event types that are listed in the segmented context also appear in any of the filter expressions of the statement, type '" + typeNameEx + "' is not one of the types listed";
    }
}
