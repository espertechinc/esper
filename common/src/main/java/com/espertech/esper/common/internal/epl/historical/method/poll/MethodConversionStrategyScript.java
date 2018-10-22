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
package com.espertech.esper.common.internal.epl.historical.method.poll;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodConversionStrategyScript extends MethodConversionStrategyBase {
    private final static Logger log = LoggerFactory.getLogger(MethodConversionStrategyScript.class);

    public List<EventBean> convert(Object invocationResult, MethodTargetStrategy origin, AgentInstanceContext agentInstanceContext) {
        if (!(invocationResult instanceof EventBean[])) {
            log.warn("Script expected return type EventBean[] does not match result " + invocationResult == null ? "null" : invocationResult.getClass().getName());
            return Collections.emptyList();
        }
        return Arrays.asList((EventBean[]) invocationResult);
    }
}
