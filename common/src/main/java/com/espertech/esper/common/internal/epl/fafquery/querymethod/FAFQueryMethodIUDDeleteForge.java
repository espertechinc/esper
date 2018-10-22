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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodIUDDeleteForge extends FAFQueryMethodIUDBaseForge {
    public FAFQueryMethodIUDDeleteForge(StatementSpecCompiled spec, Compilable compilable, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        super(spec, compilable, statementRawInfo, services);
    }

    protected void initExec(String aliasName, StatementSpecCompiled spec, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
    }

    protected Class typeOfMethod() {
        return FAFQueryMethodIUDDelete.class;
    }

    protected void makeInlineSpecificSetter(CodegenExpressionRef queryMethod, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(queryMethod, "setOptionalWhereClause", whereClause == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(whereClause.getForge(), method, this.getClass(), classScope));
    }
}
