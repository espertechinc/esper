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
package com.espertech.esper.common.internal.epl.output.view;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessView;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

/**
 * Factory for output process view that does not enforce any output policies and may simply
 * hand over events to child views, does not handle distinct.
 */
public class OutputProcessViewDirectFactory implements OutputProcessViewFactory {
    OutputStrategyPostProcessFactory postProcessFactory;

    public OutputProcessViewDirectFactory() {
    }

    public OutputProcessViewDirectFactory(OutputStrategyPostProcessFactory postProcessFactory) {
        this.postProcessFactory = postProcessFactory;
    }

    public void setPostProcessFactory(OutputStrategyPostProcessFactory postProcessFactory) {
        this.postProcessFactory = postProcessFactory;
    }

    public OutputProcessView makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {
        OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
        return new OutputProcessViewDirectPostProcess(agentInstanceContext, resultSetProcessor, postProcess);
    }
}
