package devmalik19.litrarr.helper;

import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

import devmalik19.litrarr.constants.Constants;

public class JsonDecrypt extends JsonDeserializer<String>
{
    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
    {
        String text = jsonParser.getText();
        if(StringUtils.hasText(Constants.ENCRYPTION_KEY))
        {
            text = EncryptionHelper.decrypt(text, Constants.ENCRYPTION_KEY);
        }
        return text;
    }
}


