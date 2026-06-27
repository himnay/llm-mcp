package com.org.ai.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pact consumer contract test: verifies that llm-mcp-client's expectations of
 * the MCP GitHub service's MCP Streamable-HTTP handshake are formally expressed
 * as a contract. The generated pact file can be published to a Pact Broker for
 * provider-side verification.
 * <p>
 * This covers the "initialize" endpoint that the MCP client calls on startup
 * ({@code POST /mcp/v1}) — the minimal contract that must hold for the client
 * to discover the server's capabilities.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "mcp-github-service", pactVersion = PactSpecVersion.V3)
class McpGithubServicePactTest {

    @Pact(consumer = "llm-mcp-client", provider = "mcp-github-service")
    RequestResponsePact mcpInitializePact(PactDslWithProvider builder) {
        return builder
                .given("github MCP server is running")
                .uponReceiving("MCP initialize request")
                .path("/mcp/v1")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2024-11-05",
                            "capabilities": {},
                            "clientInfo": { "name": "llm-mcp-client", "version": "1.0" }
                          }
                        }
                        """)
                .willRespondWith()
                .status(200)
                .headers(java.util.Map.of("Content-Type", "application/json"))
                .body("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "protocolVersion": "2024-11-05",
                            "capabilities": { "tools": {} },
                            "serverInfo": { "name": "mcp-github-service", "version": "1.0" }
                          }
                        }
                        """)
                .toPact();
    }

    @Test
    @DisplayName("MCP server returns 200 with matching protocol version and server info for an initialize request")
    @PactTestFor(pactMethod = "mcpInitializePact")
    void mcpServerRespondsToInitialize(MockServer mockServer) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/mcp/v1"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2024-11-05",
                            "capabilities": {},
                            "clientInfo": { "name": "llm-mcp-client", "version": "1.0" }
                          }
                        }
                        """))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("mcp-github-service");
        assertThat(response.body()).contains("protocolVersion");
    }
}
