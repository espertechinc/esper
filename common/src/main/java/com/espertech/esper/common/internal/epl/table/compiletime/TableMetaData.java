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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.Copyable;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TableMetaData implements Copyable<TableMetaData> {
    private String tableName;
    private String tableModuleName;
    private NameAccessModifier tableVisibility;
    private String optionalContextName;
    private NameAccessModifier optionalContextVisibility;
    private String optionalContextModule;
    private EventType internalEventType;
    private EventType publicEventType;
    private String[] keyColumns;
    private Class[] keyTypes;
    private int[] keyColNums;
    private Map<String, TableMetadataColumn> columns;
    private int numMethodAggs;
    private IndexMultiKey keyIndexMultiKey;
    private EventTableIndexMetadata indexMetadata = new EventTableIndexMetadata();

    public TableMetaData() {
    }

    public TableMetaData(String tableName, String tableModuleName, NameAccessModifier tableVisibility, String optionalContextName, NameAccessModifier optionalContextVisibility, String optionalContextModule, EventType internalEventType, EventType publicEventType, String[] keyColumns, Class[] keyTypes, int[] keyColNums, Map<String, TableMetadataColumn> columns, int numMethodAggs) {
        this.tableName = tableName;
        this.tableModuleName = tableModuleName;
        this.tableVisibility = tableVisibility;
        this.optionalContextName = optionalContextName;
        this.optionalContextVisibility = optionalContextVisibility;
        this.optionalContextModule = optionalContextModule;
        this.internalEventType = internalEventType;
        this.publicEventType = publicEventType;
        this.keyColumns = keyColumns;
        this.keyTypes = keyTypes;
        this.columns = columns;
        this.keyColNums = keyColNums;
        this.numMethodAggs = numMethodAggs;
        init();
    }

    private TableMetaData(String tableName, String tableModuleName, NameAccessModifier tableVisibility, String optionalContextName, NameAccessModifier optionalContextVisibility, String optionalContextModule, EventType internalEventType, EventType publicEventType, String[] keyColumns, Class[] keyTypes, int[] keyColNums, Map<String, TableMetadataColumn> columns, int numMethodAggs, IndexMultiKey keyIndexMultiKey, EventTableIndexMetadata indexMetadata) {
        this.tableName = tableName;
        this.tableModuleName = tableModuleName;
        this.tableVisibility = tableVisibility;
        this.optionalContextName = optionalContextName;
        this.optionalContextVisibility = optionalContextVisibility;
        this.optionalContextModule = optionalContextModule;
        this.internalEventType = internalEventType;
        this.publicEventType = publicEventType;
        this.keyColumns = keyColumns;
        this.keyTypes = keyTypes;
        this.keyColNums = keyColNums;
        this.columns = columns;
        this.numMethodAggs = numMethodAggs;
        this.keyIndexMultiKey = keyIndexMultiKey;
        this.indexMetadata = indexMetadata;
    }

    public TableMetaData copy() {
        return new TableMetaData(tableName, tableModuleName, tableVisibility, optionalContextName, optionalContextVisibility, optionalContextModule,
            internalEventType, publicEventType, keyColumns, keyTypes, keyColNums, columns, numMethodAggs, keyIndexMultiKey, indexMetadata.copy());
    }

    public void init() {
        // add index multi-key for implicit primary-key index
        if (keyColumns == null || keyColumns.length == 0) {
            return;
        }
        IndexedPropDesc[] props = new IndexedPropDesc[keyColumns.length];
        for (int i = 0; i < props.length; i++) {
            props[i] = new IndexedPropDesc(keyColumns[i], keyTypes[i]);
        }
        keyIndexMultiKey = new IndexMultiKey(true, props, new IndexedPropDesc[0], null);
        try {
            indexMetadata.addIndexExplicit(true, keyIndexMultiKey, tableName, tableModuleName, null, "");
        } catch (ExprValidationException e) {
            throw new EPException("Failed to add primary key index: " + e.getMessage(), e);
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TableMetaData.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(TableMetaData.class, "meta", newInstance(TableMetaData.class))
            .exprDotMethod(ref("meta"), "setTableName", constant(tableName))
            .exprDotMethod(ref("meta"), "setTableModuleName", constant(tableModuleName))
            .exprDotMethod(ref("meta"), "setTableVisibility", constant(tableVisibility))
            .exprDotMethod(ref("meta"), "setOptionalContextName", constant(optionalContextName))
            .exprDotMethod(ref("meta"), "setOptionalContextVisibility", constant(optionalContextVisibility))
            .exprDotMethod(ref("meta"), "setOptionalContextModule", constant(optionalContextModule))
            .exprDotMethod(ref("meta"), "setInternalEventType", EventTypeUtility.resolveTypeCodegen(internalEventType, symbols.getAddInitSvc(method)))
            .exprDotMethod(ref("meta"), "setPublicEventType", EventTypeUtility.resolveTypeCodegen(publicEventType, symbols.getAddInitSvc(method)))
            .exprDotMethod(ref("meta"), "setKeyColumns", constant(keyColumns))
            .exprDotMethod(ref("meta"), "setKeyTypes", constant(keyTypes))
            .exprDotMethod(ref("meta"), "setKeyColNums", constant(keyColNums))
            .exprDotMethod(ref("meta"), "setColumns", TableMetadataColumn.makeColumns(columns, method, symbols, classScope))
            .exprDotMethod(ref("meta"), "setNumMethodAggs", constant(numMethodAggs))
            .exprDotMethod(ref("meta"), "init")
            .methodReturn(ref("meta"));
        return localMethod(method);
    }

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        return newInstance(TableMetaData.class, constant(tableName),
            constant(optionalContextName), constant(optionalContextVisibility), constant(optionalContextModule),
            EventTypeUtility.resolveTypeCodegen(internalEventType, addInitSvc),
            EventTypeUtility.resolveTypeCodegen(publicEventType, addInitSvc));
    }

    public EventType getInternalEventType() {
        return internalEventType;
    }

    public EventType getPublicEventType() {
        return publicEventType;
    }

    public String getTableName() {
        return tableName;
    }

    public String getOptionalContextName() {
        return optionalContextName;
    }

    public NameAccessModifier getOptionalContextVisibility() {
        return optionalContextVisibility;
    }

    public String getOptionalContextModule() {
        return optionalContextModule;
    }

    public Class[] getKeyTypes() {
        return keyTypes;
    }

    public Map<String, TableMetadataColumn> getColumns() {
        return columns;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setOptionalContextName(String optionalContextName) {
        this.optionalContextName = optionalContextName;
    }

    public void setOptionalContextVisibility(NameAccessModifier optionalContextVisibility) {
        this.optionalContextVisibility = optionalContextVisibility;
    }

    public void setOptionalContextModule(String optionalContextModule) {
        this.optionalContextModule = optionalContextModule;
    }

    public void setInternalEventType(EventType internalEventType) {
        this.internalEventType = internalEventType;
    }

    public void setPublicEventType(EventType publicEventType) {
        this.publicEventType = publicEventType;
    }

    public void setKeyTypes(Class[] keyTypes) {
        this.keyTypes = keyTypes;
    }

    public void setColumns(Map<String, TableMetadataColumn> columns) {
        this.columns = columns;
    }

    public void setNumMethodAggs(int numMethodAggs) {
        this.numMethodAggs = numMethodAggs;
    }

    public int getNumMethodAggs() {
        return numMethodAggs;
    }

    public void setKeyColumns(String[] keyColumns) {
        this.keyColumns = keyColumns;
    }

    public String[] getKeyColumns() {
        return keyColumns;
    }

    public Set<String> getUniquenessAsSet() {
        if (keyColumns == null || keyColumns.length == 0) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(keyColumns));
    }

    public EventTableIndexMetadata getIndexMetadata() {
        return indexMetadata;
    }

    public boolean isKeyed() {
        return keyTypes != null && keyTypes.length > 0;
    }

    public int[] getKeyColNums() {
        return keyColNums;
    }

    public void setKeyColNums(int[] keyColNums) {
        this.keyColNums = keyColNums;
    }

    public IndexMultiKey getKeyIndexMultiKey() {
        return keyIndexMultiKey;
    }

    public String getTableModuleName() {
        return tableModuleName;
    }

    public void setTableModuleName(String tableModuleName) {
        this.tableModuleName = tableModuleName;
    }

    public NameAccessModifier getTableVisibility() {
        return tableVisibility;
    }

    public void setTableVisibility(NameAccessModifier tableVisibility) {
        this.tableVisibility = tableVisibility;
    }

    public void setKeyIndexMultiKey(IndexMultiKey keyIndexMultiKey) {
        this.keyIndexMultiKey = keyIndexMultiKey;
    }

    public void setIndexMetadata(EventTableIndexMetadata indexMetadata) {
        this.indexMetadata = indexMetadata;
    }

    public void addIndex(String indexName, String indexModuleName, IndexMultiKey imk, QueryPlanIndexItem indexItem) throws ExprValidationException {
        indexMetadata.addIndexExplicit(false, imk, indexName, indexModuleName, indexItem, "");
    }
}
