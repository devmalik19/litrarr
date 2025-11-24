package devmalik19.litrarr.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper
{
	static Logger logger = LoggerFactory.getLogger(EncryptionHelper.class);

	public static String encrypt(String value, String key)
    {
        try
        {
            byte[] encryptionKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(encryptionKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] response = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(response);
        }
        catch (Exception e)
        {
			logger.error(e.getMessage());
        }
		return value;
	}

    public static String decrypt(String value, String key)
    {
        try
        {
            byte[] decodedValue = Base64.getDecoder().decode(value);

            byte[] encryptionKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(encryptionKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] response = cipher.doFinal(decodedValue);
            return new String(response, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
			logger.error(e.getMessage());
		}
		return value;
	}
}
