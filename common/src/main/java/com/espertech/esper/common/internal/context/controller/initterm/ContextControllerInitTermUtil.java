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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.ContextPartitionIdentifierInitiatedTerminated;
import com.espertech.esper.common.internal.context.controller.condition.*;
import com.espertech.esper.common.internal.schedule.ScheduleComputeHelper;
import com.espertech.esper.common.internal.schedule.ScheduleExpressionUtil;
import com.espertech.esper.common.internal.schedule.ScheduleSpec;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;

import java.util.Collections;
import java.util.Map;

public class ContextControllerInitTermUtil {
    public static ContextControllerInitTermSvc getService(ContextControllerInitTermFactory factory) {
        if (factory.getFactoryEnv().isRoot()) {
            return new ContextControllerInitTermSvcLevelOne();
        }
        return new ContextControllerInitTermSvcLevelAny();
    }

    public static boolean determineCurrentlyRunning(ContextControllerCondition startCondition, ContextControllerInitTerm controller) {
        if (startCondition.isImmediate()) {
            return true;
        }

        ContextControllerDetailInitiatedTerminated spec = controller.getFactory().getInitTermSpec();
        if (spec.isOverlapping()) {
            return false;
        }

        // we are not currently running if either of the endpoints is not crontab-triggered
        if ((spec.getStartCondition() instanceof ContextConditionDescriptorCrontab) &&
                ((spec.getEndCondition() instanceof ContextConditionDescriptorCrontab))) {
            ScheduleSpec[] schedulesStart = ((ContextControllerConditionCrontab) startCondition).getSchedules();

            ContextConditionDescriptorCrontab endCron = (ContextConditionDescriptorCrontab) spec.getEndCondition();
            ScheduleSpec[] schedulesEnd = new ScheduleSpec[endCron.getEvaluatorsPerCrontab().length];
            for (int i = 0; i < schedulesEnd.length; i++) {
                schedulesEnd[i] = ScheduleExpressionUtil.crontabScheduleBuild(endCron.getEvaluatorsPerCrontab()[i], controller.getRealization().getAgentInstanceContextCreate());
            }

            ClasspathImportServiceRuntime classpathImportService = controller.getRealization().getAgentInstanceContextCreate().getClasspathImportServiceRuntime();
            long time = controller.getRealization().getAgentInstanceContextCreate().getSchedulingService().getTime();
            long nextScheduledStartTime = computeScheduleMinimumNextOccurance(schedulesStart, time, classpathImportService);
            long nextScheduledEndTime = computeScheduleMinimumNextOccurance(schedulesEnd, time, classpathImportService);
            return nextScheduledStartTime >= nextScheduledEndTime;
        }

        if (startCondition.getDescriptor() instanceof ContextConditionDescriptorTimePeriod) {
            ContextConditionDescriptorTimePeriod descriptor = (ContextConditionDescriptorTimePeriod) startCondition.getDescriptor();
            Long endTime = descriptor.getExpectedEndTime(controller.getRealization());
            if (endTime != null && endTime <= 0) {
                return true;
            }
        }

        return startCondition instanceof ContextConditionDescriptorImmediate;
    }

    public static ContextControllerInitTermPartitionKey buildPartitionKey(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerCondition endCondition, ContextControllerInitTerm controller) {
        long startTime = controller.realization.getAgentInstanceContextCreate().getSchedulingService().getTime();
        Long expectedEndTime = endCondition.getExpectedEndTime();
        return new ContextControllerInitTermPartitionKey(optionalTriggeringEvent, optionalTriggeringPattern, startTime, expectedEndTime);
    }

    public static ContextPartitionIdentifierInitiatedTerminated keyToIdentifier(int subpathIdOrCPId, ContextControllerInitTermPartitionKey key, ContextControllerInitTerm controller) {
        ContextPartitionIdentifierInitiatedTerminated identifier = new ContextPartitionIdentifierInitiatedTerminated();
        identifier.setStartTime(key.getStartTime());
        identifier.setEndTime(key.getExpectedEndTime());

        ContextConditionDescriptor start = controller.getFactory().getInitTermSpec().getStartCondition();
        if (start instanceof ContextConditionDescriptorFilter) {
            ContextConditionDescriptorFilter filter = (ContextConditionDescriptorFilter) start;
            if (filter.getOptionalFilterAsName() != null) {
                identifier.setProperties(Collections.singletonMap(filter.getOptionalFilterAsName(), key.getTriggeringEvent()));
            }
        }

        if (controller.getFactory().getFactoryEnv().isLeaf()) {
            identifier.setContextPartitionId(subpathIdOrCPId);
        }

        return identifier;
    }

    public static long computeScheduleMinimumNextOccurance(ScheduleSpec[] schedules, long time, ClasspathImportServiceRuntime classpathImportService) {
        long value = Long.MAX_VALUE;
        for (ScheduleSpec spec : schedules) {
            long computed = ScheduleComputeHelper.computeNextOccurance(spec, time, classpathImportService.getTimeZone(), classpathImportService.getTimeAbacus());
            if (computed < value) {
                value = computed;
            }
        }
        return value;
    }

    public static long computeScheduleMinimumDelta(ScheduleSpec[] schedules, long time, ClasspathImportServiceRuntime classpathImportService) {
        long value = Long.MAX_VALUE;
        for (ScheduleSpec spec : schedules) {
            long computed = ScheduleComputeHelper.computeDeltaNextOccurance(spec, time, classpathImportService.getTimeZone(), classpathImportService.getTimeAbacus());
            if (computed < value) {
                value = computed;
            }
        }
        return value;
    }
}
