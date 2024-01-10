package com.virogu.core

import kotlinx.serialization.json.Json

val json: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
}