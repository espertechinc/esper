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
package com.espertech.esper.common.internal.epl.index.compile;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import java.util.Map;

public class IndexCollectorCompileTime implements IndexCollector {
    private final Map<String, NamedWindowMetaData> moduleNamedWindows;
    private final Map<String, TableMetaData> moduleTables;
    private final PathRegistry<String, NamedWindowMetaData> pathNamedWindows;
    private final PathRegistry<String, TableMetaData> pathTables;

    public IndexCollectorCompileTime(Map<String, NamedWindowMetaData> moduleNamedWindows, Map<String, TableMetaData> moduleTables, PathRegistry<String, NamedWindowMetaData> pathNamedWindows, PathRegistry<String, TableMetaData> pathTables) {
        this.moduleNamedWindows = moduleNamedWindows;
        this.moduleTables = moduleTables;
        this.pathNamedWindows = pathNamedWindows;
        this.pathTables = pathTables;
    }

    public void registerIndex(IndexCompileTimeKey indexKey, IndexDetail indexDetail) {

        EventTableIndexMetadata indexMetadata = null;
        if (indexKey.isNamedWindow()) {
            NamedWindowMetaData localNamedWindow = moduleNamedWindows.get(indexKey.getInfraName());
            if (localNamedWindow != null) {
                indexMetadata = localNamedWindow.getIndexMetadata();
            } else {
                if (indexKey.getVisibility() == NameAccessModifier.PUBLIC) {
                    NamedWindowMetaData pathNamedWindow = pathNamedWindows.getWithModule(indexKey.getInfraName(), indexKey.getInfraModuleName());
                    if (pathNamedWindow != null) {
                        indexMetadata = pathNamedWindow.getIndexMetadata();
                    }
                }
            }
            if (indexMetadata == null) {
                throw new EPException("Failed to find named window '" + indexKey.getInfraName() + "'");
            }
        } else {
            TableMetaData localTable = moduleTables.get(indexKey.getInfraName());
            if (localTable != null) {
                indexMetadata = localTable.getIndexMetadata();
            } else {
                if (indexKey.getVisibility() == NameAccessModifier.PUBLIC) {
                    TableMetaData pathTable = pathTables.getWithModule(indexKey.getInfraName(), indexKey.getInfraModuleName());
                    if (pathTable != null) {
                        indexMetadata = pathTable.getIndexMetadata();
                    }
                }
            }
            if (indexMetadata == null) {
                throw new EPException("Failed to find table '" + indexKey.getInfraName() + "'");
            }
        }

        try {
            indexMetadata.addIndexExplicit(false, indexDetail.getIndexMultiKey(), indexKey.getIndexName(), indexKey.getInfraModuleName(), indexDetail.getQueryPlanIndexItem(), "");
        } catch (ExprValidationException ex) {
            throw new EPException(ex.getMessage(), ex);
        }
    }
}
