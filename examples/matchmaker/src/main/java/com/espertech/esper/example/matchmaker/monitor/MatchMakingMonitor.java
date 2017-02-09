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
package com.espertech.esper.example.matchmaker.monitor;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.example.matchmaker.eventbean.MatchAlertBean;
import com.espertech.esper.example.matchmaker.eventbean.MobileUserBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public class MatchMakingMonitor {
    public static final double PROXIMITY_RANGE = 1;

    private final HashSet<Integer> existingUsers = new HashSet<Integer>();
    private final EPServiceProvider epService;
    private final MatchAlertListener matchAlertListener;

    private int mobileUserId;
    private EPStatement locateOther;

    public MatchMakingMonitor(final EPServiceProvider epService, final MatchAlertListener matchAlertListener) {
        this.epService = epService;
        this.matchAlertListener = matchAlertListener;

        // Get called for any user showing up
        EPStatement factory = epService.getEPAdministrator().createPattern("every user=" + MobileUserBean.class.getName());

        factory.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                MobileUserBean user = (MobileUserBean) newEvents[0].get("user");

                // No action if user already known
                if (existingUsers.contains(user.getUserId())) {
                    return;
                }

                log.debug(".update New user encountered, user=" + user.getUserId());

                existingUsers.add(user.getUserId());
                new MatchMakingMonitor(epService, user, matchAlertListener);
            }
        });
    }

    public MatchMakingMonitor(EPServiceProvider epService, MobileUserBean mobileUser, MatchAlertListener matchAlertListener) {
        this.epService = epService;
        this.matchAlertListener = matchAlertListener;
        this.mobileUserId = mobileUser.getUserId();

        // Create patterns that listen to other users
        setupPatterns(mobileUser);

        // Listen to my own location changes so my data is up-to-date
        EPStatement locationChange = epService.getEPAdministrator().createPattern(
                "every myself=" + MobileUserBean.class.getName() +
                        "(userId=" + mobileUser.getUserId() + ")");

        locationChange.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                // When my location changed, re-establish pattern
                locateOther.removeAllListeners();
                MobileUserBean myself = (MobileUserBean) newEvents[0].get("myself");
                MatchMakingMonitor.this.setupPatterns(myself);
            }
        });
    }

    private void setupPatterns(MobileUserBean mobileUser) {
        double locXLow = mobileUser.getLocationX() - PROXIMITY_RANGE;
        double locXHigh = mobileUser.getLocationX() + PROXIMITY_RANGE;
        double locYLow = mobileUser.getLocationY() - PROXIMITY_RANGE;
        double locYHigh = mobileUser.getLocationY() + PROXIMITY_RANGE;

        this.locateOther = epService.getEPAdministrator().createPattern(
                "every other=" + MobileUserBean.class.getName() +
                        "(locationX in [" + locXLow + ":" + locXHigh + "]," +
                        "locationY in [" + locYLow + ":" + locYHigh + "]," +
                        "myGender='" + mobileUser.getPreferredGender() + "'," +
                        "myAgeRange='" + mobileUser.getPreferredAgeRange() + "'," +
                        "myHairColor='" + mobileUser.getPreferredHairColor() + "'," +
                        "preferredGender='" + mobileUser.getMyGender() + "'," +
                        "preferredAgeRange='" + mobileUser.getMyAgeRange() + "'," +
                        "preferredHairColor='" + mobileUser.getMyHairColor() + "'" +
                        ")");

        locateOther.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                MobileUserBean other = (MobileUserBean) newEvents[0].get("other");
                MatchAlertBean alert = new MatchAlertBean(other.getUserId(), MatchMakingMonitor.this.mobileUserId);
                matchAlertListener.emitted(alert);
            }
        });
    }

    private static final Logger log = LoggerFactory.getLogger(MatchMakingMonitor.class);
}
