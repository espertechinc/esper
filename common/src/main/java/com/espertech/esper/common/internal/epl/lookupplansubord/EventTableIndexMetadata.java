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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;

import java.util.*;

public class EventTableIndexMetadata {
    private final Map<IndexMultiKey, EventTableIndexMetadataEntry> indexes;

    public EventTableIndexMetadata() {
        indexes = new HashMap<IndexMultiKey, EventTableIndexMetadataEntry>();
    }

    private EventTableIndexMetadata(Map<IndexMultiKey, EventTableIndexMetadataEntry> indexes) {
        this.indexes = indexes;
    }

    public void addIndexExplicit(boolean isPrimary, IndexMultiKey indexMultiKey, String explicitIndexName, String explicitIndexModuleName, QueryPlanIndexItem explicitIndexDesc, String deploymentId)
            throws ExprValidationException {
        if (getIndexByName(explicitIndexName) != null) {
            throw new ExprValidationException("An index by name '" + explicitIndexName + "' already exists");
        }
        if (indexes.containsKey(indexMultiKey)) {
            throw new ExprValidationException("An index for the same columns already exists");
        }
        EventTableIndexMetadataEntry entry = new EventTableIndexMetadataEntry(explicitIndexName, explicitIndexModuleName, isPrimary, explicitIndexDesc, explicitIndexName, explicitIndexModuleName, deploymentId);
        entry.addReferringDeployment(deploymentId);
        indexes.put(indexMultiKey, entry);
    }

    public void addIndexNonExplicit(IndexMultiKey indexMultiKey, String deploymentId, QueryPlanIndexItem queryPlanIndexItem) {
        if (indexMultiKey == null) {
            throw new IllegalArgumentException("Null index multikey");
        }
        if (indexes.containsKey(indexMultiKey)) {
            return;
        }
        EventTableIndexMetadataEntry entry = new EventTableIndexMetadataEntry(null, null, false, queryPlanIndexItem, null, null, deploymentId);
        entry.addReferringDeployment(deploymentId);
        indexes.put(indexMultiKey, entry);
    }

    public Map<IndexMultiKey, EventTableIndexMetadataEntry> getIndexes() {
        return indexes;
    }

    public void removeIndex(IndexMultiKey imk) {
        indexes.remove(imk);
    }

    public boolean removeIndexReference(IndexMultiKey index, String referringDeploymentId) {
        if (index == null) {
            throw new IllegalArgumentException("Null index multikey");
        }
        EventTableIndexMetadataEntry entry = indexes.get(index);
        if (entry == null) {
            return false;
        }
        return entry.removeReferringStatement(referringDeploymentId);
    }

    public void addIndexReference(String indexName, String deploymentId) {
        Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry = findIndex(indexName);
        if (entry == null) {
            return;
        }
        entry.getValue().addReferringDeployment(deploymentId);
    }

    public void removeIndexReference(String indexName, String deploymentId) {
        Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry = findIndex(indexName);
        if (entry == null) {
            return;
        }
        entry.getValue().removeReferringStatement(deploymentId);
    }

    public void addIndexReference(IndexMultiKey indexMultiKey, String deploymentId) {
        EventTableIndexMetadataEntry entry = indexes.get(indexMultiKey);
        if (entry == null) {
            return;
        }
        entry.addReferringDeployment(deploymentId);
    }

    public IndexMultiKey getIndexByName(String indexName) {
        Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry = findIndex(indexName);
        if (entry == null) {
            return null;
        }
        return entry.getKey();
    }

    public String getIndexDeploymentId(String indexName) {
        Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry = findIndex(indexName);
        if (entry == null) {
            return null;
        }
        return entry.getValue().getDeploymentId();
    }

    public Collection<String> getRemoveRefIndexesDereferenced(String deploymentId) {
        Collection<String> indexNamesDerrefd = null;
        for (Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry : indexes.entrySet()) {
            boolean last = entry.getValue().removeReferringStatement(deploymentId);
            if (last) {
                if (indexNamesDerrefd == null) {
                    indexNamesDerrefd = new ArrayDeque<String>(2);
                }
                indexNamesDerrefd.add(entry.getValue().getOptionalIndexName());
            }
        }
        if (indexNamesDerrefd == null) {
            return Collections.emptyList();
        }
        for (String name : indexNamesDerrefd) {
            removeIndex(getIndexByName(name));
        }
        return indexNamesDerrefd;
    }

    private Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> findIndex(String indexName) {
        for (Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry : indexes.entrySet()) {
            if (entry.getValue().getOptionalIndexName() != null && entry.getValue().getOptionalIndexName().equals(indexName)) {
                return entry;
            }
        }
        return null;
    }

    public String[][] getUniqueIndexProps() {
        ArrayDeque<String[]> uniques = new ArrayDeque<String[]>(2);
        for (Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> entry : indexes.entrySet()) {
            if (entry.getKey().isUnique()) {
                String[] props = new String[entry.getKey().getHashIndexedProps().length];
                for (int i = 0; i < entry.getKey().getHashIndexedProps().length; i++) {
                    props[i] = entry.getKey().getHashIndexedProps()[i].getIndexPropName();
                }
                uniques.add(props);
            }
        }
        return uniques.toArray(new String[uniques.size()][]);
    }

    public EventTableIndexMetadata copy() {
        return new EventTableIndexMetadata(new HashMap<>(indexes));
    }
}
