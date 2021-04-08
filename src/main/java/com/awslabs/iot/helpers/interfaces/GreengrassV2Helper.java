package com.awslabs.iot.helpers.interfaces;

import com.amazon.aws.iot.greengrass.component.common.ComponentRecipe;
import com.awslabs.iot.data.ComponentArn;
import com.awslabs.iot.data.ComponentName;
import com.awslabs.iot.data.ComponentVersion;
import io.vavr.collection.Stream;
import software.amazon.awssdk.services.greengrassv2.model.*;

public interface GreengrassV2Helper {
    Stream<Deployment> getAllDeployments();

    Stream<CoreDevice> getAllCoreDevices();

    void deleteCoreDevice(CoreDevice coreDevice);

    Stream<Component> getAllComponents();

    Stream<Component> getAllPrivateComponents();

    Stream<ComponentVersionListItem> getComponentVersions(ComponentArn componentArn);

    CreateComponentVersionResponse createOrOverwriteComponent(ComponentRecipe componentRecipe);

    void deleteComponentVersion(ComponentName componentName, ComponentVersion componentVersion);
}
