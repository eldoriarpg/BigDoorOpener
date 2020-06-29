package de.eldoria.bigdoorsopener.localization;

import org.bukkit.entity.Player;

public final class Replacement {
    private final String key;
    private String value;
    private boolean caseInsensitive;

    private Replacement(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static Replacement create(String key, String value) {
        return new Replacement("%" + key + "%", value);
    }

    public static Replacement create(String key, Object value) {
        return new Replacement("%" + key + "%", value.toString());
    }

    public static Replacement create(String key, Player player) {
        return new Replacement("%" + key + "%", player.getName());
    }

    /**
     * Add formatting codes to the replacement. A §r will be appended after the replacement.
     * Only provide the formatting character. Without § or &.
     *
     * @param format      format which should be applied on the replacement.
     * @param afterFormat The formatting codes which should be applied after the §r.
     * @return replacement with formatting set
     */
    public Replacement addFormatting(char[] format, char... afterFormat) {
        StringBuilder builder = new StringBuilder();
        for (char aChar : format) {
            builder.append("§").append(aChar);
        }
        builder.append(value).append("§r");
        for (char aChar : afterFormat) {
            builder.append("§").append(aChar);
        }
        value = builder.toString();
        return this;
    }

    /**
     * Add formatting codes to the replacement. A §r will be appended after the replacement.
     * Only provide the formatting character. Without § or &.
     *
     * @param format format which should be applied on the replacement.
     * @return replacement with formatting set
     */
    public Replacement addFormatting(char... format) {
        return addFormatting(format, new char[0]);
    }

    public Replacement ignoreKeyCase(){
        this.caseInsensitive = true;
        return this;
    }


    /**
     * Invoke the replacement on the string.
     *
     * @param string string to replace
     * @return string with key replaced by value.
     */
    public String invoke(String string) {
        if (caseInsensitive) {
            return string.replaceAll("(?i)" + key, value);
        }
        return string.replace(key, value);
    }
}
