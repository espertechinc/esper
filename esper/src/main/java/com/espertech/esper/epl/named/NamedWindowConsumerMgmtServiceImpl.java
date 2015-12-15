/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.named;

import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.spec.NamedWindowConsumerStreamSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NamedWindowConsumerMgmtServiceImpl implements NamedWindowConsumerMgmtService {

    private static Log log = LogFactory.getLog(NamedWindowConsumerMgmtServiceImpl.class);

    public final static NamedWindowConsumerMgmtServiceImpl INSTANCE = new NamedWindowConsumerMgmtServiceImpl();

    private NamedWindowConsumerMgmtServiceImpl() {
    }

    public void addConsumer(StatementContext statementContext, NamedWindowConsumerStreamSpec namedSpec) {
        if (log.isDebugEnabled()) {
            log.debug("Statement '" + statementContext.getStatementName() + " registers consumer for '" + namedSpec.getWindowName() + "'");
        }
    }

    public void start(String statementName) {
        if (log.isDebugEnabled()) {
            log.debug("Statement '" + statementName + " starts consuming");
        }
    }

    public void stop(String statementName) {
        if (log.isDebugEnabled()) {
            log.debug("Statement '" + statementName + " stop consuming");
        }
    }

    public void destroy(String statementName) {
        if (log.isDebugEnabled()) {
            log.debug("Statement '" + statementName + " destroyed");
        }
        throw new RuntimeException("hello");
    }

    public void removeReferences(String statementName) {
        if (log.isDebugEnabled()) {
            log.debug("Statement '" + statementName + " removing references");
        }
    }
}
