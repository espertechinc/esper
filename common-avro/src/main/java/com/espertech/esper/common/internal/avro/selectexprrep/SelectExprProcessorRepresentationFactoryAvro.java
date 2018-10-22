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
package com.espertech.esper.common.internal.avro.selectexprrep;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.avro.writer.AvroRecastFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprForgeContext;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorRepresentationFactory;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;

public class SelectExprProcessorRepresentationFactoryAvro implements SelectExprProcessorRepresentationFactory {
    public SelectExprProcessorForge makeSelectNoWildcard(SelectExprForgeContext selectExprForgeContext, ExprForge[] exprForges, EventType resultEventType, TableCompileTimeResolver tableService, String statementName) throws ExprValidationException {
        return new EvalSelectNoWildcardAvro(selectExprForgeContext, exprForges, resultEventType, statementName);
    }

    public SelectExprProcessorForge makeRecast(EventType[] eventTypes, SelectExprForgeContext selectExprForgeContext, int streamNumber, AvroSchemaEventType insertIntoTargetType, ExprNode[] exprNodes, String statementName) throws ExprValidationException {
        return AvroRecastFactory.make(eventTypes, selectExprForgeContext, streamNumber, insertIntoTargetType, exprNodes, statementName);
    }

    public SelectExprProcessorForge makeJoinWildcard(String[] streamNames, EventType resultEventType) {
        return new SelectExprJoinWildcardProcessorAvro(resultEventType);
    }
}
