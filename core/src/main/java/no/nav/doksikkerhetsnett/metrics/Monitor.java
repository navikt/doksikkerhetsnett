package no.nav.doksikkerhetsnett.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {

	String value() default "";

	String[] extraTags() default {};

	double[] percentiles() default {};

	boolean histogram() default false;

	String description() default "";
}
