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
package com.espertech.esper.core.service;

import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.ManagedReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for holding references between statements and their event type use.
 */
public class StatementEventTypeRefImpl implements StatementEventTypeRef {
    private static final Logger log = LoggerFactory.getLogger(StatementEventTypeRefImpl.class);

    private final ManagedReadWriteLock mapLock;
    private final HashMap<String, Set<String>> typeToStmt;
    private final HashMap<String, String[]> stmtToType;

    /**
     * Ctor.
     */
    public StatementEventTypeRefImpl() {
        typeToStmt = new HashMap<String, Set<String>>();
        stmtToType = new HashMap<String, String[]>();
        mapLock = new ManagedReadWriteLock("StatementEventTypeRefImpl", false);
    }

    public void addReferences(String statementName, String[] eventTypesReferenced) {
        if (eventTypesReferenced.length == 0) {
            return;
        }

        mapLock.acquireWriteLock();
        try {
            for (String reference : eventTypesReferenced) {
                addReference(statementName, reference);
            }
        } finally {
            mapLock.releaseWriteLock();
        }
    }

    public void removeReferencesStatement(String statementName) {
        mapLock.acquireWriteLock();
        try {
            String[] types = stmtToType.remove(statementName);
            if (types != null) {
                for (String type : types) {
                    removeReference(statementName, type);
                }
            }
        } finally {
            mapLock.releaseWriteLock();
        }
    }

    public void removeReferencesType(String name) {
        mapLock.acquireWriteLock();
        try {
            Set<String> statementNames = typeToStmt.remove(name);
            if (statementNames != null) {
                for (String statementName : statementNames) {
                    removeReference(statementName, name);
                }
            }
        } finally {
            mapLock.releaseWriteLock();
        }
    }

    public boolean isInUse(String eventTypeName) {
        mapLock.acquireReadLock();
        try {
            return typeToStmt.containsKey(eventTypeName);
        } finally {
            mapLock.releaseReadLock();
        }
    }

    public Set<String> getStatementNamesForType(String eventTypeName) {
        mapLock.acquireReadLock();
        try {
            Set<String> types = typeToStmt.get(eventTypeName);
            if (types == null) {
                return Collections.EMPTY_SET;
            }
            return Collections.unmodifiableSet(types);
        } finally {
            mapLock.releaseReadLock();
        }
    }

    public String[] getTypesForStatementName(String statementName) {
        mapLock.acquireReadLock();
        try {
            String[] types = stmtToType.get(statementName);
            if (types == null) {
                return new String[0];
            }
            return types;
        } finally {
            mapLock.releaseReadLock();
        }
    }

    private void addReference(String statementName, String eventTypeName) {
        // add to types
        Set<String> statements = typeToStmt.get(eventTypeName);
        if (statements == null) {
            statements = new HashSet<String>();
            typeToStmt.put(eventTypeName, statements);
        }
        statements.add(statementName);

        // add to statements
        String[] types = stmtToType.get(statementName);
        if (types == null) {
            types = new String[]{eventTypeName};
        } else {
            int index = CollectionUtil.findItem(types, eventTypeName);
            if (index == -1) {
                types = (String[]) CollectionUtil.arrayExpandAddSingle(types, eventTypeName);
            }
        }
        stmtToType.put(statementName, types);
    }

    private void removeReference(String statementName, String eventTypeName) {
        // remove from types
        Set<String> statements = typeToStmt.get(eventTypeName);
        if (statements != null) {
            if (!statements.remove(statementName)) {
                log.info("Failed to find statement name '" + statementName + "' in collection");
            }

            if (statements.isEmpty()) {
                typeToStmt.remove(eventTypeName);
            }
        }

        // remove from statements
        String[] types = stmtToType.get(statementName);
        if (types != null) {
            int index = CollectionUtil.findItem(types, eventTypeName);
            if (index != -1) {
                if (types.length == 1) {
                    stmtToType.remove(statementName);
                } else {
                    types = (String[]) CollectionUtil.arrayShrinkRemoveSingle(types, index);
                    stmtToType.put(statementName, types);
                }
            } else {
                log.info("Failed to find type name '" + eventTypeName + "' in collection");
            }
        }
    }

    /**
     * For testing, returns the mapping of event type name to statement names.
     *
     * @return mapping
     */
    protected HashMap<String, Set<String>> getTypeToStmt() {
        return typeToStmt;
    }

    /**
     * For testing, returns the mapping of statement names to event type names.
     *
     * @return mapping
     */
    protected HashMap<String, String[]> getStmtToType() {
        return stmtToType;
    }
}
