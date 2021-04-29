package com.awslabs.ecr;

import com.awslabs.ecr.data.EcrRepositoryName;
import com.awslabs.ecr.data.ImageTag;
import com.awslabs.ecr.interfaces.DockerClientProvider;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

import javax.inject.Inject;

public class EcrDockerHelper extends AbstractDockerHelper {
    private static final Logger log = LoggerFactory.getLogger(EcrDockerHelper.class);
    @Inject
    EcrDockerClientProvider ecrDockerClientProvider;
    @Inject
    EcrClient ecrClient;
    @Inject
    ProgressHandler progressHandler;

    @Inject
    public EcrDockerHelper() {
    }

    @Override
    Option<ProgressHandler> getProgressHandlerOption() {
        return Option.of(progressHandler);
    }

    @Override
    DockerClientProvider getDockerClientProvider() {
        return ecrDockerClientProvider;
    }

    @Override
    public DockerClient getDockerClient() {
        return ecrDockerClientProvider.get();
    }

    protected EcrClient getEcrClient() {
        return ecrClient;
    }

    @Override
    public String getEcrProxyEndpoint() {
        return ecrDockerClientProvider.getAuthorizationData().proxyEndpoint();
    }

    @Override
    public Repository createRepositoryIfNecessary(EcrRepositoryName ecrRepositoryName) {
        return Try.of(() -> describeRepository(ecrRepositoryName))
                // If the repository was not found then we'll create it, otherwise do nothing
                .recover(RepositoryNotFoundException.class, ignore -> createAndDescribeRepository(ecrRepositoryName))
                // Throw any additional exceptions
                .get()
                // Get the first repository (there should be exactly one if nothing went wrong)
                .get();
    }

    @Override
    public DeleteRepositoryResponse deleteRepository(EcrRepositoryName ecrRepositoryName) {
        DeleteRepositoryRequest deleteRepositoryRequest = DeleteRepositoryRequest.builder()
                .repositoryName(ecrRepositoryName.getName())
                .build();

        return getEcrClient().deleteRepository(deleteRepositoryRequest);
    }

    private Option<Repository> describeRepository(EcrRepositoryName ecrRepositoryName) {
        DescribeRepositoriesRequest describeRepositoriesRequest = DescribeRepositoriesRequest.builder()
                .repositoryNames(ecrRepositoryName.getName())
                .build();

        return new ResultsIterator<Repository>(getEcrClient(), describeRepositoriesRequest).stream().toOption();
    }

    private Option<Repository> createAndDescribeRepository(EcrRepositoryName ecrRepositoryName) {
        createRepository(ecrRepositoryName);

        return describeRepository(ecrRepositoryName);
    }

    @Override
    public CreateRepositoryResponse createRepository(EcrRepositoryName ecrRepositoryName) {
        log.info(String.join("", "Attempting to create an ECR repository [", ecrRepositoryName.getName(), "]"));

        return getEcrClient().createRepository(CreateRepositoryRequest.builder()
                .repositoryName(ecrRepositoryName.getName())
                .build());
    }

    @Override
    public String getFullImageTag(EcrRepositoryName ecrRepositoryName, ImageTag imageTag) {
        return String.join(":", ecrRepositoryName.getName(), imageTag.getTag());
    }

    @Override
    public DeleteRepositoryResponse deleteRepositoryAndImages(EcrRepositoryName ecrRepositoryName) {
        ListImagesRequest listImagesRequest = ListImagesRequest.builder()
                .repositoryName(ecrRepositoryName.getName())
                .build();

        EcrClient ecrClient = getEcrClient();

        List<ImageIdentifier> imageIdentifierList = new ResultsIterator<ImageIdentifier>(getEcrClient(), listImagesRequest).stream().toList();

        BatchDeleteImageRequest batchDeleteImageRequest = BatchDeleteImageRequest.builder()
                .imageIds(imageIdentifierList.asJava())
                .repositoryName(ecrRepositoryName.getName())
                .build();

        List<ImageIdentifier> deleted = List.ofAll(ecrClient.batchDeleteImage(batchDeleteImageRequest).imageIds());

        List<ImageIdentifier> remaining = imageIdentifierList.removeAll(deleted);

        if (remaining.size() != 0) {
            throw new RuntimeException("Couldn't delete all of the images in [" + ecrRepositoryName.getName() + "], can not delete the repository");
        }

        log.info("Deleting repository [" + ecrRepositoryName.getName() + "]");

        return deleteRepository(ecrRepositoryName);
    }
}
