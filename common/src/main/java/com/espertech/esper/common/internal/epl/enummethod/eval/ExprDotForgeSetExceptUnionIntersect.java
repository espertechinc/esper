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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodDesc;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotForgeEnumMethodBase;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEnumerationSourceForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

public class ExprDotForgeSetExceptUnionIntersect extends ExprDotForgeEnumMethodBase {

    public EventType[] getAddStreamTypes(DotMethodFP footprint, int parameterNum, EnumMethodEnum enumMethod, String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, StreamTypeService streamTypeService, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        return new EventType[]{};
    }

    public EnumForge getEnumForge(DotMethodFP footprint, EnumMethodDesc enumMethodEnum, StreamTypeService streamTypeService, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        ExprDotEvalParam first = bodiesAndParameters.get(0);

        ExprDotEnumerationSourceForge enumSrc = ExprDotNodeUtility.getEnumerationSource(first.getBody(), streamTypeService, true, disablePropertyExpressionEventCollCache, statementRawInfo, services);
        if (inputEventType != null) {
            super.setTypeInfo(EPTypeHelper.collectionOfEvents(inputEventType));
        } else {
            super.setTypeInfo(EPTypeHelper.collectionOfSingleValue(collectionComponentType));
        }

        if (inputEventType != null) {
            EventType setType = enumSrc.getEnumeration() == null ? null : enumSrc.getEnumeration().getEventTypeCollection(statementRawInfo, services);
            if (setType == null) {
                String message = "Enumeration method '" + enumMethodUsedName + "' requires an expression yielding a " +
                        "collection of events of type '" + inputEventType.getName() + "' as input parameter";
                throw new ExprValidationException(message);
            }
            if (setType != inputEventType) {
                boolean isSubtype = EventTypeUtility.isTypeOrSubTypeOf(setType, inputEventType);
                if (!isSubtype) {
                    String message = "Enumeration method '" + enumMethodUsedName + "' expects event type '" + inputEventType.getName() + "' but receives event type '" + setType.getName() + "'";
                    throw new ExprValidationException(message);
                }
            }
        } else {
            Class setType = enumSrc.getEnumeration() == null ? null : enumSrc.getEnumeration().getComponentTypeCollection();
            if (setType == null) {
                String message = "Enumeration method '" + enumMethodUsedName + "' requires an expression yielding a " +
                        "collection of values of type '" + collectionComponentType.getSimpleName() + "' as input parameter";
                throw new ExprValidationException(message);
            }
            if (!JavaClassHelper.isAssignmentCompatible(setType, collectionComponentType)) {
                String message = "Enumeration method '" + enumMethodUsedName + "' expects scalar type '" + collectionComponentType.getSimpleName() + "' but receives event type '" + setType.getSimpleName() + "'";
                throw new ExprValidationException(message);
            }
        }

        if (this.getEnumMethodEnum() == EnumMethodEnum.UNION) {
            return new EnumUnionForge(numStreamsIncoming, enumSrc.getEnumeration(), inputEventType == null);
        } else if (this.getEnumMethodEnum() == EnumMethodEnum.INTERSECT) {
            return new EnumIntersectForge(numStreamsIncoming, enumSrc.getEnumeration(), inputEventType == null);
        } else if (this.getEnumMethodEnum() == EnumMethodEnum.EXCEPT) {
            return new EnumExceptForge(numStreamsIncoming, enumSrc.getEnumeration(), inputEventType == null);
        } else {
            throw new IllegalArgumentException("Invalid enumeration method for this factory: " + this.getEnumMethodDesc());
        }
    }
}
