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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseElementWildcard;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;

import java.util.ArrayList;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Works in conjunction with {@link SelectExprProcessor} to present
 * a result as an object array for 'natural' delivery.
 */
public class BindProcessorForge {
    private ExprForge[] expressionForges;
    private Class[] expressionTypes;
    private String[] columnNamesAssigned;

    public BindProcessorForge(SelectExprProcessorForge synthetic, SelectClauseElementCompiled[] selectionList,
                              EventType[] typesPerStream,
                              String[] streamNames,
                              TableCompileTimeResolver tableService)
            throws ExprValidationException {
        ArrayList<ExprForge> expressions = new ArrayList<>();
        ArrayList<Class> types = new ArrayList<Class>();
        ArrayList<String> columnNames = new ArrayList<String>();

        for (SelectClauseElementCompiled element : selectionList) {
            // handle wildcards by outputting each stream's underlying event
            if (element instanceof SelectClauseElementWildcard) {
                for (int i = 0; i < typesPerStream.length; i++) {
                    Class returnType = typesPerStream[i].getUnderlyingType();
                    TableMetaData tableMetadata = tableService.resolveTableFromEventType(typesPerStream[i]);
                    ExprForge forge;
                    if (tableMetadata != null) {
                        forge = new BindProcessorStreamTable(i, returnType, tableMetadata);
                    } else {
                        forge = new BindProcessorStream(i, returnType);
                    }
                    expressions.add(forge);
                    types.add(returnType);
                    columnNames.add(streamNames[i]);
                }
            } else if (element instanceof SelectClauseStreamCompiledSpec) {
                // handle stream wildcards by outputting the stream underlying event
                final SelectClauseStreamCompiledSpec streamSpec = (SelectClauseStreamCompiledSpec) element;
                EventType type = typesPerStream[streamSpec.getStreamNumber()];
                final Class returnType = type.getUnderlyingType();

                TableMetaData tableMetadata = tableService.resolveTableFromEventType(type);
                ExprForge forge;
                if (tableMetadata != null) {
                    forge = new BindProcessorStreamTable(streamSpec.getStreamNumber(), returnType, tableMetadata);
                } else {
                    forge = new BindProcessorStream(streamSpec.getStreamNumber(), returnType);
                }
                expressions.add(forge);
                types.add(returnType);
                columnNames.add(streamNames[streamSpec.getStreamNumber()]);
            } else if (element instanceof SelectClauseExprCompiledSpec) {
                // handle expressions
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) element;
                ExprForge forge = expr.getSelectExpression().getForge();
                expressions.add(forge);
                types.add(forge.getEvaluationType());
                if (expr.getAssignedName() != null) {
                    columnNames.add(expr.getAssignedName());
                } else {
                    columnNames.add(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expr.getSelectExpression()));
                }
            } else {
                throw new IllegalStateException("Unrecognized select expression element of type " + element.getClass());
            }
        }

        expressionForges = expressions.toArray(new ExprForge[expressions.size()]);
        expressionTypes = types.toArray(new Class[types.size()]);
        columnNamesAssigned = columnNames.toArray(new String[columnNames.size()]);
    }

    public ExprForge[] getExpressionForges() {
        return expressionForges;
    }

    public Class[] getExpressionTypes() {
        return expressionTypes;
    }

    public String[] getColumnNamesAssigned() {
        return columnNamesAssigned;
    }

    public CodegenMethod processCodegen(CodegenMethod processMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = processMethod.makeChild(Object[].class, this.getClass(), codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[].class, "parameters", newArrayByLength(Object.class, constant(expressionForges.length)));
        for (int i = 0; i < expressionForges.length; i++) {
            block.assignArrayElement("parameters", constant(i), CodegenLegoMayVoid.expressionMayVoid(Object.class, expressionForges[i], methodNode, exprSymbol, codegenClassScope));
        }
        block.methodReturn(ref("parameters"));
        return methodNode;
    }

}
