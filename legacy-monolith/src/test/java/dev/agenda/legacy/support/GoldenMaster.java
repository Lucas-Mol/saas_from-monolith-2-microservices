package dev.agenda.legacy.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public final class GoldenMaster {

    private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "golden");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

    private GoldenMaster() {
    }

    public static void assertMatches(String name, ResponseEntity<String> response) {
        String actual = toSnapshot(response);
        Path file = GOLDEN_DIR.resolve(name + ".json");
        try {
            if (Boolean.getBoolean("golden.record")) {
                Files.createDirectories(GOLDEN_DIR);
                Files.writeString(file, actual);
                return;
            }
            if (!Files.exists(file)) {
                throw new AssertionError("Missing golden file " + file + ". Record it with -Dgolden.record=true");
            }
            String expected = normalize(Files.readString(file));
            assertThat(actual).as("Golden master mismatch for %s", name).isEqualTo(expected);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String normalize(String json) throws IOException {
        return write(MAPPER.readTree(json));
    }

    private static String write(JsonNode node) throws IOException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node) + "\n";
    }

    private static String toSnapshot(ResponseEntity<String> response) {
        ObjectNode snapshot = MAPPER.createObjectNode();
        snapshot.put("status", response.getStatusCode().value());
        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location != null) {
            snapshot.put("location", location);
        }
        snapshot.set("body", parseBody(response.getBody()));
        try {
            return write(snapshot);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static JsonNode parseBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(body);
        } catch (IOException ex) {
            return TextNode.valueOf(body);
        }
    }
}
