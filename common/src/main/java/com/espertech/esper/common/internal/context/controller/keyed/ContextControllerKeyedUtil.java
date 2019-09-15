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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.MultiKey;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.collection.MultiKeyArrayWrap;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecKeyed;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecKeyedItem;
import com.espertech.esper.common.internal.context.aifactory.createwindow.StatementAgentInstanceFactoryCreateNW;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorFilter;
import com.espertech.esper.common.internal.context.mgr.ContextControllerStatementDesc;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.filterspec.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Map;

public class ContextControllerKeyedUtil {

    public static Object[] unpackKey(Object key) {
        if (key instanceof MultiKey) {
            return MultiKey.toObjectArray((MultiKey) key);
        } else if (key instanceof MultiKeyArrayWrap) {
            return new Object[]{((MultiKeyArrayWrap) key).getArray()};
        }
        return new Object[]{key};
    }

    protected static ContextControllerKeyedSvc getService(ContextControllerKeyedFactory factory, ContextManagerRealization realization) {
        if (factory.getFactoryEnv().isRoot()) {
            return new ContextControllerKeyedSvcLevelOne();
        }
        return new ContextControllerKeyedSvcLevelAny();
    }

    protected static Class[] validateContextDesc(String contextName, ContextSpecKeyed partitionSpec) throws ExprValidationException {

        if (partitionSpec.getItems().isEmpty()) {
            throw new ExprValidationException("Empty list of partition items");
        }

        // verify properties exist
        for (ContextSpecKeyedItem item : partitionSpec.getItems()) {
            EventType type = item.getFilterSpecCompiled().getFilterForEventType();
            for (String property : item.getPropertyNames()) {
                EventPropertyGetter getter = type.getGetter(property);
                if (getter == null) {
                    throw new ExprValidationException("For context '" + contextName + "' property name '" + property + "' not found on type " + type.getName());
                }
            }
        }

        // verify property number and types compatible
        ContextSpecKeyedItem firstItem = partitionSpec.getItems().get(0);
        if (partitionSpec.getItems().size() > 1) {
            // verify the same filter event type is only listed once

            for (int i = 0; i < partitionSpec.getItems().size(); i++) {
                EventType compareTo = partitionSpec.getItems().get(i).getFilterSpecCompiled().getFilterForEventType();

                for (int j = 0; j < partitionSpec.getItems().size(); j++) {
                    if (i == j) {
                        continue;
                    }

                    EventType compareFrom = partitionSpec.getItems().get(j).getFilterSpecCompiled().getFilterForEventType();
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
            for (int item = 1; item < partitionSpec.getItems().size(); item++) {
                ContextSpecKeyedItem nextItem = partitionSpec.getItems().get(item);

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

    public static FilterValueSetParam[][] getAddendumFilters(Object getterKey, FilterSpecActivatable filtersSpec, ContextControllerDetailKeyed keyedSpec, boolean includePartition, ContextControllerStatementDesc optionalStatementDesc, Map<Integer, ContextControllerStatementDesc> statements, AgentInstanceContext agentInstanceContext) {

        // determine whether create-named-window
        boolean isCreateWindow = optionalStatementDesc != null && optionalStatementDesc.getLightweight().getStatementContext().getStatementInformationals().getStatementType() == StatementType.CREATE_WINDOW;
        ContextControllerDetailKeyedItem foundPartition = null;

        if (!isCreateWindow) {
            if (filtersSpec.getFilterForEventType().getMetadata().getTypeClass() == EventTypeTypeClass.NAMED_WINDOW) {
                String declaredAsName = findNamedWindowDeclaredAsName(statements, filtersSpec.getFilterForEventType().getMetadata().getName());
                for (ContextControllerDetailKeyedItem partitionItem : keyedSpec.getItems()) {
                    if (partitionItem.getFilterSpecActivatable().getFilterForEventType().getName().equals(declaredAsName)) {
                        foundPartition = partitionItem;
                        break;
                    }
                }
            }
            if (foundPartition == null) {
                for (ContextControllerDetailKeyedItem partitionItem : keyedSpec.getItems()) {
                    boolean typeOrSubtype = EventTypeUtility.isTypeOrSubTypeOf(filtersSpec.getFilterForEventType(), partitionItem.getFilterSpecActivatable().getFilterForEventType());
                    if (typeOrSubtype) {
                        foundPartition = partitionItem;
                        break;
                    }
                }
            }
        } else {
            StatementAgentInstanceFactoryCreateNW factory = (StatementAgentInstanceFactoryCreateNW) optionalStatementDesc.getLightweight().getStatementContext().getStatementAIFactoryProvider().getFactory();
            String declaredAsName = factory.getAsEventTypeName();
            for (ContextControllerDetailKeyedItem partitionItem : keyedSpec.getItems()) {
                if (partitionItem.getFilterSpecActivatable().getFilterForEventType().getName().equals(declaredAsName)) {
                    foundPartition = partitionItem;
                    break;
                }
            }
        }

        if (foundPartition == null) {
            return null;
        }

        ExprFilterSpecLookupable[] lookupables = foundPartition.getLookupables();
        FilterValueSetParam[] addendumFilters = new FilterValueSetParam[lookupables.length];
        if (lookupables.length == 1) {
            addendumFilters[0] = getFilterMayEqualOrNull(lookupables[0], getterKey);
        } else {
            MultiKey keyProvisioning = (MultiKey) getterKey;
            for (int i = 0; i < lookupables.length; i++) {
                addendumFilters[i] = getFilterMayEqualOrNull(lookupables[i], keyProvisioning.getKey(i));
            }
        }

        FilterValueSetParam[][] addendum = new FilterValueSetParam[1][];
        addendum[0] = addendumFilters;

        FilterValueSetParam[][] partitionFilters = foundPartition.getFilterSpecActivatable().getValueSet(null, null, agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
        if (partitionFilters != null && includePartition) {
            addendum = FilterAddendumUtil.addAddendum(partitionFilters, addendum[0]);
        }

        return addendum;
    }

    private static String findNamedWindowDeclaredAsName(Map<Integer, ContextControllerStatementDesc> statements, String name) {
        for (Map.Entry<Integer, ContextControllerStatementDesc> stmtEntry : statements.entrySet()) {
            StatementContext ctx = stmtEntry.getValue().getLightweight().getStatementContext();
            if (ctx.getStatementType() == StatementType.CREATE_WINDOW) {
                StatementAgentInstanceFactoryCreateNW factory = (StatementAgentInstanceFactoryCreateNW) ctx.getStatementAIFactoryProvider().getFactory();
                if (factory.getStatementEventType().getName().equals(name)) {
                    return factory.getAsEventTypeName();
                }
            }
        }
        return null;
    }

    public static ContextControllerDetailKeyedItem findInitMatchingKey(ContextControllerDetailKeyedItem[] items, ContextConditionDescriptorFilter init) {
        ContextControllerDetailKeyedItem found = null;
        for (ContextControllerDetailKeyedItem item : items) {
            if (item.getFilterSpecActivatable().getFilterForEventType() == init.getFilterSpecActivatable().getFilterForEventType()) {
                found = item;
                break;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("Failed to find matching partition for type '" + init.getFilterSpecActivatable().getFilterForEventType());
        }
        return found;
    }

    private static FilterValueSetParam getFilterMayEqualOrNull(ExprFilterSpecLookupable lookupable, Object keyValue) {
        if (keyValue != null && keyValue.getClass().isArray()) {
            keyValue = MultiKeyPlanner.toMultiKey(keyValue);
        }
        return new FilterValueSetParamImpl(lookupable, FilterOperator.IS, keyValue);
    }

    public static void populatePriorMatch(String optionalInitCondAsName, MatchedEventMap matchedEventMap, EventBean triggeringEvent) {
        int tag = matchedEventMap.getMeta().getTagFor(optionalInitCondAsName);
        if (tag == -1) {
            return;
        }
        matchedEventMap.add(tag, triggeringEvent);
    }
}
