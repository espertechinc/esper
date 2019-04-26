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
package com.espertech.esper.common.internal.compile.multikey;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class MultiKeyClassRefUUIDBased implements MultiKeyClassRef {
    private final String uuid;
    private final Class[] mkTypes;
    private String classPostfix;

    public MultiKeyClassRefUUIDBased(Class[] mkTypes) {
        uuid = CodeGenerationIDGenerator.generateClassNameUUID();
        this.mkTypes = mkTypes;
    }

    public String getClassNameMK(String classPostfix) {
        assignPostfix(classPostfix);
        return CodeGenerationIDGenerator.generateClassNameWithUUID(HashableMultiKey.class, classPostfix, uuid);
    }

    public String getClassNameMK() {
        checkClassPostfix();
        return getClassNameMK(classPostfix);
    }

    public String getClassNameMKSerde(String classPostfix) {
        return CodeGenerationIDGenerator.generateClassNameWithUUID(DataInputOutputSerde.class, classPostfix, uuid);
    }

    public CodegenExpression getExprMKSerde(CodegenMethod method, CodegenClassScope classScope) {
        checkClassPostfix();
        return newInstance(getClassNameMKSerde(classPostfix));
    }

    public Class[] getMKTypes() {
        return mkTypes;
    }

    public String toString() {
        return "MultiKeyClassRefUUIDBased{" +
            "uuid='" + uuid + '\'' +
            ", mkTypes=" + Arrays.toString(mkTypes) +
            ", classPostfix='" + classPostfix + '\'' +
            '}';
    }

    private void checkClassPostfix() {
        if (classPostfix == null) {
            throw new IllegalArgumentException("Class postfix has not been assigned");
        }
    }

    private void assignPostfix(String classPostfix) {
        if (this.classPostfix == null) {
            this.classPostfix = classPostfix;
            return;
        }
        if (!this.classPostfix.equals(classPostfix)) {
            throw new IllegalArgumentException("Invalid class postfix");
        }
    }
}
