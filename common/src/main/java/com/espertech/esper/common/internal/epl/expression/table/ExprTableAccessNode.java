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
package com.espertech.esper.common.internal.epl.expression.table;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameTableAccess;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumn;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode.AccessEvaluationType.*;

public abstract class ExprTableAccessNode extends ExprNodeBase implements ExprForgeInstrumentable, ExprEvaluator {
    protected final String tableName;
    protected TableMetaData tableMeta;
    protected ExprTableEvalStrategyFactoryForge strategy;
    protected ExprForge[] groupKeyEvaluators;
    private int tableAccessNumber = -1;

    protected abstract void validateBindingInternal(ExprValidationContext validationContext) throws ExprValidationException;

    protected abstract boolean equalsNodeInternal(ExprTableAccessNode other);

    protected abstract String getInstrumentationQName();

    protected abstract CodegenExpression[] getInstrumentationQParams();

    public abstract ExprTableEvalStrategyFactoryForge getTableAccessFactoryForge();

    /**
     * Ctor.
     * Getting a table name allows "eplToModel" without knowing tables.
     *
     * @param tableName table name
     */
    public ExprTableAccessNode(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public final ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        tableMeta = validationContext.getTableCompileTimeResolver().resolve(tableName);
        if (tableMeta == null) {
            throw new ExprValidationException("Failed to resolve table name '" + tableName + "' to a table");
        }

        if (!validationContext.isAllowBindingConsumption()) {
            throw new ExprValidationException("Invalid use of table access expression, expression '" + getTableName() + "' is not allowed here");
        }

        if (tableMeta.getOptionalContextName() != null &&
                validationContext.getContextDescriptor() != null &&
                !tableMeta.getOptionalContextName().equals(validationContext.getContextDescriptor().getContextName())) {
            throw new ExprValidationException("Table by name '" + getTableName() + "' has been declared for context '" + tableMeta.getOptionalContextName() + "' and can only be used within the same context");
        }

        // additional validations depend on detail
        validateBindingInternal(validationContext);
        return null;
    }

    protected void validateGroupKeys(TableMetaData metadata, ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length > 0) {
            groupKeyEvaluators = ExprNodeUtilityQuery.getForges(this.getChildNodes());
        } else {
            groupKeyEvaluators = new ExprForge[0];
        }
        Class[] typesReturned = ExprNodeUtilityQuery.getExprResultTypes(this.getChildNodes());
        Class[] keyTypes = metadata.isKeyed() ? metadata.getKeyTypes() : new Class[0];
        ExprTableNodeUtil.validateExpressions(getTableName(), typesReturned, "key", this.getChildNodes(), keyTypes, "key");
    }

    public final ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return makeEvaluate(PLAIN, this, getEvaluationType(), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, getInstrumentationQName(), requiredType, parent, exprSymbol, codegenClassScope)
                .qparams(getInstrumentationQParams())
                .build();
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return makeEvaluate(GETEVENTCOLL, this, Collection.class, parent, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return makeEvaluate(GETSCALARCOLL, this, Collection.class, parent, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return makeEvaluate(GETEVENT, this, EventBean.class, parent, exprSymbol, codegenClassScope);
    }

    public final ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public final Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    protected void toPrecedenceFreeEPLInternal(StringWriter writer, String subprop) {
        toPrecedenceFreeEPLInternal(writer);
        writer.append(".");
        writer.append(subprop);
    }

    protected void toPrecedenceFreeEPLInternal(StringWriter writer) {
        writer.append(getTableName());
        if (this.getChildNodes().length > 0) {
            writer.append("[");
            String delimiter = "";
            for (ExprNode expr : this.getChildNodes()) {
                writer.append(delimiter);
                expr.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
                delimiter = ",";
            }
            writer.append("]");
        }
    }

    protected TableMetadataColumn validateSubpropertyGetCol(TableMetaData tableMetadata, String subpropName)
            throws ExprValidationException {
        TableMetadataColumn column = tableMetadata.getColumns().get(subpropName);
        if (column == null) {
            throw new ExprValidationException("A column '" + subpropName + "' could not be found for table '" + getTableName() + "'");
        }
        return column;
    }

    public boolean equalsNode(ExprNode o, boolean ignoreStreamPrefix) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprTableAccessNode that = (ExprTableAccessNode) o;
        if (!getTableName().equals(that.getTableName())) return false;

        return equalsNodeInternal(that);
    }

    public void setTableAccessNumber(int tableAccessNumber) {
        this.tableAccessNumber = tableAccessNumber;
    }

    public int getTableAccessNumber() {
        return tableAccessNumber;
    }

    public TableMetaData getTableMeta() {
        return tableMeta;
    }

    protected static CodegenExpression makeEvaluate(AccessEvaluationType evaluationType, ExprTableAccessNode accessNode, Class resultType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (accessNode.getTableAccessNumber() == -1) {
            throw new IllegalStateException("Table expression node has not been assigned");
        }
        CodegenMethod method = parent.makeChild(resultType, ExprTableAccessNode.class, classScope);

        CodegenExpression eps = symbols.getAddEPS(method);
        CodegenExpression newData = symbols.getAddIsNewData(method);
        CodegenExpression evalCtx = symbols.getAddExprEvalCtx(method);

        CodegenExpressionField future = classScope.getPackageScope().addOrGetFieldWellKnown(new CodegenFieldNameTableAccess(accessNode.tableAccessNumber), ExprTableEvalStrategy.class);
        CodegenExpression evaluation = exprDotMethod(future, evaluationType.getMethodName(), eps, newData, evalCtx);
        if (resultType != Object.class) {
            evaluation = cast(resultType, evaluation);
        }
        method.getBlock().methodReturn(evaluation);
        return localMethod(method);
    }

    static enum AccessEvaluationType {
        PLAIN("evaluate"),
        GETEVENTCOLL("evaluateGetROCollectionEvents"),
        GETSCALARCOLL("evaluateGetROCollectionScalar"),
        GETEVENT("evaluateGetEventBean"),
        EVALTYPABLESINGLE("evaluateTypableSingle");

        private final String methodName;

        AccessEvaluationType(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }
    }
}
