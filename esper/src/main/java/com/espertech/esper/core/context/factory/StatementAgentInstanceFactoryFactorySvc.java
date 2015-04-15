/*
 * *************************************************************************************
 *  Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 *  http://esper.codehaus.org                                                          *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.context.factory;

import com.espertech.esper.core.context.activator.ViewableActivator;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StreamJoinAnalysisResult;
import com.espertech.esper.epl.core.ResultSetProcessorFactoryDesc;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.core.ViewResourceDelegateVerified;
import com.espertech.esper.epl.join.base.JoinSetComposerPrototype;
import com.espertech.esper.epl.spec.StatementSpecCompiled;
import com.espertech.esper.epl.view.OutputProcessViewFactory;
import com.espertech.esper.view.ViewFactoryChain;

public interface StatementAgentInstanceFactoryFactorySvc {
    public StatementAgentInstanceFactorySelect makeFactorySelect(int numStreams, ViewableActivator[] eventStreamParentViewableActivators, StatementContext statementContext, StatementSpecCompiled statementSpec, EPServicesContext services, StreamTypeService typeService, ViewFactoryChain[] unmaterializedViewChain, ResultSetProcessorFactoryDesc resultSetProcessorPrototypeDesc, StreamJoinAnalysisResult joinAnalysisResult, boolean recoveringResilient, JoinSetComposerPrototype joinSetComposerPrototype, SubSelectStrategyCollection subSelectStrategyCollection, ViewResourceDelegateVerified viewResourceDelegateVerified, OutputProcessViewFactory outputViewFactory);
}
