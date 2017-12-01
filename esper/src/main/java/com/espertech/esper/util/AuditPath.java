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
package com.espertech.esper.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.AuditEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * Global boolean for enabling and disable audit path reporting.
 */
public class AuditPath {

    private static final Logger AUDIT_LOG_DESTINATION = LoggerFactory.getLogger(AuditPath.AUDIT_LOG);
    public final static String METHOD_AUDITLOG = "auditLog";

    private volatile static AuditCallback auditCallback;

    /**
     * Logger destination for the query plan logging.
     */
    public static final String QUERYPLAN_LOG = "com.espertech.esper.queryplan";

    /**
     * Logger destination for the JDBC logging.
     */
    public static final String JDBC_LOG = "com.espertech.esper.jdbc";

    /**
     * Logger destination for the audit logging.
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
        auditLog(engineURI, statementName, AuditEnum.INSERT, EventBeanSummarizer.summarize(theEvent));
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
            AUDIT_LOG_DESTINATION.info(text);
        } else {
            String result = auditPattern.replace("%s", statementName).replace("%u", engineURI).replace("%c", category.getValue()).replace("%m", message);
            AUDIT_LOG_DESTINATION.info(result);
        }
        if (auditCallback != null) {
            auditCallback.audit(new AuditContext(engineURI, statementName, category, message));
        }
    }

    public static boolean isInfoEnabled() {
        return AUDIT_LOG_DESTINATION.isInfoEnabled() || auditCallback != null;
    }

    public static void setAuditCallback(AuditCallback auditCallback) {
        AuditPath.auditCallback = auditCallback;
    }

    public static AuditCallback getAuditCallback() {
        return auditCallback;
    }
}
