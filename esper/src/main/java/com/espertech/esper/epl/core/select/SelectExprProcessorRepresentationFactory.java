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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.select.eval.SelectExprForgeContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.avro.AvroSchemaEventType;

public interface SelectExprProcessorRepresentationFactory {
    SelectExprProcessorForge makeSelectNoWildcard(SelectExprForgeContext selectExprForgeContext, ExprForge[] exprForges, EventType resultEventType, TableService tableService, String statementName, String engineURI) throws ExprValidationException;

    SelectExprProcessorForge makeRecast(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, AvroSchemaEventType insertIntoTargetType, ExprNode[] exprNodes, String statementName, String engineURI) throws ExprValidationException;

    SelectExprProcessorForge makeJoinWildcard(String[] streamNames, EventType resultEventType, EventAdapterService eventAdapterService);
}
