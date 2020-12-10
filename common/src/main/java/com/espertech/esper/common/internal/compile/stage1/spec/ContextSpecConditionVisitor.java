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
package com.espertech.esper.common.internal.compile.stage1.spec;

public interface ContextSpecConditionVisitor<T>  {
    T visit(ContextSpecConditionCrontab crontab);
    T visit(ContextSpecConditionFilter filter);
    T visit(ContextSpecConditionPattern pattern);
    T visit(ContextSpecConditionNever never);
    T visit(ContextSpecConditionTimePeriod timePeriod);
    T visit(ContextSpecConditionImmediate immediate);
}
