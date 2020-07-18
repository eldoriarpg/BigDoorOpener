package de.eldoria.bigdoorsopener.util;

import de.eldoria.bigdoorsopener.BigDoorsOpener;
import de.eldoria.bigdoorsopener.doors.conditions.ConditionType;
import de.eldoria.eldoutilities.container.Pair;
import de.eldoria.eldoutilities.utils.TextUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import javax.script.ScriptException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class JsSyntaxHelper {

    private static final Pattern VARIABLE = Pattern.compile("[a-zA-Z]+");
    private static final Pattern ALLOWED_OPERATORS = Pattern.compile("&&|\\|\\||!=|==|!");
    private static final Pattern UNALLOWED_OPERATORS = Pattern.compile("&|\\||=");
    private static final Pattern SYNTAX = Pattern.compile("(\\||&|!|=|\\(|\\)|\\s|\\{|}|;)*");
    private static final String PLACEHOLDER = Arrays.stream(ConditionType.ConditionGroup.values())
            .map(ConditionType.ConditionGroup::name)
            .collect(Collectors.joining("|"));


    private JsSyntaxHelper() {
    }

    /**
     * Translates the alternative easier syntax to the true javascript syntax.
     *
     * @param evaluator evaluator to translate
     * @return the translated evaluator string.
     */
    public static String translateEvaluator(String evaluator) {
        String result = evaluator;

        // replace words
        result = result.replaceAll("(?i)\\sand\\s", "&&");
        result = result.replaceAll("(?i)\\sor\\s", "||");
        result = result.replaceAll("(?i)\\sis\\snot\\s", "!=");
        result = result.replaceAll("(?i)\\sis\\s", "==");
        result = result.replaceAll("(?i)\\snot\\s", "!=");

        // remove whitespaces.
        result = result.replaceAll("\\s", "");
        return result;
    }

    /**
     * This method validates a java script string.
     * Will call {@link #translateEvaluator(String)} first and check the returned string.
     *
     * @param evaluator string to evaluate
     * @return pair which returns the result and a optinal string which contains different valued based on the result
     */
    public static Pair<ValidatorResult, String> validateEvaluator(String evaluator, CachingJSEngine engine) {
        evaluator = translateEvaluator(evaluator);

        // check for unbalanced parenthesis
        if (TextUtil.countChars(evaluator, '(') != TextUtil.countChars(evaluator, ')')) {
            return new Pair<>(ValidatorResult.UNBALANCED_PARENTHESIS, "");
        }


        String cleaned = evaluator.replaceAll("(?i)if|true|false|" + PLACEHOLDER + "|currentState|null|else", "");
        Matcher matcher = VARIABLE.matcher(cleaned);
        if (matcher.find()) {
            return new Pair<>(ValidatorResult.INVALID_VARIABLE, matcher.group());
        }

        cleaned = cleaned.replaceAll(ALLOWED_OPERATORS.pattern(), "");
        matcher = UNALLOWED_OPERATORS.matcher(cleaned);
        if (matcher.find()) {
            return new Pair<>(ValidatorResult.INVALID_OPERATOR, matcher.group());
        }

        // remove operators to see if some operators are left.
        cleaned = cleaned.replaceAll(SYNTAX.pattern(), "");
        if (!cleaned.replaceAll(SYNTAX.pattern(), "").isEmpty()) {
            return new Pair<>(ValidatorResult.INVALID_SYNTAX, cleaned.replaceAll(SYNTAX.pattern(), ""));
        }

        return checkExecution(evaluator, engine, null, true);
    }

    /**
     * Check if a js can be executed.
     *
     * @param evaluator string to evaluate
     * @param engine    engine to use
     * @param player    player to check for. can be null
     * @param vanilla   set to true to disable third party replacements
     * @return a pair which indicates if the execution was successful or the fail reason
     */
    public static Pair<ValidatorResult, String> checkExecution(String evaluator, CachingJSEngine engine, Player player, boolean vanilla) {
        evaluator = translateEvaluator(evaluator);

        evaluator = evaluator.replaceAll("(?i)currentState|" + PLACEHOLDER, "true");

        if (BigDoorsOpener.isPlaceholderEnabled() && player != null && !vanilla) {
            evaluator = PlaceholderAPI.setPlaceholders(player, evaluator);
        }

        try {
            boolean aTrue = engine.evalUnsafe(evaluator, null);
        } catch (ScriptException | ExecutionException e) {
            return new Pair<>(ValidatorResult.EXECUTION_FAILED, evaluator);
        } catch (ClassCastException | NullPointerException e) {
            return new Pair<>(ValidatorResult.NON_BOOLEAN_RESULT, evaluator);
        }

        return new Pair<>(ValidatorResult.FINE, evaluator);
    }

    public enum ValidatorResult {
        /**
         * Indicates that the parenthesis on the string are not balanced
         */
        UNBALANCED_PARENTHESIS,
        /**
         * Indicates that a variable which is not a key was used.
         * Will include the part which was not a variable
         */
        INVALID_VARIABLE,
        /**
         * Indicates that a invalid operator was used.
         * Will return the invalid operator.
         */
        INVALID_OPERATOR,
        /**
         * Indicates that the overall syntax is innvalid.
         * Will return all invalid chars.
         */
        INVALID_SYNTAX,
        /**
         * Indicates that the execution failed.
         * Will return the validator which was parsed.
         */
        EXECUTION_FAILED,
        /**
         * Indicates that the result was not a boolean.
         * Will return the validator which was parsed.
         */
        NON_BOOLEAN_RESULT,
        /**
         * Indicates that the syntax is valid and can be used.
         */
        FINE
    }
}
