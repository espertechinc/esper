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
package com.espertech.esper.client.annotation;

/**
 * Annotation for use in EPL statements to add a debug.
 */
public @interface Audit {
    /**
     * Comma-separated list of keywords (not case-sentitive), see {@link AuditEnum} for a list of keywords.
     *
     * @return comma-separated list of audit keywords
     */
    String value() default "*";
}
