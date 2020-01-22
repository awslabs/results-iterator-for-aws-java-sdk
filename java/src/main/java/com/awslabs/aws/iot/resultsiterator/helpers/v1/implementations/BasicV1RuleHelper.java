package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.ListTopicRulesRequest;
import com.amazonaws.services.iot.model.TopicRuleListItem;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1RuleHelper;

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
        return new V1ResultsIterator<TopicRuleListItem>(awsIotClient, ListTopicRulesRequest.class).resultStream();
    }

    @Override
    public Stream<String> listTopicRuleNames() {
        return listTopicRules()
                .map(TopicRuleListItem::getRuleName);
    }
}
