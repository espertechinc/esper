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
package com.espertech.esper.common.internal.compile.stage1.specmapper;

import com.espertech.esper.common.client.soda.EPStatementObjectModel;

/**
 * Return result for unmap operators unmapping an intermal statement representation to the SODA object model.
 */
public class StatementSpecUnMapResult {
    private final EPStatementObjectModel objectModel;

    /**
     * Ctor.
     *
     * @param objectModel of the statement
     */
    public StatementSpecUnMapResult(EPStatementObjectModel objectModel) {
        this.objectModel = objectModel;
    }

    /**
     * Returns the object model.
     *
     * @return object model
     */
    public EPStatementObjectModel getObjectModel() {
        return objectModel;
    }
}
