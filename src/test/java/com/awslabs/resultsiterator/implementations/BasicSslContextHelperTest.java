package com.awslabs.resultsiterator.implementations;

import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.data.ImmutablePassword;
import com.awslabs.resultsiterator.v2.implementations.BouncyCastleV2CertificateCredentialsProvider;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import com.awslabs.resultsiterator.v2.interfaces.V2CertificateCredentialsProvider;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.security.Security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BasicSslContextHelperTest {
    public static final String JUNK = "junk";
    public static final String JUNK_CORE = "junk_Core";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SESSION_TOKEN = "sessionToken";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    private final Logger log = LoggerFactory.getLogger(BouncyCastleV2CertificateCredentialsProvider.class);
    private final String testCertificate = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDWjCCAkKgAwIBAgIVAPOVItTLTODHH0+aoX32T4RalKPoMA0GCSqGSIb3DQEB\n" +
            "CwUAME0xSzBJBgNVBAsMQkFtYXpvbiBXZWIgU2VydmljZXMgTz1BbWF6b24uY29t\n" +
            "IEluYy4gTD1TZWF0dGxlIFNUPVdhc2hpbmd0b24gQz1VUzAeFw0yMDA0MDYxOTEw\n" +
            "NTBaFw00OTEyMzEyMzU5NTlaMB4xHDAaBgNVBAMME0FXUyBJb1QgQ2VydGlmaWNh\n" +
            "dGUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC8yGbU2ggByE6ORrcC\n" +
            "uZ8hHJ4PGVMnnps/Jr1NJoZv3p9wHO9B5ISVpWL/FhRc6K0FseqW6C+lsFKW7X1j\n" +
            "+3tEM1EjVCfU3jnyZN2OjBSciQYpEUCNBAz6S7ItoWHIF0Y1O4g1it5HtLUinNmn\n" +
            "e78Ik7koNWQxkiyN2LBzj7eRniUS2scVCraGVgKnh9EE1nZJd6d5YGh/8ICQBBl+\n" +
            "mPX5Yy2Div0XvHJdaIIWV++b/bF3A1fNnmlmZVJpKcrKW5itZRsNbz/vYtw/zidN\n" +
            "ScZ6TNyExNcTkC1igH8inJUu+RzRR883hVlg+qcGPstc0UP/Y+k018BbigFTVlur\n" +
            "bPPVAgMBAAGjYDBeMB8GA1UdIwQYMBaAFDqzAEShj0mkz/KTdQxah6mN42kAMB0G\n" +
            "A1UdDgQWBBQ91IRnzbOJqc2bo9rHFdNmSJw+EjAMBgNVHRMBAf8EAjAAMA4GA1Ud\n" +
            "DwEB/wQEAwIHgDANBgkqhkiG9w0BAQsFAAOCAQEAEFly5fOEczC4QDnN3/NYsKQr\n" +
            "nyryRLVilRG+q8Ij6KaSG8n+0n0ccgRhpE/qo7UEEgtyBqb7+ix1MGWhjtdfjB+R\n" +
            "CkR0w7PH5g+K+Wzb4La4meJJ/K6HRzInEmPm5ySIiEOEDw8MO1YXptiiMFMX6YKX\n" +
            "cc4LI7L8Lky40D+NWK/cbsKcQP8wwm4CtoTZJy0W9KP+1H6hNFDX5w4+7sr1wy07\n" +
            "has6RX04RZXKCr8Y+ZM0C8JR47dhAW7jGqdaaZ+3hHlb9H4M265IZXKnzxXCPjVM\n" +
            "AF+Re8o48kIkLdw7EPvtoqM64+hB6l0ODOHhNNDxt2oBcXgNbZ0XcMZXTnR/Dw==\n" +
            "-----END CERTIFICATE-----\n";
    private final String testPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpAIBAAKCAQEAvMhm1NoIAchOjka3ArmfIRyeDxlTJ56bPya9TSaGb96fcBzv\n" +
            "QeSElaVi/xYUXOitBbHqlugvpbBSlu19Y/t7RDNRI1Qn1N458mTdjowUnIkGKRFA\n" +
            "jQQM+kuyLaFhyBdGNTuINYreR7S1IpzZp3u/CJO5KDVkMZIsjdiwc4+3kZ4lEtrH\n" +
            "FQq2hlYCp4fRBNZ2SXeneWBof/CAkAQZfpj1+WMtg4r9F7xyXWiCFlfvm/2xdwNX\n" +
            "zZ5pZmVSaSnKyluYrWUbDW8/72LcP84nTUnGekzchMTXE5AtYoB/IpyVLvkc0UfP\n" +
            "N4VZYPqnBj7LXNFD/2PpNNfAW4oBU1Zbq2zz1QIDAQABAoIBAQCYz3r1hrt+fd6g\n" +
            "qjsPuKNHkTucKzq1UlyGRNxsq+ecfE8A2FsPMmPkIii9JOk8v5b2iirDFpUjAFQK\n" +
            "GZkrKnCAJy3hdAh99ZhgTidNcLRqdTwIWA+xVfsPS+ChsQVOixBonJTICm2dC3in\n" +
            "2OESAkgDMFhrZLSCr7ji5OkH0eictEo90Nq8YRZJJ6xhjdCMl263PAAy3vPaG0Aj\n" +
            "mkYEJRz5EF4SLGafwA8HxRKpvgmFD90+xSLNnTKCkAipTcRQyPa+MHN+J0bvhvXW\n" +
            "5S6VFXtMBNdvSap4fAXqHEvOMWvD6KpnplesbisgG+dKP937+fj1VKdMNK9IWWTH\n" +
            "QpvlmCjBAoGBAO/JDsr8WBjKznlbF4V7nLwmZyv7NXnR8RYjjLscEnoIzZhCxkJG\n" +
            "o//j/DM5wwRk2i7XKG5zPRgtVbIdwGkNgK7CwFu8TIAsxsIWB+h6CZXwHqK8k6e5\n" +
            "Eo3dptGhO/Vv8nnPfsv8IPVxwAPgiGjrRzr34Jx5QjcACHbh5Ru8jm/lAoGBAMmM\n" +
            "b1BLbTbSnujYzbr0jfkg+KzMRDwm0Gqx1Eyv2OqpEL1gVgsDypIVagmyBS7qP6DX\n" +
            "D0IdJYB1UTifMaCObidd3JZc5uegYGa/h/UbKdXCtpp4OrZN4PL6sE/hC9/yMiOg\n" +
            "JIV1zbMUdF7QdJ+MMSBa/rltmHuTM4hEtfob0NUxAoGBAK4XjfP2bofhhzM43cT+\n" +
            "UHeScknOY68ErENkoCKhaRDNH2gy4vrvitaY0lzmzR59kqN7d1Fpvau2Dof5bd9X\n" +
            "/Fvl7f8soWZWHCCCGk/BewAvjC6fN50Ik94IVbvRklTKaIPkEK1NayiI495swN1c\n" +
            "JSU9Hwi8SUThc0PNEqimp8u5AoGAYAStiz1D3Jhe6GNRL74OXR+eGQR/hYCgThRG\n" +
            "JfqohrLgrLfWhgzaVtoo2FGdMoqaoY+TT1X/ZcF+XlFJHUp9o/eNfXzo7HR1OL4K\n" +
            "kXTNa28F+3VH004q2tcfZA68z4Xc6SgD/ijvRF98SSdaSCBLzzRKoiBaQpUQOd0y\n" +
            "LONPjCECgYAlx5nYiywp3S/f9p298h+OiJNz4mrkMb9WomOecUNefpkzEokaLhkG\n" +
            "vLQasrkMQ6sb5CKH/N/zC6ng0qcouKvOs5U8Vwm7gMMcrELBgsBJEkBz8mRIcaQ2\n" +
            "7e0dLVnXSvKE5szHL9W+qnYeEad8wsPEn9SD5IqmLYCj+UUw89pshA==\n" +
            "-----END RSA PRIVATE KEY-----\n";
    private final String testEncryptedPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "Proc-Type: 4,ENCRYPTED\n" +
            "DEK-Info: AES-128-CBC,52D50A28762C880A362E80696B5D9FC1\n" +
            "\n" +
            "hR3M3xkzIwvr3+AQi7JCZoD0sTJnXM0jVzKMBh9grAUKE42YesKO7gUJcKIROQMh\n" +
            "PwsFLCQ/gkDcIN7KCDhMqpnTSOxzIuKz5NMRHFJuHr4AoO33dJEZFYKCulLc6MN4\n" +
            "xww896zPqMgjpskzB13Zh8iDDL3b0o7toQ3yIMc9DosIRgXBNOYXSGlxeTHT6k4X\n" +
            "MvjWRjnB7Yk6PFLPp0/hFQ+Cik334xz0K+NTl1tPG34RYXY6Y98AKKbMoXFtzS6x\n" +
            "2fXzb8wga6zT79d58la7JyR2JM8Euh+Jtx96lk9h5aZGvTSqx8hJM586NTw7PYsR\n" +
            "fDCNBXIj24LBlC71NQxJ8aif4AOCDaXT6Mpl8iVnKa8/K5h6NXAwpiBie9nF9DVJ\n" +
            "uvBIXgFOPGeg/xHiuwZLRsQh1zPl1CDpTZbqk4+OLOkYzDb9n4w+phy0tn9s/35e\n" +
            "UvD99V072MIl9eMurN+X9LJvtEdGlhU6nG9HE7s7l/dvXEr3jzu8C5tyhHAiG7tX\n" +
            "u2BHfW5RzCHuTWvd1v28xxecmQciLDmk87mCG3VFOfZynKEeJtilc0yTgwdYqgIB\n" +
            "OXbQIO3f0BI6TKOIpGFlyU1lTQV2vEdFbrcLPfUnVXjEu3fPxcNshSRlGTrgkPT9\n" +
            "njO0oslKW3PB7qm8XSmYh65nETmTuZKlRpIoS8aIz/OExT77uPbiUoaBTrWaMn9B\n" +
            "XmeUfZafQDOHjx5o/SDYQy86GMX9rzJfuEFAcLtwr/LML7CkpaPce+OPmFbHC0jw\n" +
            "ikyJphSJsZDLihldvXvPA8DSfDOmMQ3NG+kW9fgyzQ9IDLFYRagsXGAIucszv0/h\n" +
            "eziN9rqmM2A9BD3fwbwSOm92CjGZTXkMCZ+Bt6TO9d13a9kJwqEOfNPrpMaagTIt\n" +
            "coLRwOoEBdaXp9j4gdSFZBxazfSel4ZjxdXroE/6fBD8yqXaqaroXTIoG4GjEZGr\n" +
            "n/9tHEhdnjloSk1GAbu5ZKADdqPfNdW3UWQl4DoLYpMTFP7uOV1ZAFUzOBibUn/n\n" +
            "G1yG5cSXSf+Hb/WsKZFNn8+4YkaLONZeIeniqhGWS6y4oWCrHEEoUx1bODIpUhEs\n" +
            "PWd2ycIWk5ZxgEPizGJLXa9V4/6j1/OU6qM0FLJvD+atrPYmFEtZzNgzeasa/Vx4\n" +
            "Osif88HK2/QgN+5GM86jmlmtrGroIKaO4tH+JOV1HnrNp/5SEtITcslQkXxeqMo0\n" +
            "fOD9Nw5o3PvwAbuagKHolKc/DD28In8nqShpkztU+fqd90UtAQNBlRVgRaI+Ag7a\n" +
            "aJaGrAeoZ8VlX53INd0J2ByviwrPyDK07OoyADer2+ghy26j590yLOJp9efGXYkv\n" +
            "CHbYQLath8dCUlrPeClA76qYmDpgxdYporDmU1ijmo+mj3tybpfK/CAJjTFjsI1G\n" +
            "LVzrQDmhbx81W7POBzUjrqvmPL3NxUlHBGoNnd3OBfAJRT5ll5JrLL6l+oSoztff\n" +
            "ScgLNwjl1PM+PsMd+3AB6voFKEP5C9N8tp3vvajBZXoPnrQMn+udZwD7Stjam3A1\n" +
            "EHbBUMAcFmV2xbvbWKfftwzfxy+DCek4NJ0yOnNhng99gFZH5VmWU+/+uaHmddE9\n" +
            "aR7FyeJRX5QJPzsq/5sBraq3rsUsqZwmFRPQa657O1ZoAqLXrn/X3mZtY/Kp39LS\n" +
            "mROYEDfgo4BGXzAVnl51sEkKQUtk/0Nd1zmt0hFpXc0LSH1ZP5FIAESuBq1Mm5ga\n" +
            "WnLocFibtyrIIMEN4fb5IP3D2j3999L2xLx/p1yVxBqwmPrHChY8ptlxQJzLpCmH\n" +
            "pOMpJM/YmyMLrJZIG9irUJLFCyeID+do6ZvDF5W2j8ackb21FoLHEwH4J9YqTEmA\n" +
            "KyvvzODJeAD6xQJjks7VhF8WtcRfgpxWpSxao1ooPL571Yf40QeaKzeTowfnBtdl\n" +
            "FnSCpC9CY2+nkHdm56BD2coQxzy3aO4bw3p9MmyCZPtJTjgP8YA6HROkOGGIRQzn\n" +
            "DsvQ9y/Yec2IS5EWFRT/44YFZlCDpTEwXqRriuqD40gXZO//PE1DDc2GXuVvCyDD\n" +
            "DkaUD+8gDUURQmIw1KGsidC1kfQnWI/05GkPieH+lIf59NQqpbSNA6YADz+YY9zi\n" +
            "AEWMcSITxhydyiGCTn98QAhuWEqTqv39pgRqszxluQpMZtfLvZXBFuBIItoxO+mA\n" +
            "g3gSUT4/H8tf9IqXgMYt6aclPmee0SXtKmmad3uApqy41xP3VNG4lrDNMzRUi0Q3\n" +
            "9EAENOf7P0KvbjPP+o7gPe8DKlnzRuqMBSWODUiNp7h1yo2c93/Uy8nOTh47nWn5\n" +
            "o4dvJ56FzIYZ1wzg8Na3aIha5+Fj/T6nmDqCU4lpwrsEBa/cvrllMMTfo4E4hrrr\n" +
            "-----END RSA PRIVATE KEY-----\n";
    private BasicSslContextHelper basicSslContextHelper;
    private JsonHelper jsonHelper;
    private ImmutableCredentialProviderUrl immutableCredentialProviderUrl;
    private ImmutableThingName immutableThingName;
    private ImmutableRoleAlias immutableRoleAlias;
    private ImmutableCaCertFilename immutableCaCertFilename;
    private ImmutableClientCertFilename immutableClientCertFilename;
    private ImmutableClientPrivateKeyFilename immutableClientPrivateKeyFilename;
    private ImmutablePassword immutablePassword;
    private ImmutableSessionCredentials immutableSessionCredentials;
    private ImmutableIotCredentialsProviderCredentials immutableIotCredentialsProviderCredentials;
    private V2CertificateCredentialsProvider v2CertificateCredentialsProvider;
    private AwsCredentialsProvider awsCredentialsProvider;
    private V2IotHelper v2IotHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        jsonHelper = injector.jsonHelper();
        v2IotHelper = injector.v2IotHelper();

        Security.addProvider(new BouncyCastleProvider());
        v2CertificateCredentialsProvider = injector.v2CertificateCredentialsProvider();
        awsCredentialsProvider = injector.awsCredentialsProvider();
        basicSslContextHelper = mock(BasicSslContextHelper.class);

        immutableCredentialProviderUrl = ImmutableCredentialProviderUrl.builder().credentialProviderUrl(v2IotHelper.getCredentialProviderUrl()).build();
        immutableThingName = ImmutableThingName.builder().name(JUNK_CORE).build();
        immutableRoleAlias = ImmutableRoleAlias.builder().name(JUNK).build();
        immutableCaCertFilename = ImmutableCaCertFilename.builder().caCertFilename(JUNK).build();
        immutableClientCertFilename = ImmutableClientCertFilename.builder().clientCertFilename(JUNK).build();
        immutableClientPrivateKeyFilename = ImmutableClientPrivateKeyFilename.builder().clientPrivateKeyFilename(JUNK).build();
        immutablePassword = ImmutablePassword.builder().build();

        immutableSessionCredentials = ImmutableSessionCredentials.builder()
                .accessKeyId(ACCESS_KEY_ID)
                .sessionToken(SESSION_TOKEN)
                .secretAccessKey(SECRET_ACCESS_KEY)
                .build();
        immutableIotCredentialsProviderCredentials = ImmutableIotCredentialsProviderCredentials.builder()
                .credentials(immutableSessionCredentials)
                .build();
    }

    @Test
    public void shouldProduceAnX509CertificateHolderFromCertificateFile() {
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        Object objectFromParser = basicSslContextHelper.getObjectFromParser(testCertificate.getBytes());
        assertThat(objectFromParser, isA(X509CertificateHolder.class));
    }

    @Test
    public void shouldProduceAPemKeyPairFromPrivateKeyFile() {
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        Object objectFromParser = basicSslContextHelper.getObjectFromParser(testPrivateKey.getBytes());
        assertThat(objectFromParser, isA(PEMKeyPair.class));
    }

    @Test
    public void shouldProduceAPemEncryptedKeyPairFromEncryptedPrivateKeyFile() {
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        Object objectFromParser = basicSslContextHelper.getObjectFromParser(testEncryptedPrivateKey.getBytes());
        assertThat(objectFromParser, isA(PEMEncryptedKeyPair.class));
    }

    @Test
    public void shouldGetKeyPairFromPrivateKeyFile() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        basicSslContextHelper.getKeyPair(testPrivateKey.getBytes(), ImmutablePassword.builder().build());
    }

    @Test
    public void shouldNotGetKeyPairFromPrivateKeyFileWithWrongPassword() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        assertThrows(EncryptionException.class, () -> basicSslContextHelper.getKeyPair(testEncryptedPrivateKey.getBytes(), ImmutablePassword.builder().password("fake".toCharArray()).build()));
    }

    @Test
    public void shouldGetKeyPairFromPrivateKeyFileWithCorrectPassword() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        basicSslContextHelper.getKeyPair(testEncryptedPrivateKey.getBytes(), ImmutablePassword.builder().password("password".toCharArray()).build());
    }
}
