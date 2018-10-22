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
package com.espertech.esper.common.internal.epl.expression.declared.runtime;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.declared.core.ExprDeclaredCacheKeyGlobal;
import com.espertech.esper.common.internal.epl.expression.declared.core.ExprDeclaredCacheKeyLocalCodegenField;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExpressionDeployTimeResolver {

    public static CodegenExpressionField makeRuntimeCacheKeyField(ExpressionDeclItem expression, CodegenClassScope classScope, Class generator) {
        if (expression.getVisibility() == NameAccessModifier.TRANSIENT) {
            // for private expression that cache key is simply an Object shared by the name of the expression (fields are per-statement already so its safe)
            return classScope.getPackageScope().addOrGetFieldSharable(new ExprDeclaredCacheKeyLocalCodegenField(expression.getName()));
        }

        // global expressions need a cache key that derives from the deployment id of the expression and the expression name
        CodegenMethod keyInit = classScope.getPackageScope().getInitMethod().makeChild(ExprDeclaredCacheKeyGlobal.class, generator, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
        keyInit.getBlock().declareVar(String.class, "deploymentId", staticMethod(ExpressionDeployTimeResolver.class, "resolveDeploymentId",
                constant(expression.getName()), constant(expression.getVisibility()), constant(expression.getModuleName()),
                EPStatementInitServices.REF))
                .methodReturn(newInstance(ExprDeclaredCacheKeyGlobal.class, ref("deploymentId"), constant(expression.getName())));
        return classScope.getPackageScope().addFieldUnshared(true, ExprDeclaredCacheKeyGlobal.class, localMethod(keyInit, EPStatementInitServices.REF));
    }


    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param expressionName     name
     * @param visibility         visibility
     * @param optionalModuleName module name
     * @param services           services
     * @return deployment id
     */
    public static String resolveDeploymentId(String expressionName,
                                             NameAccessModifier visibility,
                                             String optionalModuleName,
                                             EPStatementInitServices services) {
        String deploymentId;
        if (visibility == NameAccessModifier.PRECONFIGURED) {
            deploymentId = null;
        } else if (visibility == NameAccessModifier.PRIVATE) {
            deploymentId = services.getDeploymentId();
        } else if (visibility == NameAccessModifier.PUBLIC) {
            deploymentId = services.getExprDeclaredPathRegistry().getDeploymentId(expressionName, optionalModuleName);
            if (deploymentId == null) {
                throw new EPException("Failed to resolve path expression '" + expressionName + "'");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized visibility " + visibility);
        }
        return deploymentId;
    }
}
