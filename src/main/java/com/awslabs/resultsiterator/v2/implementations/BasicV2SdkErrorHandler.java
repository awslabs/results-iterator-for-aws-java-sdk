package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.resultsiterator.v2.interfaces.V2SdkErrorHandler;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;

import javax.inject.Inject;

public class BasicV2SdkErrorHandler implements V2SdkErrorHandler {
    private final Logger log = LoggerFactory.getLogger(BasicV2SdkErrorHandler.class);
    private String REGION_EXCEPTION_1 = "Unable to find a region";
    private String REGION_EXCEPTION_2 = "Unable to load region from any of the providers in the chain";
    private String MISSING_CREDENTIALS_EXCEPTION = "Unable to load AWS credentials from any provider in the chain";
    private String BAD_CREDENTIALS_EXCEPTION = "The security token included in the request is invalid";
    private String BAD_PERMISSIONS_EXCEPTION = "is not authorized to perform";

    private String GENERIC_CREDENTIALS_SOLUTION = "Have you set up the .aws directory with configuration and credentials yet?";

    private String REGION_ERROR = "Could not determine the AWS region.";
    private String REGION_SOLUTION = "Set the AWS_REGION environment variable if the region needs to be explicitly set.";

    private String MISSING_CREDENTIALS_ERROR = "Could not find AWS credentials.";
    private String MISSING_CREDENTIALS_SOLUTION = "Set the AWS_ACCESS_KEY_ID and the AWS_SECRET_ACCESS_KEY environment variables if the credentials need to be explicitly set.";

    private String BAD_CREDENTIALS_ERROR = "The credentials provided may have been deleted or may be invalid.";
    private String BAD_CREDENTIALS_SOLUTION = "Make sure the credentials still exist in IAM and that they have permissions to use the IAM, Greengrass, and IoT services.";

    private String BAD_PERMISSIONS_SOLUTION = "Add the necessary permissions and try again.";

    private String HTTP_REQUEST_EXCEPTION = "Unable to execute HTTP request";
    private String HTTP_REQUEST_SOLUTION = "Couldn't contact one of the AWS services, is your Internet connection down?";

    @Inject
    public BasicV2SdkErrorHandler() {
    }

    @Override
    public Void handleSdkError(SdkClientException e) {
        String message = e.getMessage();
        List<String> errors = List.empty();

        if (message.contains(REGION_EXCEPTION_1) || message.contains(REGION_EXCEPTION_2)) {
            errors = errors.append(REGION_ERROR);
            errors = errors.append(GENERIC_CREDENTIALS_SOLUTION);
            errors = errors.append(REGION_SOLUTION);
        } else if (message.contains(MISSING_CREDENTIALS_EXCEPTION)) {
            errors = errors.append(MISSING_CREDENTIALS_ERROR);
            errors = errors.append(GENERIC_CREDENTIALS_SOLUTION);
            errors = errors.append(MISSING_CREDENTIALS_SOLUTION);
        } else if (message.contains(BAD_CREDENTIALS_EXCEPTION)) {
            errors = errors.append(BAD_CREDENTIALS_ERROR);
            errors = errors.append(BAD_CREDENTIALS_SOLUTION);
        } else if (message.contains(BAD_PERMISSIONS_EXCEPTION)) {
            errors = errors.append(message.substring(0, message.indexOf("(")));
            errors = errors.append(BAD_PERMISSIONS_SOLUTION);
        } else if (message.contains(HTTP_REQUEST_EXCEPTION)) {
            errors = errors.append(message.substring(0, message.indexOf(":")));
            errors = errors.append(HTTP_REQUEST_SOLUTION);
        }

        if (errors.size() != 0) {
            errors.forEach(log::error);
            System.exit(1);
        } else {
            throw e;
        }

        return null;
    }
}
