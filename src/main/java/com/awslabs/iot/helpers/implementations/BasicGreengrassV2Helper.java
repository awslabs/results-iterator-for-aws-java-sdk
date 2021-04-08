package com.awslabs.iot.helpers.implementations;

import com.amazon.aws.iot.greengrass.component.common.ComponentRecipe;
import com.awslabs.general.helpers.implementations.JacksonHelper;
import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.GreengrassV2Helper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.greengrassv2.GreengrassV2Client;
import software.amazon.awssdk.services.greengrassv2.model.*;

import javax.inject.Inject;

public class BasicGreengrassV2Helper implements GreengrassV2Helper {
    private final Logger log = LoggerFactory.getLogger(BasicGreengrassV2Helper.class);

    @Inject
    GreengrassV2Client greengrassV2Client;

    @Inject
    public BasicGreengrassV2Helper() {
    }

    @Override
    public Stream<Deployment> getAllDeployments() {
        return new ResultsIterator<Deployment>(greengrassV2Client, ListDeploymentsRequest.class).stream();
    }

    @Override
    public Stream<CoreDevice> getAllCoreDevices() {
        return new ResultsIterator<CoreDevice>(greengrassV2Client, ListCoreDevicesRequest.class).stream();
    }

    @Override
    public void deleteCoreDevice(CoreDevice coreDevice) {
        DeleteCoreDeviceRequest deleteCoreDeviceRequest = DeleteCoreDeviceRequest.builder()
                .coreDeviceThingName(coreDevice.coreDeviceThingName())
                .build();

        greengrassV2Client.deleteCoreDevice(deleteCoreDeviceRequest);

        log.debug(String.join("", "Deleted core device [", coreDevice.coreDeviceThingName(), "]"));
    }

    @Override
    public Stream<Component> getAllComponents() {
        return new ResultsIterator<Component>(greengrassV2Client, ListComponentsRequest.class).stream();
    }

    @Override
    public Stream<Component> getAllPrivateComponents() {
        ListComponentsRequest listComponentsRequest = ListComponentsRequest.builder()
                .scope(ComponentVisibilityScope.PRIVATE)
                .build();

        return new ResultsIterator<Component>(greengrassV2Client, listComponentsRequest).stream();
    }

    @Override
    public Stream<ComponentVersionListItem> getComponentVersions(ComponentArn componentArn) {
        ListComponentVersionsRequest listComponentVersionsRequest = ListComponentVersionsRequest.builder()
                .arn(componentArn.getArn())
                .build();

        return new ResultsIterator<ComponentVersionListItem>(greengrassV2Client, listComponentVersionsRequest).stream();
    }

    @Override
    public CreateComponentVersionResponse createOrOverwriteComponent(ComponentRecipe componentRecipe) {
        ComponentName componentName = ImmutableComponentName.builder().name(componentRecipe.getComponentName()).build();
        ComponentVersion componentVersion = ImmutableComponentVersion.builder().version(componentRecipe.getComponentVersion()).build();

        byte[] inlineRecipeBytes = JacksonHelper.toJsonBytes(componentRecipe).get();
        SdkBytes sdkBytesRecipe = SdkBytes.fromByteArray(inlineRecipeBytes);

        CreateComponentVersionRequest createComponentVersionRequest = CreateComponentVersionRequest.builder()
                .inlineRecipe(sdkBytesRecipe)
                .build();

        RetryPolicy<CreateComponentVersionResponse> createComponentVersionResponseRetryPolicy = new RetryPolicy<CreateComponentVersionResponse>()
                // If there is a conflict, this component version already exists
                .handle(ConflictException.class)
                // Retry only once
                .withMaxAttempts(1)
                // Attempt to delete the component version before retrying and emit a warning
                .onFailedAttempt(attempt -> log.warn("Component already exists, attempting to delete and recreate it"))
                .onRetry(attempt -> deleteComponentVersion(componentName, componentVersion));

        return Failsafe.with(createComponentVersionResponseRetryPolicy).get(() -> greengrassV2Client.createComponentVersion(createComponentVersionRequest));
    }

    @Override
    public void deleteComponentVersion(ComponentName componentName, ComponentVersion componentVersion) {
        Option<Component> componentOption = getAllPrivateComponents()
                .filter(component -> component.componentName().equals(componentName.getName()))
                .toOption();

        if (componentOption.isEmpty()) {
            // Component doesn't exist, do nothing
            return;
        }

        Component component = componentOption.get();

        Option<ComponentVersionListItem> componentVersionListItemOption = getComponentVersions(ImmutableComponentArn.builder().arn(component.arn()).build())
                .filter(componentVersionListItem -> componentVersionListItem.componentVersion().equals(componentVersion.getVersion().getValue()))
                .toOption();

        if (componentVersionListItemOption.isEmpty()) {
            // Component version doesn't exist, do nothing
            return;
        }

        ComponentVersionListItem componentVersionListItem = componentVersionListItemOption.get();

        DeleteComponentRequest deleteComponentRequest = DeleteComponentRequest.builder()
                .arn(componentVersionListItem.arn())
                .build();

        greengrassV2Client.deleteComponent(deleteComponentRequest);
    }
}
