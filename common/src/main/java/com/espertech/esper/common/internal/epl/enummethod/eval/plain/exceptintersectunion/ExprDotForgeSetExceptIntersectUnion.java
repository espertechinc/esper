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
package com.espertech.esper.common.internal.epl.enummethod.eval.plain.exceptintersectunion;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotForgeEnumMethodBase;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDescFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeLambdaDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEnumerationSourceForge;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeClass;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.List;

public class ExprDotForgeSetExceptIntersectUnion extends ExprDotForgeEnumMethodBase {

    public EnumForgeDescFactory getForgeFactory(DotMethodFP footprint, List<ExprNode> parameters, EnumMethodEnum enumMethod, String enumMethodUsedName, EventType inputEventType, EPTypeClass collectionComponentType, ExprValidationContext validationContext)
        throws ExprValidationException {
        ExprNode first = parameters.get(0);

        ExprDotEnumerationSourceForge enumSrc = ExprDotNodeUtility.getEnumerationSource(first, validationContext.getStreamTypeService(), true, validationContext.isDisablePropertyExpressionEventCollCache(), validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
        EPChainableType type;
        if (inputEventType != null) {
            type = EPChainableTypeHelper.collectionOfEvents(inputEventType);
        } else {
            type = EPChainableTypeHelper.collectionOfSingleValue(collectionComponentType);
        }

        if (inputEventType != null) {
            EventType setType = enumSrc.getEnumeration() == null ? null : enumSrc.getEnumeration().getEventTypeCollection(validationContext.getStatementRawInfo(), validationContext.getStatementCompileTimeService());
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
            Class setType;
            if (enumSrc.getEnumeration() == null || enumSrc.getEnumeration().getComponentTypeCollection() == null) {
                setType = null;
            } else {
                setType = enumSrc.getEnumeration().getComponentTypeCollection().getType();
            }
            if (setType == null) {
                String message = "Enumeration method '" + enumMethodUsedName + "' requires an expression yielding a " +
                    "collection of values of type '" + collectionComponentType.getType().getSimpleName() + "' as input parameter";
                throw new ExprValidationException(message);
            }
            if (!JavaClassHelper.isAssignmentCompatible(setType, collectionComponentType.getType())) {
                String message = "Enumeration method '" + enumMethodUsedName + "' expects scalar type '" + collectionComponentType.getType().getSimpleName() + "' but receives event type '" + setType.getSimpleName() + "'";
                throw new ExprValidationException(message);
            }
        }

        return new EnumForgeDescFactoryEIU(enumMethod, type, enumSrc);
    }

    private static class EnumForgeDescFactoryEIU implements EnumForgeDescFactory {
        private final EnumMethodEnum enumMethod;
        private final EPChainableType type;
        private final ExprDotEnumerationSourceForge enumSrc;

        public EnumForgeDescFactoryEIU(EnumMethodEnum enumMethod, EPChainableType type, ExprDotEnumerationSourceForge enumSrc) {
            this.enumMethod = enumMethod;
            this.type = type;
            this.enumSrc = enumSrc;
        }

        public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
            throw new IllegalStateException("No lambda expected");
        }

        public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) {
            boolean scalar = type instanceof EPChainableTypeClass;
            EnumForge forge;
            if (enumMethod == EnumMethodEnum.UNION) {
                forge = new EnumUnionForge(streamCountIncoming, enumSrc.getEnumeration(), scalar);
            } else if (enumMethod == EnumMethodEnum.INTERSECT) {
                forge = new EnumIntersectForge(streamCountIncoming, enumSrc.getEnumeration(), scalar);
            } else if (enumMethod == EnumMethodEnum.EXCEPT) {
                forge = new EnumExceptForge(streamCountIncoming, enumSrc.getEnumeration(), scalar);
            } else {
                throw new IllegalArgumentException("Invalid enumeration method for this factory: " + enumMethod);
            }
            return new EnumForgeDesc(type, forge);
        }
    }
}
