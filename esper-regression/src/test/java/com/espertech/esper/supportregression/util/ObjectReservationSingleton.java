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
package com.espertech.esper.supportregression.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton class for testing out multi-threaded code.
 * Allows reservation and de-reservation of any Object. Reserved objects are added to a HashSet and
 * removed from the HashSet upon de-reservation.
 */
public class ObjectReservationSingleton {
    private HashSet<Object> reservedObjects = new HashSet<Object>();
    private Lock reservedIdsLock = new ReentrantLock();

    private static ObjectReservationSingleton ourInstance = new ObjectReservationSingleton();

    public static ObjectReservationSingleton getInstance() {
        return ourInstance;
    }

    private ObjectReservationSingleton() {
    }

    /**
     * Reserve an object, returning true when successfully reserved or false when the object is already reserved.
     *
     * @param object - object to reserve
     * @return true if reserved, false to indicate already reserved
     */
    public boolean reserve(Object object) {
        reservedIdsLock.lock();

        if (reservedObjects.contains(object)) {
            reservedIdsLock.unlock();
            return false;
        }

        reservedObjects.add(object);

        reservedIdsLock.unlock();
        return true;
    }

    /**
     * Unreserve an object. Logs a fatal error if the unreserve failed.
     *
     * @param object - object to unreserve
     */
    public void unreserve(Object object) {
        reservedIdsLock.lock();

        if (!reservedObjects.contains(object)) {
            log.error(".unreserve FAILED, object=" + object);
            reservedIdsLock.unlock();
            return;
        }

        reservedObjects.remove(object);

        reservedIdsLock.unlock();
    }

    private static final Logger log = LoggerFactory.getLogger(ObjectReservationSingleton.class);
}
