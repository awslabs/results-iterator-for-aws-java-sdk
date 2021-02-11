package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.data.*;
import com.awslabs.resultsiterator.data.ImmutablePassword;
import com.awslabs.resultsiterator.interfaces.SslContextHelper;
import com.awslabs.resultsiterator.v2.interfaces.V2CertificateCredentialsProvider;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Properties;

public class BouncyCastleV2CertificateCredentialsProvider implements V2CertificateCredentialsProvider {
    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SslContextHelper sslContextHelper;

    @Inject
    public BouncyCastleV2CertificateCredentialsProvider() {
    }

    @Override
    public AwsCredentials resolveCredentials() {
        HashMap<String, String> properties = toHashMap(System.getProperties().entrySet().stream());
        HashMap<String, String> environment = toHashMap(System.getenv().entrySet().stream());

        Option<String> maybeCredentialProviderPropertiesFile = getOptionFromPropertiesOrEnvironment(properties, environment, AWS_CREDENTIAL_PROVIDER_PROPERTIES_FILE);

        if (maybeCredentialProviderPropertiesFile.isDefined()) {
            Properties propertiesFromFile = new Properties();

            File credentialsProviderPropertiesFile = new File(maybeCredentialProviderPropertiesFile.get());

            Option<Properties> propertiesOption = Try.of(() -> loadProperties(credentialsProviderPropertiesFile, propertiesFromFile)).get();

            if (propertiesOption.isDefined()) {
                // Got the values as we expected, use these instead of the original properties
                properties = toHashMap(propertiesFromFile.entrySet().stream());
            }
        }

        String credentialProviderUrlString = getFromPropertiesOrEnvironment(properties, environment, AWS_CREDENTIAL_PROVIDER_URL);
        String thingNameString = getFromPropertiesOrEnvironment(properties, environment, AWS_THING_NAME);
        String roleAliasString = getFromPropertiesOrEnvironment(properties, environment, AWS_ROLE_ALIAS);
        String caCertFilenameString = getFromPropertiesOrEnvironment(properties, environment, AWS_CA_CERT_FILENAME);
        String clientCertFilenameString = getFromPropertiesOrEnvironment(properties, environment, AWS_CLIENT_CERT_FILENAME);
        String clientPrivateKeyFilenameString = getFromPropertiesOrEnvironment(properties, environment, AWS_CLIENT_PRIVATE_KEY_FILENAME);
        Option<String> maybeClientPrivateKeyPassword = getOptionFromPropertiesOrEnvironment(properties, environment, AWS_CLIENT_PRIVATE_KEY_PASSWORD);

        ImmutableCredentialProviderUrl credentialProviderUrl = ImmutableCredentialProviderUrl.builder().credentialProviderUrl(credentialProviderUrlString).build();
        ImmutableThingName thingName = ImmutableThingName.builder().name(thingNameString).build();
        ImmutableRoleAlias roleAlias = ImmutableRoleAlias.builder().name(roleAliasString).build();
        ImmutableCaCertFilename caCertFilename = ImmutableCaCertFilename.builder().caCertFilename(caCertFilenameString).build();
        ImmutableClientCertFilename clientCertFilename = ImmutableClientCertFilename.builder().clientCertFilename(clientCertFilenameString).build();
        ImmutableClientPrivateKeyFilename clientPrivateKeyFilename = ImmutableClientPrivateKeyFilename.builder().clientPrivateKeyFilename(clientPrivateKeyFilenameString).build();
        ImmutablePassword.Builder passwordBuilder = ImmutablePassword.builder();

        maybeClientPrivateKeyPassword
                .map(String::toCharArray)
                .map(passwordBuilder::password);

        ImmutablePassword password = passwordBuilder.build();

        return resolveCredentials(credentialProviderUrl, thingName, roleAlias, caCertFilename, clientCertFilename, clientPrivateKeyFilename, password);
    }

    private <K, V> HashMap<String, String> toHashMap(java.util.stream.Stream<java.util.Map.Entry<K, V>> input) {
        return HashMap.ofAll(input, entry -> entry.getKey().toString(), entry -> entry.getValue().toString());
    }

    private Option<Properties> loadProperties(File credentialProviderPropertiesFile, Properties properties) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(credentialProviderPropertiesFile);
        properties.load(fileInputStream);

        String caCertFilenameString = properties.getProperty(AWS_CA_CERT_FILENAME);

        if (caCertFilenameString == null) {
            // Missing file, give up
            return Option.none();
        }

        String clientCertFilenameString = properties.getProperty(AWS_CLIENT_CERT_FILENAME);

        if (clientCertFilenameString == null) {
            // Missing file, give up
            return Option.none();
        }

        String clientPrivateKeyFilenameString = properties.getProperty(AWS_CLIENT_PRIVATE_KEY_FILENAME);

        if (clientPrivateKeyFilenameString == null) {
            // Missing file, give up
            return Option.none();
        }

        // All file names are present, make them relative to the properties file
        Path credentialProviderPropertiesPath = credentialProviderPropertiesFile.toPath().getParent();
        caCertFilenameString = credentialProviderPropertiesPath.resolve(caCertFilenameString).toAbsolutePath().toString();
        clientCertFilenameString = credentialProviderPropertiesPath.resolve(clientCertFilenameString).toAbsolutePath().toString();
        clientPrivateKeyFilenameString = credentialProviderPropertiesPath.resolve(clientPrivateKeyFilenameString).toAbsolutePath().toString();

        // Put them back into the properties object with their absolute paths
        properties.setProperty(AWS_CA_CERT_FILENAME, caCertFilenameString);
        properties.setProperty(AWS_CLIENT_CERT_FILENAME, clientCertFilenameString);
        properties.setProperty(AWS_CLIENT_PRIVATE_KEY_FILENAME, clientPrivateKeyFilenameString);

        return Option.of(properties);
    }

    private String getFromPropertiesOrEnvironment(HashMap<String, String> properties, HashMap<String, String> environment, String name) {
        return getOptionFromPropertiesOrEnvironment(properties, environment, name)
                .getOrElseThrow(() -> throwRuntimeExceptionOnMissingEnvironmentVariable(name));
    }

    private Option<String> getOptionFromPropertiesOrEnvironment(HashMap<String, String> properties, HashMap<String, String> environment, String name) {
        return properties.get(name)
                .orElse(environment.get(name));
    }

    private RuntimeException throwRuntimeExceptionOnMissingEnvironmentVariable(String environmentVariableName) {
        throw new RuntimeException(String.join(" ", "Credentials from the IoT Credentials Provider could not be loaded because the environment variable", environmentVariableName, "was not present"));
    }

    protected AwsCredentials resolveCredentials(ImmutableCredentialProviderUrl credentialProviderUrl,
                                                ImmutableThingName thingName,
                                                ImmutableRoleAlias roleAlias,
                                                ImmutableCaCertFilename caCertFilename,
                                                ImmutableClientCertFilename clientCertFilename,
                                                ImmutableClientPrivateKeyFilename clientPrivateKeyFilename,
                                                ImmutablePassword password) {
        HttpClient httpClient = getHttpClient(caCertFilename, clientCertFilename, clientPrivateKeyFilename, password);

        String credentialProviderFullUrl = String.join("",
                "https://",
                credentialProviderUrl.getCredentialProviderUrl(),
                "/",
                "role-aliases",
                "/",
                roleAlias.getName(),
                "/",
                "credentials");

        URI credentialProviderUri = Try.of(() -> new URI(credentialProviderFullUrl)).get();

        HttpGet httpGet = new HttpGet(credentialProviderUri);
        httpGet.setHeader(X_AMZN_IOT_THINGNAME, thingName.getName());

        IotCredentialsProviderCredentials iotCredentialsProviderCredentials = Try.of(() -> httpClient.execute(httpGet))
                .map(HttpResponse::getEntity)
                .mapTry(EntityUtils::toByteArray)
                .map(responseBytes -> jsonHelper.fromJson(IotCredentialsProviderCredentials.class, responseBytes))
                .get();

        return iotCredentialsProviderCredentials.getCredentials().toAwsSessionCredentials();
    }

    protected HttpClient getHttpClient(ImmutableCaCertFilename caCertFilename,
                                       ImmutableClientCertFilename clientCertFilename,
                                       ImmutableClientPrivateKeyFilename clientPrivateKeyFilename,
                                       ImmutablePassword password) {
        SSLContext sslContext = sslContextHelper.getSslContext(caCertFilename, clientCertFilename, clientPrivateKeyFilename, password);
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setSSLContext(sslContext);

        return builder.build();
    }
}
