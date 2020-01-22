package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.TopicRuleListItem;

import java.util.stream.Stream;

public interface V1RuleHelper {
    Stream<TopicRuleListItem> listTopicRules();

    Stream<String> listTopicRuleNames();
}
