package com.awslabs.iot.helpers.implementations;

import com.amazon.aws.iot.greengrass.component.common.ComponentRecipe;
import com.awslabs.general.helpers.implementations.JacksonHelper;
import com.awslabs.iam.data.ImmutableRoleName;
import com.awslabs.iam.helpers.interfaces.IamHelper;
import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.GreengrassV2Helper;
import com.awslabs.iot.helpers.interfaces.IotHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.vdurmont.semver4j.Semver;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.greengrassv2.GreengrassV2Client;
import software.amazon.awssdk.services.greengrassv2.model.*;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.RoleAliasDescription;

import javax.inject.Inject;

import static com.awslabs.iot.helpers.implementations.ArnHelper.getArnType;
import static com.awslabs.iot.helpers.interfaces.IotHelper.IOT_ASSUME_ROLE_WITH_CERTIFICATE;

public class BasicGreengrassV2Helper implements GreengrassV2Helper {
    private final Logger log = LoggerFactory.getLogger(BasicGreengrassV2Helper.class);

    @Inject
    GreengrassV2Client greengrassV2Client;
    @Inject
    IamHelper iamHelper;
    @Inject
    IotHelper iotHelper;
    @Inject
    IotClient iotClient;

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
    public CreateComponentVersionResponse updateComponent(ComponentRecipe.ComponentRecipeBuilder componentRecipeBuilder) {
        ComponentName componentName = ImmutableComponentName.builder().name(componentRecipeBuilder.build().getComponentName()).build();

        Semver nextVersion = getPrivateComponentByName(componentName)
                .map(component -> component.latestVersion().componentVersion())
                .map(Semver::new)
                .map(Semver::nextPatch)
                .getOrElse(new Semver("1.0.0"));

        if (componentRecipeBuilder.build().getComponentVersion().isLowerThanOrEqualTo(nextVersion)) {
            log.warn("Specified component version [" + componentRecipeBuilder.build().getComponentVersion() + "] conflicts with an existing version, version bumped to [" + nextVersion + "]");
        } else {
            nextVersion = componentRecipeBuilder.build().getComponentVersion();
        }

        ComponentVersion componentVersion = ImmutableComponentVersion.builder().version(nextVersion).build();

        componentRecipeBuilder.componentVersion(nextVersion);

        byte[] inlineRecipeBytes = JacksonHelper.toJsonBytes(componentRecipeBuilder.build()).get();
        SdkBytes sdkBytesRecipe = SdkBytes.fromByteArray(inlineRecipeBytes);

        CreateComponentVersionRequest createComponentVersionRequest = CreateComponentVersionRequest.builder()
                .inlineRecipe(sdkBytesRecipe)
                .build();

        return greengrassV2Client.createComponentVersion(createComponentVersionRequest);
    }

    @Override
    public void deleteComponentVersion(ComponentName componentName, ComponentVersion componentVersion) {
        Option<Component> componentOption = getPrivateComponentByName(componentName);

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

    private Option<Component> getPrivateComponentByName(ComponentName componentName) {
        return getAllPrivateComponents()
                .filter(component -> component.componentName().equals(componentName.getName()))
                .toOption();
    }

    @Override
    public Option<Role> getRoleAssumedByGreengrassThing(ThingName thingName) {
        // Get all of the principals attached to this thing
        List<String> roleAliasNames = iotHelper.getThingPrincipals(thingName)
                // Only look at certificates
                .map(ArnHelper::getCertificateArnFromThingPrincipal)
                // Remove all of the blank values
                .flatMap(Option::toStream)
                // Get the policies attached to the certificates
                .map(certificateArn -> iotHelper.getAttachedPolicies(certificateArn).toList())
                // Get the policy documents for each policy
                .map(attachedPolicyList -> attachedPolicyList.flatMap(iotHelper::getPolicyDocument))
                // Convert the policies to type safe policies
                .flatMap(policyDocumentList -> policyDocumentList.map(value -> TypeSafePolicyDocument.fromJson(value.getDocument())))
                // Get all of the statements
                .flatMap(typeSafePolicyDocument -> typeSafePolicyDocument.Statement)
                // Only look at allow statements
                .filter(statement -> statement.getEffect().equals(Effect.Allow))
                // Find the resources that have assume role with certificate permissions
                .flatMap(this::getAssumeRoleWithCertificateResources)
                // Find the resources that are role aliases
                .filter(arn -> getArnType(arn).filter(arnType -> arnType.getTypeSafeClass().isAssignableFrom(RoleAlias.class)).isDefined())
                // Extract just the role alias names from the full ARNs
                .flatMap(ArnHelper::arnToId)
                .toList();

        if (roleAliasNames.length() > 1) {
            throw new RuntimeException("Multiple resources were found that this Greengrass Group can assume. This is not supported currently.");
        }

        if (roleAliasNames.length() == 0) {
            throw new RuntimeException("No resources were found that this Greengrass Group can assume. This is a bug.");
        }

        DescribeRoleAliasRequest describeRoleAliasRequest = DescribeRoleAliasRequest.builder()
                .roleAlias(roleAliasNames.get())
                .build();

        // Describe the role alias
        return Try.of(() -> iotClient.describeRoleAlias(describeRoleAliasRequest))
                // Convert the try to an option so we return none for failures
                .toOption()
                // Extract the role alias description
                .map(DescribeRoleAliasResponse::roleAliasDescription)
                // Extract the role ARN
                .map(RoleAliasDescription::roleArn)
                // Extract the ID (name)
                .flatMap(ArnHelper::arnToId)
                .map(name -> ImmutableRoleName.builder().name(name).build())
                // Get the role object from IAM
                .flatMap(roleName -> iamHelper.getRole(roleName));
    }

    private List<String> getAssumeRoleWithCertificateResources(Statement statement) {
        return Option.of(statement)
                .filter(value -> value.getAction().contains(IOT_ASSUME_ROLE_WITH_CERTIFICATE))
                .map(Statement::getResource)
                .getOrElse(List.empty());
    }

    private List<String> getAssumeRoleWithCertificateResources(List<Statement> statements) {
        return statements
                .filter(statement -> statement.getAction().contains(IOT_ASSUME_ROLE_WITH_CERTIFICATE))
                .flatMap(Statement::getResource);
    }
}
