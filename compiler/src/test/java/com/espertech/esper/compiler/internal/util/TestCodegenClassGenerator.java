package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerByteCode;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import junit.framework.TestCase;

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class TestCodegenClassGenerator extends TestCase {
    public void testImports() {
        CodegenPackageScope packageScope = new CodegenPackageScope("somepkg", null, false, new ConfigurationCompilerByteCode());
        CodegenClassScope classScope = new CodegenClassScope(false, packageScope, null);
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenCtor ctor = new CodegenCtor(this.getClass(), true, Collections.emptyList());
        ctor.getBlock().declareVar(EPTypePremade.DATE.getEPType(), "utildate", constantNull());
        ctor.getBlock().declareVar(EPTypePremade.COLLECTION.getEPType(), "utilcoll", constantNull());
        ctor.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "anint", constant(0));
        CodegenClass clazz = new CodegenClass(CodegenClassType.STATEMENTFIELDS, null, "ABC", classScope, Collections.emptyList(), ctor, methods, Collections.emptyList());
        System.out.println(CodegenClassGenerator.compile(clazz));
    }
}
