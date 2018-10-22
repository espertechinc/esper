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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import com.espertech.esper.runtime.internal.schedulesvcimpl.ScheduleVisit;
import com.espertech.esper.runtime.internal.schedulesvcimpl.ScheduleVisitor;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;

public class SupportScheduleHelper {

    public static int scheduleCount(EPStatement statement) {
        if (statement == null) {
            throw new IllegalStateException("Statement is null");
        }
        EPStatementSPI spi = (EPStatementSPI) statement;
        SchedulingServiceSPI schedulingServiceSPI = (SchedulingServiceSPI) spi.getStatementContext().getSchedulingService();
        ScheduleVisitorStatement visitor = new ScheduleVisitorStatement(spi.getStatementId());
        schedulingServiceSPI.visitSchedules(visitor);
        return visitor.getCount();
    }

    public static int scheduleCountOverall(RegressionEnvironment env) {
        EPRuntimeSPI spi = (EPRuntimeSPI) env.runtime();
        ScheduleVisitorAll visitor = new ScheduleVisitorAll();
        spi.getServicesContext().getSchedulingService().visitSchedules(visitor);
        return visitor.getCount();
    }

    private static class ScheduleVisitorStatement implements ScheduleVisitor {
        private final int statementId;
        private int count;

        public ScheduleVisitorStatement(int statementId) {
            this.statementId = statementId;
        }

        public void visit(ScheduleVisit visit) {
            if (visit.getStatementId() == statementId) {
                count++;
            }
        }

        public int getCount() {
            return count;
        }
    }

    private static class ScheduleVisitorAll implements ScheduleVisitor {
        private int count;

        public void visit(ScheduleVisit visit) {
            count++;
        }

        public int getCount() {
            return count;
        }
    }
}

