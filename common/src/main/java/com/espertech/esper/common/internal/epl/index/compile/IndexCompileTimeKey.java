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

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class IndexCompileTimeKey {
    private final String infraModuleName;
    private final String infraName;
    private final NameAccessModifier visibility;
    private final boolean namedWindow;
    private final String indexName;
    private final String indexModuleName;

    public IndexCompileTimeKey(String infraModuleName, String infraName, NameAccessModifier visibility, boolean namedWindow, String indexName, String indexModuleName) {
        this.infraModuleName = infraModuleName;
        this.infraName = infraName;
        this.visibility = visibility;
        this.namedWindow = namedWindow;
        this.indexName = indexName;
        this.indexModuleName = indexModuleName;
    }

    public String getInfraModuleName() {
        return infraModuleName;
    }

    public String getInfraName() {
        return infraName;
    }

    public NameAccessModifier getVisibility() {
        return visibility;
    }

    public boolean isNamedWindow() {
        return namedWindow;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexModuleName() {
        return indexModuleName;
    }

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        return newInstance(IndexCompileTimeKey.class, constant(infraModuleName), constant(infraName), constant(visibility), constant(namedWindow), constant(indexName), constant(indexModuleName));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexCompileTimeKey that = (IndexCompileTimeKey) o;

        if (namedWindow != that.namedWindow) return false;
        if (infraModuleName != null ? !infraModuleName.equals(that.infraModuleName) : that.infraModuleName != null)
            return false;
        if (!infraName.equals(that.infraName)) return false;
        if (visibility != that.visibility) return false;
        if (!indexName.equals(that.indexName)) return false;
        return indexModuleName != null ? indexModuleName.equals(that.indexModuleName) : that.indexModuleName == null;
    }

    public int hashCode() {
        int result = infraModuleName != null ? infraModuleName.hashCode() : 0;
        result = 31 * result + infraName.hashCode();
        result = 31 * result + visibility.hashCode();
        result = 31 * result + (namedWindow ? 1 : 0);
        result = 31 * result + indexName.hashCode();
        result = 31 * result + (indexModuleName != null ? indexModuleName.hashCode() : 0);
        return result;
    }
}
