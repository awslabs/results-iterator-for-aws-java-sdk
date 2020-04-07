# AWS IoT Credential Provider support

This library transparently supports the [AWS IoT Credential Provider](https://docs.aws.amazon.com/iot/latest/developerguide/authorizing-direct-aws.html). To use this support you will need:

- A client certificate file for a certificate that is registered in the [AWS IoT Registry](https://docs.aws.amazon.com/iot/latest/developerguide/authorizing-direct-aws.html)
- The private key file for the certificate above. If the private key file is encrypted you will need the password as well.
- The credential provider URL for your account. This can be obtained by calling the [describe-endpoint](https://docs.aws.amazon.com/cli/latest/reference/iot/describe-endpoint.html) API with the `--endpoint-type` set to `iot:CredentialProvider`. The value should be the hostname of the credential provider only, not a JSON object. To get this raw value you can run this command: `aws iot describe-endpoint --endpoint-type iot:CredentialProvider --output text`
- The thing name that the certificate is attached to. This is the raw name, not the thing ARN.
- The role alias created in the AWS IoT Credential Provider documentation
- The [AWS CA certificate](https://www.websecurity.digicert.com/content/dam/websitesecurity/digitalassets/desktop/pdfs/roots/VeriSign-Class%203-Public-Primary-Certification-Authority-G5.pem)

With these items the following variables need to be provided to the JVM. These variables can be in the JVM's system properties, or set as environment variables. The format is the same for both:

- `AWS_CREDENTIAL_PROVIDER_URL`
  - Value - The value from `aws iot describe-endpoint --endpoint-type iot:CredentialProvider --output text`
- `AWS_THING_NAME`
  - Value - The thing name. Not the thing ARN.
- `AWS_ROLE_ALIAS`
  - Value - The role alias name. Not the role alias ARN.
- `AWS_CA_CERT_FILENAME`
  - Value - The relative or absolute path to the CA certificate file. Typically this file is called `root.ca.pem`.
- `AWS_CLIENT_CERT_FILENAME`
  - Value - The relative or absolute path to the client certificate file. Typically this file is called `client.pem`.
- `AWS_CLIENT_PRIVATE_KEY_FILENAME`
  - Value - The relative or absolute path to the client private key file. Typically this file is called `client.key`.
- `AWS_CLIENT_PRIVATE_KEY_PASSWORD`
  - Value - (Optional) The password required to access the client private key file.

Rather than putting each of these values into the system properties or environment variables a properties file can be created that contains these values in the standard Java properties format. When using this feature simply specify the path to the file in the `AWS_CREDENTIAL_PROVIDER_PROPERTIES_FILE` key in either system properties or the environment. The library will read this file, populate the values, and load everything automatically. This is useful if you use the Greengrass Provisioner and want to test out the credentials it creates. The Greengrass Provisioner creates this file for each group deployment in the group's credentials directory. The file it creates is called `iotcp.properties`.

A sample `iotcp.properties` file looks like this:

```
#Wed Apr 08 09:17:08 EDT 2020
AWS_CLIENT_PRIVATE_KEY_FILENAME=core.key
AWS_CLIENT_CERT_FILENAME=core.pem
AWS_THING_NAME=comparable_approval_Core
AWS_ROLE_ALIAS=Greengrass_CoreRoleAlias
AWS_CREDENTIAL_PROVIDER_URL=cxxxxxxxxxxxxl.credentials.iot.us-east-1.amazonaws.com
AWS_CA_CERT_FILENAME=root.ca.pem
```

*Note: The paths of the private key file, certificate file, and CA certificate file must be relative to the properties file itself.*

For this example assume that `iotcp.properties` is located in the `credentials` directory. The `credentials` directory must look like this:

```
credentials
├── core.key
├── core.pem
├── iotcp.properties
└── root.ca.pem
```

The credentials provider will resolve the relative paths of the files for you.
