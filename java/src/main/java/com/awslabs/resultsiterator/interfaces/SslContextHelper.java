package com.awslabs.resultsiterator.interfaces;

import com.awslabs.iot.data.ImmutableCaCertFilename;
import com.awslabs.iot.data.ImmutableClientCertFilename;
import com.awslabs.iot.data.ImmutableClientPrivateKeyFilename;
import com.awslabs.resultsiterator.data.ImmutablePassword;

import javax.net.ssl.SSLContext;

public interface SslContextHelper {
    SSLContext getSslContext(ImmutableCaCertFilename caCertFilename,
                             ImmutableClientCertFilename clientCertFilename,
                             ImmutableClientPrivateKeyFilename clientPrivateKeyFilename,
                             ImmutablePassword password);
}
