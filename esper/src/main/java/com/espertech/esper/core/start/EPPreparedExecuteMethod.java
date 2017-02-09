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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.core.service.EPPreparedQueryResult;

/**
 * Starts and provides the stop method for EPL statements.
 */
public interface EPPreparedExecuteMethod {
    public EPPreparedQueryResult execute(ContextPartitionSelector[] contextPartitionSelectors);

    public EventType getEventType();
}
