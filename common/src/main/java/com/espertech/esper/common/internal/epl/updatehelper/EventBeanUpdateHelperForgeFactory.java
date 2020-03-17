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
package com.espertech.esper.common.internal.epl.updatehelper;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.epl.expression.assign.*;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.common.internal.event.avro.EventTypeAvroHandler;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethodForge;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.util.JavaClassHelper.getClassNameFullyQualPretty;

public class EventBeanUpdateHelperForgeFactory {
    public static EventBeanUpdateHelperForge make(String updatedWindowOrTableName,
                                                  EventTypeSPI eventTypeSPI,
                                                  List<OnTriggerSetAssignment> assignments,
                                                  String updatedAlias,
                                                  EventType optionalTriggeringEventType,
                                                  boolean isCopyOnWrite,
                                                  String statementName,
                                                  EventTypeAvroHandler avroHandler)
        throws ExprValidationException {
        List<EventBeanUpdateItemForge> updateItems = new ArrayList<EventBeanUpdateItemForge>();
        List<String> properties = new ArrayList<String>();

        TypeWidenerCustomizer typeWidenerCustomizer = avroHandler.getTypeWidenerCustomizer(eventTypeSPI);

        for (int i = 0; i < assignments.size(); i++) {
            OnTriggerSetAssignment desc = assignments.get(i);
            ExprAssignment assignment = desc.getValidated();
            if (assignment == null) {
                throw new IllegalStateException("Assignment has not been validated");
            }
            try {
                EventBeanUpdateItemForge updateItem;
                if (assignment instanceof ExprAssignmentStraight) {
                    ExprAssignmentStraight straight = (ExprAssignmentStraight) assignment;

                    // handle assignment "property = value"
                    if (straight.getLhs() instanceof ExprAssignmentLHSIdent) {
                        ExprAssignmentLHSIdent ident = (ExprAssignmentLHSIdent) straight.getLhs();

                        String propertyName = ident.getIdent();
                        EventPropertyDescriptor writableProperty = eventTypeSPI.getWritableProperty(propertyName);

                        // check assignment to indexed or mapped property
                        if (writableProperty == null) {
                            Pair<String, EventPropertyDescriptor> nameWriteablePair = checkIndexedOrMappedProp(propertyName, updatedWindowOrTableName, updatedAlias, eventTypeSPI);
                            propertyName = nameWriteablePair.getFirst();
                            writableProperty = nameWriteablePair.getSecond();
                        }

                        ExprNode rhsExpr = straight.getRhs();
                        ExprForge rhsForge = rhsExpr.getForge();
                        EventPropertyWriterSPI writers = eventTypeSPI.getWriter(propertyName);
                        boolean notNullableField = writableProperty.getPropertyType().isPrimitive();

                        properties.add(propertyName);
                        TypeWidenerSPI widener;
                        try {
                            widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(rhsExpr), rhsForge.getEvaluationType(),
                                writableProperty.getPropertyType(), propertyName, false, typeWidenerCustomizer, statementName);
                        } catch (TypeWidenerException ex) {
                            throw new ExprValidationException(ex.getMessage(), ex);
                        }

                        // check event type assignment
                        if (optionalTriggeringEventType != null && rhsExpr instanceof ExprIdentNode) {
                            ExprIdentNode node = (ExprIdentNode) rhsExpr;
                            FragmentEventType fragmentRHS = optionalTriggeringEventType.getFragmentType(node.getResolvedPropertyName());
                            FragmentEventType fragmentLHS = eventTypeSPI.getFragmentType(propertyName);
                            if (fragmentRHS != null && fragmentLHS != null && !EventTypeUtility.isTypeOrSubTypeOf(fragmentRHS.getFragmentType(), fragmentLHS.getFragmentType())) {
                                throw new ExprValidationException("Invalid assignment to property '" +
                                    propertyName + "' event type '" + fragmentLHS.getFragmentType().getName() +
                                    "' from event type '" + fragmentRHS.getFragmentType().getName() + "'");
                            }
                        }

                        updateItem = new EventBeanUpdateItemForge(rhsForge, propertyName, writers, notNullableField, widener, null);
                    } else if (straight.getLhs() instanceof ExprAssignmentLHSArrayElement) {
                        // handle "property[expr] = value"
                        ExprAssignmentLHSArrayElement arrayElementLHS = (ExprAssignmentLHSArrayElement) straight.getLhs();
                        ExprArrayElementForge arrayElement = arrayElementLHS.getArrayElement().getForge();
                        if (arrayElement instanceof ExprArrayElementForgeVariable) {
                            throw new ExprValidationException("Only property assignments are allowed as part update");
                        }
                        ExprArrayElementForgeProperty prop = (ExprArrayElementForgeProperty) arrayElement;
                        String arrayPropertyName = arrayElementLHS.getIdent();
                        ExprNode rhs = straight.getRhs();
                        Class evaluationType = rhs.getForge().getEvaluationType();
                        Class componentType = arrayElement.getComponentType();
                        if (!JavaClassHelper.isAssignmentCompatible(evaluationType, componentType)) {
                            throw new ExprValidationException("Invalid assignment to property '" +
                                arrayPropertyName + "' component type '" + getClassNameFullyQualPretty(componentType) +
                                "' from expression returning '" + getClassNameFullyQualPretty(evaluationType) + "'");
                        }
                        if (prop.getStreamNum() != 0) {
                            throw new ExprValidationException("Property '" + arrayPropertyName + "' is not available for write access");
                        }

                        TypeWidenerSPI widener;
                        try {
                            widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(straight.getRhs()), evaluationType,
                                componentType, arrayPropertyName, false, typeWidenerCustomizer, statementName);
                        } catch (TypeWidenerException ex) {
                            throw new ExprValidationException(ex.getMessage(), ex);
                        }

                        updateItem = new EventBeanUpdateItemForge(rhs.getForge(), arrayPropertyName, null, false, widener, prop);
                    } else {
                        throw new IllegalStateException("Unrecognized LHS assignment " + straight);
                    }
                } else if (assignment instanceof ExprAssignmentCurly) {
                    // handle non-assignment, i.e. UDF or other expression
                    ExprAssignmentCurly dot = (ExprAssignmentCurly) assignment;
                    updateItem = new EventBeanUpdateItemForge(dot.getExpression().getForge(), null, null, false, null, null);
                } else {
                    throw new IllegalStateException("Unrecognized assignment " + assignment);
                }

                updateItems.add(updateItem);
            } catch (ExprValidationException ex) {
                throw new ExprValidationException("Failed to validate assignment expression '" +
                    ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(assignment.getOriginalExpression()) + "': " +
                        ex.getMessage(), ex);
            }
        }

        // copy-on-write is the default event semantics as events are immutable
        EventBeanCopyMethodForge copyMethod;
        if (isCopyOnWrite) {
            // obtain copy method
            List<String> propertiesUniqueList = new ArrayList<String>(new HashSet<String>(properties));
            String[] propertiesArray = propertiesUniqueList.toArray(new String[propertiesUniqueList.size()]);
            copyMethod = eventTypeSPI.getCopyMethodForge(propertiesArray);
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
                copyMethod = eventTypeSPI.getCopyMethodForge(propertiesInitialValueArray);
            }
        }

        EventBeanUpdateItemForge[] updateItemsArray = updateItems.toArray(new EventBeanUpdateItemForge[updateItems.size()]);
        return new EventBeanUpdateHelperForge(eventTypeSPI, copyMethod, updateItemsArray);
    }

    private static Set<String> determinePropertiesInitialValue(List<OnTriggerSetAssignment> assignments) {
        Set<String> props = new HashSet<String>();
        ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
        for (OnTriggerSetAssignment assignment : assignments) {
            assignment.getValidated().accept(visitor);
            for (ExprIdentNode node : visitor.getExprProperties()) {
                if (node.getStreamId() == 2) {
                    props.add(node.getResolvedPropertyName());
                }
            }
            visitor.reset();
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