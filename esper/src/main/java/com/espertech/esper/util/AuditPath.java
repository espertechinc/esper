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

package com.espertech.esper.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.event.EventBeanUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;

/**
 * Global boolean for enabling and disable audit path reporting.
 */
public class AuditPath {

    private static final Log auditLogDestination = LogFactory.getLog(AuditPath.AUDIT_LOG);

    private volatile static AuditCallback auditCallback;

    /**
     * Log destination for the query plan logging.
     */
    public static final String QUERYPLAN_LOG = "com.espertech.esper.queryplan"; 

    /**
     * Log destination for the JDBC logging.
     */
    public static final String JDBC_LOG = "com.espertech.esper.jdbc"; 

    /**
     * Log destination for the audit logging.
     */
    public static final String AUDIT_LOG = "com.espertech.esper.audit";

    /**
     * Public access.
     */
    public static boolean isAuditEnabled = false;

    private static String auditPattern;

    public static void setAuditPattern(String auditPattern) {
        AuditPath.auditPattern = auditPattern;
    }

    public static void auditInsertInto(String engineURI, String statementName, EventBean theEvent) {
        auditLog(engineURI, statementName, AuditEnum.INSERT, EventBeanUtility.summarize(theEvent));
    }

    public static void auditContextPartition(String engineURI, String statementName, boolean allocate, int agentInstanceId) {
        StringWriter writer = new StringWriter();
        writer.write(allocate ? "Allocate" : "Destroy");
        writer.write(" cpid ");
        writer.write(Integer.toString(agentInstanceId));
        auditLog(engineURI, statementName, AuditEnum.CONTEXTPARTITION, writer.toString());
    }

    public static void auditLog(String engineURI, String statementName, AuditEnum category, String message) {
        if (auditPattern == null) {
            String text = AuditContext.defaultFormat(statementName, category, message);
            auditLogDestination.info(text);
        }
        else {
            String result = auditPattern.replace("%s", statementName).replace("%u", engineURI).replace("%c", category.getValue()).replace("%m", message);
            auditLogDestination.info(result);
        }
        if (auditCallback != null) {
            auditCallback.audit(new AuditContext(engineURI, statementName, category, message));
        }
    }

    public static boolean isInfoEnabled() {
        return (auditLogDestination.isInfoEnabled() || auditCallback != null);
    }

    public static void setAuditCallback(AuditCallback auditCallback) {
        AuditPath.auditCallback = auditCallback;
    }

    public static AuditCallback getAuditCallback() {
        return auditCallback;
    }
}