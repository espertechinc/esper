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

import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

/**
 * Base class providing statement information for compiler options.
 */
public abstract class StatementOptionContextBase {

    private final Supplier<String> eplSupplier;
    private final String statementName;
    private final String moduleName;
    private final Annotation[] annotations;
    private final int statementNumber;

    /**
     * Ctor.
     *
     * @param base statement info
     */
    StatementOptionContextBase(StatementBaseInfo base) {
        this.eplSupplier = () -> base.getCompilable().toEPL();
        this.statementName = base.getStatementName();
        this.moduleName = base.getModuleName();
        this.annotations = base.getStatementRawInfo().getAnnotations();
        this.statementNumber = base.getStatementNumber();
    }

    /**
     * Ctor.
     *
     * @param eplSupplier     epl supplier
     * @param statementName   statement name
     * @param moduleName      module name
     * @param annotations     annotations
     * @param statementNumber statement number
     */
    public StatementOptionContextBase(Supplier<String> eplSupplier, String statementName, String moduleName, Annotation[] annotations, int statementNumber) {
        this.eplSupplier = eplSupplier;
        this.statementName = statementName;
        this.moduleName = moduleName;
        this.annotations = annotations;
        this.statementNumber = statementNumber;
    }

    /**
     * Returns the supplier of the EPL textual representation
     *
     * @return epl supplier
     */
    public Supplier<String> getEplSupplier() {
        return eplSupplier;
    }

    /**
     * Returns the statement name
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the module name
     *
     * @return module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns the annotations
     *
     * @return annotations
     */
    public Annotation[] getAnnotations() {
        return annotations;
    }

    /**
     * Returns the statement number
     *
     * @return statement number
     */
    public int getStatementNumber() {
        return statementNumber;
    }
}
