package io.github.chipitaps.booking.support;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

public final class Api {

	private final HttpClient client = HttpClient.newHttpClient();

	private final String base;

	public Api(int port) {
		this.base = "http://localhost:" + port;
	}

	public Response get(String path, String token) {
		return send(request(path, token).GET());
	}

	public Response post(String path, String token, String json) {
		return send(request(path, token).header("content-type", "application/json")
			.POST(json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json)));
	}

	public Response put(String path, String token, String json) {
		return send(request(path, token).header("content-type", "application/json").PUT(BodyPublishers.ofString(json)));
	}

	public Response delete(String path, String token) {
		return send(request(path, token).DELETE());
	}

	private HttpRequest.Builder request(String path, String token) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).header("accept", "application/json");
		return token == null ? builder : builder.header("authorization", "Bearer " + token);
	}

	private Response send(HttpRequest.Builder builder) {
		try {
			HttpResponse<String> response = client.send(builder.build(), BodyHandlers.ofString());
			return new Response(response.statusCode(), response.body(), response.headers().firstValue("location"));
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	public record Response(int status, String body, Optional<String> location) {

		public <T> T json(String path) {
			return JsonPath.read(body, path);
		}

	}

}
