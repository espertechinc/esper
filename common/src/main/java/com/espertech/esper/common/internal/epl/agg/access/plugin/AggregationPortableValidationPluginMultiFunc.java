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
package com.espertech.esper.common.internal.epl.agg.access.plugin;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.hook.aggmultifunc.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationPortableValidationPluginMultiFunc implements AggregationPortableValidation {
    private String aggregationFunctionName;
    private AggregationMultiFunctionHandler handler;

    public void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, tableExpression, intoTableAgg, intoExpression);
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationPortableValidationPluginMultiFunc.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationPortableValidationPluginMultiFunc.class, "portable", newInstance(AggregationPortableValidationPluginMultiFunc.class))
                .exprDotMethod(ref("portable"), "setAggregationFunctionName", constant(aggregationFunctionName))
                .methodReturn(ref("portable"));
        return localMethod(method);
    }

    public boolean isAggregationMethod(String name, ExprNode[] parameters, ExprValidationContext validationContext) {
        // reconstitute handler, the handler is null when the table was not declared in the same EPL and comes from path
        if (handler == null) {
            ConfigurationCompilerPlugInAggregationMultiFunction config = validationContext.getClasspathImportService().resolveAggregationMultiFunction(aggregationFunctionName);
            if (config == null) {
                return false;
            }
            AggregationMultiFunctionForge forge = (AggregationMultiFunctionForge) JavaClassHelper.instantiate(AggregationMultiFunctionForge.class, config.getMultiFunctionForgeClassName(), validationContext.getClasspathImportService().getClassForNameProvider());
            AggregationMultiFunctionValidationContext ctx = new AggregationMultiFunctionValidationContext(name, validationContext.getStreamTypeService().getEventTypes(), parameters, validationContext.getStatementName(), validationContext, config, null, null, null);
            handler = forge.validateGetHandler(ctx);
        }
        return handler.getAggregationMethodMode(new AggregationMultiFunctionAggregationMethodContext(name, parameters, validationContext)) != null;
    }

    public AggregationMultiFunctionMethodDesc validateAggregationMethod(ExprValidationContext validationContext, String aggMethodName, ExprNode[] params) throws ExprValidationException {
        // child node validation
        ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, params, validationContext);

        // set of reader
        EPType epType = handler.getReturnType();
        Class returnType = EPTypeHelper.getNormalizedClass(epType);
        AggregationMethodForgePlugIn forge = new AggregationMethodForgePlugIn(returnType, (AggregationMultiFunctionAggregationMethodModeManaged) handler.getAggregationMethodMode(new AggregationMultiFunctionAggregationMethodContext(aggMethodName, params, validationContext)));
        EventType eventTypeCollection = EPTypeHelper.optionalIsEventTypeColl(epType);
        EventType eventTypeSingle = EPTypeHelper.optionalIsEventTypeSingle(epType);
        Class componentTypeCollection = EPTypeHelper.optionalIsComponentTypeColl(epType);
        return new AggregationMultiFunctionMethodDesc(forge, eventTypeCollection, componentTypeCollection, eventTypeSingle);
    }

    public String getAggregationFunctionName() {
        return aggregationFunctionName;
    }

    public void setAggregationFunctionName(String aggregationFunctionName) {
        this.aggregationFunctionName = aggregationFunctionName;
    }

    public AggregationMultiFunctionHandler getHandler() {
        return handler;
    }

    public void setHandler(AggregationMultiFunctionHandler handler) {
        this.handler = handler;
    }
}
