/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.variable;

import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;

public class VariableServiceUtil {

    public static String getAssigmentExMessage(String variableName, Class variableType, Class initValueClass) {
        return "Variable '" + variableName
                + "' of declared type " + JavaClassHelper.getClassNameFullyQualPretty(variableType) +
                " cannot be assigned a value of type " + JavaClassHelper.getClassNameFullyQualPretty(initValueClass);
    }

    public static String checkVariableContextName(String optionalStatementContextName, VariableMetaData variableMetaData) {
        if (optionalStatementContextName == null) {
            if (variableMetaData.getContextPartitionName() != null) {
                return "Variable '" + variableMetaData.getVariableName() + "' defined for use with context '" + variableMetaData.getContextPartitionName() + "' can only be accessed within that context";
            }
        }
        else {
            if (variableMetaData.getContextPartitionName() != null &&
                !variableMetaData.getContextPartitionName().equals(optionalStatementContextName)) {
                return "Variable '" + variableMetaData.getVariableName() + "' defined for use with context '" + variableMetaData.getContextPartitionName() + "' is not available for use with context '" + optionalStatementContextName + "'";
            }
        }
        return null;
    }

    public static String checkVariableContextName(ContextDescriptor contextDescriptor, VariableMetaData variableMetaData) {
        if (contextDescriptor == null) {
            return checkVariableContextName((String) null, variableMetaData);
        }
        return checkVariableContextName(contextDescriptor.getContextName(), variableMetaData);
    }

    public static void checkAlreadyDeclaredVariable(String variableName, VariableService variableService)
            throws ExprValidationException
    {
        if (variableService.getVariableMetaData(variableName) != null) {
            throw new ExprValidationException(getAlreadyDeclaredEx(variableName));
        }
    }

    public static void checkAlreadyDeclaredTable(String variableName, TableService tableService)
            throws ExprValidationException
    {
        if (tableService.getTableMetadata(variableName) != null) {
            throw new ExprValidationException(getAlreadyDeclaredEx(variableName));
        }
    }

    public static String getAlreadyDeclaredEx(String variableName) {
        return "Variable by name '" + variableName + "' has already been created";
    }
}
