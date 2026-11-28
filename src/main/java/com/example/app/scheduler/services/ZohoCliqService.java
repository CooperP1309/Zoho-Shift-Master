package com.anticipate.listr.zoho_client.services;

/* spring specific modules */
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/* java modules */
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ZohoClientService
{
    private final String chatId;

    private final String channelName;

    private final String cliqHost;

    private final String zApiKey;

    private final RestClient restClient;

    @Autowired
    public ZohoClientService(@Value("${zoho.cliq.chatId}") String chatId,
                             @Value("${zoho.cliq.host}") String cliqHost,
                             @Value("${zoho.cliq.channelName}") String channelName,
                             @Value("${zoho.cliq.zApiKey}") String zApiKey)
    {
        this(chatId, cliqHost, channelName, zApiKey, RestClient.builder());
    }

    // allows tests to bind a mock server to the rest client
    ZohoClientService(String chatId, String cliqHost, String channelName, 
                        String zApiKey, RestClient.Builder restClientBuilder)
    {
        this.chatId = chatId;
        this.cliqHost = cliqHost;
        this.channelName = channelName;
        this.zApiKey = zApiKey;
        this.restClient = restClientBuilder.build();
    }

    /*  Posts a message
     *
     *  Posts the given text to the configured Cliq chat
     *  via POST /api/v2/chats/{chatId}/message.
     */
    public String postMessage(String text)
    {
        String uri = cliqHost + "/api/v2/channelsbyname/" + channelName + "/message?zapikey=" + zApiKey;

        log.info("Posting message to {}", uri);

        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text))
                .retrieve()
                .body(String.class);
    }
}