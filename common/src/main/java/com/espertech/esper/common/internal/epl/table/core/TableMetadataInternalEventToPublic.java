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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

public interface TableMetadataInternalEventToPublic {
    EventBean convert(EventBean event, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

    Object[] convertToUnd(EventBean event, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);
}
