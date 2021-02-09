package com.awslabs.iot.helpers.interfaces;

import com.amazonaws.services.iot.model.TopicRuleListItem;
import io.vavr.collection.Stream;

public interface V1RuleHelper {
    Stream<TopicRuleListItem> listTopicRules();

    Stream<String> listTopicRuleNames();
}
