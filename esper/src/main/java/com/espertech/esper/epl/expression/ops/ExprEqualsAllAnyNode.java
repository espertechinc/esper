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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents an equals-for-group (= ANY/ALL/SOME (expression list)) comparator in a expression tree.
 */
public class ExprEqualsAllAnyNode extends ExprNodeBase implements ExprEvaluator {
    private final boolean isNot;
    private final boolean isAll;

    private boolean mustCoerce;
    private transient SimpleNumberCoercer coercer;
    private boolean hasCollectionOrArray;

    private transient ExprEvaluator[] evaluators;

    private static final long serialVersionUID = -2410457251623137179L;

    /**
     * Ctor.
     *
     * @param isNotEquals - true if this is a (!=) not equals rather then equals, false if its a '=' equals
     * @param isAll       - true if all, false for any
     */
    public ExprEqualsAllAnyNode(boolean isNotEquals, boolean isAll) {
        this.isNot = isNotEquals;
        this.isAll = isAll;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    /**
     * Returns true if this is a NOT EQUALS node, false if this is a EQUALS node.
     *
     * @return true for !=, false for =
     */
    public boolean isNot() {
        return isNot;
    }

    /**
     * True if all.
     *
     * @return all-flag
     */
    public boolean isAll() {
        return isAll;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have 2 child nodes
        if (this.getChildNodes().length < 1) {
            throw new IllegalStateException("Equals group node does not have 1 or more parameters");
        }

        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        // Must be the same boxed type returned by expressions under this
        Class typeOne = JavaClassHelper.getBoxedType(evaluators[0].getType());

        // collections, array or map not supported
        if ((typeOne.isArray()) || (JavaClassHelper.isImplementsInterface(typeOne, Collection.class)) || (JavaClassHelper.isImplementsInterface(typeOne, Map.class))) {
            throw new ExprValidationException("Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
        }

        List<Class> comparedTypes = new ArrayList<Class>();
        comparedTypes.add(typeOne);
        hasCollectionOrArray = false;
        for (int i = 0; i < this.getChildNodes().length - 1; i++) {
            Class propType = evaluators[i + 1].getType();
            if (propType.isArray()) {
                hasCollectionOrArray = true;
                if (propType.getComponentType() != Object.class) {
                    comparedTypes.add(propType.getComponentType());
                }
            } else if (JavaClassHelper.isImplementsInterface(propType, Collection.class)) {
                hasCollectionOrArray = true;
            } else if (JavaClassHelper.isImplementsInterface(propType, Map.class)) {
                hasCollectionOrArray = true;
            } else {
                comparedTypes.add(propType);
            }
        }

        // Determine common denominator type
        Class coercionType;
        try {
            coercionType = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new Class[comparedTypes.size()]));
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
        }

        // Check if we need to coerce
        mustCoerce = false;
        if (JavaClassHelper.isNumeric(coercionType)) {
            for (Class compareType : comparedTypes) {
                if (coercionType != JavaClassHelper.getBoxedType(compareType)) {
                    mustCoerce = true;
                }
            }
            if (mustCoerce) {
                coercer = SimpleNumberCoercerFactory.getCoercer(null, JavaClassHelper.getBoxedType(coercionType));
            }
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return Boolean.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprEqualsAnyOrAll(this);
        }
        Object result = evaluateInternal(eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprEqualsAnyOrAll((Boolean) result);
        }
        return result;
    }

    private Object evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {

        Object leftResult = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

        if (hasCollectionOrArray) {
            if (isAll) {
                return compareAllColl(leftResult, eventsPerStream, isNewData, exprEvaluatorContext);
            } else {
                return compareAnyColl(leftResult, eventsPerStream, isNewData, exprEvaluatorContext);
            }
        } else {
            // coerce early if testing without collections
            if (mustCoerce && (leftResult != null)) {
                leftResult = coercer.coerceBoxed((Number) leftResult);
            }

            if (isAll) {
                return compareAll(leftResult, eventsPerStream, isNewData, exprEvaluatorContext);
            } else {
                return compareAny(leftResult, eventsPerStream, isNewData, exprEvaluatorContext);
            }
        }
    }

    private Object compareAll(Object leftResult, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (isNot) {
            int len = this.getChildNodes().length - 1;
            if ((len > 0) && (leftResult == null)) {
                return null;
            }
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (rightResult != null) {
                    hasNonNullRow = true;
                    if (!mustCoerce) {
                        if (leftResult.equals(rightResult)) {
                            return false;
                        }
                    } else {
                        Number right = coercer.coerceBoxed((Number) rightResult);
                        if (leftResult.equals(right)) {
                            return false;
                        }
                    }
                } else {
                    hasNullRow = true;
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return true;
        } else {
            int len = this.getChildNodes().length - 1;
            if ((len > 0) && (leftResult == null)) {
                return null;
            }
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (rightResult != null) {
                    hasNonNullRow = true;
                    if (!mustCoerce) {
                        if (!leftResult.equals(rightResult)) {
                            return false;
                        }
                    } else {
                        Number right = coercer.coerceBoxed((Number) rightResult);
                        if (!leftResult.equals(right)) {
                            return false;
                        }
                    }
                } else {
                    hasNullRow = true;
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return true;
        }
    }

    private Object compareAllColl(Object leftResult, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (isNot) {
            int len = this.getChildNodes().length - 1;
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (rightResult == null) {
                    hasNullRow = true;
                    continue;
                }

                if (rightResult instanceof Collection) {
                    if (leftResult == null) {
                        return null;
                    }
                    Collection coll = (Collection) rightResult;
                    if (coll.contains(leftResult)) {
                        return false;
                    }
                    hasNonNullRow = true;
                } else if (rightResult instanceof Map) {
                    if (leftResult == null) {
                        return null;
                    }
                    Map coll = (Map) rightResult;
                    if (coll.containsKey(leftResult)) {
                        return false;
                    }
                    hasNonNullRow = true;
                } else if (rightResult.getClass().isArray()) {
                    int arrayLength = Array.getLength(rightResult);
                    for (int index = 0; index < arrayLength; index++) {
                        Object item = Array.get(rightResult, index);
                        if (item == null) {
                            hasNullRow = true;
                            continue;
                        }
                        if (leftResult == null) {
                            return null;
                        }
                        hasNonNullRow = true;
                        if (!mustCoerce) {
                            if (leftResult.equals(item)) {
                                return false;
                            }
                        } else {
                            if (!(item instanceof Number)) {
                                continue;
                            }
                            Number left = coercer.coerceBoxed((Number) leftResult);
                            Number right = coercer.coerceBoxed((Number) item);
                            if (left.equals(right)) {
                                return false;
                            }
                        }
                    }
                } else {
                    if (leftResult == null) {
                        return null;
                    }
                    if (!mustCoerce) {
                        if (leftResult.equals(rightResult)) {
                            return false;
                        }
                    } else {
                        Number left = coercer.coerceBoxed((Number) leftResult);
                        Number right = coercer.coerceBoxed((Number) rightResult);
                        if (left.equals(right)) {
                            return false;
                        }
                    }
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return true;
        } else {
            int len = this.getChildNodes().length - 1;
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (rightResult == null) {
                    hasNullRow = true;
                    continue;
                }

                if (rightResult instanceof Collection) {
                    hasNonNullRow = true;
                    if (leftResult == null) {
                        return null;
                    }
                    Collection coll = (Collection) rightResult;
                    if (!coll.contains(leftResult)) {
                        return false;
                    }
                } else if (rightResult instanceof Map) {
                    if (leftResult == null) {
                        return null;
                    }
                    Map coll = (Map) rightResult;
                    if (!coll.containsKey(leftResult)) {
                        return false;
                    }
                    hasNonNullRow = true;
                } else if (rightResult.getClass().isArray()) {
                    int arrayLength = Array.getLength(rightResult);
                    for (int index = 0; index < arrayLength; index++) {
                        Object item = Array.get(rightResult, index);
                        if (item == null) {
                            hasNullRow = true;
                            continue;
                        }
                        if (leftResult == null) {
                            return null;
                        }
                        hasNonNullRow = true;
                        if (!mustCoerce) {
                            if (!leftResult.equals(item)) {
                                return false;
                            }
                        } else {
                            if (!(item instanceof Number)) {
                                continue;
                            }
                            Number left = coercer.coerceBoxed((Number) leftResult);
                            Number right = coercer.coerceBoxed((Number) item);
                            if (!left.equals(right)) {
                                return false;
                            }
                        }
                    }
                } else {
                    if (leftResult == null) {
                        return null;
                    }
                    if (!mustCoerce) {
                        if (!leftResult.equals(rightResult)) {
                            return false;
                        }
                    } else {
                        Number left = coercer.coerceBoxed((Number) leftResult);
                        Number right = coercer.coerceBoxed((Number) rightResult);
                        if (!left.equals(right)) {
                            return false;
                        }
                    }
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return true;
        }
    }

    private Object compareAny(Object leftResult, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // Return true on the first not-equal.
        if (isNot) {
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            int len = this.getChildNodes().length - 1;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (leftResult == null) {
                    return null;
                }
                if (rightResult == null) {
                    hasNullRow = true;
                    continue;
                }

                hasNonNullRow = true;
                if (!mustCoerce) {
                    if (!leftResult.equals(rightResult)) {
                        return true;
                    }
                } else {
                    Number right = coercer.coerceBoxed((Number) rightResult);
                    if (!leftResult.equals(right)) {
                        return true;
                    }
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return false;
        } else {
            // Return true on the first equal.
            int len = this.getChildNodes().length - 1;
            if ((len > 0) && (leftResult == null)) {
                return null;
            }
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (rightResult == null) {
                    hasNullRow = true;
                    continue;
                }

                hasNonNullRow = true;
                if (!mustCoerce) {
                    if (leftResult.equals(rightResult)) {
                        return true;
                    }
                } else {
                    Number right = coercer.coerceBoxed((Number) rightResult);
                    if (leftResult.equals(right)) {
                        return true;
                    }
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return false;
        }
    }

    private Object compareAnyColl(Object leftResult, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // Return true on the first not-equal.
        if (isNot) {
            int len = this.getChildNodes().length - 1;
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (rightResult == null) {
                    hasNullRow = true;
                    continue;
                }

                if (rightResult instanceof Collection) {
                    if (leftResult == null) {
                        return null;
                    }
                    Collection coll = (Collection) rightResult;
                    if (!coll.contains(leftResult)) {
                        return true;
                    }
                    hasNonNullRow = true;
                } else if (rightResult instanceof Map) {
                    if (leftResult == null) {
                        return null;
                    }
                    Map coll = (Map) rightResult;
                    if (!coll.containsKey(leftResult)) {
                        return true;
                    }
                    hasNonNullRow = true;
                } else if (rightResult.getClass().isArray()) {
                    int arrayLength = Array.getLength(rightResult);
                    if ((arrayLength > 0) && (leftResult == null)) {
                        return null;
                    }

                    for (int index = 0; index < arrayLength; index++) {
                        Object item = Array.get(rightResult, index);
                        if (item == null) {
                            hasNullRow = true;
                            continue;
                        }
                        hasNonNullRow = true;
                        if (!mustCoerce) {
                            if (!leftResult.equals(item)) {
                                return true;
                            }
                        } else {
                            if (!(item instanceof Number)) {
                                continue;
                            }
                            Number left = coercer.coerceBoxed((Number) leftResult);
                            Number right = coercer.coerceBoxed((Number) item);
                            if (!left.equals(right)) {
                                return true;
                            }
                        }
                    }
                } else {
                    if (leftResult == null) {
                        return null;
                    }
                    hasNonNullRow = true;
                    if (!mustCoerce) {
                        if (!leftResult.equals(rightResult)) {
                            return true;
                        }
                    } else {
                        Number left = coercer.coerceBoxed((Number) leftResult);
                        Number right = coercer.coerceBoxed((Number) rightResult);
                        if (!left.equals(right)) {
                            return true;
                        }
                    }
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return false;
        } else {
            // Return true on the first equal.
            int len = this.getChildNodes().length - 1;
            boolean hasNonNullRow = false;
            boolean hasNullRow = false;
            for (int i = 1; i <= len; i++) {
                Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

                if (rightResult == null) {
                    hasNonNullRow = true;
                    continue;
                }
                if (rightResult instanceof Collection) {
                    if (leftResult == null) {
                        return null;
                    }
                    hasNonNullRow = true;
                    Collection coll = (Collection) rightResult;
                    if (coll.contains(leftResult)) {
                        return true;
                    }
                } else if (rightResult instanceof Map) {
                    if (leftResult == null) {
                        return null;
                    }
                    Map coll = (Map) rightResult;
                    if (coll.containsKey(leftResult)) {
                        return true;
                    }
                    hasNonNullRow = true;
                } else if (rightResult.getClass().isArray()) {
                    int arrayLength = Array.getLength(rightResult);
                    if ((arrayLength > 0) && (leftResult == null)) {
                        return null;
                    }
                    for (int index = 0; index < arrayLength; index++) {
                        Object item = Array.get(rightResult, index);
                        if (item == null) {
                            hasNullRow = true;
                            continue;
                        }
                        hasNonNullRow = true;
                        if (!mustCoerce) {
                            if (leftResult.equals(item)) {
                                return true;
                            }
                        } else {
                            if (!(item instanceof Number)) {
                                continue;
                            }
                            Number left = coercer.coerceBoxed((Number) leftResult);
                            Number right = coercer.coerceBoxed((Number) item);
                            if (left.equals(right)) {
                                return true;
                            }
                        }
                    }
                } else {
                    if (leftResult == null) {
                        return null;
                    }
                    hasNonNullRow = true;
                    if (!mustCoerce) {
                        if (leftResult.equals(rightResult)) {
                            return true;
                        }
                    } else {
                        Number left = coercer.coerceBoxed((Number) leftResult);
                        Number right = coercer.coerceBoxed((Number) rightResult);
                        if (left.equals(right)) {
                            return true;
                        }
                    }
                }
            }

            if ((!hasNonNullRow) || hasNullRow) {
                return null;
            }
            return false;
        }
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        if (isAll) {
            if (isNot) {
                writer.append("!=all");
            } else {
                writer.append("=all");
            }
        } else {
            if (isNot) {
                writer.append("!=any");
            } else {
                writer.append("=any");
            }
        }
        writer.append("(");

        String delimiter = "";
        for (int i = 0; i < this.getChildNodes().length - 1; i++) {
            writer.append(delimiter);
            this.getChildNodes()[i + 1].toEPL(writer, getPrecedence());
            delimiter = ",";
        }
        writer.append(")");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.EQUALS;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprEqualsAllAnyNode)) {
            return false;
        }

        ExprEqualsAllAnyNode other = (ExprEqualsAllAnyNode) node;
        return (other.isNot == this.isNot) && (other.isAll == this.isAll);
    }
}
