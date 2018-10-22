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
package com.espertech.esper.common.internal.epl.resultset.order;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage1.spec.RowLimitSpec;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.core.VariableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.variable.core.VariableUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A factory for row-limit processor instances.
 */
public class RowLimitProcessorFactoryForge {

    private final VariableMetaData numRowsVariableMetaData;
    private final VariableMetaData offsetVariableMetaData;
    private int currentRowLimit;
    private int currentOffset;

    /**
     * Ctor.
     *
     * @param rowLimitSpec                specification for row limit, or null if no row limit is defined
     * @param variableCompileTimeResolver for retrieving variable state for use with row limiting
     * @param optionalContextName         context name
     * @throws ExprValidationException exception
     */
    public RowLimitProcessorFactoryForge(RowLimitSpec rowLimitSpec, VariableCompileTimeResolver variableCompileTimeResolver, String optionalContextName)
            throws ExprValidationException {
        if (rowLimitSpec.getNumRowsVariable() != null) {
            numRowsVariableMetaData = variableCompileTimeResolver.resolve(rowLimitSpec.getNumRowsVariable());
            if (numRowsVariableMetaData == null) {
                throw new ExprValidationException("Limit clause variable by name '" + rowLimitSpec.getNumRowsVariable() + "' has not been declared");
            }
            String message = VariableUtil.checkVariableContextName(optionalContextName, numRowsVariableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
            if (!JavaClassHelper.isNumeric(numRowsVariableMetaData.getType())) {
                throw new ExprValidationException("Limit clause requires a variable of numeric type");
            }
        } else {
            numRowsVariableMetaData = null;
            currentRowLimit = rowLimitSpec.getNumRows();

            if (currentRowLimit < 0) {
                currentRowLimit = Integer.MAX_VALUE;
            }
        }

        if (rowLimitSpec.getOptionalOffsetVariable() != null) {
            offsetVariableMetaData = variableCompileTimeResolver.resolve(rowLimitSpec.getOptionalOffsetVariable());
            if (offsetVariableMetaData == null) {
                throw new ExprValidationException("Limit clause variable by name '" + rowLimitSpec.getOptionalOffsetVariable() + "' has not been declared");
            }
            String message = VariableUtil.checkVariableContextName(optionalContextName, offsetVariableMetaData);
            if (message != null) {
                throw new ExprValidationException(message);
            }
            if (!JavaClassHelper.isNumeric(offsetVariableMetaData.getType())) {
                throw new ExprValidationException("Limit clause requires a variable of numeric type");
            }
        } else {
            offsetVariableMetaData = null;
            if (rowLimitSpec.getOptionalOffset() != null) {
                currentOffset = rowLimitSpec.getOptionalOffset();

                if (currentOffset <= 0) {
                    throw new ExprValidationException("Limit clause requires a positive offset");
                }
            } else {
                currentOffset = 0;
            }
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenExpression numRowsVariable = constantNull();
        if (numRowsVariableMetaData != null) {
            numRowsVariable = VariableDeployTimeResolver.makeVariableField(numRowsVariableMetaData, classScope, this.getClass());
        }

        CodegenExpression offsetVariable = constantNull();
        if (offsetVariableMetaData != null) {
            offsetVariable = VariableDeployTimeResolver.makeVariableField(offsetVariableMetaData, classScope, this.getClass());
        }

        CodegenMethod method = parent.makeChild(RowLimitProcessorFactory.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(RowLimitProcessorFactory.class, "factory", newInstance(RowLimitProcessorFactory.class))
                .exprDotMethod(ref("factory"), "setNumRowsVariable", numRowsVariable)
                .exprDotMethod(ref("factory"), "setOffsetVariable", offsetVariable)
                .exprDotMethod(ref("factory"), "setCurrentRowLimit", constant(currentRowLimit))
                .exprDotMethod(ref("factory"), "setCurrentOffset", constant(currentOffset))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
