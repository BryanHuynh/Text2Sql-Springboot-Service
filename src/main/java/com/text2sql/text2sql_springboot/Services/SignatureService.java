package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.Config.MLServiceProps;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class SignatureService {
    private final MLServiceProps props;

    public SignatureService(MLServiceProps props) {
        this.props = props;
    }

    public String generateSignature(String payload) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(props.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);

            byte[] signatureBytes = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : signatureBytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    public boolean verifySignature(String payload, String receivedSignature) {
        try {
            String expectedSignature = generateSignature(payload);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            System.err.println("Signature verification failed: " + e.getMessage());
            return false;
        }
    }


}
