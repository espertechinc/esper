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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SupportEvalAssertionBuilder {
    private final SupportEvalBuilder builder;
    private final Map<String, SupportEvalExpected> results = new HashMap<>();

    public SupportEvalAssertionBuilder(SupportEvalBuilder builder) {
        this.builder = builder;
    }

    public SupportEvalAssertionBuilder expect(String name, Object result) {
        verifyExpect(name);
        results.put(name, new SupportEvalExpectedObject(result));
        return this;
    }

    public SupportEvalAssertionBuilder verify(String name, Consumer<Object> verifier) {
        verifyExpect(name);
        results.put(name, new SupportEvalExpectedAssertion(verifier));
        return this;
    }

    public SupportEvalAssertionBuilder expect(String[] names, Object... results) {
        if (results == null) {
            throw new IllegalArgumentException("Expected result array, for 'null' use 'new Object[] {null}'");
        }
        if (names.length != results.length) {
            throw new IllegalArgumentException("Names length and results length differ");
        }
        for (int i = 0; i < names.length; i++) {
            expect(names[i], results[i]);
        }
        return this;
    }

    public Map<String, SupportEvalExpected> getResults() {
        return results;
    }

    private void verifyExpect(String name) {
        if (!builder.getExpressions().containsKey(name)) {
            throw new IllegalArgumentException("No expression for name '" + name + "'");
        }
        if (results.containsKey(name)) {
            throw new IllegalArgumentException("Already have result for name '" + name + "'");
        }
    }
}
