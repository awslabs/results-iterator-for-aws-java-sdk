package com.awslabs.resultsiterator.implementations;

import com.awslabs.iot.data.ImmutableCaCertFilename;
import com.awslabs.iot.data.ImmutableClientCertFilename;
import com.awslabs.iot.data.ImmutableClientPrivateKeyFilename;
import com.awslabs.resultsiterator.data.ImmutablePassword;
import com.awslabs.resultsiterator.data.Password;
import com.awslabs.resultsiterator.interfaces.SslContextHelper;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class BasicSslContextHelper implements SslContextHelper {
    public static final String BOUNCY_CASTLE_PROVIDER_NAME = "BC";
    public static final String CA_CERTIFICATE = "ca-certificate";
    public static final String CERTIFICATE = "certificate";
    public static final String PRIVATE_KEY = "private-key";
    public static final String TLSV1_2 = "TLSv1.2";

    @Inject
    public BasicSslContextHelper() {
    }

    @Override
    public SSLContext getSslContext(ImmutableCaCertFilename caCertFilename,
                                    ImmutableClientCertFilename clientCertFilename,
                                    ImmutableClientPrivateKeyFilename clientPrivateKeyFilename,
                                    ImmutablePassword password) {

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
