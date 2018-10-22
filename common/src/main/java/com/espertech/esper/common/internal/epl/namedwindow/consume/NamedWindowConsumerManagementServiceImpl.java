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

import com.espertech.esper.common.internal.context.util.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamedWindowConsumerManagementServiceImpl implements NamedWindowConsumerManagementService {

    private final static Logger log = LoggerFactory.getLogger(NamedWindowConsumerManagementServiceImpl.class);

    public final static NamedWindowConsumerManagementServiceImpl INSTANCE = new NamedWindowConsumerManagementServiceImpl();

    private NamedWindowConsumerManagementServiceImpl() {
    }

    public void addConsumer(String namedWindowDeploymentId, String namedWindowName, int namedWindowConsumerId, StatementContext statementContext, boolean subquery) {
    }

    public void destroyConsumer(String namedWindowDeploymentId, String namedWindowName, StatementContext statementContext) {
    }

    public int getCount() {
        return 0;
    }
}
