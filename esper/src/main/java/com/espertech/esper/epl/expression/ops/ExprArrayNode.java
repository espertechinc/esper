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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.*;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an array in a filter expressiun tree.
 */
public class ExprArrayNode extends ExprNodeBase {
    private static final long serialVersionUID = 5533223915923867651L;

    private transient ExprArrayNodeForge forge;

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

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        checkValidated(forge);
        return forge.getExprEvaluatorEnumeration();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        checkValidated(forge);
        return forge.getArrayReturnType();
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        int length = this.getChildNodes().length;

        // Can be an empty array with no content
        if (this.getChildNodes().length == 0) {
            forge = new ExprArrayNodeForge(this, Object.class, CollectionUtil.OBJECTARRAY_EMPTY);
            return null;
        }

        List<Class> comparedTypes = new LinkedList<Class>();
        for (int i = 0; i < length; i++) {
            comparedTypes.add(this.getChildNodes()[i].getForge().getEvaluationType());
        }

        // Determine common denominator type
        Class arrayReturnType = null;
        boolean mustCoerce = false;
        SimpleNumberCoercer coercer = null;
        try {
            arrayReturnType = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new Class[comparedTypes.size()]));

            // Determine if we need to coerce numbers when one type doesn't match any other type
            if (JavaClassHelper.isNumeric(arrayReturnType)) {
                mustCoerce = false;
                for (Class comparedType : comparedTypes) {
                    if (comparedType != arrayReturnType) {
                        mustCoerce = true;
                    }
                }
                if (mustCoerce) {
                    coercer = SimpleNumberCoercerFactory.getCoercer(null, arrayReturnType);
                }
            }
        } catch (CoercionException ex) {
            // expected, such as mixing String and int values, or Java classes (not boxed) and primitives
            // use Object[] in such cases
        }
        if (arrayReturnType == null) {
            arrayReturnType = Object.class;
        }

        // Determine if we are dealing with constants only
        Object[] results = new Object[length];
        int index = 0;
        for (ExprNode child : this.getChildNodes()) {
            if (!child.isConstantResult()) {
                results = null;  // not using a constant result
                break;
            }
            results[index] = getChildNodes()[index].getForge().getExprEvaluator().evaluate(null, false, validationContext.getExprEvaluatorContext());
            index++;
        }

        // Copy constants into array and coerce, if required
        Object constantResult = null;
        if (results != null) {
            constantResult = Array.newInstance(arrayReturnType, length);
            for (int i = 0; i < length; i++) {
                if (mustCoerce) {
                    Number boxed = (Number) results[i];
                    if (boxed != null) {
                        Object coercedResult = coercer.coerceBoxed(boxed);
                        Array.set(constantResult, i, coercedResult);
                    }
                } else {
                    Array.set(constantResult, i, results[i]);
                }
            }
        }
        forge = new ExprArrayNodeForge(this, arrayReturnType, mustCoerce, coercer, constantResult);
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        writer.append("{");
        for (ExprNode expr : this.getChildNodes()) {
            writer.append(delimiter);
            expr.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
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
}
