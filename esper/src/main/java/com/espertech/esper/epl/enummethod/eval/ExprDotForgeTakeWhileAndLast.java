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
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.epl.enummethod.dot.ExprDotForgeEnumMethodBase;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.List;

public class ExprDotForgeTakeWhileAndLast extends ExprDotForgeEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        EventType firstParamType;
        if (inputEventType == null) {
            firstParamType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(0), collectionComponentType, eventAdapterService);
        } else {
            firstParamType = inputEventType;
        }

        if (goesToNames.size() == 1) {
            return new EventType[]{firstParamType};
        }

        ObjectArrayEventType indexEventType = ExprDotNodeUtility.makeTransientOAType(enumMethodUsedName, goesToNames.get(1), int.class, eventAdapterService);
        return new EventType[]{firstParamType, indexEventType};
    }

    public EnumForge getEnumForge(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) {

        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);

        if (inputEventType != null) {
            super.setTypeInfo(EPTypeHelper.collectionOfEvents(inputEventType));
            if (first.getGoesToNames().size() == 1) {
                if (this.getEnumMethodEnum() == EnumMethodEnum.TAKEWHILELAST) {
                    return new EnumTakeWhileLastEventsForge(first.getBodyForge(), first.getStreamCountIncoming());
                }
                return new EnumTakeWhileEventsForge(first.getBodyForge(), first.getStreamCountIncoming());
            }

            if (this.getEnumMethodEnum() == EnumMethodEnum.TAKEWHILELAST) {
                return new EnumTakeWhileLastIndexEventsForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[1]);
            }
            return new EnumTakeWhileIndexEventsForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[1]);
        }

        super.setTypeInfo(EPTypeHelper.collectionOfSingleValue(collectionComponentType));
        if (first.getGoesToNames().size() == 1) {
            if (this.getEnumMethodEnum() == EnumMethodEnum.TAKEWHILELAST) {
                return new EnumTakeWhileLastScalarForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[0]);
            }
            return new EnumTakeWhileScalarForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }

        if (this.getEnumMethodEnum() == EnumMethodEnum.TAKEWHILELAST) {
            return new EnumTakeWhileLastIndexScalarForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[0], (ObjectArrayEventType) first.getGoesToTypes()[1]);
        }
        return new EnumTakeWhileIndexScalarForge(first.getBodyForge(), first.getStreamCountIncoming(), (ObjectArrayEventType) first.getGoesToTypes()[0], (ObjectArrayEventType) first.getGoesToTypes()[1]);
    }
}
