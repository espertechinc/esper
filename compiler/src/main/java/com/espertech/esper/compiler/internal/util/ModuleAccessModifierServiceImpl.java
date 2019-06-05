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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.annotation.BusEventType;
import com.espertech.esper.common.client.annotation.Private;
import com.espertech.esper.common.client.annotation.Protected;
import com.espertech.esper.common.client.annotation.Public;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerByteCode;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.ModuleAccessModifierService;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.option.*;

import java.lang.annotation.Annotation;
import java.util.function.Function;

public class ModuleAccessModifierServiceImpl implements ModuleAccessModifierService {
    private final CompilerOptions options;
    private final ConfigurationCompilerByteCode config;

    ModuleAccessModifierServiceImpl(CompilerOptions options, ConfigurationCompilerByteCode config) {
        this.options = options;
        this.config = config;
    }

    public NameAccessModifier getAccessModifierEventType(StatementRawInfo raw, String eventTypeName) {
        return getModifier(raw.getAnnotations(),
            opts -> opts.getAccessModifierEventType() == null ? null : opts.getAccessModifierEventType().getValue(new AccessModifierEventTypeContext(raw, eventTypeName)),
            ConfigurationCompilerByteCode::getAccessModifierEventType);
    }

    public NameAccessModifier getAccessModifierVariable(StatementBaseInfo base, String variableName) {
        return getModifier(base.getStatementRawInfo().getAnnotations(),
            opts -> opts.getAccessModifierVariable() == null ? null : opts.getAccessModifierVariable().getValue(new AccessModifierVariableContext(base, variableName)),
            ConfigurationCompilerByteCode::getAccessModifierVariable);
    }

    public NameAccessModifier getAccessModifierContext(StatementBaseInfo base, String contextName) {
        return getModifier(base.getStatementRawInfo().getAnnotations(),
            opts -> opts.getAccessModifierContext() == null ? null : opts.getAccessModifierContext().getValue(new AccessModifierContextContext(base, contextName)),
            ConfigurationCompilerByteCode::getAccessModifierContext);
    }

    public NameAccessModifier getAccessModifierExpression(StatementBaseInfo base, String expressionName) {
        return getModifier(base.getStatementRawInfo().getAnnotations(),
            opts -> opts.getAccessModifierExpression() == null ? null : opts.getAccessModifierExpression().getValue(new AccessModifierExpressionContext(base, expressionName)),
            ConfigurationCompilerByteCode::getAccessModifierExpression);
    }

    public NameAccessModifier getAccessModifierTable(StatementBaseInfo base, String tableName) {
        return getModifier(base.getStatementRawInfo().getAnnotations(),
            opts -> opts.getAccessModifierTable() == null ? null : opts.getAccessModifierTable().getValue(new AccessModifierTableContext(base, tableName)),
            ConfigurationCompilerByteCode::getAccessModifierTable);
    }

    public NameAccessModifier getAccessModifierNamedWindow(StatementBaseInfo base, String namedWindowName) {
        return getModifier(base.getStatementRawInfo().getAnnotations(),
            opts -> opts.getAccessModifierNamedWindow() == null ? null : opts.getAccessModifierNamedWindow().getValue(new AccessModifierNamedWindowContext(base, namedWindowName)),
            ConfigurationCompilerByteCode::getAccessModifierNamedWindow);
    }

    public NameAccessModifier getAccessModifierScript(StatementBaseInfo base, String scriptName, int numParameters) {
        return getModifier(base.getStatementRawInfo().getAnnotations(),
            opts -> opts.getAccessModifierScript() == null ? null : opts.getAccessModifierScript().getValue(new AccessModifierScriptContext(base, scriptName, numParameters)),
            ConfigurationCompilerByteCode::getAccessModifierScript);
    }

    public EventTypeBusModifier getBusModifierEventType(StatementRawInfo raw, String eventTypeName) {
        if (options.getBusModifierEventType() != null) {
            EventTypeBusModifier result = options.getBusModifierEventType().getValue(new BusModifierEventTypeContext(raw, eventTypeName));
            if (result != null) {
                return result;
            }
        }
        boolean busEventType = AnnotationUtil.hasAnnotation(raw.getAnnotations(), BusEventType.class);
        if (busEventType) {
            return EventTypeBusModifier.BUS;
        }
        return config.getBusModifierEventType();
    }

    private <T> NameAccessModifier getModifier(Annotation[] annotations,
                                               Function<CompilerOptions, NameAccessModifier> optionsGet,
                                               Function<ConfigurationCompilerByteCode, NameAccessModifier> configGet) {

        if (options != null) {
            NameAccessModifier result = optionsGet.apply(options);
            if (result != null) {
                return result;
            }
        }

        boolean isPrivate = AnnotationUtil.hasAnnotation(annotations, Private.class);
        boolean isProtected = AnnotationUtil.hasAnnotation(annotations, Protected.class);
        boolean isPublic = AnnotationUtil.hasAnnotation(annotations, Public.class);
        if (isPrivate) {
            if (isProtected) {
                throw new EPException("Encountered both the @private and the @protected annotation");
            }
            if (isPublic) {
                throw new EPException("Encountered both the @private and the @public annotation");
            }
        } else if (isProtected && isPublic) {
            throw new EPException("Encountered both the @protected and the @public annotation");
        }

        if (isPrivate) {
            return NameAccessModifier.PRIVATE;
        }
        if (isProtected) {
            return NameAccessModifier.PROTECTED;
        }
        if (isPublic) {
            return NameAccessModifier.PUBLIC;
        }

        return configGet.apply(config);
    }
}
