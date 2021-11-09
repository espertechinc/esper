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
package com.espertech.esper.common.internal.compile.stage1;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public interface CompilerServices {
    StatementSpecRaw parseWalk(String epl, StatementSpecMapEnv mapEnv) throws StatementSpecCompileException;

    String lexSampleSQL(String querySQL) throws ExprValidationException;

    ExprNode compileExpression(String expression, StatementCompileTimeServices services) throws ExprValidationException;

    Class compileStandInClass(CodegenClassType classType, String classNameSimple, ModuleCompileTimeServices services);
}
