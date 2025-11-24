package devmalik19.litrarr.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ControllerConfig
{
	Logger logger = LoggerFactory.getLogger(ControllerConfig.class);

	@ModelAttribute
    public void add(Model model, HttpServletRequest request)
    {
        model.addAttribute("section", request.getRequestURI());
    }

	@ResponseStatus(value = HttpStatus.BAD_GATEWAY)
	@ExceptionHandler(Exception.class)
	public void all(Exception e)
	{
		e.printStackTrace();
		logger.info(e.getLocalizedMessage());
	}
}
