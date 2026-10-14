package com.anticipate.listr.zoho_client.services;

/* spring specific modules */
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/* local modules */
import com.anticipate.listr.zoho_client.entities.ZohoAuthEntity;
/*import com.anticipate.listr.zoho_client.entities.ZohoTicket;
import com.anticipate.listr.zoho_client.entities.ZohoTicketListResponse;
import com.anticipate.listr.zoho_client.entities.TicketCountList;
import com.anticipate.listr.zoho_client.entities.TicketCount;
import com.anticipate.listr.zoho_client.utilities.AgentLoadBalancerUtility;*/

/* java modules */
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ZohoAuthService 
{
    private final RestClient restClient;

    private final String preTokenBody;

    private final String tokenHost;

    ZohoAuthEntity zohoAuthEntity;

    @Autowired
    public ZohoAuthService(
            @Value("${zoho.client.id}") String clientId,
            @Value("${zoho.client.secret}") String clientSecret,
            @Value("${zoho.client.cliqOrgId}") String cliqOrgId,
            @Value("${zoho.client.host}") String tokenHost)
    {
        this(clientId, clientSecret, cliqOrgId, tokenHost, RestClient.builder());
    }

    // allows tests to bind a mock server to the rest client
    ZohoAuthService(String clientId, String clientSecret, String cliqOrgId,
                    String tokenHost, RestClient.Builder restClientBuilder)
    {
        this.restClient = restClientBuilder.build();

        this.preTokenBody = "grant_type=client_credentials&client_id=" + clientId +
                            "&client_secret=" + clientSecret +
                            "&scope=ZohoCliq.Messages.READ,ZohoCliq.Webhooks.CREATE" +
                            "&soid=ZohoCliq." + cliqOrgId;

        this.tokenHost = tokenHost;
        this.zohoAuthEntity = new ZohoAuthEntity();

        log.info("Initialising, requesting access token from {}", tokenHost);

        getZohoAccessToken();
        this.zohoAuthEntity.setOrgId(cliqOrgId);

        log.info("Resulting access token: {}", printZohoAccessToken());
    }

    /*
    *   Gets an access token
    *
    *   Uses Zoho Client Service to fetch an access token
    *   from Zoho Desk. The token returned from this
    *   endpoint is only scoped to the Zoho Desk API.
    */
    public void getZohoAccessToken() 
    {

        ZohoAuthEntity tokenResponse =  
                
                restClient.post()
                .uri(tokenHost + "/oauth/v2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(preTokenBody)
                .retrieve()
                .body(ZohoAuthEntity.class);

        if (tokenResponse == null || tokenResponse.getAccessToken() == null)
        {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No token response from Zoho");
        }

        log.info("[ZohoAuthService] Access token obtained, expires in {}s", tokenResponse.getExpiresIn());

        this.zohoAuthEntity.setAccessToken(tokenResponse.getAccessToken());
        this.zohoAuthEntity.setExpiryTime(tokenResponse.getExpiresIn());
    }

    /*  Gets this services Auth entity
     *
     *  This function returns the auth entity of this module.
     *  This function is responsible for wrapping refreshing.
     */
    public ZohoAuthEntity getZohoAuthEntity()
    {
        ensureValidAccessToken();
        
        return this.zohoAuthEntity;
    }

    /*
    *   Ensures the access token is valid
    *
    *   Refreshes the access token if it has expired before
    *   it is used to make a request against the Zoho Desk API.
    */
    private void ensureValidAccessToken() {
        if (this.zohoAuthEntity.isExpired()) {
            getZohoAccessToken();
        }
    }

    /*
    *   Fetches the access token as string
    *
    *   Returns the retrieved access token in string format.
    */
    public String printZohoAccessToken() {
        return this.zohoAuthEntity.getAuthHeaderName() + ": " + this.zohoAuthEntity.getAuthHeaderValue() + "\n" +
                this.zohoAuthEntity.getOrgIdHeaderName() + ": " + this.zohoAuthEntity.getOrgIdHeaderValue();
    }
}