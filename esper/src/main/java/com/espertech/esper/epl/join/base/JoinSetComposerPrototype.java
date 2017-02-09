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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.view.Viewable;

/**
 * Interface for a prototype populating a join tuple result set from new data and old data for each stream.
 */
public interface JoinSetComposerPrototype {
    public JoinSetComposerDesc create(Viewable[] streamViews, boolean isFireAndForget, AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient);
}
