package com.common.library.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	public String columnName() default "";

	public String defaultValue() default "";

	public boolean notNull() default false;
	
	public boolean unique() default false;

}
