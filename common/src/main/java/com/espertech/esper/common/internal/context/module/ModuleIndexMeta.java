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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Collection;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ModuleIndexMeta {
    public final static ModuleIndexMeta[] EMPTY_ARRAY = new ModuleIndexMeta[0];

    private final boolean namedWindow;
    private final String infraName;
    private final String infraModuleName;
    private final String indexName;
    private final String indexModuleName;

    public ModuleIndexMeta(boolean namedWindow, String infraName, String infraModuleName, String indexName, String indexModuleName) {
        this.namedWindow = namedWindow;
        this.infraName = infraName;
        this.infraModuleName = infraModuleName;
        this.indexName = indexName;
        this.indexModuleName = indexModuleName;
    }

    public boolean isNamedWindow() {
        return namedWindow;
    }

    public String getInfraName() {
        return infraName;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getInfraModuleName() {
        return infraModuleName;
    }

    public String getIndexModuleName() {
        return indexModuleName;
    }

    public static CodegenExpression makeArray(Collection<ModuleIndexMeta> names) {
        if (names.isEmpty()) {
            return enumValue(ModuleIndexMeta.class, "EMPTY_ARRAY");
        }
        CodegenExpression[] expressions = new CodegenExpression[names.size()];
        int count = 0;
        for (ModuleIndexMeta entry : names) {
            expressions[count++] = entry.make();
        }
        return newArrayWithInit(ModuleIndexMeta.class, expressions);
    }

    private CodegenExpression make() {
        return newInstance(ModuleIndexMeta.class, constant(namedWindow), constant(infraName), constant(infraModuleName), constant(indexName), constant(indexModuleName));
    }

    public static ModuleIndexMeta[] toArray(Set<ModuleIndexMeta> moduleIndexes) {
        if (moduleIndexes.isEmpty()) {
            return EMPTY_ARRAY;
        }
        return moduleIndexes.toArray(new ModuleIndexMeta[moduleIndexes.size()]);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModuleIndexMeta that = (ModuleIndexMeta) o;

        if (namedWindow != that.namedWindow) return false;
        if (infraName != null ? !infraName.equals(that.infraName) : that.infraName != null) return false;
        if (infraModuleName != null ? !infraModuleName.equals(that.infraModuleName) : that.infraModuleName != null)
            return false;
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        return indexModuleName != null ? indexModuleName.equals(that.indexModuleName) : that.indexModuleName == null;
    }

    public int hashCode() {
        int result = namedWindow ? 1 : 0;
        result = 31 * result + (infraName != null ? infraName.hashCode() : 0);
        result = 31 * result + (infraModuleName != null ? infraModuleName.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        result = 31 * result + (indexModuleName != null ? indexModuleName.hashCode() : 0);
        return result;
    }
}
