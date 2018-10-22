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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.PatternStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootFactoryNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternContext;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.map.MapEventType;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ViewableActivatorPatternForge implements ViewableActivatorForge {
    private final EventType eventType;
    private final PatternStreamSpecCompiled spec;
    private final PatternContext patternContext;
    private final boolean isCanIterate;

    public ViewableActivatorPatternForge(EventType eventType, PatternStreamSpecCompiled spec, PatternContext patternContext, boolean isCanIterate) {
        this.eventType = eventType;
        this.spec = spec;
        this.patternContext = patternContext;
        this.isCanIterate = isCanIterate;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ViewableActivator.class, ViewableActivatorPatternForge.class, classScope);

        CodegenMethod childCode = spec.getRoot().makeCodegen(method, symbols, classScope);
        method.getBlock()
                .declareVar(EvalRootFactoryNode.class, "root", localMethod(childCode))
                .declareVar(ViewableActivatorPattern.class, "activator", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETVIEWABLEACTIVATORFACTORY).add("createPattern"))
                .exprDotMethod(ref("activator"), "setRootFactoryNode", ref("root"))
                .exprDotMethod(ref("activator"), "setEventBeanTypedEventFactory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETEVENTBEANTYPEDEVENTFACTORY))
                .declareVar(EventType.class, "eventType", EventTypeUtility.resolveTypeCodegen(eventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("activator"), "setEventType", ref("eventType"))
                .exprDotMethod(ref("activator"), "setPatternContext", patternContext.make(method, symbols, classScope))
                .exprDotMethod(ref("activator"), "setHasConsumingFilter", constant(spec.isConsumingFilters()))
                .exprDotMethod(ref("activator"), "setSuppressSameEventMatches", constant(spec.isSuppressSameEventMatches()))
                .exprDotMethod(ref("activator"), "setDiscardPartialsOnMatch", constant(spec.isDiscardPartialsOnMatch()))
                .exprDotMethod(ref("activator"), "setCanIterate", constant(isCanIterate))
                .methodReturn(ref("activator"));

        return localMethod(method);
    }

    public static MapEventType makeRegisterPatternType(StatementBaseInfo base, int stream, PatternStreamSpecCompiled patternStreamSpec, StatementCompileTimeServices services) {
        String patternEventTypeName = services.getEventTypeNameGeneratorStatement().getPatternTypeName(stream);
        EventTypeMetadata metadata = new EventTypeMetadata(patternEventTypeName, base.getModuleName(), EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, NameAccessModifier.PRIVATE, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        Map<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Pair<EventType, String>> entry : patternStreamSpec.getTaggedEventTypes().entrySet()) {
            propertyTypes.put(entry.getKey(), entry.getValue().getFirst());
        }
        for (Map.Entry<String, Pair<EventType, String>> entry : patternStreamSpec.getArrayEventTypes().entrySet()) {
            propertyTypes.put(entry.getKey(), new EventType[]{entry.getValue().getFirst()});
        }
        MapEventType patternType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, propertyTypes, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(patternType);
        return patternType;
    }
}
