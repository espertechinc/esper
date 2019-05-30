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
package com.espertech.esper.common.internal.bytecodemodel.base;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenIndent;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.*;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.*;
import com.espertech.esper.common.internal.util.TriConsumer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenBlock {
    private final CodegenCtor parentCtor;
    private final CodegenMethod parentMethodNode;
    private final CodegenStatementWBlockBase parentWBlock;
    private boolean closed;
    protected List<CodegenStatement> statements = new ArrayList<>(4);

    private CodegenBlock(CodegenCtor parentCtor, CodegenMethod parentMethodNode, CodegenStatementWBlockBase parentWBlock) {
        this.parentCtor = parentCtor;
        this.parentMethodNode = parentMethodNode;
        this.parentWBlock = parentWBlock;
    }

    public CodegenBlock() {
        this(null, null, null);
    }

    public CodegenBlock(CodegenCtor parentCtor) {
        this(parentCtor, null, null);
    }

    public CodegenBlock(CodegenMethod parentMethodNode) {
        this(null, parentMethodNode, null);
    }

    public CodegenBlock(CodegenStatementWBlockBase parentWBlock) {
        this(null, null, parentWBlock);
    }

    public CodegenBlock expression(CodegenExpression expression) {
        checkClosed();
        statements.add(new CodegenStatementExpression(expression));
        return this;
    }

    public CodegenBlock decrement(CodegenExpression expression) {
        checkClosed();
        statements.add(new CodegenStatementExpression(CodegenExpressionBuilder.decrement(expression)));
        return this;
    }

    public CodegenBlock decrementRef(String ref) {
        checkClosed();
        statements.add(new CodegenStatementExpression(CodegenExpressionBuilder.decrementRef(ref)));
        return this;
    }

    public CodegenBlock increment(CodegenExpression expression) {
        checkClosed();
        statements.add(new CodegenStatementExpression(CodegenExpressionBuilder.increment(expression)));
        return this;
    }

    public CodegenBlock incrementRef(String ref) {
        checkClosed();
        statements.add(new CodegenStatementExpression(CodegenExpressionBuilder.incrementRef(ref)));
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
        return ifCondition(!not ? instanceOf(ref(name), clazz) : notInstanceOf(ref(name), clazz));
    }

    public CodegenBlock ifRefNull(String ref) {
        return ifCondition(equalsNull(ref(ref)));
    }

    public CodegenBlock ifNull(CodegenExpression expression) {
        return ifCondition(equalsNull(expression));
    }

    public CodegenBlock ifRefNotNull(String ref) {
        return ifCondition(notEqualsNull(ref(ref)));
    }

    public CodegenBlock ifCondition(CodegenExpression condition) {
        checkClosed();
        CodegenStatementIf builder = new CodegenStatementIf(this);
        statements.add(builder);
        return builder.ifBlock(condition);
    }

    public CodegenBlock synchronizedOn(CodegenExpression expression) {
        checkClosed();
        CodegenStatementSynchronized builder = new CodegenStatementSynchronized(this, expression);
        statements.add(builder);
        return builder.makeBlock();
    }

    public CodegenBlock forLoopIntSimple(String name, CodegenExpression upperLimit) {
        checkClosed();
        CodegenStatementForIntSimple forStmt = new CodegenStatementForIntSimple(this, name, upperLimit);
        CodegenBlock block = new CodegenBlock(forStmt);
        forStmt.setBlock(block);
        statements.add(forStmt);
        return block;
    }

    public CodegenBlock forLoop(Class type, String name, CodegenExpression initialization, CodegenExpression termination, CodegenExpression increment) {
        checkClosed();
        CodegenStatementFor forStmt = new CodegenStatementFor(this, type, name, initialization, termination, increment);
        CodegenBlock block = new CodegenBlock(forStmt);
        forStmt.setBlock(block);
        statements.add(forStmt);
        return block;
    }

    public CodegenBlock forEach(Class type, String name, CodegenExpression target) {
        checkClosed();
        CodegenStatementForEach forStmt = new CodegenStatementForEach(this, type, name, target);
        CodegenBlock block = new CodegenBlock(forStmt);
        forStmt.setBlock(block);
        statements.add(forStmt);
        return block;
    }

    public CodegenBlock tryCatch() {
        checkClosed();
        CodegenStatementTryCatch tryCatch = new CodegenStatementTryCatch(this);
        CodegenBlock block = new CodegenBlock(tryCatch);
        tryCatch.setTry(block);
        statements.add(tryCatch);
        return block;
    }

    public CodegenBlock declareVarWCast(Class clazz, String var, String rhsName) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVarWCast(clazz, var, rhsName));
        return this;
    }

    public CodegenBlock declareVar(Class clazz, String var, CodegenExpression initializer) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVar(clazz, null, var, initializer));
        return this;
    }

    public CodegenBlock declareVar(String typeName, String var, CodegenExpression initializer) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVar(typeName, null, var, initializer));
        return this;
    }

    public CodegenBlock declareVar(Class clazz, Class optionalTypeVariable, String var, CodegenExpression initializer) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVar(clazz, optionalTypeVariable, var, initializer));
        return this;
    }

    public CodegenBlock declareVarNoInit(Class clazz, String var) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVar(clazz, null, var, null));
        return this;
    }

    public CodegenBlock declareVarNull(Class clazz, String var) {
        checkClosed();
        statements.add(new CodegenStatementDeclareVarNull(clazz, var));
        return this;
    }

    public CodegenBlock superCtor(CodegenExpression... params) {
        checkClosed();
        statements.add(new CodegenStatementSuperCtor(params));
        return this;
    }

    public CodegenBlock assignRef(String ref, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignNamed(ref(ref), assignment));
        return this;
    }

    public CodegenBlock assignMember(String ref, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignNamed(member(ref), assignment));
        return this;
    }

    public CodegenBlock assignRef(CodegenExpression ref, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignRef(ref, assignment));
        return this;
    }

    public CodegenBlock breakLoop() {
        checkClosed();
        statements.add(CodegenStatementBreakLoop.INSTANCE);
        return this;
    }

    public CodegenBlock assignArrayElement(String ref, CodegenExpression index, CodegenExpression assignment) {
        return assignArrayElement(ref(ref), index, assignment);
    }

    public CodegenBlock assignArrayElement2Dim(String ref, CodegenExpression indexOne, CodegenExpression indexTwo, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignArrayElement2Dim(ref(ref), indexOne, indexTwo, assignment));
        return this;
    }

    public CodegenBlock assignArrayElement(CodegenExpression ref, CodegenExpression index, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignArrayElement(ref, index, assignment));
        return this;
    }

    public CodegenBlock exprDotMethod(CodegenExpression expression, String method, CodegenExpression... params) {
        expression(new CodegenExpressionExprDotMethod(expression, method, params));
        return this;
    }

    public CodegenBlock staticMethod(Class clazz, String method, CodegenExpression... params) {
        expression(new CodegenExpressionStaticMethod(clazz, method, params));
        return this;
    }

    public CodegenBlock staticMethod(String className, String method, CodegenExpression... params) {
        expression(new CodegenExpressionStaticMethod(className, method, params));
        return this;
    }

    public CodegenBlock localMethod(CodegenMethod methodNode, CodegenExpression... parameters) {
        expression(new CodegenExpressionLocalMethod(methodNode, Arrays.asList(parameters)));
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
        statements.add(new CodegenStatementIfNullReturnNull(ref(ref)));
        return this;
    }

    public CodegenBlock ifNullReturnNull(CodegenExpression ref) {
        checkClosed();
        statements.add(new CodegenStatementIfNullReturnNull(ref));
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

    public CodegenBlock blockReturnNoValue() {
        if (parentWBlock == null) {
            throw new IllegalStateException("No codeblock parent, use 'methodReturn... instead");
        }
        checkClosed();
        closed = true;
        statements.add(CodegenStatementReturnNoValue.INSTANCE);
        return parentWBlock.getParent();
    }

    public CodegenStatementTryCatch tryReturn(CodegenExpression expression) {
        if (parentWBlock == null) {
            throw new IllegalStateException("No codeblock parent, use 'methodReturn... instead");
        }
        if (!(parentWBlock instanceof CodegenStatementTryCatch)) {
            throw new IllegalStateException("Codeblock parent is not try-catch");
        }
        checkClosed();
        closed = true;
        statements.add(new CodegenStatementReturnExpression(expression));
        return (CodegenStatementTryCatch) parentWBlock;
    }

    public CodegenStatementTryCatch tryEnd() {
        if (parentWBlock == null) {
            throw new IllegalStateException("No codeblock parent, use 'methodReturn... instead");
        }
        if (!(parentWBlock instanceof CodegenStatementTryCatch)) {
            throw new IllegalStateException("Codeblock parent is not try-catch");
        }
        closed = true;
        return (CodegenStatementTryCatch) parentWBlock;
    }

    public CodegenBlock blockThrow(CodegenExpression expression) {
        if (parentWBlock == null) {
            throw new IllegalStateException("No codeblock parent, use 'methodReturn... instead");
        }
        checkClosed();
        closed = true;
        statements.add(new CodegenStatementThrow(expression));
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

    public CodegenMethod methodThrowUnsupported() {
        if (parentMethodNode == null) {
            throw new IllegalStateException("No method parent, use 'blockReturn...' instead");
        }
        checkClosed();
        closed = true;
        statements.add(new CodegenStatementThrow(newInstance(UnsupportedOperationException.class)));
        return parentMethodNode;
    }

    public CodegenMethod methodReturn(CodegenExpression expression) {
        if (parentMethodNode == null) {
            throw new IllegalStateException("No method parent, use 'blockReturn...' instead");
        }
        checkClosed();
        closed = true;
        statements.add(new CodegenStatementReturnExpression(expression));
        return parentMethodNode;
    }

    public CodegenMethod methodEnd() {
        if (parentMethodNode == null) {
            throw new IllegalStateException("No method node parent, use 'blockReturn... instead");
        }
        checkClosed();
        closed = true;
        return parentMethodNode;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        for (CodegenStatement statement : statements) {
            indent.indent(builder, level);
            statement.render(builder, imports, isInnerClass, level, indent);
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

    public CodegenBlock ifElseIf(CodegenExpression condition) {
        checkClosed();
        closed = true;
        if (parentMethodNode != null) {
            throw new IllegalStateException("If-block-end in method?");
        }
        if (!(parentWBlock instanceof CodegenStatementIf)) {
            throw new IllegalStateException("If-block-end in method?");
        }
        CodegenStatementIf ifBuilder = (CodegenStatementIf) parentWBlock;
        return ifBuilder.addElseIf(condition);
    }

    public CodegenBlock ifElse() {
        closed = true;
        if (parentMethodNode != null) {
            throw new IllegalStateException("If-block-end in method?");
        }
        if (!(parentWBlock instanceof CodegenStatementIf)) {
            throw new IllegalStateException("If-block-end in method?");
        }
        CodegenStatementIf ifBuilder = (CodegenStatementIf) parentWBlock;
        return ifBuilder.addElse();
    }

    public void ifReturn(CodegenExpression result) {
        checkClosed();
        closed = true;
        if (parentMethodNode != null) {
            throw new IllegalStateException("If-block-end in method?");
        }
        if (!(parentWBlock instanceof CodegenStatementIf)) {
            throw new IllegalStateException("If-block-end in method?");
        }
        statements.add(new CodegenStatementReturnExpression(result));
    }

    public CodegenBlock blockContinue() {
        checkClosed();
        closed = true;
        if (parentMethodNode != null) {
            throw new IllegalStateException("If-block-end in method?");
        }
        statements.add(CodegenStatementContinue.INSTANCE);
        return parentWBlock.getParent();
    }

    public CodegenBlock whileLoop(CodegenExpression expression) {
        return whileOrDoLoop(expression, true);
    }

    public void returnMethodOrBlock(CodegenExpression expression) {
        if (parentMethodNode != null) {
            methodReturn(expression);
        } else {
            blockReturn(expression);
        }
    }

    public CodegenBlock[] switchBlockOfLength(CodegenExpression switchExpression, int length, boolean blocksReturnValues) {
        CodegenExpression[] expressions = new CodegenExpression[length];
        for (int i = 0; i < length; i++) {
            expressions[i] = constant(i);
        }
        return switchBlockExpressions(switchExpression, expressions, blocksReturnValues, true).getBlocks();
    }

    public CodegenBlock[] switchBlockOptions(CodegenExpression switchExpression, int[] options, boolean blocksReturnValues) {
        CodegenExpression[] expressions = new CodegenExpression[options.length];
        for (int i = 0; i < expressions.length; i++) {
            expressions[i] = constant(options[i]);
        }
        return switchBlockExpressions(switchExpression, expressions, blocksReturnValues, true).getBlocks();
    }

    public CodegenStatementSwitch switchBlockExpressions(CodegenExpression switchExpression, CodegenExpression[] expressions, boolean blocksReturnValues, boolean withDefaultUnsupported) {
        checkClosed();
        CodegenStatementSwitch switchStmt = new CodegenStatementSwitch(this, switchExpression, expressions, blocksReturnValues, withDefaultUnsupported);
        statements.add(switchStmt);
        return switchStmt;
    }

    public CodegenBlock apply(Consumer<CodegenBlock> consumer) {
        checkClosed();
        consumer.accept(this);
        return this;
    }

    public <T extends CodegenSymbolProvider> CodegenBlock applyTri(TriConsumer<CodegenMethod, CodegenBlock, T> consumer, CodegenMethod methodNode, T symbols) {
        checkClosed();
        consumer.accept(methodNode, this, symbols);
        return this;
    }

    public CodegenBlock assignCompound(CodegenExpression expression, String operator, CodegenExpression assignment) {
        checkClosed();
        statements.add(new CodegenStatementAssignCompound(expression, operator, assignment));
        return this;
    }

    public CodegenBlock assignCompound(String ref, String operator, CodegenExpression assignment) {
        return assignCompound(ref(ref), operator, assignment);
    }

    public boolean isClosed() {
        return closed;
    }

    private CodegenBlock whileOrDoLoop(CodegenExpression expression, boolean isWhile) {
        checkClosed();
        CodegenStatementWhileOrDo whileStmt = new CodegenStatementWhileOrDo(this, expression, isWhile);
        CodegenBlock block = new CodegenBlock(whileStmt);
        whileStmt.setBlock(block);
        statements.add(whileStmt);
        return block;
    }

    public boolean hasInstanceAccess(Function<CodegenMethod, Boolean> permittedMethods) {
        InstanceAccessConsumer consumer = new InstanceAccessConsumer(permittedMethods);
        for (CodegenStatement statement : statements) {
            statement.traverseExpressions(consumer);
            if (consumer.hasInstanceAccess) {
                return true;
            }
        }
        return false;
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        for (CodegenStatement statement : statements) {
            statement.traverseExpressions(consumer);
        }
    }

    private final static class InstanceAccessConsumer implements Consumer<CodegenExpression> {
        private final Function<CodegenMethod, Boolean> permittedMethod;
        private boolean hasInstanceAccess = false;

        public InstanceAccessConsumer(Function<CodegenMethod, Boolean> permittedMethod) {
            this.permittedMethod = permittedMethod;
        }

        public void accept(CodegenExpression codegenExpression) {
            if (codegenExpression instanceof CodegenExpressionMember) {
                hasInstanceAccess = true;
                return;
            }
            if (codegenExpression instanceof CodegenExpressionLocalMethod) {
                CodegenExpressionLocalMethod localMethod = (CodegenExpressionLocalMethod) codegenExpression;
                if (!permittedMethod.apply(localMethod.getMethodNode())) {
                    hasInstanceAccess = true;
                    return;
                }
            }

            codegenExpression.traverseExpressions(this);
        }
    }
}
