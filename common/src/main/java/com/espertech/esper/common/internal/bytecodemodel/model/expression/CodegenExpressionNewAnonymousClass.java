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
package com.espertech.esper.common.internal.bytecodemodel.model.expression;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenIndent;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenMethodWGraph;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.CodegenStatementWBlockBase;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.renderExpressions;

public class CodegenExpressionNewAnonymousClass extends CodegenStatementWBlockBase implements CodegenExpression {

    private final Class interfaceOrSuperClass;
    private final List<CodegenExpression> ctorParams;
    private final List<Pair<String, CodegenMethod>> methods = new ArrayList<>(2);

    public CodegenExpressionNewAnonymousClass(CodegenBlock parentBlock, Class interfaceOrSuperClass, List<CodegenExpression> ctorParams) {
        super(parentBlock);
        this.interfaceOrSuperClass = interfaceOrSuperClass;
        this.ctorParams = ctorParams;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("new ");
        appendClassName(builder, interfaceOrSuperClass, null, imports);
        builder.append("(");
        renderExpressions(builder, ctorParams.toArray(new CodegenExpression[ctorParams.size()]), imports, isInnerClass);
        builder.append(") {\n");

        CodegenClassMethods methods = new CodegenClassMethods();
        for (Pair<String, CodegenMethod> pair : this.methods) {
            CodegenStackGenerator.recursiveBuildStack(pair.getSecond(), pair.getFirst(), methods);
        }

        // public methods
        String delimiter = "";
        for (CodegenMethodWGraph publicMethod : methods.getPublicMethods()) {
            builder.append(delimiter);
            publicMethod.render(builder, imports, true, isInnerClass, indent, level + 1);
            delimiter = "\n";
        }

        // private methods
        for (CodegenMethodWGraph method : methods.getPrivateMethods()) {
            builder.append(delimiter);
            method.render(builder, imports, false, isInnerClass, indent, level + 1);
            delimiter = "\n";
        }

        indent.indent(builder, level);
        builder.append("}");
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        render(builder, imports, isInnerClass, 4, new CodegenIndent(true));
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(interfaceOrSuperClass);
        for (CodegenExpression expr : ctorParams) {
            expr.mergeClasses(classes);
        }

        for (Pair<String, CodegenMethod> additional : methods) {
            additional.getSecond().mergeClasses(classes);
        }
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        for (CodegenExpression expr : ctorParams) {
            consumer.accept(expr);
        }

        for (Pair<String, CodegenMethod> additional : methods) {
            additional.getSecond().traverseExpressions(consumer);
        }
    }

    public void addMethod(String name, CodegenMethod methodNode) {
        methods.add(new Pair<>(name, methodNode));
    }
}
