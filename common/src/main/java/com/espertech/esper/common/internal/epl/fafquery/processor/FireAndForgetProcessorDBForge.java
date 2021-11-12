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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.hook.type.SQLColumnTypeConversion;
import com.espertech.esper.common.client.hook.type.SQLOutputRowConversion;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage1.spec.DBStatementStreamSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.historical.database.core.HistoricalEventViewableDatabaseForge;
import com.espertech.esper.common.internal.epl.historical.database.core.HistoricalEventViewableDatabaseForgeFactory;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;

import java.lang.annotation.Annotation;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class FireAndForgetProcessorDBForge implements FireAndForgetProcessorForge {

    private final DBStatementStreamSpec sqlStreamSpec;
    private final HistoricalEventViewableDatabaseForge dbAccessForge;

    public FireAndForgetProcessorDBForge(DBStatementStreamSpec sqlStreamSpec, StatementSpecCompiled statementSpec, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        this.sqlStreamSpec = sqlStreamSpec;

        Annotation[] annotations = raw.getAnnotations();
        SQLColumnTypeConversion typeConversionHook = (SQLColumnTypeConversion) ClasspathImportUtil.getAnnotationHook(annotations, HookType.SQLCOL, SQLColumnTypeConversion.class, services.getClasspathImportServiceCompileTime());
        SQLOutputRowConversion outputRowConversionHook = (SQLOutputRowConversion) ClasspathImportUtil.getAnnotationHook(annotations, HookType.SQLROW, SQLOutputRowConversion.class, services.getClasspathImportServiceCompileTime());
        dbAccessForge = HistoricalEventViewableDatabaseForgeFactory.createDBStatementView(0, sqlStreamSpec, typeConversionHook, outputRowConversionHook, raw, services);
        dbAccessForge.validate(new StreamTypeServiceImpl(true), statementSpec.getRaw().getSqlParameters(), raw, services);
    }

    public String getProcessorName() {
        return sqlStreamSpec.getDatabaseName();
    }

    public String getContextName() {
        return null;
    }

    public EventType getEventTypeRSPInputEvents() {
        return dbAccessForge.getEventType();
    }

    public EventType getEventTypePublic() {
        return getEventTypeRSPInputEvents();
    }

    public String[][] getUniqueIndexes() {
        return new String[0][];
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FireAndForgetProcessorDB.EPTYPE, this.getClass(), classScope);
        CodegenExpression factory = dbAccessForge.make(method, symbols, classScope);
        CodegenExpressionRef db = ref("db");
        method.getBlock()
                .declareVarNewInstance(FireAndForgetProcessorDB.EPTYPE, db.getRef())
                .exprDotMethod(db, "setFactory", factory)
                .methodReturn(db);
        return localMethod(method);
    }
}
