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

import com.espertech.esper.client.annotation.AuditEnum;

public class AuditContext {

    private final String engineURI;
    private final String statementName;
    private final AuditEnum category;
    private final String message;

    public AuditContext(String engineURI, String statementName, AuditEnum category, String message) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.category = category;
        this.message = message;
    }

    public String getEngineURI() {
        return engineURI;
    }

    public String getStatementName() {
        return statementName;
    }

    public AuditEnum getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String format() {
        return defaultFormat(statementName, category, message);
    }

    public static String defaultFormat(String statementName, AuditEnum category, String message) {
        StringBuilder buf = new StringBuilder();
        buf.append("Statement ");
        buf.append(statementName);
        buf.append(" ");
        buf.append(category.getPrettyPrintText());
        buf.append(" ");
        buf.append(message);
        return buf.toString();
    }
}