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
package com.espertech.esper.compiler.client.option;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

/**
 * Provides the environment to {@link StatementUserObjectOption}.
 */
public class StatementUserObjectContext extends StatementOptionContextBase {
    /**
     * Ctor.
     *
     * @param eplSupplier     epl supplier
     * @param statementName   statement name
     * @param moduleName      module name
     * @param annotations     annotations
     * @param statementNumber statement number
     */
    public StatementUserObjectContext(Supplier<String> eplSupplier, String statementName, String moduleName, Annotation[] annotations, int statementNumber) {
        super(eplSupplier, statementName, moduleName, annotations, statementNumber);
    }
}
