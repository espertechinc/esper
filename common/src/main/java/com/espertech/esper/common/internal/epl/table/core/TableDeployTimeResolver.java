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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TableDeployTimeResolver {

    public static CodegenExpressionField makeTableEventToPublicField(TableMetaData table, CodegenClassScope classScope, Class generator) {
        SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
        CodegenMethod tableInit = classScope.getPackageScope().getInitMethod().makeChildWithScope(TableMetadataInternalEventToPublic.class, generator, symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
        CodegenExpression tableResolve = makeResolveTable(table, EPStatementInitServices.REF);
        tableInit.getBlock().methodReturn(exprDotMethod(tableResolve, "getEventToPublic"));
        return classScope.getPackageScope().addFieldUnshared(true, TableMetadataInternalEventToPublic.class, localMethod(tableInit, EPStatementInitServices.REF));
    }

    public static CodegenExpression makeResolveTable(TableMetaData table, CodegenExpression initSvc) {
        return staticMethod(TableDeployTimeResolver.class, "resolveTable",
                constant(table.getTableName()),
                constant(table.getTableVisibility()),
                constant(table.getTableModuleName()),
                initSvc);
    }

    public static Table resolveTable(String tableName,
                                     NameAccessModifier visibility,
                                     String optionalModuleName,
                                     EPStatementInitServices services) {
        String deploymentId = resolveDeploymentId(tableName, visibility, optionalModuleName, services);
        Table table = services.getTableManagementService().getTable(deploymentId, tableName);
        if (table == null) {
            throw new EPException("Failed to resolve table '" + tableName + "'");
        }
        return table;
    }

    private static String resolveDeploymentId(String tableName,
                                              NameAccessModifier visibility,
                                              String optionalModuleName,
                                              EPStatementInitServices services) {
        String deploymentId;
        if (visibility == NameAccessModifier.PRIVATE) {
            deploymentId = services.getDeploymentId();
        } else if (visibility == NameAccessModifier.PUBLIC || visibility == NameAccessModifier.PROTECTED) {
            deploymentId = services.getTablePathRegistry().getDeploymentId(tableName, optionalModuleName);
            if (deploymentId == null) {
                throw new EPException("Failed to resolve path table '" + tableName + "'");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized visibility " + visibility);
        }
        return deploymentId;
    }
}
