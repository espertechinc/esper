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
package com.espertech.esperio.db.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class DMLStatement {
    private final static Logger log = LoggerFactory.getLogger(DMLStatement.class);

    private final StoreExceptionHandler storeExceptionHandler;
    private final String dmlSQL;
    private final Map<Integer, BindingEntry> bindings;

    public DMLStatement(StoreExceptionHandler storeExceptionHandler, String dmlSQL, Map<Integer, BindingEntry> bindings) {
        this.storeExceptionHandler = storeExceptionHandler;
        this.dmlSQL = dmlSQL;
        this.bindings = bindings;
    }

    public void execute(Connection connection, EventBean eventBean) {
        PreparedStatement statement = null;
        try {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Executing '" + dmlSQL + ")");
            }
            statement = connection.prepareStatement(dmlSQL);
            for (Map.Entry<Integer, BindingEntry> entry : bindings.entrySet()) {
                Object value = entry.getValue().getGetter().get(eventBean);
                statement.setObject(entry.getKey(), value);
            }

            int rows = statement.executeUpdate();
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug("Execution yielded " + rows + " rows");
            }
        } catch (SQLException ex) {
            String message = "Failed to invoke : " + dmlSQL + " :" + ex.getMessage();
            log.error(message, ex);
            storeExceptionHandler.handle(message, ex);
            throw new StoreExceptionDBRel(message, ex);
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
            }
        }
    }
}
