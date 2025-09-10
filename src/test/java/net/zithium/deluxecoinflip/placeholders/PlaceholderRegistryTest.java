package net.zithium.deluxecoinflip.placeholders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class PlaceholderRegistryTest {

    private PlaceholderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PlaceholderRegistry(Logger.getLogger("test"));
        registry.register("FOO", p -> "bar");
        registry.register("HELLO_WORLD", p -> "hi");
    }

    @Test
    void basicReplacement() {
        String in = "Value: {FOO}";
        String out = registry.apply(in, null);
        assertEquals("Value: bar", out);
    }

    @Test
    void multipleTokensSinglePass() {
        String in = "{FOO}-{HELLO_WORLD}-{FOO}";
        String out = registry.apply(in, null);
        assertEquals("bar-hi-bar", out);
    }

    @Test
    void unknownKeysBecomeEmpty() {
        String in = "X{UNKNOWN}Y";
        String out = registry.apply(in, null);
        assertEquals("XY", out);
    }

    @Test
    void caseInsensitivity() {
        String in = "{foo} and {hello_world}";
        String out = registry.apply(in, null);
        assertEquals("bar and hi", out);
    }

    @Test
    void noTokensReturnsOriginal() {
        String in = "no tokens here";
        String out = registry.apply(in, null);
        assertEquals(in, out);
    }

    @Test
    void papiStyleTranslation() {
        String in = "%deluxecoinflip_foo% vs %DeluxeCoinflip_HELLO_world%";
        String out = registry.apply(in, null);
        assertEquals("bar vs hi", out);
    }
}
