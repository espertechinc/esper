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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowRootViewInstance;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnExprBaseViewResult;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public interface InfraOnExprFactory {
    InfraOnExprBaseViewResult makeNamedWindow(SubordWMatchExprLookupStrategy lookupStrategy,
                                              NamedWindowRootViewInstance namedWindowRootViewInstance,
                                              AgentInstanceContext agentInstanceContext);

    InfraOnExprBaseViewResult makeTable(SubordWMatchExprLookupStrategy lookupStrategy,
                                        TableInstance tableInstance,
                                        AgentInstanceContext agentInstanceContext);
}
