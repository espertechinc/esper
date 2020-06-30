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
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StmtClassForgeableMultiKeySerde implements StmtClassForgeable {

    private static final String OBJECT_NAME = "obj";
    private static final String OUTPUT_NAME = "output";
    private static final String INPUT_NAME = "input";
    private static final String UNITKEY_NAME = "unitKey";
    private static final String WRITER_NAME = "writer";

    private final String className;
    private final CodegenPackageScope packageScope;
    private final EPType[] types;
    private final String classNameMK;
    private final DataInputOutputSerdeForge[] forges;

    public StmtClassForgeableMultiKeySerde(String className, CodegenPackageScope packageScope, EPType[] types, String classNameMK, DataInputOutputSerdeForge[] forges) {
        this.className = className;
        this.packageScope = packageScope;
        this.types = types;
        this.classNameMK = classNameMK;
        this.forges = forges;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        CodegenMethod writeMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), StmtClassForgeableMultiKeySerde.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.OBJECT.getEPType(), OBJECT_NAME)
                .addParam(EPTypePremade.DATAOUTPUT.getEPType(), OUTPUT_NAME)
                .addParam(EPTypePremade.BYTEPRIMITIVEARRAY.getEPType(), UNITKEY_NAME)
                .addParam(EventBeanCollatedWriter.EPTYPE, WRITER_NAME)
                .addThrown(EPTypePremade.IOEXCEPTION.getEPType());
        if (!fireAndForget) {
            makeWriteMethod(writeMethod);
        }
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);

        CodegenMethod readMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), StmtClassForgeableMultiKeySerde.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EPTypePremade.DATAINPUT.getEPType(), INPUT_NAME)
                .addParam(EPTypePremade.BYTEPRIMITIVEARRAY.getEPType(), UNITKEY_NAME)
                .addThrown(EPTypePremade.IOEXCEPTION.getEPType());
        if (!fireAndForget) {
            makeReadMethod(readMethod);
        } else {
            readMethod.getBlock().methodReturn(constantNull());
        }
        CodegenStackGenerator.recursiveBuildStack(readMethod, "read", methods);

        List<CodegenTypedParam> members = new ArrayList<>();
        for (int i = 0; i < forges.length; i++) {
            members.add(new CodegenTypedParam(forges[i].forgeClassName(), "s" + i));
        }

        CodegenCtor providerCtor = new CodegenCtor(this.getClass(), includeDebugSymbols, Collections.emptyList());
        for (int i = 0; i < forges.length; i++) {
            providerCtor.getBlock().assignRef("s" + i, forges[i].codegen(providerCtor, classScope, null));
        }

        return new CodegenClass(CodegenClassType.KEYPROVISIONINGSERDE, DataInputOutputSerde.EPTYPE, className, classScope, members, providerCtor, methods, Collections.emptyList());
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.MULTIKEY;
    }

    private void makeWriteMethod(CodegenMethod writeMethod) {
        writeMethod.getBlock().declareVar(classNameMK, "key", cast(classNameMK, ref(OBJECT_NAME)));
        for (int i = 0; i < types.length; i++) {
            CodegenExpressionRef key = ref("key.k" + i);
            CodegenExpression serde = ref("s" + i);
            writeMethod.getBlock().exprDotMethod(serde, "write", key, ref(OUTPUT_NAME), ref(UNITKEY_NAME), ref(WRITER_NAME));
        }
    }

    private void makeReadMethod(CodegenMethod readMethod) {
        CodegenExpression[] params = new CodegenExpression[types.length];
        for (int i = 0; i < types.length; i++) {
            CodegenExpression serde = ref("s" + i);
            EPType boxed = JavaClassHelper.getBoxedType(types[i]);
            CodegenExpression read = exprDotMethod(serde, "read", ref(INPUT_NAME), ref(UNITKEY_NAME));
            if (boxed != EPTypeNull.INSTANCE) {
                params[i] = cast((EPTypeClass) boxed, read);
            } else {
                params[i] = read;
            }
        }
        readMethod.getBlock().methodReturn(newInstance(classNameMK, params));
    }
}
