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
package com.espertech.esper.avro.selectexprrep;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.StringWriter;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.staticMethod;

public class SelectExprProcessorEvalAvroMapToAvro implements ExprEvaluator, ExprForge, ExprNodeRenderable {

    private final ExprForge forge;
    private final Schema inner;
    private ExprEvaluator eval;

    public SelectExprProcessorEvalAvroMapToAvro(ExprForge forge, Schema schema, String columnName) {
        this.forge = forge;
        this.inner = schema.getField(columnName).schema();
        if (!(inner.getType() == Schema.Type.RECORD)) {
            throw new IllegalStateException("Column '" + columnName + "' is not a record but schema " + inner);
        }
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Map<String, Object> map = (Map<String, Object>) eval.evaluate(eventsPerStream, isNewData, context);
        return selectExprProcessAvroMap(map, inner);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember memberSchema = codegenClassScope.makeAddMember(Schema.class, inner);
        return staticMethod(SelectExprProcessorEvalAvroMapToAvro.class, "selectExprProcessAvroMap", forge.evaluateCodegen(requiredType, codegenMethodScope, exprSymbol, codegenClassScope), CodegenExpressionBuilder.member(memberSchema.getMemberId()));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param map map
     * @param inner inner
     * @return record
     */
    public static Object selectExprProcessAvroMap(Map<String, Object> map, Schema inner) {
        if (map == null) {
            return null;
        }
        GenericData.Record record = new GenericData.Record(inner);
        for (Map.Entry<String, Object> row : map.entrySet()) {
            record.put(row.getKey(), row.getValue());
        }
        return record;
    }

    public ExprEvaluator getExprEvaluator() {
        if (eval == null) {
            eval = forge.getExprEvaluator();
        }
        return this;
    }

    public Class getEvaluationType() {
        return GenericData.Record.class;
    }

    public ExprForgeComplexityEnum getComplexity() {
        return ExprForgeComplexityEnum.INTER;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
