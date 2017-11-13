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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.*;
import com.espertech.esper.core.context.util.ContextControllerSelectorUtil;
import com.espertech.esper.epl.spec.ContextDetailCategoryItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ContextControllerCategory implements ContextController {

    private final int pathId;
    private final ContextControllerLifecycleCallback activationCallback;
    private final ContextControllerCategoryFactoryImpl factory;

    private final Map<Integer, ContextControllerInstanceHandle> handleCategories = new LinkedHashMap<Integer, ContextControllerInstanceHandle>();

    private int currentSubpathId;

    public ContextControllerCategory(int pathId, ContextControllerLifecycleCallback activationCallback, ContextControllerCategoryFactoryImpl factory) {
        this.pathId = pathId;
        this.activationCallback = activationCallback;
        this.factory = factory;
    }

    public void importContextPartitions(ContextControllerState state, int pathIdToUse, ContextInternalFilterAddendum filterAddendum, AgentInstanceSelector agentInstanceSelector) {
        initializeFromState(null, null, filterAddendum, state, pathIdToUse, agentInstanceSelector, true);
    }

    public void deletePath(ContextPartitionIdentifier identifier) {
        ContextPartitionIdentifierCategory category = (ContextPartitionIdentifierCategory) identifier;
        int count = 0;
        for (ContextDetailCategoryItem cat : factory.getCategorySpec().getItems()) {
            if (cat.getName().equals(category.getLabel())) {
                handleCategories.remove(count);
                break;
            }
            count++;
        }
    }

    public void visitSelectedPartitions(ContextPartitionSelector contextPartitionSelector, ContextPartitionVisitor visitor) {
        int nestingLevel = factory.getFactoryContext().getNestingLevel();

        if (contextPartitionSelector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filter = (ContextPartitionSelectorFiltered) contextPartitionSelector;
            ContextPartitionIdentifierCategory identifier = new ContextPartitionIdentifierCategory();
            for (Map.Entry<Integer, ContextControllerInstanceHandle> entry : handleCategories.entrySet()) {
                identifier.setContextPartitionId(entry.getValue().getContextPartitionOrPathId());
                String categoryName = factory.getCategorySpec().getItems().get(entry.getKey()).getName();
                identifier.setLabel(categoryName);
                if (filter.filter(identifier)) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), entry.getKey(), this, entry.getValue());
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorCategory) {
            ContextPartitionSelectorCategory category = (ContextPartitionSelectorCategory) contextPartitionSelector;
            if (category.getLabels() == null || category.getLabels().isEmpty()) {
                return;
            }
            for (Map.Entry<Integer, ContextControllerInstanceHandle> entry : handleCategories.entrySet()) {
                String categoryName = factory.getCategorySpec().getItems().get(entry.getKey()).getName();
                if (category.getLabels().contains(categoryName)) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), entry.getKey(), this, entry.getValue());
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById byId = (ContextPartitionSelectorById) contextPartitionSelector;
            for (Map.Entry<Integer, ContextControllerInstanceHandle> entry : handleCategories.entrySet()) {
                int cpid = entry.getValue().getContextPartitionOrPathId();
                if (byId.getContextPartitionIds().contains(cpid)) {
                    visitor.visit(nestingLevel, pathId, factory.getBinding(), entry.getKey(), this, entry.getValue());
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorAll) {
            for (Map.Entry<Integer, ContextControllerInstanceHandle> entry : handleCategories.entrySet()) {
                String categoryName = factory.getCategorySpec().getItems().get(entry.getKey()).getName();
                visitor.visit(nestingLevel, pathId, factory.getBinding(), entry.getKey(), this, entry.getValue());
            }
            return;
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorCategory.class}, contextPartitionSelector);
    }

    public void activate(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerState controllerState, ContextInternalFilterAddendum activationFilterAddendum, Integer importPathId) {
        if (factory.getFactoryContext().getNestingLevel() == 1) {
            controllerState = ContextControllerStateUtil.getRecoveryStates(factory.getFactoryContext().getStateCache(), factory.getFactoryContext().getOutermostContextName());
        }

        if (controllerState == null) {
            int count = 0;
            for (ContextDetailCategoryItem category : factory.getCategorySpec().getItems()) {
                Map<String, Object> context = ContextPropertyEventType.getCategorizedBean(factory.getFactoryContext().getContextName(), 0, category.getName());
                currentSubpathId++;

                // merge filter addendum, if any
                ContextInternalFilterAddendum filterAddendumToUse = activationFilterAddendum;
                if (factory.hasFiltersSpecsNestedContexts()) {
                    filterAddendumToUse = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
                    factory.populateContextInternalFilterAddendums(filterAddendumToUse, count);
                }

                ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(null, currentSubpathId, null, this, optionalTriggeringEvent, optionalTriggeringPattern, count, context, controllerState, filterAddendumToUse, factory.getFactoryContext().isRecoveringResilient(), ContextPartitionState.STARTED, () -> new ContextPartitionIdentifierCategory(category.getName()));
                handleCategories.put(count, handle);

                factory.getFactoryContext().getStateCache().addContextPath(factory.getFactoryContext().getOutermostContextName(), factory.getFactoryContext().getNestingLevel(), pathId, currentSubpathId, handle.getContextPartitionOrPathId(), count, factory.getBinding());
                count++;
            }
            return;
        }

        int pathIdToUse = importPathId != null ? importPathId : pathId;
        initializeFromState(optionalTriggeringEvent, optionalTriggeringPattern, activationFilterAddendum, controllerState, pathIdToUse, null, false);
    }

    public ContextControllerFactory getFactory() {
        return factory;
    }

    public int getPathId() {
        return pathId;
    }

    public void deactivate() {
        handleCategories.clear();
    }

    private void initializeFromState(EventBean optionalTriggeringEvent,
                                     Map<String, Object> optionalTriggeringPattern,
                                     ContextInternalFilterAddendum activationFilterAddendum,
                                     ContextControllerState controllerState,
                                     int pathIdToUse,
                                     AgentInstanceSelector agentInstanceSelector,
                                     boolean loadingExistingState) {
        TreeMap<ContextStatePathKey, ContextStatePathValue> states = controllerState.getStates();
        NavigableMap<ContextStatePathKey, ContextStatePathValue> childContexts = ContextControllerStateUtil.getChildContexts(factory.getFactoryContext(), pathIdToUse, states);

        int maxSubpathId = Integer.MIN_VALUE;
        for (Map.Entry<ContextStatePathKey, ContextStatePathValue> entry : childContexts.entrySet()) {

            int categoryNumber = (Integer) factory.getBinding().byteArrayToObject(entry.getValue().getBlob(), null);
            ContextDetailCategoryItem category = factory.getCategorySpec().getItems().get(categoryNumber);

            // merge filter addendum, if any
            ContextInternalFilterAddendum filterAddendumToUse = activationFilterAddendum;
            if (factory.hasFiltersSpecsNestedContexts()) {
                filterAddendumToUse = activationFilterAddendum != null ? activationFilterAddendum.deepCopy() : new ContextInternalFilterAddendum();
                factory.populateContextInternalFilterAddendums(filterAddendumToUse, categoryNumber);
            }

            // check if exists already
            if (controllerState.isImported()) {
                ContextControllerInstanceHandle existingHandle = handleCategories.get(categoryNumber);
                if (existingHandle != null) {
                    activationCallback.contextPartitionNavigate(existingHandle, this, controllerState, entry.getValue().getOptionalContextPartitionId(), filterAddendumToUse, agentInstanceSelector, entry.getValue().getBlob(), loadingExistingState);
                    continue;
                }
            }

            Map<String, Object> context = ContextPropertyEventType.getCategorizedBean(factory.getFactoryContext().getContextName(), 0, category.getName());

            int contextPartitionId = entry.getValue().getOptionalContextPartitionId();
            int assignedSubPathId = !controllerState.isImported() ? entry.getKey().getSubPath() : ++currentSubpathId;
            ContextControllerInstanceHandle handle = activationCallback.contextPartitionInstantiate(contextPartitionId, assignedSubPathId, entry.getKey().getSubPath(), this, null, null, categoryNumber, context, controllerState, filterAddendumToUse, loadingExistingState || factory.getFactoryContext().isRecoveringResilient(), entry.getValue().getState(), () -> new ContextPartitionIdentifierCategory(category.getName()));
            handleCategories.put(categoryNumber, handle);

            if (entry.getKey().getSubPath() > maxSubpathId) {
                maxSubpathId = assignedSubPathId;
            }
        }
        if (!controllerState.isImported()) {
            currentSubpathId = maxSubpathId != Integer.MIN_VALUE ? maxSubpathId : 0;
        }
    }
}
