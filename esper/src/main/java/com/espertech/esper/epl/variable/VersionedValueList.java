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
package com.espertech.esper.epl.variable;

import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

/**
 * A self-cleaning list of versioned-values.
 * <p>
 * The current and prior version are held for lock-less read access in a transient variable.
 * <p>
 * The list relies on transient as well as a read-lock to guard against concurrent modification. However a read lock is only
 * taken when a list of old versions must be updated.
 * <p>
 * When a high watermark is reached, the list on write access removes old versions up to the
 * number of milliseconds compared to current write timestamp.
 * <p>
 * If an older version is requested then held by the list, the list can either throw an exception
 * or return the current value.
 */
public class VersionedValueList<T> {
    private static final Logger log = LoggerFactory.getLogger(VersionedValueList.class);

    // Variables name and read lock; read lock used when older version then the prior version is requested
    private final String name;
    private final Lock readLock;
    private final int highWatermark;    // used for removing older versions
    private final boolean errorWhenNotFound;
    private final long millisecondLifetimeOldVersions;

    // Hold the current and prior version for no-lock reading
    private volatile CurrentValue<T> currentAndPriorValue;

    // Holds the older versions
    private ArrayList<VersionedValue<T>> olderVersions;

    /**
     * Ctor.
     *
     * @param name                           variable name
     * @param initialVersion                 first version number
     * @param initialValue                   first value
     * @param timestamp                      timestamp of first version
     * @param millisecondLifetimeOldVersions number of milliseconds after which older versions get expired and removed
     * @param readLock                       for coordinating update to old versions
     * @param highWatermark                  when the number of old versions reached high watermark, the list inspects size on every write
     * @param errorWhenNotFound              true if an exception should be throw if the requested version cannot be found,
     *                                       or false if the engine should log a warning
     */
    public VersionedValueList(String name, int initialVersion, T initialValue, long timestamp, long millisecondLifetimeOldVersions, Lock readLock, int highWatermark, boolean errorWhenNotFound) {
        this.name = name;
        this.readLock = readLock;
        this.highWatermark = highWatermark;
        this.olderVersions = new ArrayList<VersionedValue<T>>();
        this.errorWhenNotFound = errorWhenNotFound;
        this.millisecondLifetimeOldVersions = millisecondLifetimeOldVersions;

        currentAndPriorValue = new CurrentValue<T>(new VersionedValue<T>(initialVersion, initialValue, timestamp),
                new VersionedValue<T>(-1, null, timestamp));
    }

    /**
     * Returns the name of the value stored.
     *
     * @return value name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve a value for the given version or older then then given version.
     * <p>
     * The implementaton only locks the read lock if an older version the the prior version is requested.
     *
     * @param versionAndOlder the version we are looking for
     * @return value for the version or the next older version, ignoring newer versions
     */
    public T getVersion(int versionAndOlder) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".getVersion Thread " + Thread.currentThread().getId() + " for '" + name + "' retrieving version " + versionAndOlder + " or older");
        }

        T resultValue = null;
        CurrentValue<T> current = currentAndPriorValue;

        if (current.getCurrentVersion().getVersion() <= versionAndOlder) {
            resultValue = current.getCurrentVersion().getValue();
        } else if ((current.getPriorVersion().getVersion() != -1) &&
                (current.getPriorVersion().getVersion() <= versionAndOlder)) {
            resultValue = current.getPriorVersion().getValue();
        } else {
            readLock.lock();

            try {
                current = currentAndPriorValue;

                if (current.getCurrentVersion().getVersion() <= versionAndOlder) {
                    resultValue = current.getCurrentVersion().getValue();
                } else if ((current.getPriorVersion().getVersion() != -1) &&
                        (current.getPriorVersion().getVersion() <= versionAndOlder)) {
                    resultValue = current.getPriorVersion().getValue();
                } else {
                    boolean found = false;
                    for (int i = olderVersions.size() - 1; i >= 0; i--) {
                        VersionedValue<T> entry = olderVersions.get(i);
                        if (entry.getVersion() <= versionAndOlder) {
                            resultValue = entry.getValue();
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        int currentVersion = current.getCurrentVersion().getVersion();
                        int priorVersion = current.getPriorVersion().getVersion();

                        Integer oldestVersion = (olderVersions.size() > 0) ? olderVersions.get(0).getVersion() : null;
                        T oldestValue = (olderVersions.size() > 0) ? olderVersions.get(0).getValue() : null;

                        String text = "Variables value for version '" + versionAndOlder + "' and older could not be found" +
                                " (currentVersion=" + currentVersion + " priorVersion=" + priorVersion + " oldestVersion=" + oldestVersion + " numOldVersions=" + olderVersions.size() + " oldestValue=" + oldestValue + ")";
                        if (errorWhenNotFound) {
                            throw new IllegalStateException(text);
                        }
                        log.warn(text);
                        return current.getCurrentVersion().getValue();
                    }
                }
            } finally {
                readLock.unlock();
            }
        }

        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".getVersion Thread " + Thread.currentThread().getId() + " for '" + name + " version " + versionAndOlder + " or older result is " + resultValue);
        }

        return resultValue;
    }

    /**
     * Add a value and version to the list, returning the prior value of the variable.
     *
     * @param version   for the value to add
     * @param value     to add
     * @param timestamp the time associated with the version
     * @return prior value
     */
    public Object addValue(int version, T value, long timestamp) {
        if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
            log.debug(".addValueMultityped Thread " + Thread.currentThread().getId() + " for '" + name + "' adding version " + version + " at value " + value);
        }

        // push to prior if not already used
        if (currentAndPriorValue.getPriorVersion().getVersion() == -1) {
            currentAndPriorValue = new CurrentValue<T>(new VersionedValue<T>(version, value, timestamp),
                    currentAndPriorValue.getCurrentVersion());
            return currentAndPriorValue.getPriorVersion().getValue();
        }

        // add to list
        VersionedValue<T> priorVersion = currentAndPriorValue.getPriorVersion();
        olderVersions.add(priorVersion);

        // check watermarks
        if (olderVersions.size() >= highWatermark) {
            long expireBefore = timestamp - millisecondLifetimeOldVersions;
            while (olderVersions.size() > 0) {
                VersionedValue<T> oldestVersion = olderVersions.get(0);
                if (oldestVersion.getTimestamp() <= expireBefore) {
                    olderVersions.remove(0);
                } else {
                    break;
                }
            }
        }

        currentAndPriorValue = new CurrentValue<T>(new VersionedValue<T>(version, value, timestamp),
                currentAndPriorValue.getCurrentVersion());
        return currentAndPriorValue.getPriorVersion().getValue();
    }

    /**
     * Returns the current and prior version.
     *
     * @return value
     */
    protected CurrentValue<T> getCurrentAndPriorValue() {
        return currentAndPriorValue;
    }

    /**
     * Returns the list of old versions, for testing purposes.
     *
     * @return list of versions older then current and prior version
     */
    protected ArrayList<VersionedValue<T>> getOlderVersions() {
        return olderVersions;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Variable '").append(name).append("' ");
        buffer.append(" current=").append(currentAndPriorValue.getCurrentVersion().toString());
        buffer.append(" prior=").append(currentAndPriorValue.getCurrentVersion().toString());

        int count = 0;
        for (VersionedValue<T> old : olderVersions) {
            buffer.append(" old(").append(count).append(")=").append(old.toString()).append("\n");
            count++;
        }
        return buffer.toString();
    }
}
