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

import java.util.ArrayList;
import java.util.List;

/**
 * Convenience factory for creating {@link PatternExpr} instances, which represent pattern expression trees.
 * <p>
 * Provides quick-access method to create all possible pattern expressions and provides typical parameter lists to each.
 * <p>
 * Note that only the typical parameter lists are provided and pattern expressions can allow adding
 * additional parameters.
 * <p>
 * Many expressions, for example logical AND and OR (conjunction and disjunction), allow
 * adding an unlimited number of additional sub-expressions to a pattern expression. For those pattern expressions
 * there are additional add methods.
 */
public class Patterns {
    /**
     * Pattern-every expression control the lifecycle of the pattern sub-expression.
     *
     * @param inner sub-expression to the every-keyword
     * @return pattern expression
     */
    public static PatternEveryExpr every(PatternExpr inner) {
        return new PatternEveryExpr(inner);
    }

    /**
     * Pattern-AND expression, allows adding sub-expressions that are connected by a logical AND.
     *
     * @return pattern expression representing the AND relationship
     */
    public static PatternAndExpr and() {
        return new PatternAndExpr();
    }

    /**
     * Pattern-AND expression, allows adding sub-expressions that are connected by a logical AND.
     *
     * @param first  is the first pattern sub-expression to add to the AND
     * @param second is a second pattern sub-expression to add to the AND
     * @param more   is optional additional pattern sub-expression to add to the AND
     * @return pattern expression representing the AND relationship
     */
    public static PatternAndExpr and(PatternExpr first, PatternExpr second, PatternExpr... more) {
        return new PatternAndExpr(first, second, more);
    }

    /**
     * Pattern-OR expression, allows adding sub-expressions that are connected by a logical OR.
     *
     * @param first  is the first pattern sub-expression to add to the OR
     * @param second is a second pattern sub-expression to add to the OR
     * @param more   is optional additional pattern sub-expression to add to the OR
     * @return pattern expression representing the OR relationship
     */
    public static PatternOrExpr or(PatternExpr first, PatternExpr second, PatternExpr... more) {
        return new PatternOrExpr(first, second, more);
    }

    /**
     * Pattern-OR expression, allows adding sub-expressions that are connected by a logical OR.
     *
     * @return pattern expression representing the OR relationship
     */
    public static PatternOrExpr or() {
        return new PatternOrExpr();
    }

    /**
     * Pattern followed-by expression, allows adding sub-expressions that are connected by a followed-by.
     *
     * @param first  is the first pattern sub-expression to add to the followed-by
     * @param second is a second pattern sub-expression to add to the followed-by
     * @param more   is optional additional pattern sub-expression to add to the followed-by
     * @return pattern expression representing the followed-by relationship
     */
    public static PatternFollowedByExpr followedBy(PatternExpr first, PatternExpr second, PatternExpr... more) {
        return new PatternFollowedByExpr(first, second, more);
    }

    /**
     * Pattern followed-by expression, allows adding sub-expressions that are connected by a followed-by.
     *
     * @return pattern expression representing the followed-by relationship
     */
    public static PatternFollowedByExpr followedBy() {
        return new PatternFollowedByExpr();
    }

    /**
     * Pattern every-operator and filter in combination, equivalent to the "every MyEvent" syntax.
     *
     * @param eventTypeName is the event type name to filter for
     * @return pattern expression
     */
    public static PatternEveryExpr everyFilter(String eventTypeName) {
        PatternExpr filter = new PatternFilterExpr(Filter.create(eventTypeName));
        return new PatternEveryExpr(filter);
    }

    /**
     * Pattern every-operator and filter in combination, equivalent to the "every tag=MyEvent" syntax.
     *
     * @param eventTypeName is the event type name to filter for
     * @param tagName       is the tag name to assign to matching events
     * @return pattern expression
     */
    public static PatternEveryExpr everyFilter(String eventTypeName, String tagName) {
        PatternExpr filter = new PatternFilterExpr(Filter.create(eventTypeName), tagName);
        return new PatternEveryExpr(filter);
    }

    /**
     * Pattern every-operator and filter in combination, equivalent to the "every MyEvent(vol &gt; 100)" syntax.
     *
     * @param filter specifies the event type name and filter expression to filter for
     * @return pattern expression
     */
    public static PatternEveryExpr everyFilter(Filter filter) {
        PatternExpr inner = new PatternFilterExpr(filter);
        return new PatternEveryExpr(inner);
    }

    /**
     * Pattern every-operator and filter in combination, equivalent to the "every tag=MyEvent(vol &gt; 100)" syntax.
     *
     * @param filter  specifies the event type name and filter expression to filter for
     * @param tagName is the tag name to assign to matching events
     * @return pattern expression
     */
    public static PatternEveryExpr everyFilter(Filter filter, String tagName) {
        PatternExpr inner = new PatternFilterExpr(filter, tagName);
        return new PatternEveryExpr(inner);
    }

    /**
     * Filter expression for use in patterns, equivalent to the simple "MyEvent" syntax.
     *
     * @param eventTypeName is the event type name of the events to filter for
     * @return pattern expression
     */
    public static PatternFilterExpr filter(String eventTypeName) {
        return new PatternFilterExpr(Filter.create(eventTypeName));
    }

    /**
     * Filter expression for use in patterns, equivalent to the simple "tag=MyEvent" syntax.
     *
     * @param eventTypeName is the event type name of the events to filter for
     * @param tagName       is the tag name to assign to matching events
     * @return pattern expression
     */
    public static PatternFilterExpr filter(String eventTypeName, String tagName) {
        return new PatternFilterExpr(Filter.create(eventTypeName), tagName);
    }

    /**
     * Filter expression for use in patterns, equivalent to the "MyEvent(vol &gt; 100)" syntax.
     *
     * @param filter specifies the event type name and filter expression to filter for
     * @return pattern expression
     */
    public static PatternFilterExpr filter(Filter filter) {
        return new PatternFilterExpr(filter);
    }

    /**
     * Filter expression for use in patterns, equivalent to the "tag=MyEvent(vol &gt; 100)" syntax.
     *
     * @param filter  specifies the event type name and filter expression to filter for
     * @param tagName is the tag name to assign to matching events
     * @return pattern expression
     */
    public static PatternFilterExpr filter(Filter filter, String tagName) {
        return new PatternFilterExpr(filter, tagName);
    }

    /**
     * Guard pattern expression guards a sub-expression, equivalent to the "every MyEvent where timer:within(1 sec)" syntax
     *
     * @param namespace  is the guard objects namespace, i.e. "timer"
     * @param name       is the guard objects name, i.e. ""within"
     * @param parameters is the guard objects optional parameters, i.e. integer 1 for 1 second
     * @param guarded    is the pattern sub-expression to be guarded
     * @return pattern guard expression
     */
    public static PatternGuardExpr guard(String namespace, String name, Expression[] parameters, PatternExpr guarded) {
        return new PatternGuardExpr(namespace, name, parameters, guarded);
    }

    /**
     * Observer pattern expression, equivalent to the "every timer:interval(1 sec)" syntax
     *
     * @param namespace  is the observer objects namespace, i.e. "timer"
     * @param name       is the observer objects name, i.e. ""within"
     * @param parameters is the observer objects optional parameters, i.e. integer 1 for 1 second
     * @return pattern observer expression
     */
    public static PatternObserverExpr observer(String namespace, String name, Expression[] parameters) {
        return new PatternObserverExpr(namespace, name, parameters);
    }

    /**
     * Timer-within guard expression.
     *
     * @param seconds is the number of seconds for the guard
     * @param guarded is the sub-expression to guard
     * @return pattern guard
     */
    public static PatternGuardExpr timerWithin(double seconds, PatternExpr guarded) {
        return new PatternGuardExpr("timer", "within", new Expression[]{Expressions.constant(seconds)}, guarded);
    }

    /**
     * While-guard expression.
     *
     * @param expression expression to evaluate against matches
     * @param guarded    is the sub-expression to guard
     * @return pattern guard
     */
    public static PatternGuardExpr whileGuard(PatternExpr guarded, Expression expression) {
        return new PatternGuardExpr(GuardEnum.WHILE_GUARD.getNamespace(), GuardEnum.WHILE_GUARD.getName(), new Expression[]{expression}, guarded);
    }

    /**
     * Timer-within-max guard expression.
     *
     * @param seconds is the number of seconds for the guard
     * @param max     the maximum number of invocations for the guard
     * @param guarded is the sub-expression to guard
     * @return pattern guard
     */
    public static PatternGuardExpr timerWithinMax(double seconds, int max, PatternExpr guarded) {
        return new PatternGuardExpr("timer", "withinmax", new Expression[]{Expressions.constant(seconds), Expressions.constant(max)}, guarded);
    }

    /**
     * Timer-interval observer expression.
     *
     * @param seconds is the number of seconds in the interval
     * @return pattern observer
     */
    public static PatternObserverExpr timerInterval(double seconds) {
        return new PatternObserverExpr("timer", "interval", new Expression[]{Expressions.constant(seconds)});
    }

    /**
     * Pattern not-operator and filter in combination, equivalent to the "not MyEvent" syntax.
     *
     * @param eventTypeName is the event type name to filter for
     * @return pattern expression
     */
    public static PatternNotExpr notFilter(String eventTypeName) {
        return new PatternNotExpr(new PatternFilterExpr(Filter.create(eventTypeName)));
    }

    /**
     * Pattern not-operator and filter in combination, equivalent to the "not tag=MyEvent" syntax.
     *
     * @param name    is the event type name to filter for
     * @param tagName is the tag name to assign to matching events
     * @return pattern expression
     */
    public static PatternNotExpr notFilter(String name, String tagName) {
        return new PatternNotExpr(new PatternFilterExpr(Filter.create(name), tagName));
    }

    /**
     * Pattern not-operator and filter in combination, equivalent to the "not MyEvent(vol &gt; 100)" syntax.
     *
     * @param filter specifies the event type name and filter expression to filter for
     * @return pattern expression
     */
    public static PatternNotExpr notFilter(Filter filter) {
        return new PatternNotExpr(new PatternFilterExpr(filter));
    }

    /**
     * Pattern not-operator and filter in combination, equivalent to the "not tag=MyEvent(vol &gt; 100)" syntax.
     *
     * @param filter  specifies the event type name and filter expression to filter for
     * @param tagName is the tag name to assign to matching events
     * @return pattern expression
     */
    public static PatternNotExpr notFilter(Filter filter, String tagName) {
        return new PatternNotExpr(new PatternFilterExpr(filter, tagName));
    }

    /**
     * Not-keyword pattern expression flips the truth-value of the pattern sub-expression.
     *
     * @param subexpression is the expression whose truth value to flip
     * @return pattern expression
     */
    public static PatternNotExpr not(PatternExpr subexpression) {
        return new PatternNotExpr(subexpression);
    }

    /**
     * Match-until-pattern expression matches a certain number of occurances until a second expression becomes true.
     *
     * @param low   - low number of matches, or null if no lower boundary
     * @param high  - high number of matches, or null if no high boundary
     * @param match - the pattern expression that is sought to match repeatedly
     * @param until - the pattern expression that ends matching (optional, can be null)
     * @return pattern expression
     */
    public static PatternMatchUntilExpr matchUntil(Expression low, Expression high, PatternExpr match, PatternExpr until) {
        return new PatternMatchUntilExpr(low, high, match, until);
    }

    /**
     * Timer-at observer
     *
     * @param minutes     a single integer value supplying the minute to fire the timer, or null for any (wildcard) minute
     * @param hours       a single integer value supplying the hour to fire the timer, or null for any (wildcard) hour
     * @param daysOfMonth a single integer value supplying the day of the month to fire the timer, or null for any (wildcard) day of the month
     * @param month       a single integer value supplying the month to fire the timer, or null for any (wildcard) month
     * @param daysOfWeek  a single integer value supplying the days of the week to fire the timer, or null for any (wildcard) day of the week
     * @param seconds     a single integer value supplying the second to fire the timer, or null for any (wildcard) second
     * @return timer-at observer
     */
    public static PatternObserverExpr timerAt(Integer minutes, Integer hours, Integer daysOfMonth, Integer month, Integer daysOfWeek, Integer seconds) {
        Expression wildcard = new CrontabParameterExpression(ScheduleItemType.WILDCARD);

        List<Expression> parameters = new ArrayList<Expression>();
        parameters.add(minutes == null ? wildcard : Expressions.constant(minutes));
        parameters.add(hours == null ? wildcard : Expressions.constant(hours));
        parameters.add(daysOfMonth == null ? wildcard : Expressions.constant(daysOfMonth));
        parameters.add(month == null ? wildcard : Expressions.constant(month));
        parameters.add(daysOfWeek == null ? wildcard : Expressions.constant(daysOfWeek));
        parameters.add(seconds == null ? wildcard : Expressions.constant(seconds));
        return new PatternObserverExpr("timer", "at", parameters);
    }
}
