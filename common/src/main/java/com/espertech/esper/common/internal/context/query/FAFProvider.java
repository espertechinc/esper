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
package com.espertech.esper.common.internal.context.query;

import com.espertech.esper.common.internal.context.module.EPModuleEventTypeInitServices;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodProvider;

public interface FAFProvider {
    void initializeEventTypes(EPModuleEventTypeInitServices svc);

    void initializeQuery(EPStatementInitServices epInitServices);

    FAFQueryMethodProvider getQueryMethodProvider();
}
