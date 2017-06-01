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
package com.espertech.esper.codegen.core;

import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.statement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.notInstanceOf;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.instanceOf;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class CodegenBlock {
    private final CodegenMethod parentMethod;
    private final CodegenStatementWBlockBase parentWBlock;
    private boolean closed;
    protected List<CodegenStatement> statements = new ArrayList<>(4);

    public CodegenBlock(CodegenMethod parentMethod) {
        this.parentMethod = parentMethod;
        this.parentWBlock = null;
    }

    public CodegenBlock(CodegenStatementWBlockBase parentWBlock) {
        this.parentWBlock = parentWBlock;
        this.parentMethod = null;
    }

    public CodegenBlock expression(CodegenExpression expression) {
        checkClosed();
        statements.add(new CodegenStatementExpression(expression));
        return this;
    }

    public CodegenBlock ifConditionReturnConst(CodegenExpression condition, Object constant) {
        checkClosed();
        statements.add(new CodegenStatementIfConditionReturnConst(condition, constant));
        return this;
    }

    public CodegenBlock ifNotInstanceOf(String name, Class clazz) {
        return ifInstanceOf(name, clazz, true);
    }

    public CodegenBlock ifInstanceOf(String name, Class clazz) {
        return ifInstanceOf(name, clazz, false);
    }

    private CodegenBlock ifInstanceOf(String name, Class clazz, boolean not) {
        checkClosed();
        CodegenStatementIf ifStmt = new CodegenStatementIf(this);
        CodegenExpression condition = !not ? instanceOf(ref(name), clazz) : notInstanceOf(ref(name), clazz);
        CodegenBlock block = new CodegenBlock(ifStmt);
        ifStmt.add(condition, block);
        statements.add(ifStmt);
        return block;
    };

    public CodegenBlock forLoopInt(String name, CodegenExpression upperLimit) {
        checkClosed();
        CodegenStatementForInt forStmt = new CodegenStatementForInt(this, name, upperLimit);
        CodegenBlock block = new CodegenBlock(forStmt);
        forStmt.setBlock(block);
        statements.add(forStmt);
        return block;
    };

    public CodegenBlock blockElseIf(CodegenExpression condition) {
        if (parentMethod != null) {
            throw new IllegalStateException("Else-If in a method-level block?");
        }
        if (!(parentWBlock instanceof CodegenStatementIf)) {
            throw new IllegalStateException("Else_if in a non-if block?");
        }
        CodegenStatementIf ifStmt = (CodegenStatementIf) parentWBlock;
        checkClosed();
        closed = true;
        CodegenBlock block = new CodegenBlock(parentWBlock);
        ifStmt.add(condition, block);
        return block;
    };

    public CodegenBlock blockElse() {
        if (parentMethod != null) {
            throw new IllegalStateException("Else in a method-level block?");
        }
        if (!(parentWBlock instanceof CodegenStatementIf)) {
            throw new IllegalStateException("Else in a non-if block?");
        }
        CodegenStatementIf ifStmt = (CodegenStatementIf) parentWBlock;
        if (ifStmt.getOptionalElse() != null) {
            throw new IllegalStateException("Else already present");
        }
        checkClosed();
        closed = true;
        CodegenBlock block = new CodegenBlock(parentWBlock);
        ifStmt.setOptionalElse(block);
        return block;
    };

    public CodegenBlock declareVarWCast(Class clazz, String var, String rhsName) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVarWCast(clazz, var, rhsName));
        return this;
    }

    public CodegenBlock declareVar(Class clazz, String var, CodegenExpression initializer) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVar(clazz, var, initializer));
        return this;
    }

    public CodegenBlock declareVarNull(Class clazz, String var) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVarNull(clazz, var));
        return this;
    }

    public CodegenBlock assignRef(String ref, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignRef(ref, assignment));
        return this;
    }

    public CodegenBlock assignArrayElement(String ref, CodegenExpression index, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignArrayElement(ref, index, assignment));
        return this;
    }

    public CodegenBlock exprDotMethod(CodegenExpression expression, String method, CodegenExpression ... params) {
        checkClosed();
        statements.add(new CodegenStatementExprDotMethod(expression, method, params));
        return this;
    }

    public CodegenBlock ifRefNullReturnFalse(String ref) {
        checkClosed();
        statements.add(new CodegenStatementIfRefNullReturnFalse(ref));
        return this;
    }

    public CodegenBlock ifRefNotTypeReturnConst(String ref, Class type, Object constant) {
        checkClosed();
        statements.add(new CodegenStatementIfRefNotTypeReturnConst(ref, type, constant));
        return this;
    }

    public CodegenBlock ifRefNullReturnNull(String ref) {
        checkClosed();
        statements.add(new CodegenStatementIfRefNullReturnNull(ref));
        return this;
    }

    public CodegenBlock declareVarEventPerStreamUnd(Class clazz, int streamNum) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVarEventPerStreamUnd(clazz, streamNum));
        return this;
    }


    public CodegenBlock blockReturn(CodegenExpression expression) {
        if (parentWBlock == null) {
            throw new IllegalStateException("No codeblock parent, use 'methodReturn... instead");
        }
        checkClosed();
        closed = true;
        statements.add(new CodegenStatementReturnExpression(expression));
        return parentWBlock.getParent();
    }

    public CodegenBlock blockEnd() {
        if (parentWBlock == null) {
            throw new IllegalStateException("No codeblock parent, use 'methodReturn... instead");
        }
        checkClosed();
        closed = true;
        return parentWBlock.getParent();
    }

    public String methodReturn(CodegenExpression expression) {
        if (parentMethod == null) {
            throw new IllegalStateException("No method parent, use 'blockReturn... instead");
        }
        checkClosed();
        closed = true;
        statements.add(new CodegenStatementReturnExpression(expression));
        return parentMethod.getMethodName();
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        for (CodegenStatement statement : statements) {
            statement.render(builder, imports);
        }
    }

    public void mergeClasses(Set<Class> classes) {
        for (CodegenStatement statement : statements) {
            statement.mergeClasses(classes);
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Code block already closed");
        }
    }
}
