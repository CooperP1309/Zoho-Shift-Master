package com.anticipate.listr.zoho_client.services;

import com.anticipate.listr.zoho_client.entities.ZohoAuthEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ZohoAuthServiceTest
{
    private static final String TOKEN_HOST = "https://accounts.zoho.test";
    private static final String TOKEN_URI = TOKEN_HOST + "/oauth/v2/token";

    private RestClient.Builder restClientBuilder;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp()
    {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    private ZohoAuthService createService()
    {
        return new ZohoAuthService("client-id", "client-secret", "12345", TOKEN_HOST, restClientBuilder);
    }

    private static String tokenResponse(String accessToken, int expiresIn)
    {
        return "{\"access_token\":\"" + accessToken + "\",\"expires_in\":" + expiresIn + "}";
    }

    @Test
    void constructor_requestsTokenWithClientCredentials()
    {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andExpect(content().string(containsString("client_id=client-id")))
                .andExpect(content().string(containsString("client_secret=client-secret")))
                .andExpect(content().string(containsString("scope=ZohoCliq.Messages.READ,ZohoCliq.Webhooks.CREATE,ZohoCliq.Users.READ")))
                .andExpect(content().string(containsString("soid=ZohoCliq.12345")))
                .andRespond(withSuccess(tokenResponse("first-token", 3600), MediaType.APPLICATION_JSON));

        createService();

        server.verify();
    }

    @Test
    void getZohoAuthEntity_returnsTokenAndOrgIdHeaders()
    {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponse("first-token", 3600), MediaType.APPLICATION_JSON));

        ZohoAuthEntity authEntity = createService().getZohoAuthEntity();

        assertEquals("Authorization", authEntity.getAuthHeaderName());
        assertEquals("Zoho-oauthtoken first-token", authEntity.getAuthHeaderValue());
        assertEquals("orgId", authEntity.getOrgIdHeaderName());
        assertEquals("12345", authEntity.getOrgIdHeaderValue());
    }

    @Test
    void getZohoAuthEntity_reusesTokenWhileValid()
    {
        // only one token request is expected; a second would fail the test
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponse("first-token", 3600), MediaType.APPLICATION_JSON));

        ZohoAuthService service = createService();

        ZohoAuthEntity first = service.getZohoAuthEntity();
        ZohoAuthEntity second = service.getZohoAuthEntity();

        assertSame(first, second);
        assertEquals("Zoho-oauthtoken first-token", second.getAuthHeaderValue());
        server.verify();
    }

    @Test
    void getZohoAuthEntity_refreshesExpiredToken()
    {
        // expires_in of 0 makes the first token expire immediately
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponse("expired-token", 0), MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponse("refreshed-token", 3600), MediaType.APPLICATION_JSON));

        ZohoAuthEntity authEntity = createService().getZohoAuthEntity();

        assertEquals("Zoho-oauthtoken refreshed-token", authEntity.getAuthHeaderValue());
        assertEquals("12345", authEntity.getOrgIdHeaderValue());
        server.verify();
    }

    @Test
    void constructor_throwsWhenZohoReturnsNoAccessToken()
    {
        // Zoho rejects bad credentials with HTTP 200 and an "error" field
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"error\":\"invalid_client\"}", MediaType.APPLICATION_JSON));

        assertThrows(ResponseStatusException.class, this::createService);
    }
}
