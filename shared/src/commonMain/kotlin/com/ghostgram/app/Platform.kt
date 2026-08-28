package com.ghostgram.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform