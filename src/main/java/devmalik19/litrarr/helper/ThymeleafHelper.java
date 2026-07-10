package devmalik19.litrarr.helper;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("imageHelper")
public class ThymeleafHelper
{
	public String getPath(String fileName)
	{
		if(StringUtils.hasText(fileName))
			return "/cache/library/" + fileName;
		else
			return "/images/default.png";
	}
}
