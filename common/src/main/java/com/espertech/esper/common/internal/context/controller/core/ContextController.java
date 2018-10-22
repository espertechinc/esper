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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionVisitor;

import java.util.Map;

public interface ContextController {
    void activate(IntSeqKey path, Object[] parentPartitionKeys, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern);

    void deactivate(IntSeqKey path, boolean terminateChildContexts);

    void visitSelectedPartitions(IntSeqKey path, ContextPartitionSelector selector, ContextPartitionVisitor visitor, ContextPartitionSelector[] selectorPerLevel);

    ContextControllerFactory getFactory();

    void destroy();

    ContextManagerRealization getRealization();
}
