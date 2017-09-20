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
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.UpdateDispatchView;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;

public class OutputProcessViewDirectPostProcess extends OutputProcessViewDirect {
    private final OutputStrategyPostProcess postProcessor;

    public OutputProcessViewDirectPostProcess(ResultSetProcessor resultSetProcessor, OutputProcessViewDirectFactory parent, OutputStrategyPostProcess postProcessor) {
        super(resultSetProcessor, parent);
        this.postProcessor = postProcessor;
    }

    @Override
    protected void postProcess(boolean force, UniformPair<EventBean[]> newOldEvents, UpdateDispatchView childView) {
        postProcessor.output(force, newOldEvents, childView);
    }
}
