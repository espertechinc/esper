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
package com.espertech.esper.epl.named;

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.spec.NamedWindowConsumerStreamSpec;

public interface NamedWindowConsumerMgmtService {
    void addConsumer(StatementContext statementContext, NamedWindowConsumerStreamSpec namedSpec);

    void start(String statementName);

    void stop(String statementName);

    void destroy(String statementName);

    void removeReferences(String statementName);
}
