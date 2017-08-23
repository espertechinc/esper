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
package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.avro.writer.AvroRecastFactory;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.core.select.SelectExprProcessorRepresentationFactory;
import com.espertech.esper.epl.core.select.eval.SelectExprForgeContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.avro.AvroSchemaEventType;

public class SelectExprProcessorRepresentationFactoryAvro implements SelectExprProcessorRepresentationFactory {
    public SelectExprProcessorForge makeSelectNoWildcard(SelectExprForgeContext selectExprForgeContext, ExprForge[] exprForges, EventType resultEventType, TableService tableService, String statementName, String engineURI) throws ExprValidationException {
        return new EvalSelectNoWildcardAvro(selectExprForgeContext, exprForges, resultEventType, statementName, engineURI);
    }

    public SelectExprProcessorForge makeRecast(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, AvroSchemaEventType insertIntoTargetType, ExprNode[] exprNodes, String statementName, String engineURI) throws ExprValidationException {
        return AvroRecastFactory.make(eventTypes, selectExprForgeContext, streamNumber, insertIntoTargetType, exprNodes, statementName, engineURI);
    }

    public SelectExprProcessorForge makeJoinWildcard(String[] streamNames, EventType resultEventType, EventAdapterService eventAdapterService) {
        return new SelectExprJoinWildcardProcessorAvro(resultEventType, eventAdapterService);
    }
}
