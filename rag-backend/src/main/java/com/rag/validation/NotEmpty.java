package com.rag.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotEmpty {
    String message() default "字段不能为空";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
