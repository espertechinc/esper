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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalEnumMethodBase;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.dot.ExprDotEnumerationSource;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeUtility;

import java.util.List;

public class ExprDotEvalSetExceptUnionIntersect extends ExprDotEvalEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        return new EventType[]{};
    }

    public EnumEval getEnumEval(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) throws ExprValidationException {
        ExprDotEvalParam first = bodiesAndParameters.get(0);

        ExprDotEnumerationSource enumSrc = ExprDotNodeUtility.getEnumerationSource(first.getBody(), streamTypeService, eventAdapterService, statementId, true, disablePropertyExpressionEventCollCache);
        if (inputEventType != null) {
            super.setTypeInfo(EPTypeHelper.collectionOfEvents(inputEventType));
        } else {
            super.setTypeInfo(EPTypeHelper.collectionOfSingleValue(collectionComponentType));
        }

        if (enumSrc.getEnumeration() == null) {
            String message = "Enumeration method '" + enumMethodUsedName + "' requires an expression yielding an event-collection as input paramater";
            throw new ExprValidationException(message);
        }

        EventType setType = enumSrc.getEnumeration().getEventTypeCollection(eventAdapterService, statementId);
        if (setType != inputEventType) {
            boolean isSubtype = EventTypeUtility.isTypeOrSubTypeOf(setType, inputEventType);
            if (!isSubtype) {
                String message = "Enumeration method '" + enumMethodUsedName + "' expects event type '" + inputEventType.getName() + "' but receives event type '" + enumSrc.getEnumeration().getEventTypeCollection(eventAdapterService, statementId).getName() + "'";
                throw new ExprValidationException(message);
            }
        }

        if (this.getEnumMethodEnum() == EnumMethodEnum.UNION) {
            return new EnumEvalUnion(numStreamsIncoming, enumSrc.getEnumeration(), inputEventType == null);
        } else if (this.getEnumMethodEnum() == EnumMethodEnum.INTERSECT) {
            return new EnumEvalIntersect(numStreamsIncoming, enumSrc.getEnumeration(), inputEventType == null);
        } else if (this.getEnumMethodEnum() == EnumMethodEnum.EXCEPT) {
            return new EnumEvalExcept(numStreamsIncoming, enumSrc.getEnumeration(), inputEventType == null);
        } else {
            throw new IllegalArgumentException("Invalid enumeration method for this factory: " + this.getEnumMethodEnum());
        }
    }
}
