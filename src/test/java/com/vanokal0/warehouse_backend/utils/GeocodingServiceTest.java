package com.vanokal0.warehouse_backend.utils;

import com.sun.net.httpserver.HttpServer;
import com.vanokal0.warehouse_backend.dto.geocoding.GeocodeLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingServiceTest {

    private static final String OK_BODY = """
            {"status":"OK","results":[{"geometry":{"location":{"lat":50.4501,"lng":30.5234}}}]}
            """;

    private HttpServer server;
    private final List<String> requestedQueries = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> responseBody = new AtomicReference<>(OK_BODY);
    private GeocodingService service;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/geocode", exchange -> {
            requestedQueries.add(exchange.getRequestURI().getRawQuery());
            byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/geocode";
        service = new GeocodingService(new RestTemplate(), "test-key", url);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsLocation_andSendsEncodedAddressAndKey() {
        GeocodeLocation location = service.getGeocodingAPIResponse("Khreshchatyk St 1, Kyiv");

        assertThat(location).isEqualTo(new GeocodeLocation(50.4501, 30.5234));
        assertThat(requestedQueries).containsExactly("address=Khreshchatyk+St+1%2C+Kyiv&key=test-key");
    }

    @Test
    void cachesSuccessfulLookups() {
        GeocodeLocation first = service.getGeocodingAPIResponse("Addr");
        GeocodeLocation second = service.getGeocodingAPIResponse("Addr");

        assertThat(second).isSameAs(first);
        assertThat(requestedQueries).hasSize(1);
    }

    @Test
    void cachesPerAddress() {
        service.getGeocodingAPIResponse("A");
        service.getGeocodingAPIResponse("B");

        assertThat(requestedQueries).hasSize(2);
    }

    @Test
    void returnsNull_andDoesNotCache_whenStatusIsNotOk() {
        responseBody.set("{\"status\":\"ZERO_RESULTS\",\"results\":[]}");

        assertThat(service.getGeocodingAPIResponse("Nowhere")).isNull();
        assertThat(service.getGeocodingAPIResponse("Nowhere")).isNull();
        assertThat(requestedQueries).hasSize(2);
    }

    @Test
    void returnsNull_whenServiceIsUnreachable() {
        int port = server.getAddress().getPort();
        server.stop(0);
        GeocodingService offline = new GeocodingService(new RestTemplate(), "k", "http://127.0.0.1:" + port + "/geocode");

        assertThat(offline.getGeocodingAPIResponse("Addr")).isNull();
    }
}
