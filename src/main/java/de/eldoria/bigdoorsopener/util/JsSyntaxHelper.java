package de.eldoria.bigdoorsopener.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsSyntaxHelper {

    private static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByExtension("nashorn");
    private static final Pattern VARIABLE = Pattern.compile("[a-zA-Z]+");
    private static final Pattern OPERATORS = Pattern.compile("[^&]&[^&]|[^|]\\|[^|]|[^=!]=[^=]|![^=]");
    private static final Pattern SYNTAX = Pattern.compile("\\||&|!|=|\\(|\\)|\\s");

    private JsSyntaxHelper() {
    }

    public static String translateEvaluator(String evaluator) {
        String result = evaluator;

        // replace words
        result = result.replaceAll("(?i)\\sand\\s", "&&");
        result = result.replaceAll("(?i)\\sor\\s", "||");
        result = result.replaceAll("(?i)\\sis\\snot\\s", "!=");
        result = result.replaceAll("(?i)\\sis\\s", "==");
        result = result.replaceAll("(?i)\\snot\\s", "!=");

        // remove whitespaces.
        result = result.replace("\\s", "");
        return result;
    }

    /**
     * This method validates a java script string.
     * Will call {@link #translateEvaluator(String)} first and check the returned string.
     *
     * @param evaluator string to evaluate
     * @return pair which returns the result and a optinal string which contains different valued based on the result
     */
    public static Pair<ValidatorResult, String> validateEvaluator(String evaluator) {
        evaluator = translateEvaluator(evaluator);

        // check for unbalanced parenthesis
        if (TextUtil.countChars(evaluator, '(') == TextUtil.countChars(evaluator, ')')) {
            return new Pair<>(ValidatorResult.UNBALANCED_PARENTHESIS, "");
        }

        String cleaned = evaluator.replaceAll("(?i)itemKey|locationKey|permissionKey|timeKey|weatherKey", "");
        Matcher matcher = VARIABLE.matcher(cleaned);
        if (matcher.find()) {
            return new Pair<>(ValidatorResult.INVALID_VARIABLE, matcher.group());
        }

        matcher = OPERATORS.matcher(evaluator);
        if (matcher.find()) {
            return new Pair<>(ValidatorResult.INVALID_OPERATOR, matcher.group());
        }

        // remove operators to see if some operators are left.
        cleaned = cleaned.replaceAll("\\||&|!|=|\\(|\\)|\\s", "");
        if (!cleaned.replaceAll("\\||&|!|=|\\(|\\)|\\s", "").isEmpty()) {
            return new Pair<>(ValidatorResult.INVALID_SYNTAX, cleaned);
        }


        try {
            boolean aTrue = (boolean) ENGINE.eval(evaluator.replaceAll("(?i)itemKey|locationKey|permissionKey|timeKey|weatherKey", "true"));
        } catch (ScriptException e) {
            return new Pair<>(ValidatorResult.EXECUTION_FAILED, evaluator);
        } catch (ClassCastException e) {
            return new Pair<>(ValidatorResult.NON_BOOLEAN_RESULT, evaluator);
        }

        return new Pair<>(ValidatorResult.FINE, evaluator);
    }

    private enum ValidatorResult {
        UNBALANCED_PARENTHESIS, INVALID_VARIABLE, INVALID_OPERATOR, INVALID_SYNTAX, EXECUTION_FAILED, NON_BOOLEAN_RESULT, FINE
    }
}
