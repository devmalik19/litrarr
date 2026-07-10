/**
 *  This class is to make HTTP requests (GET, POST) using spring boot RESTClient library.
 */
package devmalik19.litrarr.service;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HttpRequestService
{
	private static final Logger logger = LoggerFactory.getLogger(HttpRequestService.class);

	private final RestClient restClient;

	public HttpRequestService()
	{
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(30));

		this.restClient = RestClient.builder()
			.requestFactory(factory)
			.build();
	}

	/**
	 *  This method is used to send a GET request with headers.
	 */
	public String doGetRequest(String url, Map<String, String> headers)
	{
		return doGetRequest(url, headers, new HashMap<>());
	}

	/**
	 *  This method is used to send a GET request without any headers or query string.
	 */
	public String doGetRequest(String url)
	{
		return doGetRequest(url, new HashMap<>(), new HashMap<>());
	}

	/**
	 *  This method is used to send a GET request with headers and query string.
	 */
	public String doGetRequest(String url,
							   Map<String, String> headers,
							   Map<String, String> params)
	{
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
		params.forEach(uriBuilder::queryParam);
		URI finalUri = uriBuilder.build().toUri();

		try
		{
			ResponseEntity<String> response = restClient
				.get()
				.uri(finalUri)
				.headers(httpHeaders -> headers.forEach(httpHeaders::add))
				.retrieve()
				.toEntity(String.class);

			logger.debug("Request response: {}", response);

			HttpStatusCode status = response.getStatusCode();
			if (status.is2xxSuccessful())
				return response.getBody();
			else
				logger.error("Request failed with status code: {}", status.value());
		}
		catch (Exception e)
		{
			logger.error(e.getMessage());
		}

		return "";
	}

	/**
	 *  This method is used to send a GET request with headers and a pre-built URI.
	 */
	public String doGetRequest(URI uri, Map<String, String> headers)
	{
		try
		{
			ResponseEntity<String> response = restClient
				.get()
				.uri(uri)
				.headers(httpHeaders -> headers.forEach(httpHeaders::add))
				.retrieve()
				.toEntity(String.class);

			logger.debug("Request response: {}", response);

			HttpStatusCode status = response.getStatusCode();
			if (status.is2xxSuccessful())
				return response.getBody();
			else
				logger.error("Request failed with status code: {}", status.value());
		}
		catch (Exception e)
		{
			logger.error(e.getMessage());
		}

		return "";
	}

	/**
	 *  This method is used to send a POST request.
	 *
	 * @return It will return body of the response.
	 */
	public String doPostRequest(String url, String body, Map<String, String> headers)
	{
		ResponseEntity<String> response = doPostRequestRaw(url, body, headers);
		HttpStatusCode status = response.getStatusCode();
		if (status.is2xxSuccessful())
			return response.getBody();
		else
			logger.error("Request failed with status code: {}", status.value());
		return "";
	}

	/**
	 * This method is used to send a POST request.
	 *
	 * @return It will return ResponseEntity.
	 */
	public ResponseEntity<String> doPostRequestRaw(String url, String body, Map<String, String> headers)
	{
		ResponseEntity<String> response = restClient
			.post()
			.uri(url)
			.body(body)
			.headers(httpHeaders -> headers.forEach(httpHeaders::add))
			.retrieve()
			.toEntity(String.class);
		logger.debug("Request response: {}", response);
		return response;
	}
}
