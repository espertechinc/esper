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
package com.espertech.esper.epl.expression.baseagg;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationResultFuture;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Base expression node that represents an aggregation function such as 'sum' or 'count'.
 * <p>
 * In terms of validation each concrete aggregation node must implement it's own validation.
 * <p>
 * In terms of evaluation this base class will ask the assigned {@link com.espertech.esper.epl.agg.service.AggregationResultFuture} for the current state,
 * using a column number assigned to the node.
 * <p>
 * Concrete subclasses must supply an aggregation state prototype node {@link com.espertech.esper.epl.agg.aggregator.AggregationMethod} that reflects
 * each group's (there may be group-by critera) current aggregation state.
 */
public abstract class ExprAggregateNodeBase extends ExprNodeBase implements ExprEvaluator, ExprAggregateNode {
    private static final long serialVersionUID = 4859196214837888423L;

    protected transient AggregationResultFuture aggregationResultFuture;
    protected int column;
    private transient AggregationMethodFactory aggregationMethodFactory;
    protected ExprAggregateLocalGroupByDesc optionalAggregateLocalGroupByDesc;
    protected ExprNode[] positionalParams;

    /**
     * Indicator for whether the aggregation is distinct - i.e. only unique values are considered.
     */
    protected boolean isDistinct;

    /**
     * Returns the aggregation function name for representation in a generate expression string.
     *
     * @return aggregation function name
     */
    public abstract String getAggregationFunctionName();

    /**
     * Return true if a expression aggregate node semantically equals the current node, or false if not.
     * <p>For use by the equalsNode implementation which compares the distinct flag.
     *
     * @param node to compare to
     * @return true if semantically equal, or false if not equals
     */
    protected abstract boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node);

    /**
     * Gives the aggregation node a chance to validate the sub-expression types.
     *
     * @param validationContext validation information
     * @return aggregation function factory to use
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException when expression validation failed
     */
    protected abstract AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext)
            throws ExprValidationException;

    /**
     * Ctor.
     *
     * @param distinct - sets the flag indicatating whether only unique values should be aggregated
     */
    protected ExprAggregateNodeBase(boolean distinct) {
        isDistinct = distinct;
    }

    public ExprNode[] getPositionalParams() {
        return positionalParams;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public boolean isConstantResult() {
        return false;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        validatePositionals();
        aggregationMethodFactory = validateAggregationChild(validationContext);
        if (validationContext.getExprEvaluatorContext().getStatementType() == StatementType.CREATE_TABLE &&
                optionalAggregateLocalGroupByDesc != null) {
            throw new ExprValidationException("The 'group_by' parameter is not allowed in create-table statements");
        }
        return null;
    }

    public void validatePositionals() throws ExprValidationException {
        ExprAggregateNodeParamDesc paramDesc = ExprAggregateNodeUtil.getValidatePositionalParams(this.getChildNodes(), !(this instanceof ExprAggregationPlugInNodeMarker));
        this.optionalAggregateLocalGroupByDesc = paramDesc.getOptLocalGroupBy();
        if (optionalAggregateLocalGroupByDesc != null) {
            ExprNodeUtility.validateNoSpecialsGroupByExpressions(optionalAggregateLocalGroupByDesc.getPartitionExpressions());
        }
        this.positionalParams = paramDesc.getPositionalParams();
    }

    public Class getType() {
        if (aggregationMethodFactory == null) {
            throw new IllegalStateException("Aggregation method has not been set");
        }
        return aggregationMethodFactory.getResultType();
    }

    /**
     * Returns the aggregation state factory for use in grouping aggregation states per group-by keys.
     *
     * @return prototype aggregation state as a factory for aggregation states per group-by key value
     */
    public AggregationMethodFactory getFactory() {
        if (aggregationMethodFactory == null) {
            throw new IllegalStateException("Aggregation method has not been set");
        }
        return aggregationMethodFactory;
    }

    /**
     * Assigns to the node the future which can be queried for the current aggregation state at evaluation time.
     *
     * @param aggregationResultFuture - future containing state
     * @param column                  - column to hand to future for easy access
     */
    public void setAggregationResultFuture(AggregationResultFuture aggregationResultFuture, int column) {
        this.aggregationResultFuture = aggregationResultFuture;
        this.column = column;
    }

    public final Object evaluate(EventBean[] events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            Object value = aggregationResultFuture.getValue(column, exprEvaluatorContext.getAgentInstanceId(), events, isNewData, exprEvaluatorContext);
            InstrumentationHelper.get().qaExprAggValue(this, value);
            return value;
        }
        return aggregationResultFuture.getValue(column, exprEvaluatorContext.getAgentInstanceId(), events, isNewData, exprEvaluatorContext);
    }

    /**
     * Returns true if the aggregation node is only aggregatig distinct values, or false if
     * aggregating all values.
     *
     * @return true if 'distinct' keyword was given, false if not
     */
    public boolean isDistinct() {
        return isDistinct;
    }

    public final boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprAggregateNode)) {
            return false;
        }

        ExprAggregateNode other = (ExprAggregateNode) node;

        if (other.isDistinct() != this.isDistinct) {
            return false;
        }

        return this.equalsNodeAggregateMethodOnly(other);
    }

    /**
     * For use by implementing classes, validates the aggregation node expecting
     * a single numeric-type child node.
     *
     * @param hasFilter for filter indication
     * @return numeric type of single child
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException if the validation failed
     */
    protected final Class validateNumericChildAllowFilter(boolean hasFilter)
            throws ExprValidationException {
        if (positionalParams.length == 0 || positionalParams.length > 2) {
            throw makeExceptionExpectedParamNum(1, 2);
        }

        // validate child expression (filter expression is actually always the first expression)
        ExprNode child = positionalParams[0];
        if (hasFilter) {
            validateFilter(positionalParams[1].getExprEvaluator());
        }

        Class childType = child.getExprEvaluator().getType();
        if (!JavaClassHelper.isNumeric(childType)) {
            throw new ExprValidationException("Implicit conversion from datatype '" +
                    (childType == null ? "null" : childType.getSimpleName()) +
                    "' to numeric is not allowed for aggregation function '" + getAggregationFunctionName() + "'");
        }

        return childType;
    }

    protected ExprValidationException makeExceptionExpectedParamNum(int lower, int upper) {
        String message = "The '" + getAggregationFunctionName() + "' function expects ";
        if (lower == 0 && upper == 0) {
            message += "no parameters";
        } else if (lower == upper) {
            message += lower + " parameters";
        } else {
            message += "at least " + lower + " and up to " + upper + " parameters";
        }
        return new ExprValidationException(message);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(getAggregationFunctionName());
        writer.append('(');

        if (isDistinct) {
            writer.append("distinct ");
        }

        if (this.getChildNodes().length > 0) {
            this.getChildNodes()[0].toEPL(writer, getPrecedence());

            String delimiter = ",";
            for (int i = 1; i < this.getChildNodes().length; i++) {
                writer.write(delimiter);
                delimiter = ",";
                this.getChildNodes()[i].toEPL(writer, getPrecedence());
            }
        } else {
            if (isExprTextWildcardWhenNoParams()) {
                writer.append('*');
            }
        }

        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.MINIMUM;
    }

    public void validateFilter(ExprEvaluator filterEvaluator) throws ExprValidationException {
        if (JavaClassHelper.getBoxedType(filterEvaluator.getType()) != Boolean.class) {
            throw new ExprValidationException("Invalid filter expression parameter to the aggregation function '" +
                    getAggregationFunctionName() +
                    "' is expected to return a boolean value but returns " + JavaClassHelper.getClassNameFullyQualPretty(filterEvaluator.getType()));
        }
    }

    public ExprAggregateLocalGroupByDesc getOptionalLocalGroupBy() {
        return optionalAggregateLocalGroupByDesc;
    }

    protected boolean isExprTextWildcardWhenNoParams() {
        return true;
    }
}
