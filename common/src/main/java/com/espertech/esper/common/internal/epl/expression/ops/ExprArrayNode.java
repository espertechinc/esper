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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.*;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an array in a filter expressiun tree.
 */
public class ExprArrayNode extends ExprNodeBase {

    private transient ExprArrayNodeForge forge;
    private EPTypeClass optionalRequiredType;

    /**
     * Ctor.
     */
    public ExprArrayNode() {
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public boolean isConstantResult() {
        checkValidated(forge);
        return forge.getConstantResult() != null;
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public EPTypeClass getComponentTypeCollection() throws ExprValidationException {
        checkValidated(forge);
        return forge.getArrayReturnType();
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        int length = this.getChildNodes().length;

        // Can be an empty array with no content
        if (this.getChildNodes().length == 0) {
            if (optionalRequiredType == null) {
                forge = new ExprArrayNodeForge(this, EPTypePremade.OBJECT.getEPType(), CollectionUtil.OBJECTARRAY_EMPTY);
            } else {
                forge = new ExprArrayNodeForge(this, optionalRequiredType, Array.newInstance(optionalRequiredType.getType(), 0));
            }
            return null;
        }

        List<EPType> comparedTypes = new LinkedList<>();
        for (int i = 0; i < length; i++) {
            EPType evalType = this.getChildNodes()[i].getForge().getEvaluationType();
            comparedTypes.add(evalType);
        }

        // Determine common denominator type
        EPTypeClass arrayReturnType = null;
        boolean mustCoerce = false;
        SimpleNumberCoercer coercer = null;
        try {
            if (optionalRequiredType == null) {
                EPType coercionType = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new EPType[comparedTypes.size()]));
                arrayReturnType = coercionType == null || coercionType == EPTypeNull.INSTANCE ? null : (EPTypeClass) coercionType;

                // Determine if we need to coerce numbers when one type doesn't match any other type
                if (JavaClassHelper.isNumeric(arrayReturnType)) {
                    for (EPType comparedType : comparedTypes) {
                        if (!(comparedType.equals(arrayReturnType))) {
                            mustCoerce = true;
                            break;
                        }
                    }
                    if (mustCoerce) {
                        coercer = SimpleNumberCoercerFactory.getCoercer(null, arrayReturnType);
                    }
                }
            } else {
                arrayReturnType = optionalRequiredType;
                EPTypeClass arrayBoxedType = JavaClassHelper.getBoxedType(optionalRequiredType);
                for (EPType comparedType : comparedTypes) {
                    if (!JavaClassHelper.isAssignmentCompatible(JavaClassHelper.getBoxedType(comparedType), arrayBoxedType.getType())) {
                        throw new ExprValidationException("Array element type mismatch: Expecting type " + ClassHelperPrint.getClassNameFullyQualPretty(arrayReturnType) + " but received type " + ClassHelperPrint.getClassNameFullyQualPretty(comparedType));
                    }
                }
            }

        } catch (CoercionException ex) {
            // expected, such as mixing String and int values, or Java classes (not boxed) and primitives
            // use Object[] in such cases
        }
        if (arrayReturnType == null) {
            arrayReturnType = EPTypePremade.OBJECT.getEPType();
        }

        // Determine if we are dealing with constants only
        Object[] results = new Object[length];
        int index = 0;
        for (ExprNode child : this.getChildNodes()) {
            if (!child.getForge().getForgeConstantType().isCompileTimeConstant()) {
                results = null;  // not using a constant result
                break;
            }
            results[index] = getChildNodes()[index].getForge().getExprEvaluator().evaluate(null, false, null);
            index++;
        }

        // Copy constants into array and coerce, if required
        Object constantResult = null;
        if (results != null) {
            constantResult = Array.newInstance(arrayReturnType.getType(), length);
            for (int i = 0; i < length; i++) {
                if (mustCoerce) {
                    Number boxed = (Number) results[i];
                    if (boxed != null) {
                        Object coercedResult = coercer.coerceBoxed(boxed);
                        Array.set(constantResult, i, coercedResult);
                    }
                } else {
                    if (arrayReturnType.getType().isPrimitive() && results[i] == null) {
                        throw new ExprValidationException("Array element type mismatch: Expecting type " + ClassHelperPrint.getClassNameFullyQualPretty(arrayReturnType) + " but received null");
                    }
                    try {
                        Array.set(constantResult, i, results[i]);
                    } catch (IllegalArgumentException ex) {
                        throw new ExprValidationException("Array element type mismatch: Expecting type " + ClassHelperPrint.getClassNameFullyQualPretty(arrayReturnType) + " but received type " + ClassHelperPrint.getClassNameFullyQualPretty(results[i].getClass()));
                    }
                }
            }
        }
        forge = new ExprArrayNodeForge(this, arrayReturnType, mustCoerce, coercer, constantResult);
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        String delimiter = "";
        writer.append("{");
        for (ExprNode expr : this.getChildNodes()) {
            writer.append(delimiter);
            expr.toEPL(writer, ExprPrecedenceEnum.MINIMUM, flags);
            delimiter = ",";
        }
        writer.append('}');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprArrayNode)) {
            return false;
        }
        return true;
    }

    public void setOptionalRequiredType(EPTypeClass optionalRequiredType) {
        this.optionalRequiredType = optionalRequiredType;
    }

    public EPTypeClass getOptionalRequiredType() {
        return optionalRequiredType;
    }
}
