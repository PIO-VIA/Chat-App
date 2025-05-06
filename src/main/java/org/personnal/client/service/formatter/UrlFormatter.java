package org.personnal.client.service.formatter;

public class UrlFormatter implements IMessageFormatterStrategy {
    @Override
    public String format(String content) {
        // Détecte les URLs et les rend cliquables
        return content;
    }

    @Override
    public String getType() {
        return "url";
    }
}
