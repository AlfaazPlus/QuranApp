import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConcurrentCache<K, V> {

    private val cache = mutableMapOf<K, V>()
    private val mutex = Mutex()

    suspend fun get(key: K): V? = mutex.withLock {
        cache[key]
    }

    suspend fun put(key: K, value: V): V? = mutex.withLock {
        cache.put(key, value)
    }

    suspend fun remove(key: K): V? = mutex.withLock {
        cache.remove(key)
    }

    suspend fun containsKey(key: K): Boolean = mutex.withLock {
        cache.containsKey(key)
    }

    suspend fun clear() = mutex.withLock {
        cache.clear()
    }

    suspend fun size(): Int = mutex.withLock {
        cache.size
    }

    suspend fun isEmpty(): Boolean = mutex.withLock {
        cache.isEmpty()
    }

    suspend fun entries(): List<Pair<K, V>> = mutex.withLock {
        cache.map { (key, value) -> key to value }
    }

    suspend fun keys(): List<K> = mutex.withLock {
        cache.keys.toList()
    }

    suspend fun values(): List<V> = mutex.withLock {
        cache.values.toList()
    }
}
