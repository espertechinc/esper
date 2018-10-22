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
package com.espertech.esper.compiler.internal.parse;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.support.SupportClasspathImport;

public class SupportStatementSpecMapEnv {
    public static StatementSpecMapEnv make(ClasspathImportServiceCompileTime engineImportService) {
        return new StatementSpecMapEnv(engineImportService,
                VariableCompileTimeResolverEmpty.INSTANCE,
                new Configuration(),
                ExprDeclaredCompileTimeResolverEmpty.INSTANCE,
                ContextCompileTimeResolverEmpty.INSTANCE,
                TableCompileTimeResolverEmpty.INSTANCE,
                ScriptCompileTimeResolverEmpty.INSTANCE,
                null);
    }

    public static StatementSpecMapEnv make() {
        return make(SupportClasspathImport.INSTANCE);
    }
}
