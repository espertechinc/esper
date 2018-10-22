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
package com.espertech.esper.common.client.hook.forgeinject;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;

/**
 * Provides the compiler with code that allocates and initializes an instance of some class
 * by using "new" and by using setters.
 */
public class InjectionStrategyClassNewInstance implements InjectionStrategy {
    private final Class clazz;
    private final String fullyQualifiedClassName;
    private final Map<String, Object> constants = new HashMap<>();
    private final Map<String, ExprNode> expressions = new HashMap<>();
    private Consumer<SAIFFInitializeBuilder> builderConsumer;

    /**
     * The class to be instantiated.
     *
     * @param clazz class
     */
    public InjectionStrategyClassNewInstance(Class clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Invalid null value for class");
        }
        this.clazz = clazz;
        this.fullyQualifiedClassName = null;
    }

    /**
     * The class name of the class to be instantiated.
     *
     * @param fullyQualifiedClassName class name
     */
    public InjectionStrategyClassNewInstance(String fullyQualifiedClassName) {
        if (fullyQualifiedClassName == null) {
            throw new IllegalArgumentException("Invalid null value for class name");
        }
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.clazz = null;
    }

    /**
     * Returns the class, or null if providing a class name instead
     *
     * @return class
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns the class name, or null if providing a class instead
     *
     * @return class name
     */
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    /**
     * Add a constant to be provided by invoking the setter method of the class, at deployment time
     *
     * @param name  property name
     * @param value constant value
     * @return itself
     */
    public InjectionStrategyClassNewInstance addConstant(String name, Object value) {
        constants.put(name, value);
        return this;
    }

    /**
     * Add an expression to be provided by invoking the setter method of the class, at deployment time,
     * the setter should accept an {@link com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator} instance.
     *
     * @param name  property name
     * @param value expression
     * @return itself
     */
    public InjectionStrategyClassNewInstance addExpression(String name, ExprNode value) {
        expressions.put(name, value);
        return this;
    }

    /**
     * Returns the builder consumer, a consumer that the strategy invokes when it is ready to build the code
     *
     * @return builder consumer
     */
    public Consumer<SAIFFInitializeBuilder> getBuilderConsumer() {
        return builderConsumer;
    }

    /**
     * Sets the builder consumer, a consumer that the strategy invokes when it is ready to build the code
     *
     * @param builderConsumer builder consumer
     */
    public void setBuilderConsumer(Consumer<SAIFFInitializeBuilder> builderConsumer) {
        this.builderConsumer = builderConsumer;
    }

    public CodegenExpression getInitializationExpression(CodegenClassScope classScope) {
        SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
        SAIFFInitializeBuilder builder;
        CodegenMethod init;
        if (clazz != null) {
            init = classScope.getPackageScope().getInitMethod().makeChildWithScope(clazz, this.getClass(), symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
            builder = new SAIFFInitializeBuilder(clazz, this.getClass(), "instance", init, symbols, classScope);
        } else {
            init = classScope.getPackageScope().getInitMethod().makeChildWithScope(fullyQualifiedClassName, this.getClass(), symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
            builder = new SAIFFInitializeBuilder(fullyQualifiedClassName, this.getClass(), "instance", init, symbols, classScope);
        }

        if (builderConsumer != null) {
            builderConsumer.accept(builder);
        }
        for (Map.Entry<String, Object> constantEntry : constants.entrySet()) {
            builder.constant(constantEntry.getKey(), constantEntry.getValue());
        }
        for (Map.Entry<String, ExprNode> exprEntry : expressions.entrySet()) {
            builder.exprnode(exprEntry.getKey(), exprEntry.getValue());
        }
        init.getBlock().methodReturn(builder.build());
        return localMethod(init, EPStatementInitServices.REF);
    }
}
