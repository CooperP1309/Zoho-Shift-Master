package com.anticipate.listr.zoho_client.services;

import com.anticipate.listr.zoho_client.entities.ZohoAuthEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ZohoClientServiceTest
{
    private static final String CLIQ_HOST = "https://cliq.zoho.test";
    private static final String CHAT_ID = "CT_1234567890123456789_123456789";
    private static final String CHANNEL_NAME = "testchannel";
    private static final String ZAPI_KEY = "test-zapikey";
    private static final String MESSAGE_URI = CLIQ_HOST + "/api/v2/channelsbyname/" + CHANNEL_NAME
            + "/message?zapikey=" + ZAPI_KEY;

    @Mock
    private ZohoAuthService zohoAuthService;

    private MockRestServiceServer server;

    private ZohoClientService zohoClientService;

    @BeforeEach
    void setUp()
    {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();

        zohoClientService = new ZohoClientService(CHAT_ID, CLIQ_HOST, CHANNEL_NAME, ZAPI_KEY,
                zohoAuthService, restClientBuilder);

        ZohoAuthEntity authEntity = new ZohoAuthEntity();
        authEntity.setAccessToken("test-token");
        authEntity.setOrgId("12345");
        authEntity.setExpiryTime(3600);

        when(zohoAuthService.getZohoAuthEntity()).thenReturn(authEntity);
    }

    @Test
    void postMessage_postsTextToConfiguredChannelWithZapiKey()
    {
        server.expect(requestTo(MESSAGE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"text\":\"hello\"}"))
                .andRespond(withNoContent());

        zohoClientService.postMessage("hello");

        server.verify();
    }

    @Test
    void postMessage_returnsZohoResponseBody()
    {
        server.expect(requestTo(MESSAGE_URI))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertEquals("{\"status\":\"ok\"}", zohoClientService.postMessage("hello"));
    }

    @Test
    void postMessage_fetchesAuthEntityForEachRequest()
    {
        // fetching per request lets ZohoAuthService refresh an expired token
        server.expect(ExpectedCount.times(2), requestTo(MESSAGE_URI))
                .andRespond(withNoContent());

        zohoClientService.postMessage("first");
        zohoClientService.postMessage("second");

        verify(zohoAuthService, times(2)).getZohoAuthEntity();
        server.verify();
    }

    @Test
    void postMessage_throwsWhenZohoRejectsRequest()
    {
        server.expect(requestTo(MESSAGE_URI))
                .andRespond(withBadRequest());

        assertThrows(HttpClientErrorException.class, () -> zohoClientService.postMessage("hello"));
    }
}
