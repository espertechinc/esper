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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.type.ClassIdentifierWArray;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.field;

/**
 * Represents a substitution value to be substituted in an expression tree, not valid for any purpose of use
 * as an expression, however can take a place in an expression tree.
 */
public class ExprSubstitutionNode extends ExprNodeBase implements ExprForge, ExprNodeDeployTimeConst {
    private String optionalName;
    private ClassIdentifierWArray optionalType;
    private Class type = Object.class;
    private CodegenExpressionField field;

    public ExprSubstitutionNode(String optionalName, ClassIdentifierWArray optionalType) {
        this.optionalName = optionalName;
        this.optionalType = optionalType;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (optionalType != null) {
            Class clazz = null;
            try {
                clazz = JavaClassHelper.getClassForName(optionalType.getClassIdentifier(), validationContext.getClasspathImportService().getClassForNameProvider());
            } catch (ClassNotFoundException e) {
            }

            if (clazz == null) {
                clazz = JavaClassHelper.getClassForSimpleName(optionalType.getClassIdentifier(), validationContext.getClasspathImportService().getClassForNameProvider());
            }

            if (clazz != null) {
                type = clazz;
            } else {
                try {
                    type = validationContext.getClasspathImportService().resolveClass(optionalType.getClassIdentifier(), false);
                } catch (ClasspathImportException e) {
                    throw new ExprValidationException("Failed to resolve type '" + optionalType.getClassIdentifier() + "': " + e.getMessage(), e);
                }
            }
            if (type != null && optionalType.isArrayOfPrimitive() && !type.isPrimitive()) {
                throw new ExprValidationException("Invalid use of the '" + ClassIdentifierWArray.PRIMITIVE_KEYWORD + "' keyword for non-primitive type '" + type.getName() + "'");
            }
            if (!optionalType.isArrayOfPrimitive()) {
                type = JavaClassHelper.getBoxedType(type);
            }
            type = JavaClassHelper.getArrayType(type, optionalType.getArrayDimensions());
        }
        return null;
    }

    /**
     * Returns the substitution parameter name (or null if by-index).
     *
     * @return name
     */
    public String getOptionalName() {
        return optionalName;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public ExprForge getForge() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("?");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprSubstitutionNode)) {
            return false;
        }

        return true;
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return asField(codegenClassScope);
    }

    public CodegenExpression codegenGetDeployTimeConstValue(CodegenClassScope classScope) {
        return asField(classScope);
    }

    public Class getEvaluationType() {
        return type;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.DEPLOYCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return new ExprNodeRenderable() {
            public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
                writer.append("?");
            }
        };
    }

    public ClassIdentifierWArray getOptionalType() {
        return optionalType;
    }

    public Class getResolvedType() {
        return type;
    }

    private CodegenExpressionField asField(CodegenClassScope classScope) {
        if (field == null) {
            field = field(classScope.addSubstitutionParameter(optionalName, type));
        }
        return field;
    }
}
