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
import com.espertech.esper.common.client.serde.MultiKeyGeneratedSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableType;
import com.espertech.esper.common.internal.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StmtClassForgableMultiKeySerde implements StmtClassForgable {

    private static final String MK_NAME = "mk";
    private static final String SERDES_NAME = "serdes";
    private static final String OUTPUT_NAME = "output";
    private static final String INPUT_NAME = "input";
    private static final String UNITKEY_NAME = "unitKey";
    private static final String WRITER_NAME = "writer";

    private final String className;
    private final CodegenPackageScope packageScope;
    private final Class[] types;
    private final String classNameMK;

    public StmtClassForgableMultiKeySerde(String className, CodegenPackageScope packageScope, Class[] types, String classNameMK) {
        this.className = className;
        this.packageScope = packageScope;
        this.types = types;
        this.classNameMK = classNameMK;
    }

    public CodegenClass forge(boolean includeDebugSymbols) {
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        CodegenMethod writeMethod = CodegenMethod.makeParentNode(void.class, StmtClassForgableMultiKeySerde.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(Object.class, MK_NAME)
            .addParam(DataInputOutputSerde[].class, SERDES_NAME)
            .addParam(DataOutput.class, OUTPUT_NAME)
            .addParam(byte[].class, UNITKEY_NAME)
            .addParam(EventBeanCollatedWriter.class, WRITER_NAME)
            .addThrown(IOException.class);
        makeWriteMethod(writeMethod);
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);

        CodegenMethod readMethod = CodegenMethod.makeParentNode(Object.class, StmtClassForgableMultiKeySerde.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(DataInputOutputSerde[].class, SERDES_NAME)
            .addParam(DataInput.class, INPUT_NAME)
            .addParam(byte[].class, UNITKEY_NAME)
            .addThrown(IOException.class);
        makeReadMethod(readMethod);
        CodegenStackGenerator.recursiveBuildStack(readMethod, "read", methods);

        return new CodegenClass(CodegenClassType.KEYPROVISIONINGSERDE, MultiKeyGeneratedSerde.class, className, classScope, Collections.emptyList(), null, methods, Collections.emptyList());
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgableType getForgableType() {
        return StmtClassForgableType.MULTIKEY;
    }

    private void makeWriteMethod(CodegenMethod writeMethod) {
        writeMethod.getBlock().declareVar(classNameMK, "key", cast(classNameMK, ref(MK_NAME)));
        for (int i = 0; i < types.length; i++) {
            CodegenExpressionRef key = ref("key.k" + i);
            CodegenExpression serde = arrayAtIndex(ref(SERDES_NAME), constant(i));
            writeMethod.getBlock().exprDotMethod(serde, "write", key, ref(OUTPUT_NAME), ref(UNITKEY_NAME), ref(WRITER_NAME));
        }
    }

    private void makeReadMethod(CodegenMethod readMethod) {
        CodegenExpression[] params = new CodegenExpression[types.length];
        for (int i = 0; i < types.length; i++) {
            CodegenExpression serde = arrayAtIndex(ref(SERDES_NAME), constant(i));
            params[i] = cast(JavaClassHelper.getBoxedType(types[i]), exprDotMethod(serde, "read", ref(INPUT_NAME), ref(UNITKEY_NAME)));
        }
        readMethod.getBlock().methodReturn(newInstance(classNameMK, params));
    }
}
