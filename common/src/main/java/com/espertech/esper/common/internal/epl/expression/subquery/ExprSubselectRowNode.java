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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
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
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents a subselect in an expression tree.
 */
public class ExprSubselectRowNode extends ExprSubselectNode {
    protected EventType subselectMultirowType;
    private SubselectForgeRow evalStrategy;

    /**
     * Ctor.
     *
     * @param statementSpec is the lookup statement spec from the parser, unvalidated
     */
    public ExprSubselectRowNode(StatementSpecRaw statementSpec) {
        super(statementSpec);
    }

    public Class getEvaluationType() {
        if (selectClause == null) {   // wildcards allowed
            return rawEventType.getUnderlyingType();
        }
        if (selectClause.length == 1) {
            return JavaClassHelper.getBoxedType(selectClause[0].getForge().getEvaluationType());
        }
        return Map.class;
    }

    public void validateSubquery(ExprValidationContext validationContext) throws ExprValidationException {
        // Strategy for subselect depends on presence of filter + presence of select clause expressions
        // the filter expression is handled elsewhere if there is any aggregation
        if (filterExpr == null) {
            if (selectClause == null) {
                TableMetaData table = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(rawEventType);
                if (table != null) {
                    evalStrategy = new SubselectForgeStrategyRowUnfilteredUnselectedTable(this, table);
                } else {
                    evalStrategy = new SubselectForgeStrategyRowPlain(this);
                }
            } else {
                if (getStatementSpecCompiled().getRaw().getGroupByExpressions() != null && getStatementSpecCompiled().getRaw().getGroupByExpressions().size() > 0) {
                    if (havingExpr != null) {
                        evalStrategy = new SubselectForgeRowUnfilteredSelectedGroupedWHaving(this);
                    } else {
                        evalStrategy = new SubselectForgeRowUnfilteredSelectedGroupedNoHaving(this);
                    }
                } else {
                    if (havingExpr != null) {
                        evalStrategy = new SubselectForgeRowHavingSelected(this);
                    } else {
                        evalStrategy = new SubselectForgeStrategyRowPlain(this);
                    }
                }
            }
        } else {
            if (selectClause == null) {
                TableMetaData table = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(rawEventType);
                if (table != null) {
                    evalStrategy = new SubselectForgeStrategyRowFilteredUnselectedTable(this, table);
                } else {
                    evalStrategy = new SubselectForgeStrategyRowPlain(this);
                }
            } else {
                evalStrategy = new SubselectForgeStrategyRowPlain(this);
            }
        }
    }

    public LinkedHashMap<String, Object> typableGetRowProperties() throws ExprValidationException {
        if ((selectClause == null) || (selectClause.length < 2)) {
            return null;
        }
        return getRowType();
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (selectClause == null) {
            return rawEventType;
        }
        if (this.getSubselectAggregationType() != SubqueryAggregationType.FULLY_AGGREGATED_NOPROPS) {
            return null;
        }
        return getAssignAnonymousType(statementRawInfo, compileTimeServices);
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (selectClause == null) {   // wildcards allowed
            return rawEventType;
        }

        // special case: selecting a single property that is itself an event
        if (selectClause.length == 1 && selectClause[0] instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) selectClause[0];
            FragmentEventType fragment = rawEventType.getFragmentType(identNode.getResolvedPropertyName());
            if (fragment != null && !fragment.isIndexed()) {
                return fragment.getFragmentType();
            }
        }

        // select of a single value otherwise results in a collection of scalar values
        if (selectClause.length == 1) {
            return null;
        }

        // fully-aggregated always returns zero or one row
        if (this.getSubselectAggregationType() == SubqueryAggregationType.FULLY_AGGREGATED_NOPROPS) {
            return null;
        }

        return getAssignAnonymousType(statementRawInfo, compileTimeServices);
    }

    private EventType getAssignAnonymousType(StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        Map<String, Object> rowType = getRowType();
        String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousTypeSubselectMultirow(this.getSubselectNumber());
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, statementRawInfo.getModuleName(), EventTypeTypeClass.SUBQDERIVED, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        MapEventType maptype = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, rowType, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(maptype);
        subselectMultirowType = maptype;
        return maptype;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        if (selectClause == null) {   // wildcards allowed
            return null;
        }
        if (selectClause.length > 1) {
            return null;
        }
        return selectClause[0].getForge().getEvaluationType();
    }

    public boolean isAllowMultiColumnSelect() {
        return true;
    }

    protected CodegenExpression evalMatchesPlainCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(getEvaluationType(), this.getClass(), classScope);
        method.getBlock()
                .applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantNull(), constantNull()), method, symbols)
                .methodReturn(evalStrategy.evaluateCodegen(method, symbols, classScope));
        return localMethod(method);
    }

    protected CodegenExpression evalMatchesGetCollEventsCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Collection.class, this.getClass(), classScope);
        method.getBlock()
                .applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantNull(), CollectionUtil.EMPTY_LIST_EXPRESSION), method, symbols)
                .methodReturn(evalStrategy.evaluateGetCollEventsCodegen(method, symbols, classScope));
        return localMethod(method);
    }

    protected CodegenExpression evalMatchesGetCollScalarCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Collection.class, this.getClass(), classScope);
        method.getBlock()
                .applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantNull(), CollectionUtil.EMPTY_LIST_EXPRESSION), method, symbols)
                .methodReturn(evalStrategy.evaluateGetCollScalarCodegen(method, symbols, classScope));
        return localMethod(method);
    }

    protected CodegenExpression evalMatchesGetEventBeanCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventBean.class, this.getClass(), classScope);
        method.getBlock()
                .applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantNull(), constantNull()), method, symbols)
                .methodReturn(evalStrategy.evaluateGetBeanCodegen(method, symbols, classScope));
        return localMethod(method);
    }

    private LinkedHashMap<String, Object> getRowType() throws ExprValidationException {
        Set<String> uniqueNames = new HashSet<String>();
        LinkedHashMap<String, Object> type = new LinkedHashMap<String, Object>();

        for (int i = 0; i < selectClause.length; i++) {
            String assignedName = this.selectAsNames[i];
            if (assignedName == null) {
                assignedName = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(selectClause[i]);
            }
            if (uniqueNames.add(assignedName)) {
                type.put(assignedName, selectClause[i].getForge().getEvaluationType());
            } else {
                throw new ExprValidationException("Column " + i + " in subquery does not have a unique column name assigned");
            }
        }
        return type;
    }

    public CodegenMethod evaluateRowCodegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        ExprForgeCodegenSymbol symbols = new ExprForgeCodegenSymbol(true, true);
        CodegenMethod method = parent.makeChildWithScope(Map.class, CodegenLegoMethodExpression.class, symbols, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression[] expressions = new CodegenExpression[selectClause.length];
        for (int i = 0; i < selectClause.length; i++) {
            expressions[i] = selectClause[i].getForge().evaluateCodegen(Object.class, method, symbols, classScope);
        }

        symbols.derivedSymbolsCodegen(method, method.getBlock(), classScope);

        method.getBlock().declareVar(Map.class, "map", newInstance(HashMap.class));
        for (int i = 0; i < selectClause.length; i++) {
            method.getBlock().exprDotMethod(ref("map"), "put", constant(selectAsNames[i]), expressions[i]);
        }
        method.getBlock().methodReturn(ref("map"));
        return method;
    }

    protected CodegenExpression evalMatchesTypableSingleCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Object[].class, this.getClass(), classScope);
        method.getBlock()
                .applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantNull(), publicConstValue(CollectionUtil.class, "OBJECTARRAYARRAY_EMPTY")), method, symbols)
                .methodReturn(evalStrategy.evaluateTypableSinglerowCodegen(method, symbols, classScope));
        return localMethod(method);
    }

    protected CodegenExpression evalMatchesTypableMultiCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Object[][].class, this.getClass(), classScope);
        method.getBlock()
                .applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantNull(), publicConstValue(CollectionUtil.class, "OBJECTARRAYARRAY_EMPTY")), method, symbols)
                .methodReturn(evalStrategy.evaluateTypableMultirowCodegen(method, symbols, classScope));
        return localMethod(method);
    }
}
