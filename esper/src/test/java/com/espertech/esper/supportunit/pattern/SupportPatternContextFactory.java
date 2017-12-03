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
package com.espertech.esper.supportunit.pattern;

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.pattern.PatternAgentInstanceContext;
import com.espertech.esper.pattern.PatternContext;
import com.espertech.esper.schedule.SchedulingService;

public class SupportPatternContextFactory {
    public static PatternAgentInstanceContext makePatternAgentInstanceContext() {
        return makePatternAgentInstanceContext(null);
    }

    public static PatternAgentInstanceContext makePatternAgentInstanceContext(SchedulingService scheduleService) {
        StatementContext stmtContext;
        if (scheduleService == null) {
            stmtContext = SupportStatementContextFactory.makeContext();
        } else {
            stmtContext = SupportStatementContextFactory.makeContext(scheduleService);
        }
        PatternContext context = new PatternContext(stmtContext, 1, new MatchedEventMapMeta(new String[0], false), false);
        return new PatternAgentInstanceContext(context, SupportStatementContextFactory.makeAgentInstanceContext(), false, null);
    }

    public static PatternContext makeContext() {
        StatementContext stmtContext = SupportStatementContextFactory.makeContext();
        return new PatternContext(stmtContext, 1, new MatchedEventMapMeta(new String[0], false), false);
    }
}
