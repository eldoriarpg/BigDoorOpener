package de.eldoria.bigdoorsopener.doors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConditionScope {

    Scope value();

    public enum Scope {
        WORLD, PLAYER
    }
}
