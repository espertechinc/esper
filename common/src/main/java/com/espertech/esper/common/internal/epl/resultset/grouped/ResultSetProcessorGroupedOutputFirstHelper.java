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
package com.espertech.esper.common.internal.epl.resultset.grouped;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolled;
import com.espertech.esper.common.internal.epl.output.polled.OutputConditionPolledFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputHelper;

public interface ResultSetProcessorGroupedOutputFirstHelper extends ResultSetProcessorOutputHelper {
    OutputConditionPolled getOrAllocate(Object mk, AgentInstanceContext agentInstanceContext, OutputConditionPolledFactory optionalOutputFirstConditionFactory);

    void remove(Object key);

    void destroy();
}
