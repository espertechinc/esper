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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.StatementContext;

import java.util.Set;

public class OutputProcessViewAfterStateNone implements OutputProcessViewAfterState {
    public final static OutputProcessViewAfterStateNone INSTANCE = new OutputProcessViewAfterStateNone();

    private OutputProcessViewAfterStateNone() {
    }

    public boolean checkUpdateAfterCondition(EventBean[] newEvents, StatementContext statementContext) {
        return true;
    }

    public boolean checkUpdateAfterCondition(Set<MultiKey<EventBean>> newEvents, StatementContext statementContext) {
        return true;
    }

    public boolean checkUpdateAfterCondition(UniformPair<EventBean[]> newOldEvents, StatementContext statementContext) {
        return true;
    }

    public void destroy() {
        // no action required
    }
}
