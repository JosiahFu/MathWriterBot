package archives.tater.bot.mathwriter.lib

abstract class ObservableMap<K, V>(private val base: MutableMap<K, V>): MutableMap<K, V> by base {
    abstract fun onChange()

    override fun put(key: K, value: V): V? {
        return base.put(key, value).also { onChange() }
    }

    override fun putAll(from: Map<out K, V>) {
        return base.putAll(from).also { onChange() }
    }

    override fun remove(key: K): V? {
        return base.remove(key).also { onChange() }
    }

    override fun remove(key: K, value: V): Boolean {
        return base.remove(key, value).also { onChange() }
    }

    override fun clear() {
        base.clear()
        onChange()
    }
}

inline fun <K, V> ObservableMap(base: MutableMap<K, V>, crossinline onchange: () -> Unit) = object : ObservableMap<K, V>(base) {
    override fun onChange() {
        onchange()
    }
}
