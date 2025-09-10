package net.zithium.deluxecoinflip.placeholders;

import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread-safe registry for brace-style placeholders like {KEY}.
 * KEY must match [A-Z0-9_]+ and lookups are case-insensitive (normalized to upper-case).
 * Unknown keys are replaced with an empty string and logged at most once per key.
 */
public class PlaceholderRegistry {

    public interface Provider {
        String provide(Player player);
    }

    private static final Pattern BRACE_TOKEN = Pattern.compile("\\{([A-Z0-9_]+)\\}");
    private static final Pattern PAPI_SELF_TOKEN = Pattern.compile("%deluxecoinflip_([a-z0-9_]+)%", Pattern.CASE_INSENSITIVE);

    private final ConcurrentMap<String, Provider> providers = new ConcurrentHashMap<>();
    private final Set<String> unknownLogged = ConcurrentHashMap.newKeySet();
    private final Logger logger;

    public PlaceholderRegistry(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Register or replace a provider for a KEY. KEY is normalized to upper-case.
     */
    public void register(String key, Provider provider) {
        if (key == null || key.isEmpty() || provider == null) return;
        providers.put(normalize(key), provider);
    }

    /**
     * Unregister a provider if present.
     */
    public void unregister(String key) {
        if (key == null || key.isEmpty()) return;
        providers.remove(normalize(key));
    }

    /**
     * Get a provider for a KEY (case-insensitive), or null if none.
     */
    public Provider get(String key) {
        if (key == null || key.isEmpty()) return null;
        return providers.get(normalize(key));
    }

    /**
     * Applies placeholder replacements to the input, supporting both {KEY} and %deluxecoinflip_key% styles.
     * - Translates %deluxecoinflip_key% to {KEY} first, then performs a single-pass brace replacement.
     * - Unknown keys are replaced with an empty string and logged once per unknown key.
     */
    public String apply(String input, Player player) {
        if (input == null || input.isEmpty()) return "";

        // First, translate our own PAPI-style tokens into brace tokens for unified processing
        String translated = translatePapiTokens(input);

        Matcher m = BRACE_TOKEN.matcher(translated);
        StringBuilder out = new StringBuilder(translated.length());
        int last = 0;
        while (m.find()) {
            out.append(translated, last, m.start());
            String rawKey = m.group(1);
            String key = normalize(rawKey);
            Provider provider = providers.get(key);
            if (provider != null) {
                String val = safeProvide(provider, player);
                out.append(val != null ? val : "");
            } else {
                // Unknown - replace with empty and log once
                out.append("");
                if (unknownLogged.add(key)) {
                    logger.log(Level.FINE, "Unknown placeholder key: {" + key + "}");
                }
            }
            last = m.end();
        }
        if (last < translated.length()) out.append(translated, last, translated.length());
        return out.toString();
    }

    private String translatePapiTokens(String s) {
        Matcher m = PAPI_SELF_TOKEN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1).toUpperCase();
            m.appendReplacement(sb, '{' + key + '}');
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String normalize(String key) {
        return key.toUpperCase();
    }

    private String safeProvide(Provider provider, Player player) {
        try {
            return provider.provide(player);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Placeholder provider threw an exception", t);
            return "";
        }
    }
}
