package com.awslabs.ecr.interfaces;

import com.awslabs.ecr.data.*;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Image;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import software.amazon.awssdk.services.ecr.model.CreateRepositoryResponse;
import software.amazon.awssdk.services.ecr.model.DeleteRepositoryResponse;
import software.amazon.awssdk.services.ecr.model.Repository;

import java.nio.file.Path;

public interface DockerHelper {
    boolean isDockerAvailable();

    Repository createRepositoryIfNecessary(EcrRepositoryName ecrRepositoryName);

    String getEcrProxyEndpoint();

    DeleteRepositoryResponse deleteRepository(EcrRepositoryName ecrRepositoryName);

    CreateRepositoryResponse createRepository(EcrRepositoryName ecrRepositoryName);

    String getFullImageTag(EcrRepositoryName ecrRepositoryName, ImageTag imageTag);

    Tuple2<DockerClient, Option<Image>> getImageFromTag(DockerClient dockerClient, ImageTag imageTag);

    Tuple2<DockerClient, Option<ImageId>> buildImage(DockerClient dockerClient, Path directory, Option<Path> dockerfilePathOption, List<DockerClient.BuildParam> buildParamList);

    DockerClient pushImage(DockerClient dockerClient, EcrRepositoryName ecrRepositoryName, ImageId imageId);

    Tuple2<DockerClient, Option<ContainerId>> createContainer(DockerClient dockerClient, ContainerName containerName, ImageTag imageTag);

    DockerClient createAndStartContainer(DockerClient dockerClient, ContainerName containerName, ImageTag imageTag);

    Tuple2<DockerClient, Option<Container>> getContainerFromImageId(DockerClient dockerClient, ImageId imageId);

    DockerClient dumpImagesInfo(DockerClient dockerClient);

    DockerClient dumpContainersInfo(DockerClient dockerClient);

    DockerClient stopContainerByImageTag(DockerClient dockerClient, ImageTag imageTag);

    DockerClient stopContainer(DockerClient dockerClient, Container container);

    DockerClient pullImage(DockerClient dockerClient, ImageId imageId);

    DockerClient pullImage(DockerClient dockerClient, ImageTag imageTag);
}
