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
package com.espertech.esper.common.internal.epl.output.core;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;

public class OutputProcessViewDirectSimpleFactory implements OutputProcessViewFactory {
    public final static EPTypeClass EPTYPE = new EPTypeClass(OutputProcessViewDirectSimpleFactory.class);

    public final static OutputProcessViewDirectSimpleFactory INSTANCE = new OutputProcessViewDirectSimpleFactory();

    private OutputProcessViewDirectSimpleFactory() {
    }

    public OutputProcessView makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {
        return new OutputProcessViewDirectSimpleImpl(resultSetProcessor, agentInstanceContext);
    }
}
