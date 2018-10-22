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

/**
 * Provides the environment to {@link AccessModifierTableOption}.
 */
public class AccessModifierTableContext extends StatementOptionContextBase {

    private final String tableName;

    /**
     * Ctor.
     *
     * @param base      statement info
     * @param tableName table name
     */
    public AccessModifierTableContext(StatementBaseInfo base, String tableName) {
        super(base);
        this.tableName = tableName;
    }

    /**
     * Returns the table name
     *
     * @return table name
     */
    public String getTableName() {
        return tableName;
    }
}
