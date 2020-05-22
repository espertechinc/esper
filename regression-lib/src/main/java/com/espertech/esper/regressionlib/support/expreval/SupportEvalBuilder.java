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
package com.espertech.esper.regressionlib.support.expreval;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

public class SupportEvalBuilder {
    private final String eventType;
    private final String streamAlias;
    private RegressionPath path;
    private LinkedHashMap<String, String> expressions = new LinkedHashMap<>();
    private List<SupportEvalAssertionPair> assertions = new ArrayList<>();
    private Consumer<EPStatement> statementConsumer;
    private Integer exludeAssertionsExcept;
    private String exludeNamesExcept;
    private boolean excludeEPLAssertion;
    private boolean logging;

    public SupportEvalBuilder(String eventType) {
        this(eventType, null);
    }

    public SupportEvalBuilder(String eventType, String streamAlias) {
        this.eventType = eventType;
        this.streamAlias = streamAlias;
    }

    public SupportEvalBuilder statementConsumer(Consumer<EPStatement> statementConsumer) {
        this.statementConsumer = statementConsumer;
        return this;
    }

    public SupportEvalBuilder expression(String name, String expression) {
        if (expressions.containsKey(name)) {
            throw new IllegalArgumentException("Expression '" + name + "' already provided");
        }
        this.expressions.put(name, expression);
        return this;
    }

    public SupportEvalBuilder expressions(String[] names, String... expressions) {
        if (names.length != expressions.length) {
            throw new IllegalArgumentException("Names length and expressions length differ");
        }
        for (int i = 0; i < names.length; i++) {
            expression(names[i], expressions[i]);
        }
        return this;
    }

    public SupportEvalAssertionBuilder assertion(Object underlying) {
        SupportEvalAssertionBuilder builder = new SupportEvalAssertionBuilder(this);
        assertions.add(new SupportEvalAssertionPair(underlying, builder));
        return builder;
    }

    public SupportEvalBuilder withPath(RegressionPath path) {
        this.path = path;
        return this;
    }

    public LinkedHashMap<String, String> getExpressions() {
        return expressions;
    }

    public String getEventType() {
        return eventType;
    }

    public List<SupportEvalAssertionPair> getAssertions() {
        return assertions;
    }

    public Consumer<EPStatement> getStatementConsumer() {
        return statementConsumer;
    }

    public String getStreamAlias() {
        return streamAlias;
    }

    public RegressionPath getPath() {
        return path;
    }

    public Integer getExludeAssertionsExcept() {
        return exludeAssertionsExcept;
    }

    public String getExludeNamesExcept() {
        return exludeNamesExcept;
    }

    public boolean isLogging() {
        return logging;
    }

    public boolean isExcludeEPLAssertion() {
        return excludeEPLAssertion;
    }

    public void run(RegressionEnvironment environment) {
        run(environment, false);
    }

    public void run(RegressionEnvironment environment, boolean soda) {
        SupportEvalRunner.run(environment, soda, this);
    }

    public SupportEvalBuilder setExludeAssertionsExcept(int included) {
        this.exludeAssertionsExcept = included;
        return this;
    }

    public SupportEvalBuilder setExcludeNamesExcept(String name) {
        this.exludeNamesExcept = name;
        return this;
    }

    public SupportEvalBuilder setLogging(boolean flag) {
        this.logging = flag;
        return this;
    }

    public SupportEvalBuilder setExcludeEPLAssertion(boolean flag) {
        this.excludeEPLAssertion = flag;
        return this;
    }
}
