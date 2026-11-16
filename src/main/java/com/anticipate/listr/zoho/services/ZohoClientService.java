package com.anticipate.listr.zoho_client.services;

/* spring specific modules */
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/* local modules */
import com.anticipate.listr.zoho_client.entities.ZohoAuthEntity;

/* java modules */
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ZohoClientService
{
    private final String chatId;

    private final String cliqHost;

    private final ZohoAuthService zohoAuthService;

    private final RestClient restClient;

    @Autowired
    public ZohoClientService(@Value("${zoho.cliq.chatId}") String chatId,
                             @Value("${zoho.cliq.host}") String cliqHost,
                             ZohoAuthService zohoAuthService)
    {
        this(chatId, cliqHost, zohoAuthService, RestClient.builder());
    }

    // allows tests to bind a mock server to the rest client
    ZohoClientService(String chatId, String cliqHost, ZohoAuthService zohoAuthService,
                      RestClient.Builder restClientBuilder)
    {
        this.chatId = chatId;
        this.cliqHost = cliqHost;
        this.zohoAuthService = zohoAuthService;
        this.restClient = restClientBuilder.build();
    }

    /*  Posts a message
     *
     *  Posts the given text to the configured Cliq chat
     *  via POST /api/v2/chats/{chatId}/message.
     */
    public String postMessage(String text)
    {
        ZohoAuthEntity authEntity = zohoAuthService.getZohoAuthEntity();

        //String uri = cliqHost + "/api/v2/chats/" + chatId + "/message";
        String uri = cliqHost + "/api/v2/channelsbyname/" + channelName + "/message?zapikey=" + zApiKey;


        log.info("Posting message to {}", uri);

        return restClient.post()
                .uri(uri)
                //.header(authEntity.getAuthHeaderName(), authEntity.getAuthHeaderValue())
                //.header(authEntity.getOrgIdHeaderName(), authEntity.getOrgIdHeaderValue())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text))
                .retrieve()
                .body(String.class);
    }
}
