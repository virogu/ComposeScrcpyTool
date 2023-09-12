package com.virogu.tools

import kotlinx.serialization.json.Json

val json: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
}