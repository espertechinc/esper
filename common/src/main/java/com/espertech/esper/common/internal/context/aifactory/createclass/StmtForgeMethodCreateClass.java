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
package com.espertech.esper.common.internal.context.aifactory.createclass;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.context.aifactory.core.StmtForgeMethodCreateSimpleBase;
import com.espertech.esper.common.internal.context.aifactory.core.StmtForgeMethodRegisterResult;
import com.espertech.esper.common.internal.epl.classprovided.compiletime.ClassProvidedPrecompileResult;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.fabric.FabricCharge;

public class StmtForgeMethodCreateClass extends StmtForgeMethodCreateSimpleBase {

    private final ClassProvidedPrecompileResult classProvidedPrecompileResult;
    private final String className;

    public StmtForgeMethodCreateClass(StatementBaseInfo base, ClassProvidedPrecompileResult classProvidedPrecompileResult, String className) {
        super(base);
        this.classProvidedPrecompileResult = classProvidedPrecompileResult;
        this.className = className;
    }

    protected StmtForgeMethodRegisterResult register(StatementCompileTimeServices services) throws ExprValidationException {
        if (services.getClassProvidedCompileTimeResolver().resolveClass(className) != null) {
            throw new ExprValidationException("Class '" + className + "' has already been declared");
        }
        ClassProvided classProvided = new ClassProvided(classProvidedPrecompileResult.getBytes(), className);
        NameAccessModifier visibility = services.getModuleVisibilityRules().getAccessModifierExpression(base, className);
        classProvided.setModuleName(base.getModuleName());
        classProvided.setVisibility(visibility);
        classProvided.loadClasses(services.getParentClassLoader());
        services.getClassProvidedCompileTimeRegistry().newClass(classProvided);

        FabricCharge fabricCharge = services.getStateMgmtSettingsProvider().newCharge();
        services.getStateMgmtSettingsProvider().inlinedClasses(fabricCharge, classProvided);
        return new StmtForgeMethodRegisterResult(className, fabricCharge);
    }

    protected StmtClassForgeable aiFactoryForgable(String className, CodegenPackageScope packageScope, EventType statementEventType, String objectName) {
        StatementAgentInstanceFactoryCreateClassForge forge = new StatementAgentInstanceFactoryCreateClassForge(statementEventType, className);
        return new StmtClassForgeableAIFactoryProviderCreateClass(className, packageScope, forge);
    }
}
