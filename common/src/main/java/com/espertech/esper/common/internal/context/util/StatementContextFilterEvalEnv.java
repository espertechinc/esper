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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;

import java.lang.annotation.Annotation;

public class StatementContextFilterEvalEnv {
    private final ClasspathImportServiceRuntime classpathImportServiceRuntime;
    private final Annotation[] annotations;
    private final VariableManagementService variableManagementService;
    private final TableExprEvaluatorContext tableExprEvaluatorContext;

    public StatementContextFilterEvalEnv(ClasspathImportServiceRuntime classpathImportServiceRuntime, Annotation[] annotations, VariableManagementService variableManagementService, TableExprEvaluatorContext tableExprEvaluatorContext) {
        this.classpathImportServiceRuntime = classpathImportServiceRuntime;
        this.annotations = annotations;
        this.variableManagementService = variableManagementService;
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
    }

    public ClasspathImportServiceRuntime getClasspathImportServiceRuntime() {
        return classpathImportServiceRuntime;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
    }

    public VariableManagementService getVariableManagementService() {
        return variableManagementService;
    }
}
