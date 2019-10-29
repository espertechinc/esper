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
import com.espertech.esper.common.internal.epl.enummethod.dot.*;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.List;

public class ExprDotForgeSelectFrom extends ExprDotForgeEnumMethodBase {

    public EventType[] getAddStreamTypes(DotMethodFP footprint, int parameterNum, EnumMethodEnum enumMethod, String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, StreamTypeService streamTypeService, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        if (goesToNames.size() == 1) {
            return ExprDotNodeUtility.getSingleLambdaParamEventType(enumMethodUsedName, goesToNames, inputEventType, collectionComponentType, statementRawInfo, services);
        }

        EventType firstParamType;
        if (inputEventType == null) {
            firstParamType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(0), collectionComponentType, statementRawInfo, services);
        } else {
            firstParamType = inputEventType;
        }

        ObjectArrayEventType indexEventType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(1), int.class, statementRawInfo, services);
        return new EventType[]{firstParamType, indexEventType};
    }

    public EnumForge getEnumForge(DotMethodFP footprint, EnumMethodDesc enumMethodEnum, StreamTypeService streamTypeService, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        Class returnType = first.getBodyForge().getEvaluationType();
        super.setTypeInfo(EPTypeHelper.collectionOfSingleValue(returnType));

        if (inputEventType != null) {
            if (first.getGoesToNames().size() == 1) {
                return new EnumSelectFromEventsForge(first.getBodyForge(), first.getStreamCountIncoming());
            }
            return new EnumSelectFromIndexEventsForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[1]);
        }

        if (first.getGoesToNames().size() == 1) {
            return new EnumSelectFromScalarLambdaForge(first.getBodyForge(), first.getStreamCountIncoming(),
                (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }
        return new EnumSelectFromScalarIndexForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[0],
            (ObjectArrayEventType) first.getGoesToTypes()[1]);
    }
}
