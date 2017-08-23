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
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.epl.enummethod.dot.ExprDotForgeEnumMethodBase;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;

import java.util.List;

public class ExprDotForgeSequenceEqual extends ExprDotForgeEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        return new EventType[0];
    }

    public EnumForge getEnumForge(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) throws ExprValidationException {
        super.setTypeInfo(EPTypeHelper.singleValue(Boolean.class));
        ExprForge body = bodiesAndParameters.get(0).getBodyForge();
        return new EnumSequenceEqualForge(body, numStreamsIncoming);
    }
}
