package com.awslabs.s3.helper.implementations;

import com.awslabs.resultsiterator.implementations.BasicInjector;
import com.awslabs.resultsiterator.implementations.DaggerBasicInjector;
import com.awslabs.s3.helpers.interfaces.S3Helper;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicS3HelperTests {
    private final Logger log = LoggerFactory.getLogger(BasicS3HelperTests.class);
    private S3Helper s3Helper;

    @Before
    public void setup() {
        BasicInjector injector = DaggerBasicInjector.create();
        s3Helper = injector.s3Helper();
    }

    // TODO: Add presign tests here
}
