package com.awslabs.resultsiterator.implementations;

import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.IotHelper;
import com.awslabs.resultsiterator.data.ImmutablePassword;
import com.awslabs.resultsiterator.interfaces.CertificateCredentialsProvider;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

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
    private final Logger log = LoggerFactory.getLogger(BouncyCastleCertificateCredentialsProvider.class);
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
    // Generated with "openssl genrsa 4096 | pbcopy"
    private final String testRSAPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIJJwIBAAKCAgEAscU2DonndcpYIK5fVvnsK+p6N91rTFqimmE9hf6o3ggBqcLC\n" +
            "Hpq7CvcXZMTMPTjU/FlNGALMR1SJdJTIds0T8AuZaaa9VPT5hxGU0DyuuMXtttuP\n" +
            "aYgSS1dIhKHnwyETWLujrTgl/jhnt929sl8K/Dda6n27uWyBesjDa8T+8O0a52Q4\n" +
            "GiZo8YHnsigaqckn/ZkobyQKT2ru+8j0fFZ0nvRx/68RH5jAiM78eTcwM7S5dPkB\n" +
            "V48svFhixtn2KWn4RSN2EmtDwtCHyqAf/xHZ/19mOMkjUSat4bD4d7GUxwjn9qnc\n" +
            "oJ2mLALY7gvCkVyHz3CrHMl+tk5lOmOo7AhOwUfGCMdMBEVHdLBQDDueI5cXfWnI\n" +
            "dWmDZx7kCX+8fnzIYwUG9HNw1Hr8nrP4TaBLHRYL5CZQxnpg/j3TO8DoVqlbl1W0\n" +
            "rNwfiPeVtYM/5t2GVs8Tb0wmOffJl1oc0S2qHeVK8Vm4c68f6d6YvToJkZhyZRpG\n" +
            "b0NsHtlZBS3APhEa1BkXlxP/7P148bUut9APLGpRG4OZTEIm83B2lfEYz+vKijai\n" +
            "PcbvyWyaSfRjZDIk3Cd9zpzucwjyhWy/iCfMYtFj5fz7lMtRHE3H1prQEo2gPcSH\n" +
            "ZJoC1Mt4gWWsK9nErNFD0HyDEc9nIa5//vZp3DszkUcIycVonxJkNGwQ/k0CAwEA\n" +
            "AQKCAgBq2rDIgmoHam4Yjtet7yHfr9xw8f3J8nzksOiLP2x/sW5WfUOzo6wkRivg\n" +
            "nU5qyltzp7XoZd4mThElaz0n89M1KbO4RsptY6cNYisCFEettwNRM557f5gHg1qK\n" +
            "pssphhsb7gXPW/2yVnM5mOaqbeirfBaA9ry9ExStGjok8E+Rv+O5DIuQZGAWMtBz\n" +
            "TRnhzpDkJYihbZdo70zhOYSDrHADD8nqN2/ify3Tzh8COYMWwV3sirQczmnEeJg/\n" +
            "EMqfUBw0asFRBvq/AhttV1yENFXF47ENFxHMeKRyEa74O1zDWn4bzTRZswksUTXV\n" +
            "uGnmeCkGLqIb9p3ctTixpS/nWIMFOA1fSiPzjNhTlQerJrFiwwUuNflrqZHzgQ8g\n" +
            "a1y/7H67w2j8LxQgsAnQ4EeJ8e2Go8VHJ/JttkZ61ZMfBS/3BZFY7Qg8ufJ3W/vR\n" +
            "XtSVTsLf+OT9dzlxpkPBjXZphJMGlsFPU2zHY41C9JfzzFFEbgQ0gb4CHZeBkjMZ\n" +
            "7E3UN0pl/mnnJ+BqsZ1EFWiFxyAKWANFfGOhYhtJkHcMuYCrHJ+htTI5zaT12Wwv\n" +
            "S3hy2TjLfad22r7wumjhJSQj+amgHX6beHmziMOBIl/OVPB8RZAZfWsXFX3lz96f\n" +
            "J1p9IryLy46OANNd9YEYw5mgBLLBPNOcETY9VojbxH+IoV0TXQKCAQEA3r4M4bBw\n" +
            "YZm6LIs8lBW9uvsJN1wGKDz0fNCbXwATufalj/KHIMv74bERgEgUXP5ABlK2DlV/\n" +
            "bXj48QmhauZaHuWt/TIcbiRj+0W90wLLjGL7GMBOFGo3I+lblw4aLy8kxp23zew/\n" +
            "E444fZ6w8jUKcg5mdfk4XR74aASmud2ui4bHk72rJ107iDMAB3FktBII/ngelM8t\n" +
            "EyEQvF7PTTVJAIypI4RAd/1fL1MjmXuzPH+nLHG7eDk9XTec2ejBiUGSs8cesg4d\n" +
            "waARxGwwpc5p1AJjLaySEG5qaQTlKeYdewmIPsr7DMEvKQ0fDNjtJQJrdcS1J6cw\n" +
            "beHzO2eWT+bxbwKCAQEAzFAubu2tPeZUKMEIxmDU09oOV9hSCzPkzP0ZC3ivxKrJ\n" +
            "ImmbZ3NSV8KCYY81kZKxp71R9UTFydHivW2ma+At6Eqt89HDrtlWMLCofE79cMkC\n" +
            "fNnoogNs12o+6Y/f/MfbcSu2nRpR8LdcsJXc3Wvh4b3ZAI15RB4rrQ3RtaIT9ns1\n" +
            "l+fNWzZWitD/Nf36aXkXBYXcaM6K12rS/3kLxn9zaielMOhAnzTVisK+CqLDzRpQ\n" +
            "Wz1vWdQxJUATFLZDNJ+2xTGszcUGJ2PWmAnsb6tHMYon/a25qdbIsaq7PRG3YnTw\n" +
            "/Ee7qRrxnEdEOv95BuOffGpjtfwUnZlv8jvIiJ12AwKCAQAx4hQWcyIn6XqVQTV2\n" +
            "4IuWFWRR/ozudAA496rkEqtYSVF4tFLo2GX3fGUz+nB9Bv+lqt5UxXb9OEtgB80b\n" +
            "mKz0IHHfs1pEnGe7vTmuyQB21y/ushqKXeMtarR1VtYsXP16cZXZSyAxK/egwmpU\n" +
            "Q+ar9meh4gdqa2YsPWZtV6UgYDXP83kiHrIXZXyLLizkWumiu5n1r5QjpZMO7Ji2\n" +
            "bMmFkvrKFnMtrBOmEJN3awmP8fKpdGsMmJ0ShELfVk4JhCLrmhtYuuqVE1kHSUd3\n" +
            "yotOzJ4Te4NWpLO2Az+jK4LMrCzUCzc/+v+pzON4SBiL4kfYnw4G7F1fLmv/kMpL\n" +
            "6KeHAoIBABH2kr6KAwtILuoOXrkiVlPx/gTXLg9yFpG3RvZtO/bslRaDdnhX+Uth\n" +
            "/JibQLh1z0zSJlyMGV+vJmJFO7aMVTzxI+4l6TB8R0msnoOfZkT+R0u21O85od4m\n" +
            "pzVdwvG0mKSQlVOmtsGVPX8BDhQhohB44pVb2ueUR37FkkSH5X1sQ8ABT1rPojg+\n" +
            "O9IBbHzNeqqvpDtKvYZHDBoOCG0BU6Jnrexo3xWgTY4PpSnGObTUtW/wLNQXBeMi\n" +
            "iQrvI2WDDUy1G15UDkB0VK+1X6ZJxs6iOPXiykoWajrEqgWqgxcS9QUZQXSSu99j\n" +
            "nKobQAbNCH6l0/JyIVXh4afIfc8VGQUCggEASSuG3geuBcDKggtikckJwfsQM6Er\n" +
            "GugATaGgtZbnCDr0AeNst3EvscItoX5ZBNs+ZRxcPV0mv1U+c2g+93vZqi+1Y3JC\n" +
            "3PJ5mYY46tSjKmc2m/536kCBfQf0kk+xRM9DfTvjVbl+gslo7Tmo0uJXXfeMKhLT\n" +
            "ak2qe7KysLu5pbhf8SFU+khpbiAgomgqq3BbF8sVyodvMoXA4KULWSdQbvGLvdet\n" +
            "7IFC7OidgyoP0KCC5wD3v7J3sY5+UqNg1EIHNARDiohjat6f2rw81zEvU2z1tTMT\n" +
            "9ve5xAUvfhkOLz76LEyhISNUV3sMjheyHd5qQw14jivLQITXgQzggIZqJw==\n" +
            "-----END RSA PRIVATE KEY-----\n";
    // Generated with "openssl genrsa -aes128 -passout pass:password 4096 | pbcopy"
    private final String testEncryptedRSAPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "Proc-Type: 4,ENCRYPTED\n" +
            "DEK-Info: AES-128-CBC,A18E707E9F8FDA99194DC89DDE23E8FA\n" +
            "\n" +
            "Y4sH++r6za2WheAAuhwyFQcbdeXqxft+4vPFtHxRi8O71K78qD3m3ppagHwuXzAp\n" +
            "Xbu5/GkgMH6iV0p0rRqrzWNGzhK8wCraMWl9g6QQ3KqDqqn4hgbdyLXxKriq7Hmg\n" +
            "XRHt0YbCdPD0XgZ2LldcX9DnlkJCiLh/eW4QTBfeZZ5t3I06Hzk7aqhVED/n573k\n" +
            "VMFAo7hlQch7pGCbZo6tQnUt1ZVVtqfQJX2XWwNZH1qXPlVuumchB9ze4HtDPiZl\n" +
            "t3Y+CGIvnp5nz8CIcU5W0DTqov+NeHYbn2+YtWjcANccOhpqlF4DnwU44BwzJvOD\n" +
            "12qDoNuxOZ5T3XrsEljv8V8VaOGF6mlKqLcFa0WIswuN3spSIjbB5QJRgzl5BIJw\n" +
            "+yqa6kVUqG58a7rT3W5mLQ2tCGSJnmBMtXjvwA1T4IoZdhqKbDrF7Ab0BTt9AAnZ\n" +
            "Gp2x8i4zfb8vHJq3fvFho35XBFotnlHHtzbq4gj+D5My1Pk0JsxazNdVQlIFzQzS\n" +
            "JLNSxTmN0+EzpqtI5LSvDJDqSbeFV4ZaEtGp12D67bctfv3h6fxznVlrnq3w01Ol\n" +
            "z/jLd0s/qzFRjLzr3zs8hzo5Oz7DxDKV3SIDaEDm5+oYFH4kOpRuP1dJy57RXk92\n" +
            "Djj1ccne7PGXRygRv3LrH755gzPnYrxI3U0jdi++TPuFbIlJNURBPusTNQfSt2D+\n" +
            "4BkoSBcnd6m30xyYWpSgmsbCnRAhzk7sW/BiIbaEBmQOkQmSvHrDeMp7ZOhzphEJ\n" +
            "m/7AcYgMFkj/Pvb2qD82kpj9jezUccMoVY0iaym3DK2Jb5Rppc4Q3BV7Ds6tysP7\n" +
            "hiF1jx5UfhuSWBAU4eZDSFTQ+p4JFp1knDfCcpmj520iSiK+8p8nhQXNrT+qeHhl\n" +
            "TnYapak9khXjo5+6Xw2l000cJFgDsoiGMKxHlJwU57T6+RY52Dl8VikCiBt5xCAq\n" +
            "WtpmMyMoErgUu9VZ9zGdXr3TBg8dTkYKC9Rlbc/l0NAonOr/9do6zKqwaFhij/u9\n" +
            "nnxRNNsxfDIGm1cIArJgInUicN/9XKBRyy4r8r+4Zksx1bfbIbpovAKkGWKD/wEQ\n" +
            "JcPu38fBrn/WLbewPRLpfdXP+D3tsJ5Wl9ACbXo1oLFqQR9CKkTLRjMyd7BZc+P2\n" +
            "IfwQ83Fkjuc93B9569Qgj/w+Pzq1gxzswqKHpV0jj40Fb5YR7gnLvKL0TEO4TZDJ\n" +
            "Qd3FT9s2tjp+wD8AC0ybDYq10+po1wKj0+7vBN7Rs8EUo0ypeP5F+DVOeDFt3rL3\n" +
            "XRZ/EdggIB7tRetR6yIEgCFVzzP28l53zan0FSSBu5b39rCwyz9Y1osXMy4Mm1MZ\n" +
            "yw3jB+hqkBi1nrbEplARWXKA6Zbcai5uwt7GjtkWBa1h+bqNs/C9WEOMWbIyBL2H\n" +
            "y+3evyhYp4ojeJN40w8UoGz4N+KX8pHxfbnYd9Pk0JpDri8JD7omkknREHBfAX6T\n" +
            "4glimVPNK2M2Dpm497749F0BlodCaNmH0js4jJ7PytEP2uZH5ySUqkjxC/lQuj8/\n" +
            "TDXeH6MO4J+W2Z4L+PLijOaltoLO73ZjZ1Y2GmOk+oqWNzB5tGTfaUmkp8FsyPiU\n" +
            "tzqTHr8uGbZgh6tSiw3dOCDXLWjhIIzhh7gD/BoAsvw5gP+DSX0lSpcPWf2pH9uG\n" +
            "4uGU0gALpZFEHm+jJM5B27HgbaWfTcGfGNVseNwqs235JQ2hpb743MRVXTdHLbL5\n" +
            "qQoX/cGUDnZwknQhVDw8kqDGqiV9FxfO9AXSRzlreV5rUzx8QxppnuIMqAqwo02Q\n" +
            "8Mi0gvBHQrULfD0A51/oN1Y9caQutsjYYzirRCnddN0kNI3qomr2FFsZDLGWn1XB\n" +
            "JWO9PaH7mLvI7tQ/XnLXJoCQQBidS9nCm8ZhKnKR5Gs1Zau9d3TKTcGstTlwbyTq\n" +
            "oV2MVwS/50/QeIzVOHOY1YS9NHiCYezmnIeTB+3HvZbmSEmJYNgZDbV38BhUh6hi\n" +
            "VoE+hwPU224VGmisRLBsXU1QyvVh8flm7L7mnrgdMoUy6h5piTPisyLu2UTAhpmq\n" +
            "YVwDfCOrRhYCA3gTtB5AjXFfxi9eYBQXYjJH+bGZVe5nKFpdHbk0Nma4ujOT23A4\n" +
            "79bGncVYp4lYE1v3hViWtraR6TneF4G7DGR8UZp1XJ8D4edzo75qj+YYfJfd94w9\n" +
            "B0CJ0RaoFxDA6HuBqtzNwP9knqj4STPIDydXLZp4XZZHJN4GYKRYas8+7V8oD+8a\n" +
            "v/bQra+PYuxXdCsUESpOHBmBYLAKS3/ZksOuOmztf156VkfXuiR4RwsaLmAHfXXq\n" +
            "yXtGngadNU1/AlxVgmMDmbNfaG6XXZxOIQd5UhhOfo9jJTpTlJBmuZKT0gHwbLVc\n" +
            "ir8moiZluBx650Nw/mqLpz273QhxpnpPwaJ1noQPyMmL6xWIfXRNsFqGGjTGVviN\n" +
            "69hDFvvDP3epY0AHeCjo4c0RPXTqdbYbij8yckLMeHLn5w0l5+zan5X7VdyBfgYR\n" +
            "qKfDG6aatzpTeLsxBqgMKkn6Rdc8DPPs1ApdFcUS2QZ+T6lcGpsVSdljf2V6B8Tj\n" +
            "C6KG3bRfeNpvD5WyUswGjNQIlkSAuZD/KPUckThftK4VvGtWX9HsBaW4ILf4zvfi\n" +
            "TSdrxN8WeDb2CDwcLOy310lJNEcbwrYy+GZ2szn3kn6yEUjSu+SLkkVL29JHGu5L\n" +
            "6m9y2tVWbx2pqrznvnwjHWysX9Nv1s1/SIGBzNXuvwUXZvY8Y77JbgYiLsN8t+k6\n" +
            "UgXBQxz5F/6YIvaJkLWZp/fCF5Uc7xMUUrpc4VFgG5judcFwrsj8fdocPeoVxNOx\n" +
            "d9EHgU8/FAutqecmKSzDFgsRG8G7pwyUX8X0o00SQi/vid2C2WbkI/TN+WyTxbgv\n" +
            "CcdF63PVWVOfI68PQBz8Aa5hU+jHYaVvLwYzu/Z/w3Pn+KZxOQGW4r25y2mX+1z1\n" +
            "8GAPGuA2pvfZSZECGnR/s36TaJU+wMkXA8pvsIX1QW0st/i2hRgst4JR/3PARKkJ\n" +
            "/oHMwfvex1YGXq4f10OhTL0BReYOviiPs0idASkLNg/wo/Zj3ZkaFGgoVgXJAh3c\n" +
            "rREMfjcTRCNoYgJ9nHQMc6Vuj/oGlGRzDsVABIi+WRGH+QxLLnQE+mic0+HLNTRN\n" +
            "-----END RSA PRIVATE KEY-----\n";
    // Generated with "openssl ecparam -name prime256v1 -genkey -noout | pbcopy"
    private final String testECPrivateKey = "-----BEGIN EC PRIVATE KEY-----\n" +
            "MHcCAQEEIPm1w6XlLlBnzEtkCPE55zDV0iefFWgy8Q0ThKjEFLO5oAoGCCqGSM49\n" +
            "AwEHoUQDQgAEg8ONn7hmCLnVoXDodZH7UyAj85/fPtNg9Az+K+3Jjmc+8Pz8AWoK\n" +
            "hLLA92N4KiphWWXRyaf/nGXqzMLX/mRhFA==\n" +
            "-----END EC PRIVATE KEY-----\n";
    // Generated with "openssl ecparam -genkey -name prime256v1 | openssl ec -aes128 -passout pass:password | pbcopy"
    private final String testEncryptedECPrivateKey = "-----BEGIN EC PRIVATE KEY-----\n" +
            "Proc-Type: 4,ENCRYPTED\n" +
            "DEK-Info: AES-128-CBC,F755BAFC95F58D55EC28BF62BDA00779\n" +
            "\n" +
            "MF4UIIprneEye1t1tNClh3Pg2Dak61xJmPRvFElXy8bzRayWsYmisdZoiXXqrloI\n" +
            "ZtSpnx6UVLI2lRyfV/vuiDnjnquE1fJ5fP3P22dMMv8wR7d8kxsCNrUYYF8v1hv6\n" +
            "r/5KuV5s8eIKcmS2XgDOCWrAmpeJGObWVa7xClZr8lE=\n" +
            "-----END EC PRIVATE KEY-----\n";
    private BasicSslContextHelper basicSslContextHelper;
    private ImmutableCredentialProviderUrl immutableCredentialProviderUrl;
    private ImmutableThingName immutableThingName;
    private ImmutableRoleAlias immutableRoleAlias;
    private ImmutableCaCertFilename immutableCaCertFilename;
    private ImmutableClientCertFilename immutableClientCertFilename;
    private ImmutableClientPrivateKeyFilename immutableClientPrivateKeyFilename;
    private ImmutablePassword immutablePassword;
    private ImmutableSessionCredentials immutableSessionCredentials;
    private ImmutableIotCredentialsProviderCredentials immutableIotCredentialsProviderCredentials;
    private CertificateCredentialsProvider certificateCredentialsProvider;
    private AwsCredentialsProvider awsCredentialsProvider;
    private IotHelper iotHelper;

    @Before
    public void setup() {
        BasicInjector injector = DaggerBasicInjector.create();
        iotHelper = injector.iotHelper();

        certificateCredentialsProvider = injector.certificateCredentialsProvider();
        awsCredentialsProvider = injector.awsCredentialsProvider();
        basicSslContextHelper = mock(BasicSslContextHelper.class);

        immutableCredentialProviderUrl = ImmutableCredentialProviderUrl.builder().credentialProviderUrl(iotHelper.getCredentialProviderUrl()).build();
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
        Object objectFromParser = basicSslContextHelper.getObjectFromParser(testRSAPrivateKey.getBytes());
        assertThat(objectFromParser, isA(PEMKeyPair.class));
    }

    @Test
    public void shouldProduceAPemEncryptedKeyPairFromEncryptedPrivateKeyFile() {
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        Object objectFromParser = basicSslContextHelper.getObjectFromParser(testEncryptedRSAPrivateKey.getBytes());
        assertThat(objectFromParser, isA(PEMEncryptedKeyPair.class));
    }

    @Test
    public void shouldGetRSAKeyPairFromPrivateKeyFile() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        basicSslContextHelper.getKeyPair(testRSAPrivateKey.getBytes(), ImmutablePassword.builder().build());
    }

    @Test
    public void shouldNotGetRSAKeyPairFromPrivateKeyFileWithWrongPassword() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        assertThrows(EncryptionException.class, () -> basicSslContextHelper.getKeyPair(testEncryptedRSAPrivateKey.getBytes(), ImmutablePassword.builder().password("fake".toCharArray()).build()));
    }

    @Test
    public void shouldGetRSAKeyPairFromPrivateKeyFileWithCorrectPassword() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        basicSslContextHelper.getKeyPair(testEncryptedRSAPrivateKey.getBytes(), ImmutablePassword.builder().password("password".toCharArray()).build());
    }

    @Test
    public void shouldGetECKeyPairFromPrivateKeyFile() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        basicSslContextHelper.getKeyPair(testECPrivateKey.getBytes(), ImmutablePassword.builder().build());
    }

    @Test
    public void shouldNotGetECKeyPairFromPrivateKeyFileWithWrongPassword() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        assertThrows(EncryptionException.class, () -> basicSslContextHelper.getKeyPair(testEncryptedECPrivateKey.getBytes(), ImmutablePassword.builder().password("fake".toCharArray()).build()));
    }

    @Test
    public void shouldGetECKeyPairFromPrivateKeyFileWithCorrectPassword() {
        when(basicSslContextHelper.getKeyPair(any(), any())).thenCallRealMethod();
        when(basicSslContextHelper.getObjectFromParser(any())).thenCallRealMethod();
        basicSslContextHelper.getKeyPair(testEncryptedECPrivateKey.getBytes(), ImmutablePassword.builder().password("password".toCharArray()).build());
    }
}
