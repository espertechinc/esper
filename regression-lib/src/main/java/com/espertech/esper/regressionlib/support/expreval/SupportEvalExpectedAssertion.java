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

import java.util.function.Consumer;

public class SupportEvalExpectedAssertion extends SupportEvalExpected {
    private final Consumer<Object> verifier;

    public SupportEvalExpectedAssertion(Consumer<Object> verifier) {
        this.verifier = verifier;
    }

    public void assertValue(String message, Object actual) {
        verifier.accept(actual);
    }
}
