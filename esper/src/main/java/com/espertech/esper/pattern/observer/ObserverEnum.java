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
package com.espertech.esper.pattern.observer;

/**
 * Enum for all build-in observers.
 */
public enum ObserverEnum {
    /**
     * Observer for letting pass/waiting an interval amount of time.
     */
    TIMER_INTERVAL("timer", "interval", TimerIntervalObserverFactory.class),

    /**
     * Observer for 'at' (crontab) observation of timer events.
     */
    TIMER_CRON("timer", "at", TimerAtObserverFactory.class),

    /**
     * Observer for iso8601 date observation of timer events.
     */
    TIMER_ISO8601("timer", "schedule", TimerScheduleObserverFactory.class);

    private final String namespace;
    private final String name;
    private final Class clazz;

    ObserverEnum(String namespace, String name, Class clazz) {
        this.namespace = namespace;
        this.name = name;
        this.clazz = clazz;
    }

    /**
     * Returns the observer namespace name.
     *
     * @return namespace name
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns name.
     *
     * @return short name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the implementation class.
     *
     * @return implementation class
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns observer enum for namespace name and observer name.
     *
     * @param namespace - namespace name
     * @param name      - observer name
     * @return enum
     */
    public static ObserverEnum forName(String namespace, String name) {
        for (ObserverEnum observerEnum : ObserverEnum.values()) {
            if ((observerEnum.namespace.equals(namespace)) && (observerEnum.name.equals(name))) {
                return observerEnum;
            }
        }

        return null;
    }
}
