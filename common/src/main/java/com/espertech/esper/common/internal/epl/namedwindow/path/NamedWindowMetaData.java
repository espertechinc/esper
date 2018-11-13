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
package com.espertech.esper.common.internal.epl.namedwindow.path;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.Copyable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class NamedWindowMetaData implements Copyable<NamedWindowMetaData> {
    private final EventType eventType;
    private final String namedWindowModuleName;
    private final String contextName;
    private final String[] uniqueness;
    private final boolean isChildBatching;
    private final boolean isEnableIndexShare;
    private final EventType optionalEventTypeAs;
    private final boolean virtualDataWindow;
    private final EventTableIndexMetadata indexMetadata;

    public NamedWindowMetaData(EventType eventType, String namedWindowModuleName, String contextName, String[] uniqueness, boolean isChildBatching, boolean isEnableIndexShare, EventType optionalEventTypeAs, boolean virtualDataWindow) {
        this.eventType = eventType;
        this.namedWindowModuleName = namedWindowModuleName;
        this.contextName = contextName;
        this.uniqueness = uniqueness;
        this.isChildBatching = isChildBatching;
        this.isEnableIndexShare = isEnableIndexShare;
        this.optionalEventTypeAs = optionalEventTypeAs;
        this.indexMetadata = new EventTableIndexMetadata();
        this.virtualDataWindow = virtualDataWindow;
    }

    public NamedWindowMetaData(EventType eventType, String namedWindowModuleName, String contextName, String[] uniqueness, boolean isChildBatching, boolean isEnableIndexShare, EventType optionalEventTypeAs, boolean virtualDataWindow, EventTableIndexMetadata indexMetadata) {
        this.eventType = eventType;
        this.namedWindowModuleName = namedWindowModuleName;
        this.contextName = contextName;
        this.uniqueness = uniqueness;
        this.isChildBatching = isChildBatching;
        this.isEnableIndexShare = isEnableIndexShare;
        this.optionalEventTypeAs = optionalEventTypeAs;
        this.virtualDataWindow = virtualDataWindow;
        this.indexMetadata = indexMetadata;
    }

    public NamedWindowMetaData copy() {
        return new NamedWindowMetaData(eventType, namedWindowModuleName, contextName, uniqueness, isChildBatching, isEnableIndexShare, optionalEventTypeAs, virtualDataWindow, indexMetadata.copy());
    }

    public EventType getEventType() {
        return eventType;
    }

    public String[] getUniqueness() {
        return uniqueness;
    }

    public EventTableIndexMetadata getIndexMetadata() {
        return indexMetadata;
    }

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        return newInstance(NamedWindowMetaData.class, EventTypeUtility.resolveTypeCodegen(eventType, addInitSvc), constant(namedWindowModuleName), constant(contextName), constant(uniqueness),
            constant(isChildBatching), constant(isEnableIndexShare),
            optionalEventTypeAs == null ? constantNull() : EventTypeUtility.resolveTypeCodegen(optionalEventTypeAs, addInitSvc),
            constant(virtualDataWindow));
    }

    public Set<String> getUniquenessAsSet() {
        if (uniqueness == null || uniqueness.length == 0) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(uniqueness));
    }

    public String getContextName() {
        return contextName;
    }

    public void addIndex(String indexName, String indexModuleName, IndexMultiKey imk, QueryPlanIndexItem optionalQueryPlanIndexItem) throws ExprValidationException {
        indexMetadata.addIndexExplicit(false, imk, indexName, indexModuleName, optionalQueryPlanIndexItem, "");
    }

    public boolean isChildBatching() {
        return isChildBatching;
    }

    public boolean isEnableIndexShare() {
        return isEnableIndexShare;
    }

    public EventType getOptionalEventTypeAs() {
        return optionalEventTypeAs;
    }

    public boolean isVirtualDataWindow() {
        return virtualDataWindow;
    }

    public String getNamedWindowModuleName() {
        return namedWindowModuleName;
    }
}
