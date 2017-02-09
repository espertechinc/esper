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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.eval.SelectExprContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.avro.AvroSchemaEventType;

public interface SelectExprProcessorRepresentationFactory {
    SelectExprProcessor makeSelectNoWildcard(SelectExprContext selectExprContext, EventType resultEventType, TableService tableService, String statementName, String engineURI) throws ExprValidationException;

    SelectExprProcessor makeRecast(EventType[] eventTypes, SelectExprContext selectExprContext, int streamNumber, AvroSchemaEventType insertIntoTargetType, ExprNode[] exprNodes, String statementName, String engineURI) throws ExprValidationException;

    SelectExprProcessor makeJoinWildcard(String[] streamNames, EventType resultEventType, EventAdapterService eventAdapterService);
}
