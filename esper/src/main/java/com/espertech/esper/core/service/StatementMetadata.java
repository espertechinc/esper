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
package com.espertech.esper.core.service;

import java.io.Serializable;

/**
 * Statement metadata.
 */
public class StatementMetadata implements Serializable {
    private static final long serialVersionUID = -484497485008513239L;

    private StatementType statementType;

    /**
     * Ctor.
     *
     * @param statementType the type of statement
     */
    public StatementMetadata(StatementType statementType) {
        this.statementType = statementType;
    }

    /**
     * Returns the statement type.
     *
     * @return statement type.
     */
    public StatementType getStatementType() {
        return statementType;
    }
}
