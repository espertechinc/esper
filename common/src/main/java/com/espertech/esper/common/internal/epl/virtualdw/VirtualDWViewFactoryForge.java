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
package com.espertech.esper.common.internal.epl.virtualdw;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.client.hook.vdw.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SerializerUtil;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class VirtualDWViewFactoryForge implements ViewFactoryForge, DataWindowViewForge {
    private final VirtualDataWindowForge forge;
    private final String namedWindowName;
    private final Serializable customConfigs;

    private List<ExprNode> parameters;
    private ViewForgeEnv viewForgeEnv;
    private int streamNumber;
    ExprNode[] validatedParameterExpressions;
    private EventType parentEventType;
    private Object[] parameterValues;

    public VirtualDWViewFactoryForge(Class clazz, String namedWindowName, Serializable customConfigs) {
        if (!JavaClassHelper.isImplementsInterface(clazz, VirtualDataWindowForge.class)) {
            throw new ViewProcessingException("Virtual data window forge class " + clazz.getName() + " does not implement the interface " + VirtualDataWindowForge.class.getName());
        }
        this.forge = (VirtualDataWindowForge) JavaClassHelper.instantiate(VirtualDataWindowForge.class, clazz);
        this.namedWindowName = namedWindowName;
        this.customConfigs = customConfigs;
    }

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.parameters = parameters;
        this.viewForgeEnv = viewForgeEnv;
        this.streamNumber = streamNumber;
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        this.parentEventType = parentEventType;

        validatedParameterExpressions = ViewForgeSupport.validate(getViewName(), parentEventType, parameters, true, viewForgeEnv, streamNumber);
        parameterValues = new Object[validatedParameterExpressions.length];
        for (int i = 0; i < validatedParameterExpressions.length; i++) {
            try {
                parameterValues[i] = ViewForgeSupport.evaluateAssertNoProperties(getViewName(), validatedParameterExpressions[i], i);
            } catch (Exception ex) {
                // expected
            }
        }

        // initialize
        try {
            forge.initialize(new VirtualDataWindowForgeContext(parentEventType, parameterValues, validatedParameterExpressions, namedWindowName, viewForgeEnv, customConfigs));
        } catch (RuntimeException ex) {
            throw new ViewParameterException("Validation exception initializing virtual data window '" + namedWindowName + "': " + ex.getMessage(), ex);
        }
    }

    public EventType getEventType() {
        return parentEventType;
    }

    public String getViewName() {
        return "virtual-data-window";
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        VirtualDataWindowFactoryMode mode = forge.getFactoryMode();
        if (!(mode instanceof VirtualDataWindowFactoryModeManaged)) {
            throw new IllegalArgumentException("Unexpected factory mode " + mode);
        }

        VirtualDataWindowFactoryModeManaged managed = (VirtualDataWindowFactoryModeManaged) mode;
        InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) managed.getInjectionStrategyFactoryFactory();
        CodegenExpressionField factoryField = classScope.addFieldUnshared(true, VirtualDataWindowFactoryFactory.class, injectionStrategy.getInitializationExpression(classScope));

        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(VirtualDWViewFactory.class, this.getClass(), "factory", parent, symbols, classScope);
        builder.eventtype("eventType", parentEventType)
                .expression("factory", exprDotMethod(factoryField, "createFactory", newInstance(VirtualDataWindowFactoryFactoryContext.class)))
                .constant("parameters", parameterValues)
                .expression("parameterExpressions", ExprNodeUtilityCodegen.codegenEvaluators(validatedParameterExpressions, builder.getMethod(), this.getClass(), classScope))
                .constant("namedWindowName", namedWindowName)
                .expression("compileTimeConfiguration", SerializerUtil.expressionForUserObject(customConfigs));
        return builder.build();
    }

    public Set<String> getUniqueKeys() {
        return forge.getUniqueKeyPropertyNames();
    }
}
