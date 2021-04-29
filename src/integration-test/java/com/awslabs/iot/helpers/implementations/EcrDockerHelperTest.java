package com.awslabs.iot.helpers.implementations;

import com.awslabs.ecr.EcrDockerHelper;
import com.awslabs.ecr.data.EcrRepositoryName;
import com.awslabs.ecr.data.ImageId;
import com.awslabs.ecr.data.ImmutableEcrRepositoryName;
import com.awslabs.resultsiterator.implementations.BasicInjector;
import com.awslabs.resultsiterator.implementations.DaggerBasicInjector;
import com.spotify.docker.client.DockerClient;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.ecr.model.DeleteRepositoryResponse;
import software.amazon.awssdk.services.ecr.model.Repository;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Logger;

import static com.awslabs.resultsiterator.TestResultsIteratorInterface.JUNKFORECRTESTING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class EcrDockerHelperTest {
    private static final Logger log = Logger.getLogger(EcrDockerHelperTest.class.getName());
    private BasicInjector injector;
    private Lazy<EcrDockerHelper> helper;
    private EcrRepositoryName ecrRepositoryName;

    @Before
    public void setup() {
        injector = DaggerBasicInjector.create();
        helper = Lazy.of(() -> injector.ecrDockerHelper());
        ecrRepositoryName = ImmutableEcrRepositoryName.builder().name(JUNKFORECRTESTING).build();
    }

    @Test
    public void shouldCreateDockerHelper() {
        injector.ecrDockerHelper();
    }

    @Test
    public void shouldReportDockerIsAvailable() {
        assertThat("Can not determine if Docker is available. Is the JVM at least version 11?", helper.get().isDockerAvailable(), is(true));
    }

    @Test
    public void shouldBuildAnImageWithoutSpecifyingTheDockerfileExplicitly() {
        EcrDockerHelper ecrDockerHelper = helper.get();
        Try.withResources(ecrDockerHelper::getDockerClient)
                .of(dockerClient -> ecrDockerHelper.buildImage(dockerClient,
                        getDockertest1Path(),
                        Option.none(),
                        List.empty()))
                .get();
    }

    @Test
    public void shouldBuildAnImageWhenSpecifyingTheDockerfileExplicitly() {
        EcrDockerHelper ecrDockerHelper = helper.get();
        Try.withResources(ecrDockerHelper::getDockerClient)
                .of(dockerClient -> ecrDockerHelper.buildImage(dockerClient,
                        getDockertest1Path(),
                        Option.of(new File("Dockerfile").toPath()),
                        List.empty()))
                .get();
    }

    @Test
    public void shouldThrowAnExceptionWithInvalidDockerfileValues() {
        EcrDockerHelper ecrDockerHelper = helper.get();

        Try<Tuple2<DockerClient, Option<ImageId>>> tryResult1 = Try.withResources(ecrDockerHelper::getDockerClient)
                .of(dockerClient -> ecrDockerHelper.buildImage(dockerClient,
                        getDockertest1Path(),
                        Option.of(new File("/test/Dockerfile").toPath()),
                        List.empty()));

        assertThat(tryResult1.isFailure(), is(true));

        Try<Tuple2<DockerClient, Option<ImageId>>> tryResult2 = Try.withResources(ecrDockerHelper::getDockerClient)
                .of(dockerClient -> ecrDockerHelper.buildImage(dockerClient,
                        getDockertest1Path(),
                        Option.of(new File("/Dockerfile").toPath()),
                        List.empty()));

        assertThat(tryResult2.isFailure(), is(true));

        Try<Tuple2<DockerClient, Option<ImageId>>> tryResult3 = Try.withResources(ecrDockerHelper::getDockerClient)
                .of(dockerClient -> ecrDockerHelper.buildImage(dockerClient,
                        getDockertest1Path(),
                        Option.of(new File("../Dockerfile").toPath()),
                        List.empty()));

        assertThat(tryResult3.isFailure(), is(true));
    }

    @Test
    public void shouldCreateAndDeleteRepositoryWithoutThrowingExceptions() {
        EcrDockerHelper ecrDockerHelper = helper.get();

        Repository repository = ecrDockerHelper.createRepositoryIfNecessary(ecrRepositoryName);

        DeleteRepositoryResponse deleteRepositoryResponse = ecrDockerHelper.deleteRepository(ecrRepositoryName);
    }

    @Test
    public void shouldBuildTestImageAndNotThrowException() {
        Path dockertest1 = getDockertest1Path();

        EcrDockerHelper ecrDockerHelper = helper.get();

        Try.withResources(ecrDockerHelper::getDockerClient)
                .of(dockerClient -> ecrDockerHelper.buildImage(dockerClient,
                        dockertest1,
                        Option.none(),
                        List.empty()))
                .get();
    }

    @Test
    public void shouldPushImageToEcrAndNotThrowException() {
        Path dockertest1 = getDockertest1Path();

        EcrDockerHelper ecrDockerHelper = helper.get();

        Try.of(ecrDockerHelper::getDockerClient)
                .map(dockerClient -> ecrDockerHelper.buildImage(dockerClient,
                        dockertest1,
                        Option.none(),
                        List.empty()))
                .filter(tuple -> tuple._2.isDefined())
                .map(tuple -> ecrDockerHelper.pushImage(tuple._1, ecrRepositoryName, tuple._2.get()))
                .map(tuple -> ecrDockerHelper.deleteRepositoryAndImages(ecrRepositoryName))
                .get();
    }

    private Path getDockertest1Path() {
        return Try.of(EcrDockerHelperTest.class::getClassLoader)
                .map(classloader -> classloader.getResource("dockertest1"))
                .mapTry(URL::toURI)
                .map(File::new)
                .map(File::toPath)
                .get();
    }
}
