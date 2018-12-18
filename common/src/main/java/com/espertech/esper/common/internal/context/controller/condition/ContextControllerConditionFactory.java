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
package com.espertech.esper.common.internal.context.controller.condition;

import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextController;
import com.espertech.esper.common.internal.schedule.ScheduleExpressionUtil;
import com.espertech.esper.common.internal.schedule.ScheduleSpec;

public class ContextControllerConditionFactory {

    public static ContextControllerConditionNonHA getEndpoint(IntSeqKey conditionPath,
                                                              Object[] partitionKeys,
                                                              ContextConditionDescriptor endpoint,
                                                              ContextControllerConditionCallback callback,
                                                              ContextController controller,
                                                              boolean isStartEndpoint) {
        if (endpoint instanceof ContextConditionDescriptorFilter) {
            ContextConditionDescriptorFilter filter = (ContextConditionDescriptorFilter) endpoint;
            return new ContextControllerConditionFilter(conditionPath, partitionKeys, filter, callback, controller);
        }
        if (endpoint instanceof ContextConditionDescriptorTimePeriod) {
            ContextConditionDescriptorTimePeriod timePeriod = (ContextConditionDescriptorTimePeriod) endpoint;
            long scheduleSlot = controller.getRealization().getAgentInstanceContextCreate().getScheduleBucket().allocateSlot();
            return new ContextControllerConditionTimePeriod(scheduleSlot, timePeriod, conditionPath, callback, controller);
        }
        if (endpoint instanceof ContextConditionDescriptorCrontab) {
            ContextConditionDescriptorCrontab crontab = (ContextConditionDescriptorCrontab) endpoint;
            ScheduleSpec[] schedules = new ScheduleSpec[crontab.getEvaluatorsPerCrontab().length];
            for (int i = 0; i < schedules.length; i++) {
                schedules[i] = ScheduleExpressionUtil.crontabScheduleBuild(crontab.getEvaluatorsPerCrontab()[i], controller.getRealization().getAgentInstanceContextCreate());
            }
            long scheduleSlot = controller.getRealization().getAgentInstanceContextCreate().getScheduleBucket().allocateSlot();
            return new ContextControllerConditionCrontabImpl(conditionPath, scheduleSlot, schedules, crontab, callback, controller);
        }
        if (endpoint instanceof ContextConditionDescriptorPattern) {
            ContextConditionDescriptorPattern pattern = (ContextConditionDescriptorPattern) endpoint;
            return new ContextControllerConditionPattern(conditionPath, partitionKeys, pattern, callback, controller);
        }
        if (endpoint instanceof ContextConditionDescriptorNever) {
            return ContextControllerConditionNever.INSTANCE;
        }
        if (endpoint instanceof ContextConditionDescriptorImmediate) {
            return ContextControllerConditionImmediate.INSTANCE;
        }
        throw new IllegalStateException("Unrecognized context range endpoint " + endpoint.getClass());
    }
}
