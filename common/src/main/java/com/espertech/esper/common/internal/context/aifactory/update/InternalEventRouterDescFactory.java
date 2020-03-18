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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage1.spec.UpdateDesc;
import com.espertech.esper.common.internal.epl.expression.assign.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethodForge;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Routing implementation that allows to pre-process events.
 */
public class InternalEventRouterDescFactory {
    public static InternalEventRouterDescForge getValidatePreprocessing(EventType eventType, UpdateDesc desc, Annotation[] annotations)
        throws ExprValidationException {

        if (!(eventType instanceof EventTypeSPI)) {
            throw new ExprValidationException("Update statements require the event type to implement the " + EventTypeSPI.class + " interface");
        }
        EventTypeSPI eventTypeSPI = (EventTypeSPI) eventType;

        List<TypeWidenerSPI> wideners = new ArrayList<>();
        List<String> properties = new ArrayList<>();
        List<String> propertiesTouched = new ArrayList<>();
        List<ExprNode> expressions = new ArrayList<>();
        List<InternalEventRouterWriterForge> specialWriters = new ArrayList<>();

        for (int i = 0; i < desc.getAssignments().size(); i++) {
            OnTriggerSetAssignment onSet = desc.getAssignments().get(i);
            ExprAssignment assignmentDesc = onSet.getValidated();

            try {
                if (assignmentDesc instanceof ExprAssignmentStraight) {
                    ExprAssignmentStraight assignment = (ExprAssignmentStraight) assignmentDesc;
                    ExprAssignmentLHS lhs = assignment.getLhs();

                    if (lhs instanceof ExprAssignmentLHSIdent) {
                        ExprAssignmentLHSIdent ident = (ExprAssignmentLHSIdent) lhs;
                        String propertyName = ident.getIdent();
                        EventPropertyDescriptor writableProperty = eventTypeSPI.getWritableProperty(propertyName);
                        if (writableProperty == null) {
                            throw new ExprValidationException("Property '" + propertyName + "' is not available for write access");
                        }

                        TypeWidenerSPI widener;
                        try {
                            widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(assignment.getRhs()), assignment.getRhs().getForge().getEvaluationType(),
                                writableProperty.getPropertyType(), propertyName, false, null, null);
                        } catch (TypeWidenerException ex) {
                            throw new ExprValidationException(ex.getMessage(), ex);
                        }

                        properties.add(propertyName);
                        propertiesTouched.add(propertyName);
                        expressions.add(assignment.getRhs());
                        wideners.add(widener);
                    } else if (lhs instanceof ExprAssignmentLHSIdentWSubprop) {
                        ExprAssignmentLHSIdentWSubprop subprop = (ExprAssignmentLHSIdentWSubprop) lhs;
                        throw new ExprValidationException("Property '" + subprop.getSubpropertyName() + "' is not available for write access");
                    } else if (lhs instanceof ExprAssignmentLHSArrayElement) {
                        ExprAssignmentLHSArrayElement arrayElement = (ExprAssignmentLHSArrayElement) lhs;
                        String propertyName = lhs.getIdent();
                        EventPropertyDescriptor writableProperty = eventTypeSPI.getWritableProperty(propertyName);
                        if (writableProperty == null) {
                            throw new ExprValidationException("Property '" + propertyName + "' is not available for write access");
                        }
                        if (!writableProperty.getPropertyType().isArray()) {
                            throw new ExprValidationException("Property '" + propertyName + "' type is not array");
                        }

                        TypeWidenerSPI widener;
                        try {
                            widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(assignment.getRhs()), assignment.getRhs().getForge().getEvaluationType(),
                                writableProperty.getPropertyType().getComponentType(), propertyName, false, null, null);
                        } catch (TypeWidenerException ex) {
                            throw new ExprValidationException(ex.getMessage(), ex);
                        }

                        InternalEventRouterWriterArrayElementForge special = new InternalEventRouterWriterArrayElementForge(arrayElement.getIndexExpression(), assignment.getRhs(), widener, propertyName);
                        specialWriters.add(special);
                    } else {
                        throw new IllegalStateException("Unrecognized left hande side assignment " + lhs);
                    }
                } else if (assignmentDesc instanceof ExprAssignmentCurly) {
                    ExprAssignmentCurly curly = (ExprAssignmentCurly) assignmentDesc;
                    InternalEventRouterWriterCurlyForge special = new InternalEventRouterWriterCurlyForge(curly.getExpression());
                    specialWriters.add(special);
                } else {
                    throw new IllegalStateException("Unrecognized assignment " + assignmentDesc);
                }
            } catch (ExprValidationException ex) {
                throw new ExprValidationException("Failed to validate assignment expression '" +
                    ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(assignmentDesc.getOriginalExpression()) + "': " +
                    ex.getMessage(), ex);
            }
        }

        // check copy-able
        EventBeanCopyMethodForge copyMethod = eventTypeSPI.getCopyMethodForge(propertiesTouched.toArray(new String[0]));
        if (copyMethod == null) {
            throw new ExprValidationException("The update-clause requires the underlying event representation to support copy (via Serializable by default)");
        }

        return new InternalEventRouterDescForge(copyMethod, wideners.toArray(new TypeWidenerSPI[0]), eventType, annotations, desc.getOptionalWhereClause(),
            properties.toArray(new String[0]), expressions.toArray(new ExprNode[0]), specialWriters.toArray(new InternalEventRouterWriterForge[0]));
    }
}
