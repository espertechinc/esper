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
package com.espertech.esper.epl.updatehelper;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.*;
import com.espertech.esper.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventBeanUpdateHelperFactory {
    public static EventBeanUpdateHelper make(String updatedWindowOrTableName,
                                             EventTypeSPI eventTypeSPI,
                                             List<OnTriggerSetAssignment> assignments,
                                             String updatedAlias,
                                             EventType optionalTriggeringEventType,
                                             boolean isCopyOnWrite,
                                             String statementName,
                                             String engineURI,
                                             EventAdapterService eventAdapterService,
                                             boolean isFireAndForget)
            throws ExprValidationException {
        List<EventBeanUpdateItem> updateItems = new ArrayList<EventBeanUpdateItem>();
        List<String> properties = new ArrayList<String>();

        TypeWidenerCustomizer typeWidenerCustomizer = eventAdapterService.getTypeWidenerCustomizer(eventTypeSPI);
        for (int i = 0; i < assignments.size(); i++) {
            OnTriggerSetAssignment assignment = assignments.get(i);
            EventBeanUpdateItem updateItem;

            // determine whether this is a "property=value" assignment, we use property setters in this case
            Pair<String, ExprNode> possibleAssignment = ExprNodeUtilityRich.checkGetAssignmentToProp(assignment.getExpression());

            // handle assignment "property = value"
            if (possibleAssignment != null) {

                String propertyName = possibleAssignment.getFirst();
                EventPropertyDescriptor writableProperty = eventTypeSPI.getWritableProperty(propertyName);

                // check assignment to indexed or mapped property
                if (writableProperty == null) {
                    Pair<String, EventPropertyDescriptor> nameWriteablePair = checkIndexedOrMappedProp(possibleAssignment.getFirst(), updatedWindowOrTableName, updatedAlias, eventTypeSPI);
                    propertyName = nameWriteablePair.getFirst();
                    writableProperty = nameWriteablePair.getSecond();
                }

                ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(possibleAssignment.getSecond().getForge(), eventAdapterService.getEngineImportService(), EventBeanUpdateHelperFactory.class, isFireAndForget, statementName);
                EventPropertyWriter writers = eventTypeSPI.getWriter(propertyName);
                boolean notNullableField = writableProperty.getPropertyType().isPrimitive();

                properties.add(propertyName);
                TypeWidener widener;
                try {
                    widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(possibleAssignment.getSecond()), possibleAssignment.getSecond().getForge().getEvaluationType(),
                        writableProperty.getPropertyType(), propertyName, false, typeWidenerCustomizer, statementName, engineURI);
                } catch (TypeWidenerException ex) {
                    throw new ExprValidationException(ex.getMessage(), ex);
                }

                // check event type assignment
                if (optionalTriggeringEventType != null && possibleAssignment.getSecond() instanceof ExprIdentNode) {
                    ExprIdentNode node = (ExprIdentNode) possibleAssignment.getSecond();
                    FragmentEventType fragmentRHS = optionalTriggeringEventType.getFragmentType(node.getResolvedPropertyName());
                    FragmentEventType fragmentLHS = eventTypeSPI.getFragmentType(possibleAssignment.getFirst());
                    if (fragmentRHS != null && fragmentLHS != null && !EventTypeUtility.isTypeOrSubTypeOf(fragmentRHS.getFragmentType(), fragmentLHS.getFragmentType())) {
                        throw new ExprValidationException("Invalid assignment to property '" +
                                possibleAssignment.getFirst() + "' event type '" + fragmentLHS.getFragmentType().getName() +
                                "' from event type '" + fragmentRHS.getFragmentType().getName() + "'");
                    }
                }

                updateItem = new EventBeanUpdateItem(evaluator, propertyName, writers, notNullableField, widener);
            } else {
                // handle non-assignment, i.e. UDF or other expression
                ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(assignment.getExpression().getForge(), eventAdapterService.getEngineImportService(), EventBeanUpdateHelperFactory.class, isFireAndForget, statementName);
                updateItem = new EventBeanUpdateItem(evaluator, null, null, false, null);
            }

            updateItems.add(updateItem);
        }


        // copy-on-write is the default event semantics as events are immutable
        EventBeanCopyMethod copyMethod;
        if (isCopyOnWrite) {
            // obtain copy method
            List<String> propertiesUniqueList = new ArrayList<String>(new HashSet<String>(properties));
            String[] propertiesArray = propertiesUniqueList.toArray(new String[propertiesUniqueList.size()]);
            copyMethod = eventTypeSPI.getCopyMethod(propertiesArray);
            if (copyMethod == null) {
                throw new ExprValidationException("Event type does not support event bean copy");
            }
        } else {
            // for in-place update, determine assignment expressions to use "initial" to access prior-change values
            // the copy-method is optional
            copyMethod = null;
            Set<String> propertiesInitialValue = determinePropertiesInitialValue(assignments);
            if (!propertiesInitialValue.isEmpty()) {
                String[] propertiesInitialValueArray = propertiesInitialValue.toArray(new String[propertiesInitialValue.size()]);
                copyMethod = eventTypeSPI.getCopyMethod(propertiesInitialValueArray);
            }
        }

        EventBeanUpdateItem[] updateItemsArray = updateItems.toArray(new EventBeanUpdateItem[updateItems.size()]);
        return new EventBeanUpdateHelper(copyMethod, updateItemsArray);
    }

    private static Set<String> determinePropertiesInitialValue(List<OnTriggerSetAssignment> assignments) {
        Set<String> props = new HashSet<String>();
        ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
        for (OnTriggerSetAssignment assignment : assignments) {
            if (assignment.getExpression() instanceof ExprEqualsNode) {
                assignment.getExpression().getChildNodes()[1].accept(visitor);
            } else {
                assignment.getExpression().accept(visitor);
            }
            for (ExprIdentNode node : visitor.getExprProperties()) {
                if (node.getStreamId() == 2) {
                    props.add(node.getResolvedPropertyName());
                }
            }
        }
        return props;
    }

    private static Pair<String, EventPropertyDescriptor> checkIndexedOrMappedProp(String propertyName, String updatedWindowOrTableName, String namedWindowAlias, EventTypeSPI eventTypeSPI) throws ExprValidationException {

        EventPropertyDescriptor writableProperty = null;

        int indexDot = propertyName.indexOf(".");
        if ((namedWindowAlias != null) && (indexDot != -1)) {
            String prefix = StringValue.unescapeBacktick(propertyName.substring(0, indexDot));
            String name = propertyName.substring(indexDot + 1);
            if (prefix.equals(namedWindowAlias)) {
                writableProperty = eventTypeSPI.getWritableProperty(name);
                propertyName = name;
            }
        }
        if (writableProperty == null && indexDot != -1) {
            String prefix = propertyName.substring(0, indexDot);
            String name = propertyName.substring(indexDot + 1);
            if (prefix.equals(updatedWindowOrTableName)) {
                writableProperty = eventTypeSPI.getWritableProperty(name);
                propertyName = name;
            }
        }
        if (writableProperty == null) {
            throw new ExprValidationException("Property '" + propertyName + "' is not available for write access");
        }
        return new Pair<String, EventPropertyDescriptor>(propertyName, writableProperty);
    }
}