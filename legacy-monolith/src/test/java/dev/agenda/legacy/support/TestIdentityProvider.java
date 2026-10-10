package dev.agenda.legacy.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;

public final class TestIdentityProvider {

    private static final WireMockServer SERVER = start();

    private TestIdentityProvider() {
    }

    public static String jwkSetUri() {
        return SERVER.baseUrl() + "/certs";
    }

    private static WireMockServer start() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        server.stubFor(get("/certs").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(TestJwts.jwks())));
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        return server;
    }
}
