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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationValidationUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationPortableValidationPluginMultiFunc implements AggregationPortableValidation {
    public final static EPTypeClass EPTYPE = new EPTypeClass(AggregationPortableValidationPluginMultiFunc.class);

    private String aggregationFunctionName;
    private ConfigurationCompilerPlugInAggregationMultiFunction config;
    private AggregationMultiFunctionHandler handler;
    private boolean parametersValidated;

    public void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException {
        AggregationValidationUtil.validateAggregationType(this, tableExpression, intoTableAgg, intoExpression);
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationPortableValidationPluginMultiFunc.EPTYPE, this.getClass(), classScope);
        method.getBlock()
            .declareVarNewInstance(AggregationPortableValidationPluginMultiFunc.EPTYPE, "portable")
            .exprDotMethod(ref("portable"), "setAggregationFunctionName", constant(aggregationFunctionName))
            .exprDotMethod(ref("portable"), "setConfig", config == null ? constantNull() : config.toExpression())
            .methodReturn(ref("portable"));
        return localMethod(method);
    }

    public boolean isAggregationMethod(String name, ExprNode[] parameters, ExprValidationContext validationContext) throws ExprValidationException {
        // always obtain a new handler since the name may have changes
        Pair<ConfigurationCompilerPlugInAggregationMultiFunction, Class> configPair = validationContext.getClasspathImportService().resolveAggregationMultiFunction(aggregationFunctionName, validationContext.getClassProvidedClasspathExtension());
        if (configPair == null) {
            return false;
        }
        AggregationMultiFunctionForge forge;
        if (configPair.getSecond() != null) {
            forge = (AggregationMultiFunctionForge) JavaClassHelper.instantiate(AggregationMultiFunctionForge.class, configPair.getSecond());
        } else {
            forge = (AggregationMultiFunctionForge) JavaClassHelper.instantiate(AggregationMultiFunctionForge.class, configPair.getFirst().getMultiFunctionForgeClassName(), validationContext.getClasspathImportService().getClassForNameProvider());
        }

        validateParamsUnless(validationContext, parameters);

        AggregationMultiFunctionValidationContext ctx = new AggregationMultiFunctionValidationContext(name, validationContext.getStreamTypeService().getEventTypes(), parameters, validationContext.getStatementName(), validationContext, config, parameters, null);
        handler = forge.validateGetHandler(ctx);
        return handler.getAggregationMethodMode(new AggregationMultiFunctionAggregationMethodContext(name, parameters, validationContext)) != null;
    }

    public AggregationMultiFunctionMethodDesc validateAggregationMethod(ExprValidationContext validationContext, String aggMethodName, ExprNode[] params) throws ExprValidationException {
        validateParamsUnless(validationContext, params);

        // set of reader
        EPChainableType epType = handler.getReturnType();
        EPType returnType = EPChainableTypeHelper.getNormalizedEPType(epType);
        if (returnType == EPTypeNull.INSTANCE) {
            throw new ExprValidationException("Null-type value returned by aggregation function '" + aggMethodName + "' is not allowed");
        }
        AggregationMethodForgePlugIn forge = new AggregationMethodForgePlugIn((EPTypeClass) returnType, (AggregationMultiFunctionAggregationMethodModeManaged) handler.getAggregationMethodMode(new AggregationMultiFunctionAggregationMethodContext(aggMethodName, params, validationContext)));
        EventType eventTypeCollection = EPChainableTypeHelper.optionalIsEventTypeColl(epType);
        EventType eventTypeSingle = EPChainableTypeHelper.optionalIsEventTypeSingle(epType);
        EPTypeClass componentTypeCollection = EPChainableTypeHelper.getCollectionOrArrayComponentTypeOrNull(epType);
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

    public ConfigurationCompilerPlugInAggregationMultiFunction getConfig() {
        return config;
    }

    public void setConfig(ConfigurationCompilerPlugInAggregationMultiFunction config) {
        this.config = config;
    }

    private void validateParamsUnless(ExprValidationContext validationContext, ExprNode[] parameters) throws ExprValidationException {
        if (parametersValidated) {
            return;
        }
        ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, parameters, validationContext);
        parametersValidated = true;
    }
}
