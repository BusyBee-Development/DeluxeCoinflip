/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class DiscordIntegration {

    private final List<EmbedObject> embeds = new ArrayList<>();
    private String token;
    private String channel;
    private String url;
    private String content;
    private String username;
    private String avatarUrl;
    private boolean tts;
    private boolean suppressMentions;
    private boolean debug = false;

    public DiscordIntegration(String url) {
        this.url = url;
    }

    public DiscordIntegration(String token, String channel) {
        this.token = token;
        this.channel = channel;
    }

    public DiscordIntegration setContent(String content) {
        this.content = content;
        return this;
    }

    public DiscordIntegration setSuppressMentions(boolean suppressed) {
        this.suppressMentions = suppressed;
        return this;
    }

    public DiscordIntegration debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public DiscordIntegration setUsername(String username) {
        this.username = username;
        return this;
    }

    public DiscordIntegration setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public DiscordIntegration setTts(boolean tts) {
        this.tts = tts;
        return this;
    }

    public DiscordIntegration addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
        return this;
    }

    public void execute() throws IOException {
        if (this.content == null && this.embeds.isEmpty()) {
            throw new IllegalArgumentException("Set content or add at least one EmbedObject");
        }

        JSONObject json = new JSONObject();

        if (this.suppressMentions) {
            JSONObject allowedMentions = new JSONObject();

            allowedMentions.put("parse", new String[]{});

            json.put("allowed_mentions", allowedMentions);
        }

        json.put("content", this.content.replaceAll("@", debug ? "@ " : "@"));
        json.put("username", this.username);
        json.put("avatar_url", this.avatarUrl);
        json.put("tts", this.tts);

        if (!this.embeds.isEmpty()) {
            json.put("embeds", generateEmbeds().toArray());
        }

        URL url = new URL(this.url == null ? "https://discord.com/api/v10/channels/" + channel + "/messages" : this.url);

        if (debug) {
            System.out.println("sending request to " + url);
            System.out.println("request:");
            System.out.println(json);
        }

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.addRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        if (this.url == null)
            connection.addRequestProperty("Authorization", "Bot " + this.token);

        connection.setRequestMethod("POST");

        OutputStream stream = connection.getOutputStream();
        stream.write(json.toString().getBytes());
        stream.flush();
        stream.close();

        connection.getInputStream().close(); // I'm not sure why, but it doesn't work without getting the InputStream
        connection.disconnect();
    }

    private List<JSONObject> generateEmbeds() {
        List<JSONObject> embedObjects = new ArrayList<>();

        for (EmbedObject embed : this.embeds) {
            JSONObject jsonEmbed = new JSONObject();

            jsonEmbed.put("title", embed.getTitle());
            jsonEmbed.put("description", embed.getDescription());
            jsonEmbed.put("url", embed.getUrl());

            if (embed.getColor() != null) {
                Color color = embed.getColor();
                int rgb = color.getRed();
                rgb = (rgb << 8) + color.getGreen();
                rgb = (rgb << 8) + color.getBlue();

                jsonEmbed.put("color", rgb);
            }

            EmbedObject.Footer footer = embed.getFooter();
            EmbedObject.Image image = embed.getImage();
            EmbedObject.Thumbnail thumbnail = embed.getThumbnail();
            EmbedObject.Author author = embed.getAuthor();
            List<EmbedObject.Field> fields = embed.getFields();

            if (footer != null) {
                JSONObject jsonFooter = new JSONObject();

                jsonFooter.put("text", footer.text());
                jsonFooter.put("icon_url", footer.iconUrl());
                jsonEmbed.put("footer", jsonFooter);
            }

            if (embed.timestamp) {
                TimeZone tz = TimeZone.getTimeZone("UTC");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(tz);
                String nowAsISO = df.format(new Date());

                jsonEmbed.put("timestamp", nowAsISO);
            }

            if (image != null) {
                JSONObject jsonImage = new JSONObject();

                jsonImage.put("url", image.url());
                jsonEmbed.put("image", jsonImage);
            }

            if (thumbnail != null) {
                JSONObject jsonThumbnail = new JSONObject();

                jsonThumbnail.put("url", thumbnail.url());
                jsonEmbed.put("thumbnail", jsonThumbnail);
            }

            if (author != null) {
                JSONObject jsonAuthor = new JSONObject();

                jsonAuthor.put("name", author.name());
                jsonAuthor.put("url", author.url());
                jsonAuthor.put("icon_url", author.iconUrl());
                jsonEmbed.put("author", jsonAuthor);
            }

            List<JSONObject> jsonFields = new ArrayList<>();
            for (EmbedObject.Field field : fields) {
                JSONObject jsonField = new JSONObject();

                jsonField.put("name", field.name());
                jsonField.put("value", field.value());
                jsonField.put("inline", field.inline());

                jsonFields.add(jsonField);
            }

            jsonEmbed.put("fields", jsonFields.toArray());
            embedObjects.add(jsonEmbed);
        }

        return embedObjects;
    }

    public static class EmbedObject {
        private final List<Field> fields = new ArrayList<>();
        private String title;
        private String description;
        private String url;
        private boolean timestamp;
        private Color color;
        private Footer footer;
        private Thumbnail thumbnail;
        private Image image;
        private Author author;

        public String getTitle() {
            return title;
        }

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public Color getColor() {
            return color;
        }

        public EmbedObject setColor(Color color) {
            this.color = color;
            return this;
        }

        public boolean getTimestamp() {
            return timestamp;
        }

        public EmbedObject setTimestamp(boolean status) {
            this.timestamp = status;
            return this;
        }

        public Footer getFooter() {
            return footer;
        }

        public Thumbnail getThumbnail() {
            return thumbnail;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public Image getImage() {
            return image;
        }

        public EmbedObject setImage(String url) {
            this.image = new Image(url);
            return this;
        }

        public Author getAuthor() {
            return author;
        }

        public List<Field> getFields() {
            return fields;
        }

        public EmbedObject setFooter(String text, String icon) {
            this.footer = new Footer(text, icon);
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String icon) {
            this.author = new Author(name, url, icon);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        private record Footer(String text, String iconUrl) {
        }

        private record Thumbnail(String url) {
        }

        private record Image(String url) {
        }

        private record Author(String name, String url, String iconUrl) {
        }

        private record Field(String name, String value, boolean inline) {
        }
    }

    public static class WebhookExecutionException extends RuntimeException {
        public WebhookExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class JSONObject {

        private final HashMap<String, Object> map = new HashMap<>();

        void put(String key, Object value) {
            if (value != null) {
                map.put(key, value);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            Set<Map.Entry<String, Object>> entrySet = map.entrySet();
            builder.append("{");

            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                Object val = entry.getValue();
                builder.append(quote(entry.getKey())).append(":");

                if (val instanceof String) {
                    builder.append(quote(String.valueOf(val)));
                } else if (val instanceof Integer) {
                    builder.append(Integer.valueOf(String.valueOf(val)));
                } else if (val instanceof Boolean) {
                    builder.append(val);
                } else if (val instanceof JSONObject) {
                    builder.append(val);
                } else if (val.getClass().isArray()) {
                    builder.append("[");
                    int len = Array.getLength(val);
                    for (int j = 0; j < len; j++) {
                        builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
                    }
                    builder.append("]");
                }

                builder.append(++i == entrySet.size() ? "}" : ",");
            }

            return builder.toString();
        }

        private String quote(String string) {
            return "\"" + string + "\"";
        }
    }
}
