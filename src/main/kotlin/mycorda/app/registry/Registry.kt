package mycorda.app.registry

import java.util.HashMap

/**
 * ** Copied from Azure Workbench - move into a commons jar **
 *
 * Simple registry for basic DI
 */
@Suppress("UNCHECKED_CAST")
class Registry {
    private var registry: MutableMap<Class<*>, Any> = HashMap()

    constructor() {}

    constructor(reg: Map<Class<*>, Any>) {
        registry = HashMap(reg)
    }

    constructor(`object`: Any) {
        registry[`object`.javaClass] = `object`
    }

    constructor(vararg objects: Any) {
        for (o in objects) {
            registry[o.javaClass] = o
        }
    }

    fun store(`object`: Any): Registry {
        registry[`object`.javaClass] = `object`
        return this
    }

    fun <T> get(clazz: Class<T>): T {
        val matches = HashSet<T>()
        if (registry.containsKey(clazz)) {
            matches.add((registry[clazz] as T))
        }

        for ((_, value) in registry) {

            if (clazz.isInterface) {
                if (clazz.isAssignableFrom(value.javaClass)) {
                    matches.add(value as T)
                }
            }


            // check subclasses
            var superClazz = value.javaClass.superclass as Class<Any>
            while (superClazz.name != "java.lang.Object") {
                if (clazz.name == superClazz.name) {
                    matches.add(value as T)
                }
                superClazz = superClazz.superclass as Class<Any>
            }
        }

        if (matches.isEmpty()) throw RuntimeException("Class $clazz in not in the registry")
        if (matches.size > 1) throw RuntimeException("Class $clazz in the registry multiple times - ${matches.joinToString()}")
        return matches.first()
    }

    fun <T> geteOrElse(clazz: Class<T>, other: T): T {
        try {
            return get(clazz)
        } catch (ex: RuntimeException) {
            return other
        }
    }

    fun <T> getOrNull(clazz: Class<T>): T? {
        try {
            return get(clazz)
        } catch (ex: RuntimeException) {
            return null
        }
    }

    fun <T> contains(clazz: Class<T>): Boolean {
        return try {
            get(clazz)
            true
        } catch (ex: RuntimeException) {
            false
        }
    }

    fun <T> missing(clazz: Class<T>): Boolean {
        return !contains(clazz)
    }

    fun flush() {
        registry = HashMap<Class<*>, Any>()
    }

    /**
     * Make a copy of the original registry with the stored
     * objects overridden.
     *
     * @param objects
     * @return
     */
    fun clone(): Registry {
        val cloned = HashMap<Class<*>, Any>(registry.size)
        registry.entries.forEach { cloned.put(it.key, it.value) }
        return Registry(cloned)
    }
}


