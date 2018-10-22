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
package com.espertech.esper.common.internal.epl.join.support;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;

import java.lang.annotation.Annotation;

public class QueryPlanIndexHookUtil {

    public static QueryPlanIndexHook getHook(Annotation[] annotations, ClasspathImportService classpathImportService) {
        try {
            return (QueryPlanIndexHook) ClasspathImportUtil.getAnnotationHook(annotations, HookType.INTERNAL_QUERY_PLAN, QueryPlanIndexHook.class, classpathImportService);
        } catch (ExprValidationException e) {
            throw new EPException("Failed to obtain hook for " + HookType.INTERNAL_QUERY_PLAN);
        }
    }

}
