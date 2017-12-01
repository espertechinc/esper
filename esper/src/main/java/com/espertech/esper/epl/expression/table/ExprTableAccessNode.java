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
package com.espertech.esper.epl.expression.table;

import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumn;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;

import java.io.StringWriter;

public abstract class ExprTableAccessNode extends ExprNodeBase implements ExprForge {
    private static final long serialVersionUID = -2048267912299812034L;

    protected final String tableName;
    protected transient ExprTableAccessEvalStrategy strategy;
    protected transient ExprEvaluator[] groupKeyEvaluators;

    protected abstract void validateBindingInternal(ExprValidationContext validationContext, TableMetadata tableMetadata)
            throws ExprValidationException;

    protected abstract boolean equalsNodeInternal(ExprTableAccessNode other);

    protected ExprTableAccessNode(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public ExprEvaluator[] getGroupKeyEvaluators() {
        return groupKeyEvaluators;
    }

    public void setStrategy(ExprTableAccessEvalStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean isConstantResult() {
        return false;
    }

    public final ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (validationContext.getTableService() == null || !validationContext.isAllowBindingConsumption()) {
            throw new ExprValidationException("Invalid use of table access expression, expression '" + tableName + "' is not allowed here");
        }
        TableMetadata metadata = validationContext.getTableService().getTableMetadata(tableName);
        if (metadata == null) {
            throw new ExprValidationException("A table '" + tableName + "' could not be found");
        }

        if (metadata.getContextName() != null &&
                validationContext.getContextDescriptor() != null &&
                !metadata.getContextName().equals(validationContext.getContextDescriptor().getContextName())) {
            throw new ExprValidationException("Table by name '" + tableName + "' has been declared for context '" + metadata.getContextName() + "' and can only be used within the same context");
        }

        // additional validations depend on detail
        validateBindingInternal(validationContext, metadata);
        return null;
    }

    protected void validateGroupKeys(TableMetadata metadata, ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length > 0) {
            groupKeyEvaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(this.getChildNodes(), validationContext.getEngineImportService(), ExprTableAccessNode.class, validationContext.getStreamTypeService().isOnDemandStreams(), validationContext.getStatementName());
        } else {
            groupKeyEvaluators = new ExprEvaluator[0];
        }
        Class[] typesReturned = ExprNodeUtilityCore.getExprResultTypes(this.getChildNodes());
        ExprTableNodeUtil.validateExpressions(tableName, typesReturned, "key", this.getChildNodes(), metadata.getKeyTypes(), "key");
    }

    public final ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    protected void toPrecedenceFreeEPLInternal(StringWriter writer, String subprop) {
        toPrecedenceFreeEPLInternal(writer);
        writer.append(".");
        writer.append(subprop);
    }

    protected void toPrecedenceFreeEPLInternal(StringWriter writer) {
        writer.append(tableName);
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

    protected TableMetadataColumn validateSubpropertyGetCol(TableMetadata tableMetadata, String subpropName)
            throws ExprValidationException {
        TableMetadataColumn column = tableMetadata.getTableColumns().get(subpropName);
        if (column == null) {
            throw new ExprValidationException("A column '" + subpropName + "' could not be found for table '" + tableName + "'");
        }
        return column;
    }

    public boolean equalsNode(ExprNode o, boolean ignoreStreamPrefix) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprTableAccessNode that = (ExprTableAccessNode) o;
        if (!tableName.equals(that.tableName)) return false;

        return equalsNodeInternal(that);
    }

    public int hashCode() {
        return tableName != null ? tableName.hashCode() : 0;
    }
}
