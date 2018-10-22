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
package com.espertech.esper.common.internal.context.controller.category;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.internal.collection.IntSeqKey;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerBase;
import com.espertech.esper.common.internal.context.mgr.ContextControllerSelectorUtil;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionInstantiationResult;
import com.espertech.esper.common.internal.context.mgr.ContextPartitionVisitor;

import java.util.Map;

public abstract class ContextControllerCategory extends ContextControllerBase {
    protected final ContextControllerCategoryFactory factory;
    protected ContextControllerCategorySvc categorySvc;

    public ContextControllerCategory(ContextManagerRealization realization, ContextControllerCategoryFactory factory) {
        super(realization);
        this.factory = factory;
    }

    public void activate(IntSeqKey path, Object[] parentPartitionKeys, EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern) {
        int count = 0;
        ContextControllerDetailCategoryItem[] categories = factory.getCategorySpec().getItems();
        int[] subpathOrCPIds = new int[categories.length];

        for (int i = 0; i < categories.length; i++) {
            ContextPartitionInstantiationResult result = realization.contextPartitionInstantiate(path, count, this, null, null, parentPartitionKeys, count);
            subpathOrCPIds[i] = result.getSubpathOrCPId();
            count++;
        }

        categorySvc.mgmtCreate(path, parentPartitionKeys, subpathOrCPIds);
    }

    public void deactivate(IntSeqKey path, boolean terminateChildContexts) {
        int[] subpathIdorCPs = categorySvc.mgmtDelete(path);
        if (subpathIdorCPs != null && terminateChildContexts) {
            for (int i = 0; i < factory.getCategorySpec().getItems().length; i++) {
                realization.contextPartitionTerminate(path, subpathIdorCPs[i], this, null, false, null);
            }
        }
    }

    public ContextControllerCategoryFactory getFactory() {
        return factory;
    }

    public void visitSelectedPartitions(IntSeqKey path, ContextPartitionSelector contextPartitionSelector, ContextPartitionVisitor visitor, ContextPartitionSelector[] selectorPerLevel) {
        if (contextPartitionSelector instanceof ContextPartitionSelectorCategory) {
            ContextPartitionSelectorCategory category = (ContextPartitionSelectorCategory) contextPartitionSelector;
            if (category.getLabels() == null || category.getLabels().isEmpty()) {
                return;
            }
            int[] ids = categorySvc.mgmtGetSubpathOrCPIds(path);
            if (ids != null) {
                int count = -1;
                for (ContextControllerDetailCategoryItem categoryItem : factory.getCategorySpec().getItems()) {
                    count++;
                    int subpathOrCPID = ids[count];
                    if (category.getLabels().contains(categoryItem.getName())) {
                        realization.contextPartitionRecursiveVisit(path, subpathOrCPID, this, visitor, selectorPerLevel);
                    }
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorFiltered) {
            ContextPartitionSelectorFiltered filter = (ContextPartitionSelectorFiltered) contextPartitionSelector;
            int[] ids = categorySvc.mgmtGetSubpathOrCPIds(path);
            if (ids != null) {
                int count = -1;
                for (ContextControllerDetailCategoryItem categoryItem : factory.getCategorySpec().getItems()) {
                    ContextPartitionIdentifierCategory identifierCategory = new ContextPartitionIdentifierCategory(categoryItem.getName());
                    count++;
                    if (factory.getFactoryEnv().isLeaf()) {
                        identifierCategory.setContextPartitionId(ids[count]);
                    }
                    if (filter.filter(identifierCategory)) {
                        realization.contextPartitionRecursiveVisit(path, ids[count], this, visitor, selectorPerLevel);
                    }
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorAll) {
            int[] ids = categorySvc.mgmtGetSubpathOrCPIds(path);
            if (ids != null) {
                for (int id : ids) {
                    realization.contextPartitionRecursiveVisit(path, id, this, visitor, selectorPerLevel);
                }
            }
            return;
        }
        if (contextPartitionSelector instanceof ContextPartitionSelectorById) {
            ContextPartitionSelectorById byId = (ContextPartitionSelectorById) contextPartitionSelector;
            int[] ids = categorySvc.mgmtGetSubpathOrCPIds(path);
            for (int id : ids) {
                if (byId.getContextPartitionIds().contains(id)) {
                    realization.contextPartitionRecursiveVisit(path, id, this, visitor, selectorPerLevel);
                }
            }
        }
        throw ContextControllerSelectorUtil.getInvalidSelector(new Class[]{ContextPartitionSelectorCategory.class}, contextPartitionSelector);
    }

    public void destroy() {
        categorySvc.destroy();
    }
}
