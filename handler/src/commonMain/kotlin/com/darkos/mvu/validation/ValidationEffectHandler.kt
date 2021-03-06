package com.darkos.mvu.validation

import com.darkos.mvu.model.Effect
import com.darkos.mvu.model.Message
import com.darkos.mvu.validation.model.Field
import com.darkos.mvu.validation.model.FieldValidationStatus
import com.darkos.mvu.validation.model.ValidationFieldType
import com.darkos.mvu.validation.model.mvu.ValidationEffect
import com.darkos.mvu.validation.model.mvu.ValidationMessage
import kotlin.reflect.KClass

typealias FieldTypeClass = KClass<out ValidationFieldType>

class ValidationEffectHandler private constructor(
    private val processors: HashMap<FieldTypeClass, suspend (Field) -> Boolean>
) : IValidationHandler {

    //region validators

    private fun defaultEmailValidate(string: String): Boolean {
        val regex =
            Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])")
        return regex.matches(string)
    }

    private fun minSymbolValidate(string: String, min: Int): Boolean {
        return string.length >= min
    }

    private fun maxSymbolValidate(string: String, max: Int): Boolean {
        return string.length < max
    }

    private fun intervalSymbolValidate(string: String, min: Int, max: Int): Boolean {
        return string.length in min until max
    }

    //endregion

    private suspend fun validate(field: Field): Boolean {
        return when (val it = field.type) {
            is ValidationFieldType.Email -> {
                processors[ValidationFieldType.Email::class]?.invoke(field) ?: run {
                    defaultEmailValidate(field.value)
                }
            }
            is ValidationFieldType.MinSymbols -> {
                minSymbolValidate(field.value, it.minCount)
            }
            is ValidationFieldType.MaxSymbols -> {
                maxSymbolValidate(field.value, it.maxCount)
            }
            is ValidationFieldType.IntervalSymbols -> {
                intervalSymbolValidate(field.value, it.minCount, it.maxCount)
            }
            is ValidationFieldType.Custom -> {
                processors[ValidationFieldType.Custom::class]?.invoke(field)
                    ?: throw IllegalStateException("validator for type ${field.type} not found")
            }
        }
    }

    override suspend fun call(effect: Effect): Message {
        require(effect is ValidationEffect.Validate) {
            "supported only ValidationEffect.Validate"
        }

        return effect.fields.filter {
            validate(it).not()
        }.takeIf {
            it.isNotEmpty()
        }?.map {
            it.id
        }?.let {
            ValidationMessage.Error(it)
        } ?: run {
            ValidationMessage.Success
        }
    }

    @ValidationHandlerDsl
    class Builder {
        private val processors = hashMapOf<FieldTypeClass, suspend (Field) -> Boolean>()

        fun ValidateEmail(block: suspend (Field) -> Boolean) {
            processors[ValidationFieldType.Email::class] = block
        }

        fun ValidateCustom(block: suspend (Field) -> Boolean) {
            processors[ValidationFieldType.Custom::class] = block
        }

        fun build() = ValidationEffectHandler(processors)
    }
}

@ValidationHandlerDsl
fun ValidationHandler(block: ValidationEffectHandler.Builder.() -> Unit): ValidationEffectHandler =
    ValidationEffectHandler.Builder().apply(block).build()