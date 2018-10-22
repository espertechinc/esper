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
package com.espertech.esper.common.internal.epl.rowrecog.state;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAView;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPartitionTerminationStateComparator;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategyImpl;

/**
 * Service for creating match-recognize factory and state services.
 */
public interface RowRecogStateRepoFactory {
    RowRecogPartitionStateRepo makeSingle(RowRecogPreviousStrategyImpl prevGetter, AgentInstanceContext agentInstanceContext, RowRecogNFAView view, boolean keepScheduleState, RowRecogPartitionTerminationStateComparator terminationStateCompare);

    RowRecogPartitionStateRepo makePartitioned(RowRecogPreviousStrategyImpl prevGetter, RowRecogPartitionStateRepoGroupMeta stateRepoGroupMeta, AgentInstanceContext agentInstanceContext, RowRecogNFAView view, boolean keepScheduleState, RowRecogPartitionTerminationStateComparator terminationStateCompare);
}
