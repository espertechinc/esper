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
package com.espertech.esperio.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EsperIOKafkaInputSubscriberByTopicList implements EsperIOKafkaInputSubscriber {
    private static final Logger log = LoggerFactory.getLogger(EsperIOKafkaInputSubscriberByTopicList.class);

    public void subscribe(EsperIOKafkaInputSubscriberContext context) {
        String topicsCSV = EsperIOKafkaInputAdapter.getRequiredProperty(context.getProperties(), EsperIOKafkaConfig.TOPICS_CONFIG);
        String[] topicNames = topicsCSV.split(",");
        List<String> topics = new ArrayList<>();
        for (String topicName : topicNames) {
            if (topicName.trim().length() > 0) {
                topics.add(topicName.trim());
            }
        }

        log.info("Subscribing to topics {}", topics);
        context.getConsumer().subscribe(topics);
    }
}
