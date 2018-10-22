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
package com.espertech.esper.common.internal.metrics.audit;

public interface AuditProvider extends AuditProviderView, AuditProviderStream, AuditProviderSchedule, AuditProviderProperty, AuditProviderInsert,
        AuditProviderExpression, AuditProviderPattern, AuditProviderPatternInstances, AuditProviderExprDef, AuditProviderDataflowTransition,
        AuditProviderDataflowSource, AuditProviderDataflowOp, AuditProviderContextPartition {
    boolean activated();
}
