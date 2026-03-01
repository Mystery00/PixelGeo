package vip.mystery0.pixel.geo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform