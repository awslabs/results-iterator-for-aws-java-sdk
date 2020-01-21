package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.TopicRuleListItem;

import java.util.List;

public interface V1RuleHelper {
    List<TopicRuleListItem> listTopicRules();

    List<String> listTopicRuleNames();
}
