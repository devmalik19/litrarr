package devmalik19.litrarr.helper;

import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

import devmalik19.litrarr.constants.Constants;

public class JsonEncrypt extends JsonSerializer<String>
{
    @Override
    public void serialize(String s, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException
    {
        if(StringUtils.hasText(Constants.ENCRYPTION_KEY))
        {
            String encryptValue = EncryptionHelper.encrypt(s, Constants.ENCRYPTION_KEY);
            jsonGenerator.writeString(encryptValue);
        }
    }
}


