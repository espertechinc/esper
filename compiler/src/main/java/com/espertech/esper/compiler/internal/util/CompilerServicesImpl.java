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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.internal.compile.stage1.CompilerServices;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public class CompilerServicesImpl implements CompilerServices {
    public StatementSpecRaw parseWalk(String epl, StatementSpecMapEnv mapEnv) throws StatementSpecCompileException {
        return CompilerHelperSingleEPL.parseWalk(epl, mapEnv);
    }

    public String lexSampleSQL(String querySQL) throws ExprValidationException {
        return SQLLexer.lexSampleSQL(querySQL);
    }

    public ExprNode compileExpression(String expression, StatementCompileTimeServices services)
            throws ExprValidationException {
        String toCompile = "select * from java.lang.Object#time(" + expression + ")";

        StatementSpecRaw raw;
        try {
            raw = services.getCompilerServices().parseWalk(toCompile, services.getStatementSpecMapEnv());
        } catch (StatementSpecCompileException e) {
            throw new ExprValidationException("Failed to compile expression '" + expression + "': " + e.getExpression(), e);
        }

        return raw.getStreamSpecs().get(0).getViewSpecs()[0].getObjectParameters().get(0);
    }
}
