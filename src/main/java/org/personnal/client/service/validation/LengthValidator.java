package org.personnal.client.service.validation;

public class LengthValidator implements IMessageValidationStrategy{
    private final int minLength;
    private final int maxLength;

    public LengthValidator(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public boolean isValid(String content) {
        return content.length() >= minLength && content.length() <= maxLength;
    }

    @Override
    public String getErrorMessage() {
        return "Le message doit contenir entre " + minLength + " et " + maxLength + " caractères.";
    }
}
