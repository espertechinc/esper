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
package com.espertech.esper.common.internal.view.groupwin;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.annotation.Hint;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.core.WrapperEventTypeUtil;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.util.ViewForgeSupport;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.view.util.ViewMultiKeyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil.makeViewFactories;

/**
 * Factory for {@link GroupByView} instances.
 */
public class GroupByViewFactoryForge extends ViewFactoryForgeBase {
    private final static Logger log = LoggerFactory.getLogger(GroupByViewFactoryForge.class);

    protected List<ExprNode> viewParameters;
    protected List<ViewFactoryForge> groupeds;
    protected ExprNode[] criteriaExpressions;
    protected String[] propertyNames;
    protected boolean addingProperties; // when adding additional properties to output events
    protected boolean isReclaimAged;
    protected long reclaimMaxAge;
    protected long reclaimFrequency;
    protected MultiKeyClassRef multiKeyClassNames;

    public void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException {
        this.viewParameters = parameters;

        TimeAbacus timeAbacus = viewForgeEnv.getClasspathImportServiceCompileTime().getTimeAbacus();
        Hint reclaimGroupAged = HintEnum.RECLAIM_GROUP_AGED.getHint(viewForgeEnv.getAnnotations());

        if (reclaimGroupAged != null) {
            isReclaimAged = true;
            String hintValueMaxAge = HintEnum.RECLAIM_GROUP_AGED.getHintAssignedValue(reclaimGroupAged);
            if (hintValueMaxAge == null) {
                throw new ViewParameterException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_AGED + "' has not been provided");
            }
            try {
                reclaimMaxAge = timeAbacus.deltaForSecondsDouble(Double.parseDouble(hintValueMaxAge));
            } catch (RuntimeException ex) {
                throw new ViewParameterException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_AGED + "' value '" + hintValueMaxAge + "' could not be parsed as a double value");
            }

            String hintValueFrequency = HintEnum.RECLAIM_GROUP_FREQ.getHintAssignedValue(reclaimGroupAged);
            if (hintValueFrequency == null) {
                reclaimFrequency = reclaimMaxAge;
            } else {
                try {
                    reclaimFrequency = timeAbacus.deltaForSecondsDouble(Double.parseDouble(hintValueFrequency));
                } catch (RuntimeException ex) {
                    throw new ViewParameterException("Required hint value for hint '" + HintEnum.RECLAIM_GROUP_FREQ + "' value '" + hintValueFrequency + "' could not be parsed as a double value");
                }
            }
            if (reclaimMaxAge < 1) {
                log.warn("Reclaim max age parameter is less then 1, are your sure?");
            }

            if (log.isDebugEnabled()) {
                log.debug("Using reclaim-aged strategy for group-window age " + reclaimMaxAge + " frequency " + reclaimFrequency);
            }
        }
    }

    public void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException {
        criteriaExpressions = ViewForgeSupport.validate(getViewName(), parentEventType, viewParameters, false, viewForgeEnv, streamNumber);

        if (criteriaExpressions.length == 0) {
            String errorMessage = getViewName() + " view requires a one or more expressions provinding unique values as parameters";
            throw new ViewParameterException(errorMessage);
        }

        propertyNames = new String[criteriaExpressions.length];
        for (int i = 0; i < criteriaExpressions.length; i++) {
            propertyNames[i] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(criteriaExpressions[i]);
        }

        EventType groupedEventType = groupeds.get(groupeds.size() - 1).getEventType();
        eventType = determineEventType(groupedEventType, criteriaExpressions, streamNumber, viewForgeEnv);
        if (eventType != groupedEventType) {
            addingProperties = true;
        }
    }

    @Override
    public List<StmtClassForgeableFactory> initAdditionalForgeables(ViewForgeEnv viewForgeEnv) {
        MultiKeyPlan desc = MultiKeyPlanner.planMultiKey(criteriaExpressions, false, viewForgeEnv.getStatementRawInfo(), viewForgeEnv.getSerdeResolver());
        multiKeyClassNames = desc.getClassRef();
        return desc.getMultiKeyForgeables();
    }

    protected Class typeOfFactory() {
        return GroupByViewFactory.class;
    }

    protected String factoryMethod() {
        return "group";
    }

    protected void assign(CodegenMethod method, CodegenExpressionRef factory, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (groupeds == null) {
            throw new IllegalStateException("Empty grouped forges");
        }
        method.getBlock()
            .exprDotMethod(factory, "setReclaimAged", constant(isReclaimAged))
            .exprDotMethod(factory, "setReclaimMaxAge", constant(reclaimMaxAge))
            .exprDotMethod(factory, "setReclaimFrequency", constant(reclaimFrequency))
            .exprDotMethod(factory, "setPropertyNames", constant(propertyNames))
            .exprDotMethod(factory, "setGroupeds", localMethod(makeViewFactories(groupeds, this.getClass(), method, classScope, symbols)))
            .exprDotMethod(factory, "setEventType", EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF))
            .exprDotMethod(factory, "setAddingProperties", constant(addingProperties));
        ViewMultiKeyHelper.assign(criteriaExpressions, multiKeyClassNames, method, factory, symbols, classScope);
    }

    @Override
    public void accept(ViewForgeVisitor visitor) {
        visitor.visit(this);
        for (ViewFactoryForge forge : groupeds) {
            forge.accept(visitor);
        }
    }

    public List<ViewFactoryForge> getGroupeds() {
        return groupeds;
    }

    public void setGroupeds(List<ViewFactoryForge> groupeds) {
        this.groupeds = groupeds;
    }

    public String getViewName() {
        return "Group-By";
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public ExprNode[] getCriteriaExpressions() {
        return criteriaExpressions;
    }

    public List<ExprNode> getViewParameters() {
        return viewParameters;
    }

    @Override
    public List<ViewFactoryForge> getInnerForges() {
        return groupeds;
    }

    private static EventType determineEventType(EventType groupedEventType, ExprNode[] criteriaExpressions, int streamNum, ViewForgeEnv viewForgeEnv) {

        // determine types of fields
        Class[] fieldTypes = new Class[criteriaExpressions.length];
        for (int i = 0; i < fieldTypes.length; i++) {
            fieldTypes[i] = criteriaExpressions[i].getForge().getEvaluationType();
        }

        // Determine the final event type that the merge view generates
        // This event type is ultimatly generated by AddPropertyValueView which is added to each view branch for each
        // group key.

        // If the parent event type contains the merge fields, we use the same event type
        boolean parentContainsMergeKeys = true;
        String[] fieldNames = new String[criteriaExpressions.length];
        for (int i = 0; i < criteriaExpressions.length; i++) {
            String name = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(criteriaExpressions[i]);
            fieldNames[i] = name;
            try {
                if (!(groupedEventType.isProperty(name))) {
                    // for ident-nodes we also use the unresolved name as that has the unescaped property name
                    if (criteriaExpressions[i] instanceof ExprIdentNode) {
                        ExprIdentNode identNode = (ExprIdentNode) criteriaExpressions[i];
                        if (!(groupedEventType.isProperty(identNode.getUnresolvedPropertyName()))) {
                            parentContainsMergeKeys = false;
                        }
                    }
                }
            } catch (PropertyAccessException ex) {
                // expected
                parentContainsMergeKeys = false;
            }
        }

        // If the parent view contains the fields to group by, the event type after merging stays the same
        if (parentContainsMergeKeys) {
            return groupedEventType;
        }
        // If the parent event type does not contain the fields, such as when a statistics views is
        // grouped which simply provides a map of calculated values,
        // then we need to add in the merge field as an event property thus changing event types.
        Map<String, Object> additionalProps = new HashMap<String, Object>();
        for (int i = 0; i < fieldNames.length; i++) {
            additionalProps.put(fieldNames[i], fieldTypes[i]);
        }

        String outputEventTypeName = viewForgeEnv.getStatementCompileTimeServices().getEventTypeNameGeneratorStatement().getViewGroup(streamNum);
        EventTypeMetadata metadata = new EventTypeMetadata(outputEventTypeName, viewForgeEnv.getModuleName(), EventTypeTypeClass.VIEWDERIVED, EventTypeApplicationType.WRAPPER, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        EventType eventType = WrapperEventTypeUtil.makeWrapper(metadata, groupedEventType, additionalProps, EventBeanTypedEventFactoryCompileTime.INSTANCE, viewForgeEnv.getBeanEventTypeFactoryProtected(), viewForgeEnv.getEventTypeCompileTimeResolver());
        viewForgeEnv.getEventTypeModuleCompileTimeRegistry().newType(eventType);
        return eventType;
    }
}
