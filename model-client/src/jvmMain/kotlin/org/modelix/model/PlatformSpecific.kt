package org.modelix.model

import org.apache.commons.collections4.map.LRUMap
import org.apache.log4j.Logger
import java.util.*
import kotlin.reflect.KClass

actual fun sleep(milliseconds: Long) {
    Thread.sleep(milliseconds)
}

actual fun logError(message: String, exception: Exception, contextClass: KClass<*>) {
    Logger.getLogger(contextClass.java).error(message, exception)
}

actual fun logWarning(message: String, exception: Exception, contextClass: KClass<*>) {
    Logger.getLogger(contextClass.java).warn(message, exception)
}

actual fun logDebug(message: () -> String, contextClass: KClass<*>) {
    val logger = Logger.getLogger(contextClass.java)
    if (logger.isDebugEnabled) logger.debug(message())
}

actual fun bitCount(bits: Int): Int {
    return Integer.bitCount(bits)
}

actual fun <K, V> createLRUMap(size: Int): MutableMap<K, V> {
    return Collections.synchronizedMap(LRUMap<K, V>(100000))
}

actual fun randomUUID(): String {
    return UUID.randomUUID().toString()
}