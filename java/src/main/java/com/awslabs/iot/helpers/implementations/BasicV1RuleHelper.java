package com.awslabs.iot.helpers.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.ListTopicRulesRequest;
import com.amazonaws.services.iot.model.TopicRuleListItem;
import com.awslabs.iot.helpers.interfaces.V1RuleHelper;
import com.awslabs.resultsiterator.v1.implementations.V1ResultsIterator;

import javax.inject.Inject;
import java.util.stream.Stream;

public class BasicV1RuleHelper implements V1RuleHelper {
    @Inject
    AWSIotClient awsIotClient;

    @Inject
    public BasicV1RuleHelper() {
    }

    @Override
    public Stream<TopicRuleListItem> listTopicRules() {
        return new V1ResultsIterator<TopicRuleListItem>(awsIotClient, ListTopicRulesRequest.class).stream();
    }

    @Override
    public Stream<String> listTopicRuleNames() {
        return listTopicRules()
                .map(TopicRuleListItem::getRuleName);
    }
}
