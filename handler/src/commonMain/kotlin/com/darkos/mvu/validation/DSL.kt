package com.darkos.mvu.validation

@DslMarker
annotation class ValidationHandlerDsl

@ValidationHandlerDsl
fun ValidationHandler(block: ValidationEffectHandler.Builder.() -> Unit): ValidationEffectHandler =
    ValidationEffectHandler.Builder().apply(block).build()