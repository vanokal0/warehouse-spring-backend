package com.vanokal0.warehouse_backend.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vanokal0.warehouse_backend.dto.geocoding.GeocodeLocation;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String geocodeUrl;

    private final ConcurrentHashMap<String, GeocodeLocation> cache = new ConcurrentHashMap<>();

    public GeocodingService(
            RestTemplate restTemplate,
            @Value("${google.maps.api.key}") String apiKey,
            @Value("${google.maps.geocode.url}") String geocodeUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.geocodeUrl = geocodeUrl;
    }

    public GeocodeLocation getGeocodingAPIResponse(String address) {
        if (cache.containsKey(address)) {
            return cache.get(address);
        }

        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = geocodeUrl + "?address=" + encodedAddress + "&key=" + apiKey;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());

            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (jsonObject.get("status").getAsString().equals("OK")) {
                JsonObject jsonLocation = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject()
                        .getAsJsonObject("geometry").getAsJsonObject("location");

                GeocodeLocation location = new GeocodeLocation(
                        jsonLocation.get("lat").getAsDouble(),
                        jsonLocation.get("lng").getAsDouble()
                );
                cache.put(address, location);
                return location;
            } else {
                System.out.println("Error: " + jsonObject.get("status").getAsString());
            }
        } catch (IOException exception) {
            System.out.println("Could not establish HTTP client.");
        }

        return null;
    }
}
