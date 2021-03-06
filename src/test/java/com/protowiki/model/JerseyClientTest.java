package com.protowiki.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Nick
 */
@Ignore
public class JerseyClientTest {

    private static final String TRAGET_URL = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=Potato";

    public static Logger logger = LoggerFactory.getLogger(JerseyClientTest.class);

    /**
     * Test HttpUrlConnection
     */
    @Test
    public void testHtppUrlConnection() {

        try {
            URL con = new URL(TRAGET_URL);
            HttpURLConnection request = (HttpURLConnection) con.openConnection();
            request.setRequestMethod("GET");

            int status = request.getResponseCode();
            logger.info("Status: {}", status);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                logger.info("Response: {}", sb.toString());
            }

            JsonObject jsonObject = jsonObject = new JsonParser().parse(sb.toString()).getAsJsonObject();
            JsonObject jo = jsonObject.getAsJsonObject("query").getAsJsonObject("pages");

            JsonObject v = null;
            for (Entry<String, JsonElement> elem : jo.entrySet()) {
                v = (JsonObject) elem.getValue();
            }
            String extract = v.getAsJsonPrimitive("extract").toString();
            logger.info("Extract: {}", extract);

        } catch (Throwable ex) {
            logger.error("Exception", ex);
        }
    }

    /**
     * Test jersey client
     */
    @Test
    public void testClientGET() {

        Configuration clientConfiguration = new ClientConfig().register(LoggingFilter.class);

        Client client = ClientBuilder.newClient(clientConfiguration);

        WebTarget target = client.target(TRAGET_URL);

        Invocation.Builder invocationBuilder = target.request(MediaType.TEXT_HTML);
        Response response = invocationBuilder.get();

        logger.info("Status: {}", response.getStatus());
    }
}
