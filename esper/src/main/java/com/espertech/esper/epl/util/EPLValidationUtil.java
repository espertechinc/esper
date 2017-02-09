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
package com.espertech.esper.epl.util;

import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.table.mgmt.TableService;

public class EPLValidationUtil {

    public static void validateTableExists(TableService tableService, String name) throws ExprValidationException {
        if (tableService.getTableMetadata(name) != null) {
            throw new ExprValidationException("A table by name '" + name + "' already exists");
        }
    }

    public static void validateContextName(boolean table, String tableOrNamedWindowName, String tableOrNamedWindowContextName, String optionalContextName, boolean mustMatchContext)
            throws ExprValidationException {
        if (tableOrNamedWindowContextName != null) {
            if (optionalContextName == null || !optionalContextName.equals(tableOrNamedWindowContextName)) {
                throw getCtxMessage(table, tableOrNamedWindowName, tableOrNamedWindowContextName);
            }
        } else {
            if (mustMatchContext && optionalContextName != null) {
                throw getCtxMessage(table, tableOrNamedWindowName, tableOrNamedWindowContextName);
            }
        }
    }

    private static ExprValidationException getCtxMessage(boolean table, String tableOrNamedWindowName, String tableOrNamedWindowContextName) {
        String prefix = table ? "Table" : "Named window";
        return new ExprValidationException(prefix + " by name '" + tableOrNamedWindowName + "' has been declared for context '" + tableOrNamedWindowContextName + "' and can only be used within the same context");
    }
}
