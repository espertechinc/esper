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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnMergeHelperForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.util.UuidGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodIUDInsertIntoForge extends FAFQueryMethodIUDBaseForge {
    private final static int MAX_MULTIROW = 1000;

    private SelectExprProcessorForge[] insertHelpers;

    public FAFQueryMethodIUDInsertIntoForge(StatementSpecCompiled specCompiled, Compilable compilable, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        super(associatedFromClause(specCompiled, services), compilable, statementRawInfo, services);
    }

    protected void initExec(String aliasName, StatementSpecCompiled spec, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {

        StreamTypeService streamTypeService = new StreamTypeServiceImpl(true);

        // assign names
        ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, statementRawInfo, services)
                .withAllowBindingConsumption(true).build();

        // determine whether column names are provided
        // if the "values" keyword was used, allow sequential automatic name assignment
        String[] assignedSequentialNames = null;
        if (spec.getRaw().getInsertIntoDesc().getColumnNames().isEmpty()) {
            FireAndForgetSpecInsert insert = (FireAndForgetSpecInsert) spec.getRaw().getFireAndForgetSpec();
            if (insert.isUseValuesKeyword()) {
                assignedSequentialNames = processor.getEventTypePublic().getPropertyNames();
            }
        }
        if (spec.getRaw().getInsertIntoDesc().getEventPrecedence() != null) {
            throw new ExprValidationException("Fire-and-forget insert-queries do not allow event-precedence");
        }

        FireAndForgetSpecInsert insertSpec = (FireAndForgetSpecInsert) spec.getRaw().getFireAndForgetSpec();
        if (insertSpec.getMultirow().isEmpty()) {
            List<SelectClauseElementCompiled> selectNoWildcard = InfraOnMergeHelperForge.compileSelectNoWildcard(UuidGenerator.generate(), Arrays.asList(spec.getSelectClauseCompiled().getSelectExprList()));
            SelectExprProcessorForge insert = initExecRow(selectNoWildcard, spec, assignedSequentialNames, validationContext, streamTypeService, statementRawInfo, services);
            insertHelpers = new SelectExprProcessorForge[]{insert};
        } else {
            int numRows = insertSpec.getMultirow().size();
            if (numRows > MAX_MULTIROW) {
                throw new ExprValidationException("Insert-into number-of-rows exceeds the maximum of " + MAX_MULTIROW + " rows as the query provides " + numRows + " rows");
            }
            int count = 0;
            insertHelpers = new SelectExprProcessorForge[insertSpec.getMultirow().size()];
            for (List<ExprNode> row : insertSpec.getMultirow()) {
                List<SelectClauseElementCompiled> selected = new ArrayList<>(row.size());
                for (ExprNode expr : row) {
                    selected.add(new SelectClauseExprCompiledSpec(expr, null, null, false));
                }
                try {
                    insertHelpers[count++] = initExecRow(selected, spec, assignedSequentialNames, validationContext, streamTypeService, statementRawInfo, services);
                } catch (ExprValidationException ex) {
                    if (insertSpec.getMultirow().size() == 1) {
                        throw ex;
                    }
                    throw new ExprValidationException("Failed to validate multi-row insert at row " + count + " of " + insertHelpers.length + ": " + ex.getMessage(), ex);
                }
            }
        }
    }

    private SelectExprProcessorForge initExecRow(List<SelectClauseElementCompiled> select, StatementSpecCompiled spec, String[] assignedSequentialNames, ExprValidationContext validationContext, StreamTypeService streamTypeService, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        int count = -1;
        for (SelectClauseElementCompiled compiled : select) {
            count++;
            if (compiled instanceof SelectClauseExprCompiledSpec) {
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) compiled;
                ExprNode validatedExpression = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SELECT, expr.getSelectExpression(), validationContext);
                expr.setSelectExpression(validatedExpression);
                if (expr.getAssignedName() == null) {
                    if (expr.getProvidedName() == null) {
                        if (assignedSequentialNames != null && count < assignedSequentialNames.length) {
                            expr.setAssignedName(assignedSequentialNames[count]);
                        } else {
                            expr.setAssignedName(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(expr.getSelectExpression()));
                        }
                    } else {
                        expr.setAssignedName(expr.getProvidedName());
                    }
                }
            }
        }
        SelectClauseElementCompiled[] selected = select.toArray(new SelectClauseElementCompiled[0]);

        EventType optionalInsertIntoEventType = processor.getEventTypeRSPInputEvents();
        SelectProcessorArgs args = new SelectProcessorArgs(selected, null,
                false, optionalInsertIntoEventType, null, streamTypeService,
            true, spec.getAnnotations(), statementRawInfo, services);
        return SelectExprProcessorFactory.getProcessor(args, spec.getRaw().getInsertIntoDesc(), false).getForge();
    }

    protected EPTypeClass typeOfMethod() {
        return FAFQueryMethodIUDInsertInto.EPTYPE;
    }

    protected void makeInlineSpecificSetter(CodegenExpressionRef queryMethod, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock().declareVar(SelectExprProcessor.EPTYPEARRAY, "helpers", newArrayByLength(SelectExprProcessor.EPTYPE, constant(insertHelpers.length)));
        for (int i = 0; i < insertHelpers.length; i++) {
            CodegenExpressionNewAnonymousClass select = SelectExprProcessorUtil.makeAnonymous(insertHelpers[i], method, symbols.getAddInitSvc(method), classScope);
            method.getBlock().assignArrayElement("helpers", constant(i), select);
        }
        method.getBlock().exprDotMethod(queryMethod, "setInsertHelpers", ref("helpers"));
    }

    private static StatementSpecCompiled associatedFromClause(StatementSpecCompiled statementSpec, StatementCompileTimeServices services) throws ExprValidationException {
        StatementSpecRaw raw = statementSpec.getRaw();
        if (raw.getWhereClause() != null ||
                statementSpec.getStreamSpecs().length > 0 ||
                raw.getHavingClause() != null ||
                raw.getOutputLimitSpec() != null ||
                raw.getForClauseSpec() != null ||
                raw.getMatchRecognizeSpec() != null ||
                (raw.getOrderByList() != null && !raw.getOrderByList().isEmpty()) ||
                raw.getRowLimitSpec() != null) {
            throw new ExprValidationException("Insert-into fire-and-forget query can only consist of an insert-into clause and a select-clause");
        }

        String infraName = statementSpec.getRaw().getInsertIntoDesc().getEventTypeName();
        NamedWindowMetaData namedWindow = services.getNamedWindowCompileTimeResolver().resolve(infraName);
        TableMetaData table = services.getTableCompileTimeResolver().resolve(infraName);
        if (namedWindow == null && table == null) {
            throw new ExprValidationException("Failed to find named window or table '" + infraName + "'");
        }

        StreamSpecCompiled stream;
        if (namedWindow != null) {
            stream = new NamedWindowConsumerStreamSpec(namedWindow, null, new ViewSpec[0], Collections.<ExprNode>emptyList(), StreamSpecOptions.DEFAULT, null);
        } else {
            stream = new TableQueryStreamSpec(null, new ViewSpec[0], StreamSpecOptions.DEFAULT, table, Collections.<ExprNode>emptyList());
        }
        return new StatementSpecCompiled(statementSpec, new StreamSpecCompiled[]{stream});
    }
}
