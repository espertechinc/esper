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
package com.espertech.esper.runtime.internal.support;

import com.espertech.esper.common.internal.context.module.StatementInformationalsRuntime;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;
import com.espertech.esper.common.internal.schedule.ScheduleBucket;

public class SupportStatementContextFactory {
    public static StatementContext makeContext(int statementId) {
        StatementInformationalsRuntime informationals = new StatementInformationalsRuntime();
        return new StatementContext(null, "deployment1", statementId, "s0", null, informationals, null, new StatementContextRuntimeServices(), null, null, null, null, new ScheduleBucket(statementId), null, null, null, null, null);
    }
}
