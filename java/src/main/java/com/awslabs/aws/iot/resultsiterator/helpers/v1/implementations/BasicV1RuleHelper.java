package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.ListTopicRulesRequest;
import com.amazonaws.services.iot.model.TopicRuleListItem;
import com.awslabs.aws.iot.resultsiterator.ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1RuleHelper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class BasicV1RuleHelper implements V1RuleHelper {
    @Inject
    AWSIotClient awsIotClient;

    @Inject
    public BasicV1RuleHelper() {
    }

    @Override
    public List<TopicRuleListItem> listTopicRules() {
        List<TopicRuleListItem> topicRules = new ResultsIterator<TopicRuleListItem>(awsIotClient, ListTopicRulesRequest.class).iterateOverResults();

        return topicRules;
    }

    @Override
    public List<String> listTopicRuleNames() {
        List<TopicRuleListItem> topicRules = listTopicRules();

        List<String> topicRuleNames = new ArrayList<>();

        for (TopicRuleListItem topicRuleListItem : topicRules) {
            topicRuleNames.add(topicRuleListItem.getRuleName());
        }

        return topicRuleNames;
    }
}
