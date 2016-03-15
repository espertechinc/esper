/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.named;

import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;

import java.util.List;
import java.util.Map;

public abstract class NamedWindowConsumerLatch
{
    public abstract void await();
    public abstract NamedWindowDeltaData getDeltaData();
    public abstract Map<EPStatementAgentInstanceHandle, List<NamedWindowConsumerView>> getDispatchTo();
    public abstract Thread getCurrentThread();
    public abstract void done();
}
