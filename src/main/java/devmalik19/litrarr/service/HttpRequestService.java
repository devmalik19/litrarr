/**
 *  This class is to make HTTP requests (GET, POST) using spring boot RESTClient library.
 *  Includes rate limiting and retry with exponential backoff to avoid 429/503 errors.
 */
package devmalik19.litrarr.service;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HttpRequestService
{
	private static final Logger logger = LoggerFactory.getLogger(HttpRequestService.class);

	private static final int MAX_RETRIES = 3;
	private static final long INITIAL_BACKOFF_MS = 1000;
	private static final long MIN_REQUEST_INTERVAL_MS = 1100; // ~1 req/s to respect API limits

	private final RestClient restClient;
	private final ReentrantLock rateLimitLock = new ReentrantLock();
	private volatile long lastRequestTime = 0;

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
	 * Enforces a minimum interval between outgoing requests to avoid rate limiting.
	 */
	private void throttle()
	{
		rateLimitLock.lock();
		try
		{
			long now = System.currentTimeMillis();
			long elapsed = now - lastRequestTime;
			if (elapsed < MIN_REQUEST_INTERVAL_MS)
			{
				long sleepTime = MIN_REQUEST_INTERVAL_MS - elapsed;
				logger.debug("Rate limiter: sleeping {}ms before next request", sleepTime);
				Thread.sleep(sleepTime);
			}
			lastRequestTime = System.currentTimeMillis();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		finally
		{
			rateLimitLock.unlock();
		}
	}

	/**
	 * Executes a request with retry and exponential backoff on 429/503 responses.
	 */
	private ResponseEntity<String> executeWithRetry(Supplier<ResponseEntity<String>> requestSupplier)
	{
		long backoff = INITIAL_BACKOFF_MS;

		for (int attempt = 0; attempt <= MAX_RETRIES; attempt++)
		{
			try
			{
				throttle();
				return requestSupplier.get();
			}
			catch (HttpClientErrorException | HttpServerErrorException e)
			{
				int status = e.getStatusCode().value();
				if ((status == 429 || status == 503) && attempt < MAX_RETRIES)
				{
					logger.warn("Request got HTTP {}. Retrying in {}ms (attempt {}/{})",
						status, backoff, attempt + 1, MAX_RETRIES);
					sleep(backoff);
					backoff *= 2;
				}
				else
				{
					throw e;
				}
			}
		}
		return null; // unreachable
	}

	private void sleep(long ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
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
			ResponseEntity<String> response = executeWithRetry(() ->
				restClient
					.get()
					.uri(finalUri)
					.headers(httpHeaders -> headers.forEach(httpHeaders::add))
					.retrieve()
					.toEntity(String.class)
			);

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
			ResponseEntity<String> response = executeWithRetry(() ->
				restClient
					.get()
					.uri(uri)
					.headers(httpHeaders -> headers.forEach(httpHeaders::add))
					.retrieve()
					.toEntity(String.class)
			);

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
		ResponseEntity<String> response = executeWithRetry(() ->
			restClient
				.post()
				.uri(url)
				.body(body)
				.headers(httpHeaders -> headers.forEach(httpHeaders::add))
				.retrieve()
				.toEntity(String.class)
		);
		logger.debug("Request response: {}", response);
		return response;
	}
}
