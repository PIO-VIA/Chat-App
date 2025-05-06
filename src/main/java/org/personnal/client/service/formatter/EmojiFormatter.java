package org.personnal.client.service.formatter;

public class EmojiFormatter implements IMessageFormatterStrategy {
    @Override
    public String format(String content) {
        // Convertir les codes emoji en emoji réels
        // Par exemple, :smile: en 😊
        return content
                .replace(":smile:", "😊")
                .replace(":heart:", "❤️")
                .replace(":thumbsup:", "👍");
    }

    @Override
    public String getType() {
        return "emoji";
    }
}
