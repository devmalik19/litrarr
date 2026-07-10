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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@ControllerAdvice
public class ControllerConfig
{
	private static final Logger logger = LoggerFactory.getLogger(ControllerConfig.class);

	@ModelAttribute
	public void add(Model model, HttpServletRequest request)
	{
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		String section = uri.substring(contextPath.length());
		model.addAttribute("section", section);
	}

	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	@ExceptionHandler(ResourceAccessException.class)
	public void networkError(ResourceAccessException e)
	{
		logger.error("Network error - external service unreachable: {}", e.getMessage());
	}

	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	@ExceptionHandler(HttpServerErrorException.class)
	public void upstreamError(HttpServerErrorException e)
	{
		logger.error("Upstream server error: {} {}", e.getStatusCode(), e.getMessage());
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(HttpClientErrorException.class)
	public void clientError(HttpClientErrorException e)
	{
		logger.warn("Client error from upstream: {} {}", e.getStatusCode(), e.getMessage());
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(IllegalArgumentException.class)
	public void badArgument(IllegalArgumentException e)
	{
		logger.warn("Bad request: {}", e.getMessage());
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	public void all(Exception e)
	{
		logger.error("Unhandled exception: {}", e.getMessage(), e);
	}
}
