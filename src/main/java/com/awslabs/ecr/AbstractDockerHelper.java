package com.awslabs.ecr;

import com.awslabs.ecr.data.*;
import com.awslabs.ecr.interfaces.DockerClientProvider;
import com.awslabs.ecr.interfaces.DockerHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.*;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

import javax.ws.rs.ProcessingException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractDockerHelper implements DockerHelper {
    private final Logger log = LoggerFactory.getLogger(AbstractDockerHelper.class);

    abstract Option<ProgressHandler> getProgressHandlerOption();

    abstract DockerClientProvider getDockerClientProvider();

    @Override
    public boolean isDockerAvailable() {
        return Try.withResources(this::getDockerClient)
                // Try to list the images Docker has
                .of(this::listImages)
                // If no exceptions are thrown then Docker is available (even if the list is empty)
                .map(list -> true)
                // Either of these exceptions indicate Docker is not available
                .recover(DockerException.class, false)
                .recover(InterruptedException.class, false)
                .onFailure(Throwable::printStackTrace)
                // Any other exceptions indicate that something else went wrong, rethrow them
                .get();
    }

    private boolean printDockerNotAvailableErrorAndReturnFalse(Throwable throwable) {
        log.error("Failed to connect to Docker.  Is it running on this host?  Is it listening on the standard Unix socket?");
        log.error(String.join("", "Docker exception message [", throwable.getMessage(), "]"));

        return false;
    }

    private Tuple2<DockerClient, List<Image>> listImages(DockerClient dockerClient) {
        return dockerTryOf(() -> dockerClient.listImages(DockerClient.ListImagesParam.allImages()))
                .map(List::ofAll)
                .map(list -> Tuple.of(dockerClient, list))
                .get();
    }

    private void checkFfiException(DockerException dockerException) {
        if (!(dockerException.getCause() instanceof ExecutionException)) {
            return;
        }

        ExecutionException executionException = (ExecutionException) dockerException.getCause();

        if (!(executionException.getCause() instanceof ProcessingException)) {
            return;
        }

        ProcessingException processingException = (ProcessingException) executionException.getCause();

        if (!(processingException.getCause() instanceof UnsatisfiedLinkError)) {
            return;
        }

        dockerException.printStackTrace();

        log.error("Detected an UnsatisfiedLinkError. This typically occurs when running on an architecture that is not supported by the jnr-unixsocket project.");
        log.error("For example: Apple M1 Macs with Java 8 experience this issue but Apple M1 Macs with Java 11 do not.");
        log.error("The full stack trace is above. Try switching JVMs or open a ticket with the jnr-unixsocket project to support your platform.");
        log.error("jnr-unixsocket WILL NOT provide support for this application. They can only add support for new architectures which this project can then utilize.");
        log.error("jnr-unixsocket on Github - https://github.com/jnr/jnr-unixsocket");
    }

    public abstract DockerClient getDockerClient();

   protected abstract EcrClient getEcrClient();

    @Override
    public Tuple2<DockerClient, Option<Image>> getImageFromTag(DockerClient dockerClient, ImageTag imageTag) {
        Option<Image> imageOption = Stream.ofAll(listImages(dockerClient)._2)
                // Get the image with its list of tags
                .map(image -> Tuple.of(image, Option.of(image.repoTags())))
                // Only look at images with a defined tag list
                .filter(tuple -> tuple._2.isDefined())
                // Get the option since we know it is defined now
                .map(tuple -> Tuple.of(tuple._1, tuple._2.get()))
                // Convert the tag list to a vavr list
                .map(tuple -> Tuple.of(tuple._1, List.ofAll(tuple._2)))
                // Find any image that is associated with the requested tag
                .filter(tuple -> tuple._2.contains(imageTag.getTag()))
                // Pull out just the image
                .map(tuple -> tuple._1)
                // Convert it to an option (take the first result and handle the case where there are no results)
                .toOption();

        if (imageOption.isDefined()) {
            log.info(String.join("", "Found tag [", imageTag.getTag(), "] with ID [", imageOption.get().id(), "]"));
        } else {
            log.info(String.join("", "Tag [", imageTag.getTag(), "] not found"));
        }

        return Tuple.of(dockerClient, imageOption);
    }

    @Override
    public Tuple2<DockerClient, Option<ImageId>> buildImage(DockerClient dockerClient, Path directory, Option<Path> dockerfileOption, List<DockerClient.BuildParam> buildParamList) {
        if (dockerfileOption.isDefined()) {
            // Docker client will not accept any absolute or relative paths. To avoid ignoring invalid paths we'll throw
            //   an exception here if we see either of those.
            Path dockerfile = dockerfileOption.get();

            if (dockerfile.isAbsolute() || (dockerfile.getNameCount() != 1)) {
                throw new RuntimeException("Docker only accepts filenames for the Dockerfile option. It must not be a full path (relative or absolute).");
            }

            buildParamList = buildParamList.append(DockerClient.BuildParam.dockerfile(dockerfile));
        }

        List<DockerClient.BuildParam> finalBuildParamList = buildParamList;

        return dockerTryOf(() -> Tuple.of(dockerClient, dockerClient.build(directory,
                getProgressHandlerOption().getOrNull(),
                finalBuildParamList.toJavaArray(DockerClient.BuildParam[]::new))))
                .map(tuple -> Tuple.of(tuple._1, Option.of(tuple._2)))
                .map(tuple -> Tuple.of(tuple._1, tuple._2.map(value -> (ImageId) ImmutableImageId.builder().id(value).build())))
                .get();
    }

    private DockerClient tagImage(DockerClient dockerClient, ImageId imageId, ImageTag imageTag) {
        dockerTryRun(() -> dockerClient.tag(imageId.getId(), imageTag.getTag())).get();

        return dockerClient;
    }

    private DockerClient push(DockerClient dockerClient, String shortEcrEndpointAndRepo) {
        dockerTryRun(() -> dockerClient.push(shortEcrEndpointAndRepo, getProgressHandlerOption().getOrNull(), getRegistryAuth())).get();

        return dockerClient;
    }

    private RegistryAuth getRegistryAuth() {
        return Try.of(() -> getDockerClientProvider().getRegistryAuthSupplier().authFor("")).get();
    }

    @Override
    public DockerClient pushImage(DockerClient dockerClient, EcrRepositoryName ecrRepositoryName, ImageId imageId) {
        createRepositoryIfNecessary(ecrRepositoryName);
        String shortEcrEndpoint = getEcrProxyEndpoint().substring("https://".length()); // Remove leading https://
        String shortEcrEndpointAndRepo = String.join("/", shortEcrEndpoint, ecrRepositoryName.getName());

        ImageTag imageTag = ImmutableImageTag.builder().tag(String.join(":", shortEcrEndpointAndRepo, "latest")).build();

        // Does the error handling from dockerTryOf propagate to the .andThen() call?
        dockerTryOf(() -> tagImage(dockerClient, imageId, imageTag))
                .andThen(() -> push(dockerClient, shortEcrEndpointAndRepo))
                .get();

        log.info(String.join("", "Pushed image tag [", imageTag.getTag(), "]"));

        return dockerClient;
    }

    @Override
    public Tuple2<DockerClient, Option<ContainerId>> createContainer(DockerClient dockerClient, ContainerName containerName, ImageTag imageTag) {
        Option<ContainerConfig> containerConfigOption = getImageFromTag(dockerClient, imageTag)._2
                .map(image -> ContainerConfig.builder().image(image.id()).build());

        if (containerConfigOption.isEmpty()) {
            return Tuple.of(dockerClient, Option.none());
        }

        return Tuple.of(dockerClient,
                dockerTryOf(() -> dockerClient.createContainer(containerConfigOption.get(), containerName.getName()))
                        .map(ContainerCreation::id)
                        .map(id -> (ContainerId) ImmutableContainerId.builder().id(id).build())
                        .toOption());
    }

    @Override
    public DockerClient createAndStartContainer(DockerClient dockerClient, ContainerName containerName, ImageTag imageTag) {
        Option<ContainerId> containerIdOption = createContainer(dockerClient, containerName, imageTag)._2;

        if (containerIdOption.isEmpty()) {
            log.warn(String.join("", "Container [", imageTag.getTag(), "] not started because it couldn't be created"));
        } else {
            startContainerIfNecessary(dockerClient, containerName, containerIdOption.get());
        }

        return dockerClient;
    }

    protected Tuple2<DockerClient, Option<Container>> getContainerByName(DockerClient dockerClient, ContainerName containerName) {
        return Tuple.of(dockerClient,
                Stream.ofAll(listContainers(dockerClient)._2)
                        .filter(getContainerPredicate(containerName))
                        .toOption());
    }

    protected Tuple2<DockerClient, Boolean> isContainerRunning(DockerClient dockerClient, ContainerName containerName) {
        return Tuple.of(dockerClient,
                dockerTryOf(() -> dockerClient.listContainers(DockerClient.ListContainersParam.withStatusRunning())).get()
                        .stream()
                        .anyMatch(getContainerPredicate(containerName)));
    }

    protected DockerClient startContainerIfNecessary(DockerClient dockerClient, ContainerName containerName, ContainerId containerId) {
        if (!isContainerRunning(dockerClient, containerName)._2) {
            dockerTryRun(() -> dockerClient.startContainer(containerId.getId())).get();
        }

        return dockerClient;
    }

    protected Predicate<Container> getContainerPredicate(ContainerName containerName) {
        String dockerContainerName = String.join("", "/", containerName.getName());
        return container -> Objects.requireNonNull(container.names()).contains(dockerContainerName);
    }

    @Override
    public Tuple2<DockerClient, Option<Container>> getContainerFromImageId(DockerClient dockerClient, ImageId imageId) {
        Option<Container> containerOption = Stream.ofAll(listContainers(dockerClient)._2)
                .filter(container -> container.image().equals(imageId.getId()))
                .toOption();

        if (containerOption.isDefined()) {
            log.info(String.join("", "Found container [", containerOption.get().id(), "] with image ID [", imageId.getId(), "]"));
        } else {
            log.info(String.join("", "No container running image [", imageId.getId(), "] found"));
        }

        return Tuple.of(dockerClient, containerOption);
    }

    private Option<Container> printFailedToGetContainerFromImageAndThrow(Throwable throwable) {
        log.error(String.join("", "Failed to get container from image [", throwable.getMessage(), "]"));
        throw new RuntimeException(throwable);
    }

    @Override
    public DockerClient dumpImagesInfo(DockerClient dockerClient) {
        // Do not call get() on this Try so we can fail and keep going
        dockerTryOf(() -> Stream.ofAll(listImages(dockerClient)._2)
                // Get the image with its list of tags
                .map(image -> Tuple.of(image, Option.of(image.repoTags())))
                // Only look at images with a defined tag list
                .filter(tuple -> tuple._2.isDefined())
                // Get the option since we know it is defined now
                .map(tuple -> Tuple.of(tuple._1, tuple._2.get()))
                // Convert the tag list to a vavr list
                .map(tuple -> Tuple.of(tuple._1, List.ofAll(tuple._2)))
                .map(tuple -> String.join("", tuple._1.id(), " [", String.join("|", tuple._2), "]"))
                .collect(Collectors.joining(", ")))
                .onSuccess(string -> log.info(String.join("", "Images [", string, "]")))
                .onFailure(Throwable::printStackTrace);

        return dockerClient;
    }

    private Tuple2<DockerClient, List<Container>> listContainers(DockerClient dockerClient) {
        return Tuple.of(dockerClient,
                dockerTryOf(() -> List.ofAll(dockerClient.listContainers(DockerClient.ListContainersParam.allContainers()))).get());
    }

    @Override
    public DockerClient dumpContainersInfo(DockerClient dockerClient) {
        dockerTryOf(() -> Stream.ofAll(listContainers(dockerClient)._2)
                .map(Container::id)
                .collect(Collectors.joining(", ")))
                .onSuccess(string -> log.info(String.join("", "Containers [", string, "]")))
                .get();

        return dockerClient;
    }

    private String printFailedToListContainersAndThrow(Throwable throwable) {
        log.error(String.join("", "Failed to list containers [", throwable.getMessage(), "]"));
        throw new RuntimeException(throwable);
    }

    @Override
    public DockerClient stopContainerByImageTag(DockerClient dockerClient, ImageTag imageTag) {
        // Get the image for the tag
        getImageFromTag(dockerClient, imageTag)._2
                // Convert the image to an image ID
                .map(value -> ImmutableImageId.builder().id(value.id()).build())
                // Get the container from the image ID
                .map(imageId -> getContainerFromImageId(dockerClient, imageId))
                // Pull out just the container info
                .flatMap(tuple -> tuple._2)
                // Stop the container
                .map(container -> stopContainer(dockerClient, container));

        return dockerClient;
    }

    @Override
    public DockerClient stopContainer(DockerClient dockerClient, Container container) {
        dockerTryRun(() -> dockerClient.stopContainer(container.id(), 5)).get();

        return dockerClient;
    }

    private Void printFailedToStopContainerAndThrow(Throwable throwable) {
        log.error(String.join("", "Failed to stop container [", throwable.getMessage(), "]"));
        throw new RuntimeException(throwable);
    }

    @Override
    public DockerClient pullImage(DockerClient dockerClient, ImageId imageId) {
        dockerTryRun(() -> dockerClient.pull(imageId.getId(), getRegistryAuth(), getProgressHandlerOption().getOrNull())).get();

        return dockerClient;
    }

    @Override
    public DockerClient pullImage(DockerClient dockerClient, ImageTag imageTag) {
        dockerTryRun(() -> dockerClient.pull(imageTag.getTag(), getRegistryAuth(), getProgressHandlerOption().getOrNull())).get();

        return dockerClient;
    }

    // NOTE: Wrap all calls to Docker with either dockerTryOf or dockerTryRun. This bakes in some default exception
    //         handling that can make certain errors very clear to the end user.
    private <T> Try<? extends T> dockerTryOf(CheckedFunction0<? extends T> supplier) {
        return Try.of(supplier)
                .onFailure(DockerException.class, this::checkFfiException);
    }

    private Try<Void> dockerTryRun(CheckedRunnable runnable) {
        return Try.run(runnable)
                .onFailure(DockerException.class, this::checkFfiException);
    }

    public abstract DeleteRepositoryResponse deleteRepositoryAndImages(EcrRepositoryName ecrRepositoryName);
}
