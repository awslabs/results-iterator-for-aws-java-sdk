package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.data.*;
import com.awslabs.resultsiterator.data.ImmutablePassword;
import com.awslabs.resultsiterator.data.Password;
import com.awslabs.resultsiterator.v2.interfaces.V2CertificateCredentialsProvider;
import io.vavr.collection.HashMap;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class BouncyCastleV2CertificateCredentialsProvider implements V2CertificateCredentialsProvider {
    public static final String BOUNCY_CASTLE_PROVIDER_NAME = "BC";
    public static final String CA_CERTIFICATE = "ca-certificate";
    public static final String CERTIFICATE = "certificate";
    public static final String PRIVATE_KEY = "private-key";
    public static final String TLSV1_2 = "TLSv1.2";

    @Inject
    IoHelper ioHelper;
    @Inject
    JsonHelper jsonHelper;

    @Inject
    public BouncyCastleV2CertificateCredentialsProvider() {
    }

    @Override
    public AwsCredentials resolveCredentials() {
        HashMap<String, String> properties = toHashMap(System.getProperties().entrySet());
        HashMap<String, String> environment = toHashMap(System.getenv().entrySet());

        Option<String> maybeCredentialProviderPropertiesFile = getOptionFromPropertiesOrEnvironment(properties, environment, AWS_CREDENTIAL_PROVIDER_PROPERTIES_FILE);

        if (maybeCredentialProviderPropertiesFile.isDefined()) {
            Properties propertiesFromFile = new Properties();

            File credentialsProviderPropertiesFile = new File(maybeCredentialProviderPropertiesFile.get());

            Optional<Properties> optionalProperties = Try.of(() -> loadProperties(credentialsProviderPropertiesFile, propertiesFromFile)).get();

            if (optionalProperties.isPresent()) {
                // Got the values as we expected, use these instead of the original properties
                properties = toHashMap(propertiesFromFile.entrySet());
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

    private <U, V> HashMap<String, String> toHashMap(Set<Map.Entry<U, V>> entrySet) {
        return entrySet.stream()
                .collect(HashMap.collector(e -> (String) e.getKey(), e -> (String) e.getValue()));
    }

    private Optional<Properties> loadProperties(File credentialProviderPropertiesFile, Properties properties) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(credentialProviderPropertiesFile);
        properties.load(fileInputStream);

        String caCertFilenameString = properties.getProperty(AWS_CA_CERT_FILENAME);

        if (caCertFilenameString == null) {
            // Missing file, give up
            return Optional.empty();
        }

        String clientCertFilenameString = properties.getProperty(AWS_CLIENT_CERT_FILENAME);

        if (clientCertFilenameString == null) {
            // Missing file, give up
            return Optional.empty();
        }

        String clientPrivateKeyFilenameString = properties.getProperty(AWS_CLIENT_PRIVATE_KEY_FILENAME);

        if (clientPrivateKeyFilenameString == null) {
            // Missing file, give up
            return Optional.empty();
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

        return Optional.of(properties);
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
        SSLContext sslContext = getSslContext(caCertFilename, clientCertFilename, clientPrivateKeyFilename, password);
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setSSLContext(sslContext);

        return builder.build();
    }

    protected SSLContext getSslContext(ImmutableCaCertFilename caCertFilename,
                                       ImmutableClientCertFilename clientCertFilename,
                                       ImmutableClientPrivateKeyFilename clientPrivateKeyFilename,
                                       ImmutablePassword password) {
        Security.addProvider(new BouncyCastleProvider());

        JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter()
                .setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

        // Load CA certificate
        X509Certificate caCertificate = getCertificate(jcaX509CertificateConverter, Paths.get(caCertFilename.getCaCertFilename()));

        // Load client certificate
        X509Certificate clientCertificate = getCertificate(jcaX509CertificateConverter, Paths.get(clientCertFilename.getClientCertFilename()));

        // Load client private key
        KeyPair key = getKeyPair(Paths.get(clientPrivateKeyFilename.getClientPrivateKeyFilename()), password);

        // Get the CA keystore and rethrow all exceptions
        KeyStore caKeyStore = getDefaultKeystore();

        // Add the CA certificate and rethrow all exceptions
        Try.run(() -> caKeyStore.setCertificateEntry(CA_CERTIFICATE, caCertificate));

        // Get a trust manager factory and rethrow all exceptions
        TrustManagerFactory trustManagerFactory = Try.of(() -> TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())).get();

        // Initialize the trust manager factory with the CA keystore and rethrow all exceptions
        Try.run(() -> trustManagerFactory.init(caKeyStore)).get();

        // Get the client keystore and rethrow all exceptions
        KeyStore clientKeyStore = getDefaultKeystore();

        // Add the client certificate and rethrow all exceptions
        Try.run(() -> clientKeyStore.setCertificateEntry(CERTIFICATE, clientCertificate)).get();

        // Add the client private key and rethrow all exceptions
        Try.run(() -> clientKeyStore.setKeyEntry(PRIVATE_KEY, key.getPrivate(), Password.BLANK_PASSWORD, new Certificate[]{clientCertificate})).get();

        // Get a key manager factory and rethrow all exceptions
        KeyManagerFactory keyManagerFactory = Try.of(() -> KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())).get();

        // Initialize the key manager factory with the client keystore and a blank password and rethrow all exceptions
        Try.run(() -> keyManagerFactory.init(clientKeyStore, Password.BLANK_PASSWORD)).get();

        // Create the SSL context and rethrow all exceptions
        SSLContext sslContext = Try.of(() -> SSLContext.getInstance(TLSV1_2)).get();

        // Initialize the SSL context and rethrow all exceptions
        Try.run(() -> sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null)).get();

        return sslContext;
    }

    protected X509Certificate getCertificate(JcaX509CertificateConverter jcaX509CertificateConverter, Path path) {
        // Get the certificate and rethrow all exceptions
        return Try.of(() -> jcaX509CertificateConverter.getCertificate(getCertificateHolder(path))).get();
    }

    private KeyStore getDefaultKeystore() {
        // Get a default keystore instance and rethrow all exceptions
        KeyStore keyStore = Try.of(() -> KeyStore.getInstance(KeyStore.getDefaultType())).get();
        // Get an empty keystore with no password and rethrow all exceptions
        Try.run(() -> keyStore.load(null, null)).get();

        return keyStore;
    }

    private X509CertificateHolder getCertificateHolder(Path certificatePath) {
        return (X509CertificateHolder) getObjectFromParser(certificatePath);
    }

    private KeyPair getKeyPair(Path keyPairPath, ImmutablePassword password) {
        byte[] keyPairBytes = Try.of(() -> Files.readAllBytes(keyPairPath)).get();

        return getKeyPair(keyPairBytes, password);
    }

    protected KeyPair getKeyPair(byte[] keyPairBytes, ImmutablePassword password) {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BOUNCY_CASTLE_PROVIDER_NAME);

        // Get the object from the parser
        return Try.of(() -> getObjectFromParser(keyPairBytes))
                // Determine the type of the key
                .map(objectFromParser -> determineKeyType(objectFromParser)
                        // If the key is encrypted, decrypt it
                        .mapLeft(pemEncryptedKeyPair -> decryptKeyPair(pemEncryptedKeyPair, password))
                        // Now both left and right values are the same type, fold them into one type
                        .fold(key -> key, key -> key))
                // Convert the PEM key pair to a key pair
                .mapTry(converter::getKeyPair)
                // Get the key pair and rethrow all exceptions
                .get();
    }

    private Either<PEMEncryptedKeyPair, PEMKeyPair> determineKeyType(Object objectFromParser) {
        if (objectFromParser instanceof PEMEncryptedKeyPair) {
            return Either.left((PEMEncryptedKeyPair) objectFromParser);
        }

        return Either.right((PEMKeyPair) objectFromParser);
    }

    private PEMKeyPair decryptKeyPair(PEMEncryptedKeyPair pemEncryptedKeyPair, ImmutablePassword password) {
        PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder().build(password.getPassword());
        return Try.of(() -> pemEncryptedKeyPair.decryptKeyPair(pemDecryptorProvider)).get();
    }


    private Object getObjectFromParser(Path path) {
        byte[] fileContents = Try.of(() -> Files.readAllBytes(path)).get();

        return getObjectFromParser(fileContents);
    }

    protected Object getObjectFromParser(byte[] bytes) {
        return Try.withResources(() -> new PEMParser(new InputStreamReader(new ByteArrayInputStream(bytes))))
                .of(PEMParser::readObject)
                .get();
    }
}
