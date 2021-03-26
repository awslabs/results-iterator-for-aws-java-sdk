package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.helpers.interfaces.GreengrassV2Helper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import io.vavr.collection.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
}
