package com.ghostgram.data.repository.handlers

import kotlinx.serialization.json.JsonObject

interface TdlibUpdateHandler {
    // Возвращает true, если хэндлер обработал этот ивент
    fun handle(type: String, jsonObject: JsonObject): Boolean
}