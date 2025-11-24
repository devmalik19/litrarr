/**
 *  This class is to make HTTP requests (GET, POST) using spring boot RESTClient library.
 */
package devmalik19.litrarr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class HttpRequestService
{
	Logger logger = LoggerFactory.getLogger(HttpRequestService.class);

	/**
	 *  This method is used to send a GET request with headers.
	 *
	 * @param url  Url in string format.
	 * @param headers A hashmap of headers with key and value as header key and value.
	 * @return It will return in same class type object that you pass in cls param.
	 *
	 */
	public String doGetRequest(String url, Map<String, String> headers)
	{
		return doGetRequest(url, headers, new HashMap<>());
	}

	/**
	 *  This method is used to send a GET request without any headers or query string.
	 *
	 * @param url  Url in string format.
	 * @return It will return in same class type object that you pass in cls param.
	 *
	 */
	public String doGetRequest(String url)
	{
		return doGetRequest(url, new HashMap<>(), new HashMap<>());
	}

	/**
	 *  This method is used to send a GET request with headers and query string.
	 *
	 * @param url  Url in string format.
	 * @param headers A hashmap of headers with key and value as header key and value.
	 * @param params Query params as key and value of hashmap, we will build the query like this &key=value
	 * @return It will return in same class type object that you pass in cls param.
	 *
	 */
    public String doGetRequest(String url,
							  Map<String, String> headers,
							  Map<String, String> params)
    {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
		params.forEach(uriBuilder::queryParam);
		URI finalUri = uriBuilder.build().toUri();

        RestClient restClient = RestClient.create();
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
			if(status.is2xxSuccessful())
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
	 *  This method is used to send a GET request with headers and query string.
	 *
	 * @param headers A hashmap of headers with key and value as header key and value.
	 * @param uri A uri object build using UriComponentsBuilder
	 * @return It will return in same class type object that you pass in cls param.
	 *
	 */
	public String doGetRequest(URI uri,
							   Map<String, String> headers)
	{
		RestClient restClient = RestClient.create();
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
			if(status.is2xxSuccessful())
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
	 * @param url Url in string format.
	 * @param body Body of the request.
	 * @param headers A hashmap of headers with key and value as header key and value.
	 * @return It will return body of the response.
	 *
	 */
    public String doPostRequest(String url, String body, Map<String, String> headers)
    {
		ResponseEntity<String> response =  doPostRequestRaw(url, body, headers);
		HttpStatusCode status = response.getStatusCode();
		if(status.is2xxSuccessful())
			return response.getBody();
		else
			logger.error("Request failed with status code: {}", status.value());
		return "";
    }

	/**
	 * This method is used to send a POST request.
	 *
	 * @param url  Url in string format.
	 * @param body  Body of the request.
	 * @param headers A hashmap of headers with key and value as header key and value.
	 * @return It will return ResponseEntity.
	 */
	public ResponseEntity<String> doPostRequestRaw(String url, String body, Map<String, String> headers)
	{
		RestClient restClient = RestClient.create();
		ResponseEntity<String> response =  restClient
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
