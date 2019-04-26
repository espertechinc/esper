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
package com.espertech.esper.common.client.meta;

/**
 * Pair of public and protected event type id.
 * <p>
 * Preconfigured event types only have a public id. Their public id is derived from the event type name.
 * </p>
 * <p>
 * All other event types have a public id and protected id.
 * Their public id is derived from the deployment id.
 * Their protected id is derived from the event type name.
 * </p>
 */
public class EventTypeIdPair {
    private final long publicId;
    private final long protectedId;
    private final int hash;

    /**
     * Ctor.
     *
     * @param publicId    public id
     * @param protectedId protected if
     */
    public EventTypeIdPair(long publicId, long protectedId) {
        this.publicId = publicId;
        this.protectedId = protectedId;

        int result = (int) (publicId ^ (publicId >>> 32));
        hash = 31 * result + (int) (protectedId ^ (protectedId >>> 32));
    }

    /**
     * Returns the public id
     *
     * @return public id
     */
    public long getPublicId() {
        return publicId;
    }

    /**
     * Returns the protected id
     *
     * @return protected id
     */
    public long getProtectedId() {
        return protectedId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventTypeIdPair that = (EventTypeIdPair) o;

        if (publicId != that.publicId) return false;
        return protectedId == that.protectedId;
    }

    public int hashCode() {
        return hash;
    }

    /**
     * Returns an unassigned value that has -1 as the public and protected id
     *
     * @return pair with unassigned (-1) values
     */
    public static EventTypeIdPair unassigned() {
        return new EventTypeIdPair(-1, -1);
    }

    public String toString() {
        return "EventTypeIdPair{" +
            "publicId=" + publicId +
            ", protectedId=" + protectedId +
            ", hash=" + hash +
            '}';
    }
}
