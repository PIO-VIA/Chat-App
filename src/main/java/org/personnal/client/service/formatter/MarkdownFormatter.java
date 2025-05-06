package org.personnal.client.service.formatter;

public class MarkdownFormatter implements IMessageFormatterStrategy {
    @Override
    public String format(String content) {
        // Simple implémentation pour l'exemple
        // Convertir du markdown en HTML ou autre format
        return content;
    }

    @Override
    public String getType() {
        return "markdown";
    }
}
