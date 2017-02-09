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
package com.espertech.esper.client.soda;

import com.espertech.esper.type.BitWiseOpEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience factory for creating {@link Expression} instances.
 * <p>
 * Provides quick-access methods to create all possible expressions and provides typical parameter lists to each.
 * <p>
 * Note that only the typical parameter lists are provided and expressions can allow adding
 * additional parameters.
 * <p>
 * Many expressions, for example logical AND and OR (conjunction and disjunction), allow
 * adding an unlimited number of additional sub-expressions to an expression. For those expressions
 * there are additional add methods provided.
 */
public class Expressions implements Serializable {
    private static final long serialVersionUID = 0L;

    /**
     * Current system time supplies internal-timer provided time or
     * the time provided by external timer events.
     *
     * @return expression
     */
    public static CurrentTimestampExpression currentTimestamp() {
        return new CurrentTimestampExpression();
    }

    /**
     * Exists-function for use with dynamic properties to test property existence.
     *
     * @param propertyName name of the property to test whether it exists or not
     * @return expression
     */
    public static PropertyExistsExpression existsProperty(String propertyName) {
        return new PropertyExistsExpression(propertyName);
    }

    /**
     * Cast function, casts the result on an expression to the desired type, or
     * returns null if the type cannot be casted to the type.
     * <p>
     * The list of types can include fully-qualified class names plus any of the
     * Java primitive type names: byte, char, short, int, long, float, double, boolean.
     * Alternatively to "java.lang.String" the simple "string" is also permitted.
     * <p>
     * Type checks include all superclasses and interfaces of the value returned by the expression.
     *
     * @param expression returns the value to cast
     * @param typeName   is type to cast to
     * @return expression
     */
    public static CastExpression cast(Expression expression, String typeName) {
        return new CastExpression(expression, typeName);
    }

    /**
     * Cast function, casts the result on an expression to the desired type, or
     * returns null if the type cannot be casted to the type.
     * <p>
     * The list of types can include fully-qualified class names plus any of the
     * Java primitive type names: byte, char, short, int, long, float, double, boolean.
     * Alternatively to "java.lang.String" the simple "string" is also permitted.
     * <p>
     * Type checks include all superclasses and interfaces of the value returned by the expression.
     *
     * @param propertyName name of the property supplying the value to cast
     * @param typeName     is type to cast to
     * @return expression
     */
    public static CastExpression cast(String propertyName, String typeName) {
        return new CastExpression(getPropExpr(propertyName), typeName);
    }

    /**
     * Instance-of function, tests if the type of the return value of an expression is in a list of types.
     * <p>
     * The list of types can include fully-qualified class names plus any of the
     * Java primitive type names: byte, char, short, int, long, float, double, boolean.
     * Alternatively to "java.lang.String" the simple "string" is also permitted.
     * <p>
     * Type checks include all superclasses and interfaces of the value returned by the expression.
     *
     * @param expression returns the value to test whether the type returned is any of the  is the function name
     * @param typeName   is one type to check for
     * @param typeNames  is optional additional types to check for in a list
     * @return expression
     */
    public static InstanceOfExpression instanceOf(Expression expression, String typeName, String... typeNames) {
        return new InstanceOfExpression(expression, typeName, typeNames);
    }

    /**
     * Instance-of function, tests if the type of the return value of a property is in a list of types.
     * <p>
     * Useful with dynamic (unchecked) properties to check the type of property returned.
     * <p>
     * The list of types can include fully-qualified class names plus any of the
     * Java primitive type names: byte, char, short, int, long, float, double, boolean.
     * Alternatively to "java.lang.String" the simple "string" is also permitted.
     * <p>
     * Type checks include all superclasses and interfaces of the value returned by the expression.
     *
     * @param propertyName name of the property supplying the value to test
     * @param typeName     is one type to check for
     * @param typeNames    is optional additional types to check for in a list
     * @return expression
     */
    public static InstanceOfExpression instanceOf(String propertyName, String typeName, String... typeNames) {
        return new InstanceOfExpression(getPropExpr(propertyName), typeName, typeNames);
    }

    /**
     * Type-of function, returns the event type name or result type as a string of a stream name, property or expression.
     *
     * @param expression to evaluate and return it's result type as a string
     * @return expression
     */
    public static TypeOfExpression typeOf(Expression expression) {
        return new TypeOfExpression(expression);
    }

    /**
     * Type-of function, returns the event type name or result type as a string of a stream name, property or expression.
     *
     * @param propertyName returns the property to evaluate and return its event type name or property class type
     * @return expression
     */
    public static TypeOfExpression typeOf(String propertyName) {
        return new TypeOfExpression(getPropExpr(propertyName));
    }

    /**
     * Plug-in aggregation function.
     *
     * @param functionName    is the function name
     * @param moreExpressions provides the values to aggregate
     * @return expression
     */
    public static PlugInProjectionExpression plugInAggregation(String functionName, Expression... moreExpressions) {
        return new PlugInProjectionExpression(functionName, false, moreExpressions);
    }

    /**
     * Regular expression.
     *
     * @param left  returns the values to match
     * @param right returns the value to match against
     * @return expression
     */
    public static RegExpExpression regexp(Expression left, Expression right) {
        return new RegExpExpression(left, right);
    }

    /**
     * Regular expression.
     *
     * @param left   returns the values to match
     * @param right  returns the value to match against
     * @param escape is the escape character
     * @return expression
     */
    public static RegExpExpression regexp(Expression left, Expression right, String escape) {
        return new RegExpExpression(left, right, new ConstantExpression(escape));
    }

    /**
     * Regular expression.
     *
     * @param property        the name of the property returning values to match
     * @param regExExpression a regular expression to match against
     * @return expression
     */
    public static RegExpExpression regexp(String property, String regExExpression) {
        return new RegExpExpression(getPropExpr(property), new ConstantExpression(regExExpression));
    }

    /**
     * Regular expression.
     *
     * @param property        the name of the property returning values to match
     * @param regExExpression a regular expression to match against
     * @param escape          is the escape character
     * @return expression
     */
    public static RegExpExpression regexp(String property, String regExExpression, String escape) {
        return new RegExpExpression(getPropExpr(property), new ConstantExpression(regExExpression), new ConstantExpression(escape));
    }

    /**
     * Regular expression negated (not regexp).
     *
     * @param left  returns the values to match
     * @param right returns the value to match against
     * @return expression
     */
    public static RegExpExpression notRegexp(Expression left, Expression right) {
        return new RegExpExpression(left, right, true);
    }

    /**
     * Regular expression negated (not regexp).
     *
     * @param left   returns the values to match
     * @param right  returns the value to match against
     * @param escape is the escape character
     * @return expression
     */
    public static RegExpExpression notRegexp(Expression left, Expression right, String escape) {
        return new RegExpExpression(left, right, new ConstantExpression(escape), true);
    }

    /**
     * Regular expression negated (not regexp).
     *
     * @param property        the name of the property returning values to match
     * @param regExExpression a regular expression to match against
     * @return expression
     */
    public static RegExpExpression notRegexp(String property, String regExExpression) {
        return new RegExpExpression(getPropExpr(property), new ConstantExpression(regExExpression), true);
    }

    /**
     * Regular expression negated (not regexp).
     *
     * @param property        the name of the property returning values to match
     * @param regExExpression a regular expression to match against
     * @param escape          is the escape character
     * @return expression
     */
    public static RegExpExpression notRegexp(String property, String regExExpression, String escape) {
        return new RegExpExpression(getPropExpr(property), new ConstantExpression(regExExpression), new ConstantExpression(escape), true);
    }

    /**
     * Array expression, representing the syntax of "{1, 2, 3}" returning an integer array of 3 elements valued 1, 2, 3.
     *
     * @return expression
     */
    public static ArrayExpression array() {
        return new ArrayExpression();
    }

    /**
     * Bitwise (binary) AND.
     *
     * @return expression
     */
    public static BitwiseOpExpression binaryAnd() {
        return new BitwiseOpExpression(BitWiseOpEnum.BAND);
    }

    /**
     * Bitwise (binary) OR.
     *
     * @return expression
     */
    public static BitwiseOpExpression binaryOr() {
        return new BitwiseOpExpression(BitWiseOpEnum.BOR);
    }

    /**
     * Bitwise (binary) XOR.
     *
     * @return expression
     */
    public static BitwiseOpExpression binaryXor() {
        return new BitwiseOpExpression(BitWiseOpEnum.BXOR);
    }

    /**
     * Minimum value per-row function (not aggregating).
     *
     * @param propertyOne    the name of a first property to compare
     * @param propertyTwo    the name of a second property to compare
     * @param moreProperties optional additional properties to compare
     * @return expression
     */
    public static MinRowExpression min(String propertyOne, String propertyTwo, String... moreProperties) {
        return new MinRowExpression(propertyOne, propertyTwo, moreProperties);
    }

    /**
     * Minimum value per-row function (not aggregating).
     *
     * @param exprOne         returns the first value to compare
     * @param exprTwo         returns the second value to compare
     * @param moreExpressions optional additional values to compare
     * @return expression
     */
    public static MinRowExpression min(Expression exprOne, Expression exprTwo, Expression... moreExpressions) {
        return new MinRowExpression(exprOne, exprTwo, moreExpressions);
    }

    /**
     * Maximum value per-row function (not aggregating).
     *
     * @param propertyOne    the name of a first property to compare
     * @param propertyTwo    the name of a second property to compare
     * @param moreProperties optional additional properties to compare
     * @return expression
     */
    public static MaxRowExpression max(String propertyOne, String propertyTwo, String... moreProperties) {
        return new MaxRowExpression(propertyOne, propertyTwo, moreProperties);
    }

    /**
     * Maximum value per-row function (not aggregating).
     *
     * @param exprOne         returns the first value to compare
     * @param exprTwo         returns the second value to compare
     * @param moreExpressions optional additional values to compare
     * @return expression
     */
    public static MaxRowExpression max(Expression exprOne, Expression exprTwo, Expression... moreExpressions) {
        return new MaxRowExpression(exprOne, exprTwo, moreExpressions);
    }

    /**
     * Coalesce.
     *
     * @param propertyOne    name of the first property returning value to coealesce
     * @param propertyTwo    name of the second property returning value to coealesce
     * @param moreProperties name of the optional additional properties returning values to coealesce
     * @return expression
     */
    public static CoalesceExpression coalesce(String propertyOne, String propertyTwo, String... moreProperties) {
        return new CoalesceExpression(propertyOne, propertyTwo, moreProperties);
    }

    /**
     * Coalesce.
     *
     * @param exprOne         returns value to coalesce
     * @param exprTwo         returns value to coalesce
     * @param moreExpressions returning optional additional values to coalesce
     * @return expression
     */
    public static CoalesceExpression coalesce(Expression exprOne, Expression exprTwo, Expression... moreExpressions) {
        return new CoalesceExpression(exprOne, exprTwo, moreExpressions);
    }

    /**
     * Constant.
     *
     * @param value is the constant value
     * @return expression
     */
    public static ConstantExpression constant(Object value) {
        return new ConstantExpression(value);
    }

    /**
     * Constant, use when the value is null.
     *
     * @param value        is the constant value
     * @param constantType is the type of the constant
     * @return expression
     */
    public static ConstantExpression constant(Object value, Class constantType) {
        return new ConstantExpression(value, constantType.getName());
    }

    /**
     * Case-when-then expression.
     *
     * @return expression
     */
    public static CaseWhenThenExpression caseWhenThen() {
        return new CaseWhenThenExpression();
    }

    /**
     * Case-switch expresssion.
     *
     * @param valueToSwitchOn provides the switch value
     * @return expression
     */
    public static CaseSwitchExpression caseSwitch(Expression valueToSwitchOn) {
        return new CaseSwitchExpression(valueToSwitchOn);
    }

    /**
     * Case-switch expresssion.
     *
     * @param propertyName the name of the property that provides the switch value
     * @return expression
     */
    public static CaseSwitchExpression caseSwitch(String propertyName) {
        return new CaseSwitchExpression(getPropExpr(propertyName));
    }

    /**
     * In-expression that is equivalent to the syntax of "property in (value, value, ... value)".
     *
     * @param property is the name of the property
     * @param values   are the constants to check against
     * @return expression
     */
    public static InExpression in(String property, Object... values) {
        return new InExpression(getPropExpr(property), false, values);
    }

    /**
     * Not-In-expression that is equivalent to the syntax of "property not in (value, value, ... value)".
     *
     * @param property is the name of the property
     * @param values   are the constants to check against
     * @return expression
     */
    public static InExpression notIn(String property, Object... values) {
        return new InExpression(getPropExpr(property), true, values);
    }

    /**
     * In-expression that is equivalent to the syntax of "property in (value, value, ... value)".
     *
     * @param value provides values to match
     * @param set   are expressons that provide match-against values
     * @return expression
     */
    public static InExpression in(Expression value, Expression... set) {
        return new InExpression(value, false, set);
    }

    /**
     * Not-In-expression that is equivalent to the syntax of "property not in (value, value, ... value)".
     *
     * @param value provides values to match
     * @param set   are expressons that provide match-against values
     * @return expression
     */
    public static InExpression notIn(Expression value, Expression... set) {
        return new InExpression(value, true, (Object) set);
    }

    /**
     * Not expression negates the sub-expression to the not which is expected to return boolean-typed values.
     *
     * @param inner is the sub-expression
     * @return expression
     */
    public static NotExpression not(Expression inner) {
        return new NotExpression(inner);
    }

    /**
     * Static method invocation.
     *
     * @param className  the name of the class to invoke a method on
     * @param method     the name of the method to invoke
     * @param parameters zero, one or more constants that are the parameters to the static method
     * @return expression
     */
    public static StaticMethodExpression staticMethod(String className, String method, Object... parameters) {
        return new StaticMethodExpression(className, method, parameters);
    }

    /**
     * Static method invocation.
     *
     * @param className  the name of the class to invoke a method on
     * @param method     the name of the method to invoke
     * @param parameters zero, one or more expressions that provide parameters to the static method
     * @return expression
     */
    public static StaticMethodExpression staticMethod(String className, String method, Expression... parameters) {
        return new StaticMethodExpression(className, method, parameters);
    }

    /**
     * Prior function.
     *
     * @param index    the numeric index of the prior event
     * @param property the name of the property to obtain the value for
     * @return expression
     */
    public static PriorExpression prior(int index, String property) {
        return new PriorExpression(index, property);
    }

    /**
     * Previous function.
     *
     * @param expression provides the numeric index of the previous event
     * @param property   the name of the property to obtain the value for
     * @return expression
     */
    public static PreviousExpression previous(Expression expression, String property) {
        return new PreviousExpression(expression, property);
    }

    /**
     * Previous function.
     *
     * @param index    the numeric index of the previous event
     * @param property the name of the property to obtain the value for
     * @return expression
     */
    public static PreviousExpression previous(int index, String property) {
        return new PreviousExpression(index, property);
    }

    /**
     * Previous tail function.
     *
     * @param expression provides the numeric index of the previous event
     * @param property   the name of the property to obtain the value for
     * @return expression
     */
    public static PreviousExpression previousTail(Expression expression, String property) {
        PreviousExpression expr = new PreviousExpression(expression, property);
        expr.setType(PreviousExpressionType.PREVTAIL);
        return expr;
    }

    /**
     * Previous tail function.
     *
     * @param index    the numeric index of the previous event
     * @param property the name of the property to obtain the value for
     * @return expression
     */
    public static PreviousExpression previousTail(int index, String property) {
        PreviousExpression expr = new PreviousExpression(index, property);
        expr.setType(PreviousExpressionType.PREVTAIL);
        return expr;
    }

    /**
     * Previous count function.
     *
     * @param property provides the properties or stream name to select for the previous event
     * @return expression
     */
    public static PreviousExpression previousCount(String property) {
        return new PreviousExpression(PreviousExpressionType.PREVCOUNT, property(property));
    }

    /**
     * Previous window function.
     *
     * @param property provides the properties or stream name to select for the previous event
     * @return expression
     */
    public static PreviousExpression previousWindow(String property) {
        return new PreviousExpression(PreviousExpressionType.PREVWINDOW, property(property));
    }

    /**
     * Between.
     *
     * @param property             the name of the property supplying data points.
     * @param lowBoundaryProperty  the name of the property supplying lower boundary.
     * @param highBoundaryProperty the name of the property supplying upper boundary.
     * @return expression
     */
    public static BetweenExpression betweenProperty(String property, String lowBoundaryProperty, String highBoundaryProperty) {
        return new BetweenExpression(getPropExpr(property), getPropExpr(lowBoundaryProperty), getPropExpr(highBoundaryProperty));
    }

    /**
     * Between.
     *
     * @param property     the name of the property that returns the datapoint to check range
     * @param lowBoundary  constant indicating the lower boundary
     * @param highBoundary constant indicating the upper boundary
     * @return expression
     */
    public static BetweenExpression between(String property, Object lowBoundary, Object highBoundary) {
        return new BetweenExpression(getPropExpr(property), new ConstantExpression(lowBoundary), new ConstantExpression(highBoundary));
    }

    /**
     * Between.
     *
     * @param datapoint    returns the datapoint to check range
     * @param lowBoundary  returns values for the lower boundary
     * @param highBoundary returns values for the upper boundary
     * @return expression
     */
    public static BetweenExpression between(Expression datapoint, Expression lowBoundary, Expression highBoundary) {
        return new BetweenExpression(datapoint, lowBoundary, highBoundary);
    }

    /**
     * Between (or range).
     *
     * @param datapoint      returns the datapoint to check range
     * @param lowBoundary    returns values for the lower boundary
     * @param highBoundary   returns values for the upper boundary
     * @param isLowIncluded  true to indicate lower boundary itself is included in the range
     * @param isHighIncluded true to indicate upper boundary itself is included in the range
     * @return expression
     */
    public static BetweenExpression range(Expression datapoint, Expression lowBoundary, Expression highBoundary, boolean isLowIncluded, boolean isHighIncluded) {
        return new BetweenExpression(datapoint, lowBoundary, highBoundary, isLowIncluded, isHighIncluded, false);
    }

    /**
     * Logical OR disjunction. Use add methods to add expressions.
     *
     * @return expression
     */
    public static Disjunction or() {
        return new Disjunction();
    }

    /**
     * Logical OR disjunction.
     *
     * @param first       an expression returning values to junction
     * @param second      an expression returning values to junction
     * @param expressions an optional list of expressions returning values to junction
     * @return expression
     */
    public static Disjunction or(Expression first, Expression second, Expression... expressions) {
        return new Disjunction(first, second, expressions);
    }

    /**
     * Logical AND conjunction. Use add methods to add expressions.
     *
     * @return expression
     */
    public static Conjunction and() {
        return new Conjunction();
    }

    /**
     * Logical AND conjunction.
     *
     * @param first       an expression returning values to junction
     * @param second      an expression returning values to junction
     * @param expressions an optional list of expressions returning values to junction
     * @return expression
     */
    public static Conjunction and(Expression first, Expression second, Expression... expressions) {
        return new Conjunction(first, second, expressions);
    }

    /**
     * Greater-or-equal between a property and a constant.
     *
     * @param propertyName the name of the property providing left hand side values
     * @param value        is the constant to compare
     * @return expression
     */
    public static RelationalOpExpression ge(String propertyName, Object value) {
        return new RelationalOpExpression(getPropExpr(propertyName), ">=", new ConstantExpression(value));
    }

    /**
     * Greater-or-equals between expression results.
     *
     * @param left  the expression providing left hand side values
     * @param right the expression providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression ge(Expression left, Expression right) {
        return new RelationalOpExpression(left, ">=", right);
    }

    /**
     * Greater-or-equal between properties.
     *
     * @param propertyLeft  the name of the property providing left hand side values
     * @param propertyRight the name of the property providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression geProperty(String propertyLeft, String propertyRight) {
        return new RelationalOpExpression(getPropExpr(propertyLeft), ">=", new PropertyValueExpression(propertyRight));
    }

    /**
     * Greater-then between a property and a constant.
     *
     * @param propertyName the name of the property providing left hand side values
     * @param value        is the constant to compare
     * @return expression
     */
    public static RelationalOpExpression gt(String propertyName, Object value) {
        return new RelationalOpExpression(getPropExpr(propertyName), ">", new ConstantExpression(value));
    }

    /**
     * Greater-then between expression results.
     *
     * @param left  the expression providing left hand side values
     * @param right the expression providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression gt(Expression left, Expression right) {
        return new RelationalOpExpression(left, ">", right);
    }

    /**
     * Greater-then between properties.
     *
     * @param propertyLeft  the name of the property providing left hand side values
     * @param propertyRight the name of the property providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression gtProperty(String propertyLeft, String propertyRight) {
        return new RelationalOpExpression(getPropExpr(propertyLeft), ">", new PropertyValueExpression(propertyRight));
    }

    /**
     * Less-or-equals between a property and a constant.
     *
     * @param propertyName the name of the property providing left hand side values
     * @param value        is the constant to compare
     * @return expression
     */
    public static RelationalOpExpression le(String propertyName, Object value) {
        return new RelationalOpExpression(getPropExpr(propertyName), "<=", new ConstantExpression(value));
    }

    /**
     * Less-or-equal between properties.
     *
     * @param propertyLeft  the name of the property providing left hand side values
     * @param propertyRight the name of the property providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression leProperty(String propertyLeft, String propertyRight) {
        return new RelationalOpExpression(getPropExpr(propertyLeft), "<=", new PropertyValueExpression(propertyRight));
    }

    /**
     * Less-or-equal between expression results.
     *
     * @param left  the expression providing left hand side values
     * @param right the expression providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression le(Expression left, Expression right) {
        return new RelationalOpExpression(left, "<=", right);
    }

    /**
     * Less-then between a property and a constant.
     *
     * @param propertyName the name of the property providing left hand side values
     * @param value        is the constant to compare
     * @return expression
     */
    public static RelationalOpExpression lt(String propertyName, Object value) {
        return new RelationalOpExpression(getPropExpr(propertyName), "<", new ConstantExpression(value));
    }

    /**
     * Less-then between properties.
     *
     * @param propertyLeft  the name of the property providing left hand side values
     * @param propertyRight the name of the property providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression ltProperty(String propertyLeft, String propertyRight) {
        return new RelationalOpExpression(getPropExpr(propertyLeft), "<", new PropertyValueExpression(propertyRight));
    }

    /**
     * Less-then between expression results.
     *
     * @param left  the expression providing left hand side values
     * @param right the expression providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression lt(Expression left, Expression right) {
        return new RelationalOpExpression(left, "<", right);
    }

    /**
     * Equals between a property and a constant.
     *
     * @param propertyName the name of the property providing left hand side values
     * @param value        is the constant to compare
     * @return expression
     */
    public static RelationalOpExpression eq(String propertyName, Object value) {
        return new RelationalOpExpression(getPropExpr(propertyName), "=", new ConstantExpression(value));
    }

    /**
     * Not-Equals between a property and a constant.
     *
     * @param propertyName the name of the property providing left hand side values
     * @param value        is the constant to compare
     * @return expression
     */
    public static RelationalOpExpression neq(String propertyName, Object value) {
        return new RelationalOpExpression(getPropExpr(propertyName), "!=", new ConstantExpression(value));
    }

    /**
     * Equals between properties.
     *
     * @param propertyLeft  the name of the property providing left hand side values
     * @param propertyRight the name of the property providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression eqProperty(String propertyLeft, String propertyRight) {
        return new RelationalOpExpression(getPropExpr(propertyLeft), "=", new PropertyValueExpression(propertyRight));
    }

    /**
     * Not-Equals between properties.
     *
     * @param propertyLeft  the name of the property providing left hand side values
     * @param propertyRight the name of the property providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression neqProperty(String propertyLeft, String propertyRight) {
        return new RelationalOpExpression(getPropExpr(propertyLeft), "!=", new PropertyValueExpression(propertyRight));
    }

    /**
     * Equals between expression results.
     *
     * @param left  the expression providing left hand side values
     * @param right the expression providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression eq(Expression left, Expression right) {
        return new RelationalOpExpression(left, "=", right);
    }

    /**
     * Not-Equals between expression results.
     *
     * @param left  the expression providing left hand side values
     * @param right the expression providing right hand side values
     * @return expression
     */
    public static RelationalOpExpression neq(Expression left, Expression right) {
        return new RelationalOpExpression(left, "!=", right);
    }

    /**
     * Not-null test.
     *
     * @param property the name of the property supplying the value to check for null
     * @return expression
     */
    public static RelationalOpExpression isNotNull(String property) {
        return new RelationalOpExpression(getPropExpr(property), "is not", null);
    }

    /**
     * Not-null test.
     *
     * @param expression supplies the value to check for null
     * @return expression
     */
    public static RelationalOpExpression isNotNull(Expression expression) {
        return new RelationalOpExpression(expression, "is not", null);
    }

    /**
     * Is-null test.
     *
     * @param property the name of the property supplying the value to check for null
     * @return expression
     */
    public static RelationalOpExpression isNull(String property) {
        return new RelationalOpExpression(getPropExpr(property), "is", null);
    }

    /**
     * Is-null test.
     *
     * @param expression supplies the value to check for null
     * @return expression
     */
    public static RelationalOpExpression isNull(Expression expression) {
        return new RelationalOpExpression(expression, "is", null);
    }

    /**
     * Property value.
     * <p>
     * An expression that returns the value of the named property.
     * <p>
     * Nested, indexed or mapped properties follow the documented sytnax.
     *
     * @param propertyName is the name of the property to return the value for.
     * @return expression
     */
    public static PropertyValueExpression property(String propertyName) {
        return getPropExpr(propertyName);
    }

    /**
     * SQL-Like.
     *
     * @param propertyName the name of the property providing values to match
     * @param value        is the string to match against
     * @return expression
     */
    public static LikeExpression like(String propertyName, String value) {
        return new LikeExpression(getPropExpr(propertyName), new ConstantExpression(value));
    }

    /**
     * SQL-Like.
     *
     * @param left  provides value to match
     * @param right provides string to match against
     * @return expression
     */
    public static LikeExpression like(Expression left, Expression right) {
        return new LikeExpression(left, right);
    }

    /**
     * SQL-Like.
     *
     * @param propertyName the name of the property providing values to match
     * @param value        is the string to match against
     * @param escape       the escape character(s)
     * @return expression
     */
    public static LikeExpression like(String propertyName, Object value, String escape) {
        return new LikeExpression(getPropExpr(propertyName), new ConstantExpression(value), new ConstantExpression(escape));
    }

    /**
     * SQL-Like.
     *
     * @param left   provides value to match
     * @param right  provides string to match against
     * @param escape the escape character(s)
     * @return expression
     */
    public static LikeExpression like(Expression left, Expression right, Expression escape) {
        return new LikeExpression(left, right, escape);
    }

    /**
     * SQL-Like negated (not like).
     *
     * @param propertyName the name of the property providing values to match
     * @param value        is the string to match against
     * @return expression
     */
    public static LikeExpression notLike(String propertyName, String value) {
        return new LikeExpression(getPropExpr(propertyName), new ConstantExpression(value), true);
    }

    /**
     * SQL-Like negated (not like).
     *
     * @param left  provides value to match
     * @param right provides string to match against
     * @return expression
     */
    public static LikeExpression notLike(Expression left, Expression right) {
        return new LikeExpression(left, right, true);
    }

    /**
     * SQL-Like negated (not like).
     *
     * @param propertyName the name of the property providing values to match
     * @param value        is the string to match against
     * @param escape       the escape character(s)
     * @return expression
     */
    public static LikeExpression notLike(String propertyName, Object value, String escape) {
        return new LikeExpression(getPropExpr(propertyName), new ConstantExpression(value), new ConstantExpression(escape), true);
    }

    /**
     * SQL-Like negated (not like).
     *
     * @param left   provides value to match
     * @param right  provides string to match against
     * @param escape the escape character(s)
     * @return expression
     */
    public static LikeExpression notLike(Expression left, Expression right, Expression escape) {
        return new LikeExpression(left, right, escape, true);
    }

    /**
     * Average aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static AvgProjectionExpression avg(String propertyName) {
        return new AvgProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Average aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static AvgProjectionExpression avg(Expression expression) {
        return new AvgProjectionExpression(expression, false);
    }

    /**
     * Average aggregation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static AvgProjectionExpression avgDistinct(String propertyName) {
        return new AvgProjectionExpression(getPropExpr(propertyName), true);
    }

    /**
     * Average aggregation function considering distinct values only.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static AvgProjectionExpression avgDistinct(Expression expression) {
        return new AvgProjectionExpression(expression, true);
    }

    /**
     * Median aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static MedianProjectionExpression median(String propertyName) {
        return new MedianProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Median aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static MedianProjectionExpression median(Expression expression) {
        return new MedianProjectionExpression(expression, false);
    }

    /**
     * Median aggregation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static MedianProjectionExpression medianDistinct(String propertyName) {
        return new MedianProjectionExpression(getPropExpr(propertyName), true);
    }

    /**
     * Median aggregation function considering distinct values only.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static MedianProjectionExpression medianDistinct(Expression expression) {
        return new MedianProjectionExpression(expression, true);
    }

    /**
     * Standard deviation aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static StddevProjectionExpression stddev(String propertyName) {
        return new StddevProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Standard deviation aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static StddevProjectionExpression stddev(Expression expression) {
        return new StddevProjectionExpression(expression, false);
    }

    /**
     * Standard deviation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static StddevProjectionExpression stddevDistinct(String propertyName) {
        return new StddevProjectionExpression(getPropExpr(propertyName), true);
    }

    /**
     * Standard deviation function considering distinct values only.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static StddevProjectionExpression stddevDistinct(Expression expression) {
        return new StddevProjectionExpression(expression, true);
    }

    /**
     * Mean deviation aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static AvedevProjectionExpression avedev(String propertyName) {
        return new AvedevProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Lastever-value aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static LastEverProjectionExpression lastEver(String propertyName) {
        return new LastEverProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Lastever-value aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static LastProjectionExpression last(String propertyName) {
        return new LastProjectionExpression(getPropExpr(propertyName));
    }

    /**
     * Lastever-value aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static LastEverProjectionExpression lastEver(Expression expression) {
        return new LastEverProjectionExpression(expression, false);
    }

    /**
     * Lastever-value aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static LastProjectionExpression last(Expression expression) {
        return new LastProjectionExpression(expression);
    }

    /**
     * First-value (windowed) aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static FirstProjectionExpression first(String propertyName) {
        return new FirstProjectionExpression(getPropExpr(propertyName));
    }

    /**
     * First-value (ever) aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static FirstEverProjectionExpression firstEver(String propertyName) {
        return new FirstEverProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * First-value (in window) aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static FirstProjectionExpression first(Expression expression) {
        return new FirstProjectionExpression(expression);
    }

    /**
     * First-value (ever) aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static FirstEverProjectionExpression firstEver(Expression expression) {
        return new FirstEverProjectionExpression(expression, false);
    }

    /**
     * Mean deviation aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static AvedevProjectionExpression avedev(Expression expression) {
        return new AvedevProjectionExpression(expression, false);
    }

    /**
     * Mean deviation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static AvedevProjectionExpression avedevDistinct(String propertyName) {
        return new AvedevProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Mean deviation function considering distinct values only.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static AvedevProjectionExpression avedevDistinct(Expression expression) {
        return new AvedevProjectionExpression(expression, false);
    }

    /**
     * Sum aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static SumProjectionExpression sum(String propertyName) {
        return new SumProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Sum aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static SumProjectionExpression sum(Expression expression) {
        return new SumProjectionExpression(expression, false);
    }

    /**
     * Sum aggregation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static SumProjectionExpression sumDistinct(String propertyName) {
        return new SumProjectionExpression(getPropExpr(propertyName), true);
    }

    /**
     * Sum aggregation function considering distinct values only.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static SumProjectionExpression sumDistinct(Expression expression) {
        return new SumProjectionExpression(expression, true);
    }

    /**
     * Count aggregation function not counting values, equivalent to "count(*)".
     *
     * @return expression
     */
    public static CountStarProjectionExpression countStar() {
        CountStarProjectionExpression expr = new CountStarProjectionExpression();
        expr.addChild(new WildcardExpression());
        return expr;
    }

    /**
     * Count aggregation function.
     *
     * @param propertyName name of the property providing the values to count.
     * @return expression
     */
    public static CountProjectionExpression count(String propertyName) {
        return new CountProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Count aggregation function.
     *
     * @param expression provides the values to count.
     * @return expression
     */
    public static CountProjectionExpression count(Expression expression) {
        return new CountProjectionExpression(expression, false);
    }

    /**
     * Count aggregation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to count.
     * @return expression
     */
    public static CountProjectionExpression countDistinct(String propertyName) {
        return new CountProjectionExpression(getPropExpr(propertyName), true);
    }

    /**
     * Count aggregation function considering distinct values only.
     *
     * @param expression provides the values to count.
     * @return expression
     */
    public static CountProjectionExpression countDistinct(Expression expression) {
        return new CountProjectionExpression(expression, true);
    }

    /**
     * Minimum aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static MinProjectionExpression min(String propertyName) {
        return new MinProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Minimum aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static MinProjectionExpression min(Expression expression) {
        return new MinProjectionExpression(expression, false);
    }

    /**
     * Minimum aggregation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static MinProjectionExpression minDistinct(String propertyName) {
        return new MinProjectionExpression(getPropExpr(propertyName), true);
    }

    /**
     * Minimum aggregation function considering distinct values only.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static MinProjectionExpression minDistinct(Expression expression) {
        return new MinProjectionExpression(expression, true);
    }

    /**
     * Maximum aggregation function.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static MaxProjectionExpression max(String propertyName) {
        return new MaxProjectionExpression(getPropExpr(propertyName), false);
    }

    /**
     * Maximum aggregation function.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static MaxProjectionExpression max(Expression expression) {
        return new MaxProjectionExpression(expression, false);
    }

    /**
     * Maximum aggregation function considering distinct values only.
     *
     * @param propertyName name of the property providing the values to aggregate.
     * @return expression
     */
    public static MaxProjectionExpression maxDistinct(String propertyName) {
        return new MaxProjectionExpression(getPropExpr(propertyName), true);
    }

    /**
     * Maximum aggregation function considering distinct values only.
     *
     * @param expression provides the values to aggregate.
     * @return expression
     */
    public static MaxProjectionExpression maxDistinct(Expression expression) {
        return new MaxProjectionExpression(expression, true);
    }

    /**
     * Modulo.
     *
     * @param left  the expression providing left hand values
     * @param right the expression providing right hand values
     * @return expression
     */
    public static ArithmaticExpression modulo(Expression left, Expression right) {
        return new ArithmaticExpression(left, "%", right);
    }

    /**
     * Modulo.
     *
     * @param propertyLeft  the name of the property providing left hand values
     * @param propertyRight the name of the property providing right hand values
     * @return expression
     */
    public static ArithmaticExpression modulo(String propertyLeft, String propertyRight) {
        return new ArithmaticExpression(new PropertyValueExpression(propertyLeft), "%", new PropertyValueExpression(propertyRight));
    }

    /**
     * Subtraction.
     *
     * @param left  the expression providing left hand values
     * @param right the expression providing right hand values
     * @return expression
     */
    public static ArithmaticExpression minus(Expression left, Expression right) {
        return new ArithmaticExpression(left, "-", right);
    }

    /**
     * Subtraction.
     *
     * @param propertyLeft  the name of the property providing left hand values
     * @param propertyRight the name of the property providing right hand values
     * @return expression
     */
    public static ArithmaticExpression minus(String propertyLeft, String propertyRight) {
        return new ArithmaticExpression(new PropertyValueExpression(propertyLeft), "-", new PropertyValueExpression(propertyRight));
    }

    /**
     * Addition.
     *
     * @param left  the expression providing left hand values
     * @param right the expression providing right hand values
     * @return expression
     */
    public static ArithmaticExpression plus(Expression left, Expression right) {
        return new ArithmaticExpression(left, "+", right);
    }

    /**
     * Addition.
     *
     * @param propertyLeft  the name of the property providing left hand values
     * @param propertyRight the name of the property providing right hand values
     * @return expression
     */
    public static ArithmaticExpression plus(String propertyLeft, String propertyRight) {
        return new ArithmaticExpression(new PropertyValueExpression(propertyLeft), "+", new PropertyValueExpression(propertyRight));
    }

    /**
     * Multiplication.
     *
     * @param left  the expression providing left hand values
     * @param right the expression providing right hand values
     * @return expression
     */
    public static ArithmaticExpression multiply(Expression left, Expression right) {
        return new ArithmaticExpression(left, "*", right);
    }

    /**
     * Multiplication.
     *
     * @param propertyLeft  the name of the property providing left hand values
     * @param propertyRight the name of the property providing right hand values
     * @return expression
     */
    public static ArithmaticExpression multiply(String propertyLeft, String propertyRight) {
        return new ArithmaticExpression(new PropertyValueExpression(propertyLeft), "*", new PropertyValueExpression(propertyRight));
    }

    /**
     * Division.
     *
     * @param left  the expression providing left hand values
     * @param right the expression providing right hand values
     * @return expression
     */
    public static ArithmaticExpression divide(Expression left, Expression right) {
        return new ArithmaticExpression(left, "/", right);
    }

    /**
     * Division.
     *
     * @param propertyLeft  the name of the property providing left hand values
     * @param propertyRight the name of the property providing right hand values
     * @return expression
     */
    public static ArithmaticExpression divide(String propertyLeft, String propertyRight) {
        return new ArithmaticExpression(new PropertyValueExpression(propertyLeft), "/", new PropertyValueExpression(propertyRight));
    }

    /**
     * Concatenation.
     *
     * @param property   the name of property returning values to concatenate
     * @param properties the names of additional properties returning values to concatenate
     * @return expression
     */
    public static ConcatExpression concat(String property, String... properties) {
        ConcatExpression concat = new ConcatExpression();
        concat.getChildren().add(new PropertyValueExpression(property));
        concat.getChildren().addAll(toPropertyExpressions(properties));
        return concat;
    }

    /**
     * Subquery.
     *
     * @param model is the object model of the lookup
     * @return expression
     */
    public static SubqueryExpression subquery(EPStatementObjectModel model) {
        return new SubqueryExpression(model);
    }

    /**
     * Subquery with in-clause, represents the syntax of "value in (select ... from ...)".
     *
     * @param property is the name of the property that returns the value to match against the values returned by the lookup
     * @param model    is the object model of the lookup
     * @return expression
     */
    public static SubqueryInExpression subqueryIn(String property, EPStatementObjectModel model) {
        return new SubqueryInExpression(getPropExpr(property), model, false);
    }

    /**
     * Subquery with not-in-clause, represents the syntax of "value not in (select ... from ...)".
     *
     * @param property is the name of the property that returns the value to match against the values returned by the lookup
     * @param model    is the object model of the lookup
     * @return expression
     */
    public static SubqueryInExpression subqueryNotIn(String property, EPStatementObjectModel model) {
        return new SubqueryInExpression(getPropExpr(property), model, true);
    }

    /**
     * Subquery with exists-clause, represents the syntax of "select * from ... where exists (select ... from ...)".
     *
     * @param model is the object model of the lookup
     * @return expression
     */
    public static SubqueryExistsExpression subqueryExists(EPStatementObjectModel model) {
        return new SubqueryExistsExpression(model);
    }

    /**
     * Subquery with in-clause, represents the syntax of "value in (select ... from ...)".
     *
     * @param expression returns the value to match against the values returned by the lookup
     * @param model      is the object model of the lookup
     * @return expression
     */
    public static SubqueryInExpression subqueryIn(Expression expression, EPStatementObjectModel model) {
        return new SubqueryInExpression(expression, model, false);
    }

    /**
     * Subquery with not-in-clause, represents the syntax of "value not in (select ... from ...)".
     *
     * @param expression returns the value to match against the values returned by the lookup
     * @param model      is the object model of the lookup
     * @return expression
     */
    public static SubqueryInExpression subqueryNotIn(Expression expression, EPStatementObjectModel model) {
        return new SubqueryInExpression(expression, model, true);
    }

    /**
     * Returns a time period expression for the specified parts.
     * <p>
     * Each part can be a null value in which case the part is left out.
     *
     * @param days         day part
     * @param hours        hour part
     * @param minutes      minute part
     * @param seconds      seconds part
     * @param milliseconds milliseconds part
     * @return time period expression
     */
    public static TimePeriodExpression timePeriod(Double days, Double hours, Double minutes, Double seconds, Double milliseconds) {
        Expression daysExpr = (days != null) ? constant(days) : null;
        Expression hoursExpr = (hours != null) ? constant(hours) : null;
        Expression minutesExpr = (minutes != null) ? constant(minutes) : null;
        Expression secondsExpr = (seconds != null) ? constant(seconds) : null;
        Expression millisecondsExpr = (milliseconds != null) ? constant(milliseconds) : null;
        return new TimePeriodExpression(daysExpr, hoursExpr, minutesExpr, secondsExpr, millisecondsExpr);
    }

    /**
     * Returns a time period expression for the specified parts.
     * <p>
     * Each part can be a null value in which case the part is left out.
     * <p>
     * Each object value may be a String value for an event property, or a number for a constant.
     *
     * @param days         day part
     * @param hours        hour part
     * @param minutes      minute part
     * @param seconds      seconds part
     * @param milliseconds milliseconds part
     * @return time period expression
     */
    public static TimePeriodExpression timePeriod(Object days, Object hours, Object minutes, Object seconds, Object milliseconds) {
        Expression daysExpr = convertVariableNumeric(days);
        Expression hoursExpr = convertVariableNumeric(hours);
        Expression minutesExpr = convertVariableNumeric(minutes);
        Expression secondsExpr = convertVariableNumeric(seconds);
        Expression millisecondsExpr = convertVariableNumeric(milliseconds);
        return new TimePeriodExpression(daysExpr, hoursExpr, minutesExpr, secondsExpr, millisecondsExpr);
    }

    /**
     * Creates a wildcard parameter.
     *
     * @return parameter
     */
    public static CrontabParameterExpression crontabScheduleWildcard() {
        return new CrontabParameterExpression(ScheduleItemType.WILDCARD);
    }

    /**
     * Creates a parameter of the given type and parameterized by a number.
     *
     * @param parameter the constant parameter for the type
     * @param type      the type of crontab parameter
     * @return crontab parameter
     */
    public static CrontabParameterExpression crontabScheduleItem(Integer parameter, ScheduleItemType type) {
        CrontabParameterExpression param = new CrontabParameterExpression(type);
        if (parameter != null) {
            param.addChild(Expressions.constant(parameter));
        }
        return param;
    }

    /**
     * Creates a frequency cron parameter.
     *
     * @param frequency the constant for the frequency
     * @return cron parameter
     */
    public static CrontabFrequencyExpression crontabScheduleFrequency(int frequency) {
        return new CrontabFrequencyExpression(constant(frequency));
    }

    /**
     * Creates a range cron parameter.
     *
     * @param lowerBounds the lower bounds
     * @param upperBounds the upper bounds
     * @return crontab parameter
     */
    public static CrontabRangeExpression crontabScheduleRange(int lowerBounds, int upperBounds) {
        return new CrontabRangeExpression(constant(lowerBounds), constant(upperBounds));
    }

    /**
     * Returns a list of expressions returning property values for the property names passed in.
     *
     * @param properties is a list of property names
     * @return list of property value expressions
     */
    protected static List<PropertyValueExpression> toPropertyExpressions(String... properties) {
        List<PropertyValueExpression> expr = new ArrayList<PropertyValueExpression>();
        for (String property : properties) {
            expr.add(getPropExpr(property));
        }
        return expr;
    }

    /**
     * Returns an expression returning the propertyName value for the propertyName name passed in.
     *
     * @param propertyName the name of the property returning property values
     * @return expression
     */
    protected static PropertyValueExpression getPropExpr(String propertyName) {
        return new PropertyValueExpression(propertyName);
    }

    private static Expression convertVariableNumeric(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return property(object.toString());
        }
        if (object instanceof Number) {
            return constant(object);
        }
        throw new IllegalArgumentException("Invalid object value, expecting String or numeric value");
    }
}
