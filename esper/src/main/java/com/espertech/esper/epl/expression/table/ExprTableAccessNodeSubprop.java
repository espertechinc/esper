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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotEnumerationSourceForProps;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumn;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnPlain;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;
import java.util.Collection;

public class ExprTableAccessNodeSubprop extends ExprTableAccessNode implements ExprEvaluator, ExprEvaluatorEnumeration {
    private static final long serialVersionUID = 1779238498208599159L;

    private final String subpropName;

    private Class bindingReturnType;
    private transient EPType optionalEnumerationType;
    private transient ExprEvaluatorEnumerationGivenEvent optionalPropertyEnumEvaluator;

    public ExprTableAccessNodeSubprop(String tableName, String subpropName) {
        super(tableName);
        this.subpropName = subpropName;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    protected void validateBindingInternal(ExprValidationContext validationContext, TableMetadata tableMetadata)
            throws ExprValidationException {
        validateGroupKeys(tableMetadata);
        TableMetadataColumn column = validateSubpropertyGetCol(tableMetadata, subpropName);
        if (column instanceof TableMetadataColumnPlain) {
            bindingReturnType = tableMetadata.getInternalEventType().getPropertyType(subpropName);
            ExprDotEnumerationSourceForProps enumerationSource = ExprDotNodeUtility.getPropertyEnumerationSource(subpropName, 0, tableMetadata.getInternalEventType(), true, true);
            optionalEnumerationType = enumerationSource.getReturnType();
            optionalPropertyEnumEvaluator = enumerationSource.getEnumerationGivenEvent();
        } else {
            TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) column;
            optionalEnumerationType = aggcol.getOptionalEnumerationType();
            bindingReturnType = aggcol.getFactory().getResultType();
        }
    }

    public Class getType() {
        return bindingReturnType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprTableSubproperty(this, tableName, subpropName);
            Object result = strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            InstrumentationHelper.get().aExprTableSubproperty(result);
            return result;
        }

        return strategy.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer, subpropName);
    }

    public String getSubpropName() {
        return subpropName;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return EPTypeHelper.optionalIsEventTypeColl(optionalEnumerationType);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return EPTypeHelper.optionalIsComponentTypeColl(optionalEnumerationType);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return EPTypeHelper.optionalIsEventTypeSingle(optionalEnumerationType);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return strategy.evaluateGetEventBean(eventsPerStream, isNewData, context);
    }

    public ExprEvaluatorEnumerationGivenEvent getOptionalPropertyEnumEvaluator() {
        return optionalPropertyEnumEvaluator;
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        ExprTableAccessNodeSubprop that = (ExprTableAccessNodeSubprop) other;
        return subpropName.equals(that.subpropName);
    }
}
