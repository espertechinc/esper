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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.client.annotation.Audit;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.PropertyResolutionDescriptor;
import com.espertech.esper.epl.expression.table.ExprTableIdentNode;
import com.espertech.esper.epl.parse.ASTUtil;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.filter.FilterSpecLookupable;

import java.io.StringWriter;

/**
 * Represents an stream property identifier in a filter expressiun tree.
 */
public class ExprIdentNodeImpl extends ExprNodeBase implements ExprIdentNode {
    private static final long serialVersionUID = 5882493771230745244L;

    // select myprop from...        is a simple property, no stream supplied
    // select s0.myprop from...     is a simple property with a stream supplied, or a nested property (cannot tell until resolved)
    // select indexed[1] from ...   is a indexed property

    private final String unresolvedPropertyName;
    private String streamOrPropertyName;

    private String resolvedStreamName;
    private String resolvedPropertyName;
    private transient ExprIdentNodeEvaluator evaluator;

    /**
     * Ctor.
     *
     * @param unresolvedPropertyName is the event property name in unresolved form, ie. unvalidated against streams
     */
    public ExprIdentNodeImpl(String unresolvedPropertyName) {
        if (unresolvedPropertyName == null) {
            throw new IllegalArgumentException("Property name is null");
        }
        this.unresolvedPropertyName = unresolvedPropertyName;
        this.streamOrPropertyName = null;
    }

    /**
     * Ctor.
     *
     * @param unresolvedPropertyName is the event property name in unresolved form, ie. unvalidated against streams
     * @param streamOrPropertyName   is the stream name, or if not a valid stream name a possible nested property name
     *                               in one of the streams.
     */
    public ExprIdentNodeImpl(String unresolvedPropertyName, String streamOrPropertyName) {
        if (unresolvedPropertyName == null) {
            throw new IllegalArgumentException("Property name is null");
        }
        if (streamOrPropertyName == null) {
            throw new IllegalArgumentException("Stream (or property name) name is null");
        }
        this.unresolvedPropertyName = unresolvedPropertyName;
        this.streamOrPropertyName = streamOrPropertyName;
    }

    public ExprIdentNodeImpl(EventType eventType, String propertyName, int streamNumber) {
        unresolvedPropertyName = propertyName;
        resolvedPropertyName = propertyName;
        EventPropertyGetter propertyGetter = eventType.getGetter(propertyName);
        if (propertyGetter == null) {
            throw new IllegalArgumentException("Ident-node constructor could not locate property " + propertyName);
        }
        Class propertyType = eventType.getPropertyType(propertyName);
        evaluator = new ExprIdentNodeEvaluatorImpl(streamNumber, propertyGetter, propertyType, this);
    }

    public ExprEvaluator getExprEvaluator() {
        return evaluator;
    }

    /**
     * For unit testing, returns unresolved property name.
     *
     * @return property name
     */
    public String getUnresolvedPropertyName() {
        return unresolvedPropertyName;
    }

    /**
     * For unit testing, returns stream or property name candidate.
     *
     * @return stream name, or property name of a nested property of one of the streams
     */
    public String getStreamOrPropertyName() {
        return streamOrPropertyName;
    }

    /**
     * Set name.
     *
     * @param streamOrPropertyName to use
     */
    public void setStreamOrPropertyName(String streamOrPropertyName) {
        this.streamOrPropertyName = streamOrPropertyName;
    }

    /**
     * Returns the unresolved property name in it's complete form, including
     * the stream name if there is one.
     *
     * @return property name
     */
    public String getFullUnresolvedName() {
        if (streamOrPropertyName == null) {
            return unresolvedPropertyName;
        } else {
            return streamOrPropertyName + "." + unresolvedPropertyName;
        }
    }

    public boolean getFilterLookupEligible() {
        return evaluator.getStreamNum() == 0 && !(evaluator.isContextEvaluated());
    }

    public FilterSpecLookupable getFilterLookupable() {
        return new FilterSpecLookupable(resolvedPropertyName, evaluator.getGetter(), evaluator.getType(), false);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // rewrite expression into a table-access expression
        if (validationContext.getStreamTypeService().hasTableTypes()) {
            ExprTableIdentNode tableIdentNode = validationContext.getTableService().getTableIdentNode(validationContext.getStreamTypeService(), unresolvedPropertyName, streamOrPropertyName);
            if (tableIdentNode != null) {
                return tableIdentNode;
            }
        }

        String unescapedPropertyName = PropertyParser.unescapeBacktick(unresolvedPropertyName);
        Pair<PropertyResolutionDescriptor, String> propertyInfoPair = ExprIdentNodeUtil.getTypeFromStream(validationContext.getStreamTypeService(), unescapedPropertyName, streamOrPropertyName, false);
        resolvedStreamName = propertyInfoPair.getSecond();
        int streamNum = propertyInfoPair.getFirst().getStreamNum();
        Class propertyType = propertyInfoPair.getFirst().getPropertyType();
        resolvedPropertyName = propertyInfoPair.getFirst().getPropertyName();
        EventPropertyGetter propertyGetter;
        try {
            propertyGetter = propertyInfoPair.getFirst().getStreamEventType().getGetter(resolvedPropertyName);
        } catch (PropertyAccessException ex) {
            throw new ExprValidationException("Property '" + unresolvedPropertyName + "' is not valid: " + ex.getMessage(), ex);
        }

        if (propertyGetter == null) {
            throw new ExprValidationException("Property getter returned was invalid for property '" + unresolvedPropertyName + "'");
        }

        Audit audit = AuditEnum.PROPERTY.getAudit(validationContext.getAnnotations());
        if (audit != null) {
            evaluator = new ExprIdentNodeEvaluatorLogging(streamNum, propertyGetter, propertyType, this, resolvedPropertyName, validationContext.getStatementName(), validationContext.getStreamTypeService().getEngineURIQualifier());
        } else {
            evaluator = new ExprIdentNodeEvaluatorImpl(streamNum, propertyGetter, propertyType, this);
        }

        // if running in a context, take the property value from context
        if (validationContext.getContextDescriptor() != null && !validationContext.isFilterExpression()) {
            EventType fromType = validationContext.getStreamTypeService().getEventTypes()[streamNum];
            String contextPropertyName = validationContext.getContextDescriptor().getContextPropertyRegistry().getPartitionContextPropertyName(fromType, resolvedPropertyName);
            if (contextPropertyName != null) {
                EventType contextType = validationContext.getContextDescriptor().getContextPropertyRegistry().getContextEventType();
                evaluator = new ExprIdentNodeEvaluatorContext(streamNum, contextType.getPropertyType(contextPropertyName), contextType.getGetter(contextPropertyName));
            }
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    /**
     * Returns stream id supplying the property value.
     *
     * @return stream number
     */
    public int getStreamId() {
        if (evaluator == null) {
            throw new IllegalStateException("Identifier expression has not been validated");
        }
        return evaluator.getStreamNum();
    }

    public Integer getStreamReferencedIfAny() {
        return getStreamId();
    }

    public String getRootPropertyNameIfAny() {
        return getResolvedPropertyNameRoot();
    }

    public Class getType() {
        if (evaluator == null) {
            throw new IllegalStateException("Identifier expression has not been validated");
        }
        return evaluator.getType();
    }

    /**
     * Returns stream name as resolved by lookup of property in streams.
     *
     * @return stream name
     */
    public String getResolvedStreamName() {
        if (resolvedStreamName == null) {
            throw new IllegalStateException("Identifier node has not been validated");
        }
        return resolvedStreamName;
    }

    /**
     * Return property name as resolved by lookup in streams.
     *
     * @return property name
     */
    public String getResolvedPropertyName() {
        if (resolvedPropertyName == null) {
            throw new IllegalStateException("Identifier node has not been validated");
        }
        return resolvedPropertyName;
    }

    /**
     * Returns the root of the resolved property name, if any.
     *
     * @return root
     */
    public String getResolvedPropertyNameRoot() {
        if (resolvedPropertyName == null) {
            throw new IllegalStateException("Identifier node has not been validated");
        }
        if (resolvedPropertyName.indexOf('[') != -1) {
            return resolvedPropertyName.substring(0, resolvedPropertyName.indexOf('['));
        }
        if (resolvedPropertyName.indexOf('(') != -1) {
            return resolvedPropertyName.substring(0, resolvedPropertyName.indexOf('('));
        }
        if (resolvedPropertyName.indexOf('.') != -1) {
            return resolvedPropertyName.substring(0, resolvedPropertyName.indexOf('.'));
        }
        return resolvedPropertyName;
    }

    public String toString() {
        return "unresolvedPropertyName=" + unresolvedPropertyName +
                " streamOrPropertyName=" + streamOrPropertyName +
                " resolvedPropertyName=" + resolvedPropertyName;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPL(writer, streamOrPropertyName, unresolvedPropertyName);
    }

    public static void toPrecedenceFreeEPL(StringWriter writer, String streamOrPropertyName, String unresolvedPropertyName) {
        if (streamOrPropertyName != null) {
            writer.append(ASTUtil.unescapeDot(streamOrPropertyName)).append('.');
        }
        writer.append(ASTUtil.unescapeDot(PropertyParser.unescapeBacktick(unresolvedPropertyName)));
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprIdentNode)) {
            return false;
        }

        ExprIdentNode other = (ExprIdentNode) node;

        if (streamOrPropertyName != null ? !streamOrPropertyName.equals(other.getStreamOrPropertyName()) : other.getStreamOrPropertyName() != null)
            return false;
        if (unresolvedPropertyName != null ? !unresolvedPropertyName.equals(other.getUnresolvedPropertyName()) : other.getUnresolvedPropertyName() != null)
            return false;
        return true;
    }

    public ExprIdentNodeEvaluator getExprEvaluatorIdent() {
        return evaluator;
    }
}
