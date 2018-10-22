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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Iterator;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;

public class VariantEventTypeUtil {

    public static CodegenExpressionField getField(VariantEventType variantEventType, CodegenClassScope codegenClassScope) {
        return codegenClassScope.addFieldUnshared(true, VariantEventType.class, cast(VariantEventType.class, EventTypeUtility.resolveTypeCodegen(variantEventType, EPStatementInitServices.REF)));
    }

    public static void validateInsertedIntoEventType(EventType eventType, VariantEventType variantEventType) throws ExprValidationException {
        if (variantEventType.isVariantAny()) {
            return;
        }

        if (eventType == null) {
            throw new ExprValidationException(getMessage(variantEventType.getName()));
        }

        // try each permitted type
        EventType[] variants = variantEventType.getVariants();
        for (EventType variant : variants) {
            if (variant == eventType) {
                return;
            }
        }

        // test if any of the supertypes of the eventtype is a variant type
        for (EventType variant : variants) {
            // Check all the supertypes to see if one of the matches the full or delta types
            Iterator<EventType> deepSupers = eventType.getDeepSuperTypes();
            if (deepSupers == null) {
                continue;
            }

            EventType superType;
            for (; deepSupers.hasNext(); ) {
                superType = deepSupers.next();
                if (superType == variant) {
                    return;
                }
            }
        }

        throw new ExprValidationException(getMessage(variantEventType.getName()));
    }

    private static String getMessage(String name) {
        return "Selected event type is not a valid event type of the variant stream '" + name + "'";
    }
}
