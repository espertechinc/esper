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
package com.espertech.esper.common.internal.epl.table.compiletime;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;
import com.espertech.esper.common.internal.event.core.EventTypeNameUtil;

import java.util.Set;

public class TableCompileTimeResolverImpl implements TableCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final TableCompileTimeRegistry compileTimeRegistry;
    private final PathRegistry<String, TableMetaData> pathTables;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public TableCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, TableCompileTimeRegistry compileTimeRegistry, PathRegistry<String, TableMetaData> pathTables, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.compileTimeRegistry = compileTimeRegistry;
        this.pathTables = pathTables;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public TableMetaData resolveTableFromEventType(EventType containedType) {
        if (containedType != null && containedType.getMetadata().getTypeClass() == EventTypeTypeClass.TABLE_INTERNAL) {
            String tableName = EventTypeNameUtil.getTableNameFromInternalTypeName(containedType.getName());
            return resolve(tableName);
        }
        return null;
    }

    public TableMetaData resolve(String tableName) {
        TableMetaData metaData = compileTimeRegistry.getTable(tableName);
        if (metaData != null) {
            return metaData;
        }

        try {
            Pair<TableMetaData, String> data = pathTables.getAnyModuleExpectSingle(tableName, moduleUses);
            if (data != null) {
                if (!isFireAndForget && !NameAccessModifier.visible(data.getFirst().getTableVisibility(), data.getFirst().getTableModuleName(), moduleName)) {
                    return null;
                }

                moduleDependencies.addPathTable(tableName, data.getSecond());
                return data.getFirst();
            }
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.TABLE, tableName, e);
        }

        return null;
    }
}
