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
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage1.spec.UpdateDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityValidate;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethodForge;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.lang.annotation.Annotation;

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

        int size = desc.getAssignments().size();
        TypeWidenerSPI[] wideners = new TypeWidenerSPI[size];
        String[] properties = new String[size];
        ExprNode[] expressions = new ExprNode[size];
        for (int i = 0; i < size; i++) {
            OnTriggerSetAssignment onSet = desc.getAssignments().get(i);
            Pair<String, ExprNode> assignmentPair = ExprNodeUtilityValidate.checkGetAssignmentToProp(onSet.getExpression());
            if (assignmentPair == null) {
                throw new ExprValidationException("Missing property assignment expression in assignment number " + i);
            }
            properties[i] = assignmentPair.getFirst();
            expressions[i] = assignmentPair.getSecond();
            EventPropertyDescriptor writableProperty = eventTypeSPI.getWritableProperty(assignmentPair.getFirst());

            if (writableProperty == null) {
                throw new ExprValidationException("Property '" + assignmentPair.getFirst() + "' is not available for write access");
            }

            try {
                wideners[i] = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(assignmentPair.getSecond()), assignmentPair.getSecond().getForge().getEvaluationType(),
                        writableProperty.getPropertyType(), assignmentPair.getFirst(), false, null, null);
            } catch (TypeWidenerException ex) {
                throw new ExprValidationException(ex.getMessage(), ex);
            }
        }

        // check copy-able
        EventBeanCopyMethodForge copyMethod = eventTypeSPI.getCopyMethodForge(properties);
        if (copyMethod == null) {
            throw new ExprValidationException("The update-clause requires the underlying event representation to support copy (via Serializable by default)");
        }

        return new InternalEventRouterDescForge(copyMethod, wideners, eventType, annotations, desc.getOptionalWhereClause(),
                properties, expressions);
    }
}
