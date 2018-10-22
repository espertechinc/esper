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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeable;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class TableLookupIndexReqKey implements CodegenMakeable<SAIFFInitializeSymbol> {
    private final String indexName;
    private final String indexModuleName;
    private final String tableName;

    public TableLookupIndexReqKey(String indexName, String indexModuleName) {
        this(indexName, indexModuleName, null);
    }

    public TableLookupIndexReqKey(String indexName, String indexModuleName, String tableName) {
        this.indexName = indexName;
        this.indexModuleName = indexModuleName;
        this.tableName = tableName;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getIndexModuleName() {
        return indexModuleName;
    }

    public String toString() {
        if (tableName == null) {
            return indexName;
        } else {
            return "table '" + tableName + "' index '" + indexName + "'";
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(TableLookupIndexReqKey.class, constant(indexName), constant(indexModuleName), constant(tableName));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableLookupIndexReqKey that = (TableLookupIndexReqKey) o;

        if (!indexName.equals(that.indexName)) return false;
        if (indexModuleName != null ? !indexModuleName.equals(that.indexModuleName) : that.indexModuleName != null)
            return false;
        return tableName != null ? tableName.equals(that.tableName) : that.tableName == null;
    }

    public int hashCode() {
        int result = indexName.hashCode();
        result = 31 * result + (indexModuleName != null ? indexModuleName.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }
}
