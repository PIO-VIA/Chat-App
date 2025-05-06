package org.personnal.client.service.validation;

import java.util.HashSet;
import java.util.Set;

public class ProfanityValidator implements IMessageValidationStrategy{
    private final Set<String> bannedWords;

    public ProfanityValidator() {
        this.bannedWords = new HashSet<>();
        // Ajouter les mots interdits
        this.bannedWords.add("badword1");
        this.bannedWords.add("badword2");
    }

    @Override
    public boolean isValid(String content) {
        String lowerContent = content.toLowerCase();
        for (String word : bannedWords) {
            if (lowerContent.contains(word.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getErrorMessage() {
        return "Le message contient des mots interdits.";
    }
}
