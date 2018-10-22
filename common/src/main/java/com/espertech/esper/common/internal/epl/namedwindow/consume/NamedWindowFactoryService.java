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
package com.espertech.esper.common.internal.epl.namedwindow.consume;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowTailView;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

public interface NamedWindowFactoryService {
    NamedWindow createNamedWindow(NamedWindowMetaData metadata, EPStatementInitServices services);

    NamedWindowTailView createNamedWindowTailView(EventType eventType, boolean isParentBatchWindow, EPStatementInitServices services, String contextNameWindow);
}
