/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.avro.writer.AvroRecastFactory;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.SelectExprProcessorRepresentationFactory;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.core.eval.SelectExprContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.avro.AvroSchemaEventType;

public class SelectExprProcessorRepresentationFactoryAvro implements SelectExprProcessorRepresentationFactory {
    public SelectExprProcessor makeNoWildcard(SelectExprContext selectExprContext, EventType resultEventType) {
        return new EvalSelectNoWildcardAvro(selectExprContext, resultEventType);
    }

    public SelectExprProcessor makeRecast(EventType[] eventTypes, SelectExprContext selectExprContext, int streamNumber, AvroSchemaEventType insertIntoTargetType, ExprNode[] exprNodes, EngineImportService engineImportService) throws ExprValidationException {
        return AvroRecastFactory.make(eventTypes, selectExprContext, streamNumber, insertIntoTargetType, exprNodes, engineImportService);
    }

    public SelectExprProcessor makeJoinWildcard(String[] streamNames, EventType resultEventType, EventAdapterService eventAdapterService) {
        return new SelectExprJoinWildcardProcessorAvro(resultEventType, eventAdapterService);
    }
}
