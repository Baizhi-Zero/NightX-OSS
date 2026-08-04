package net.baizhi.client.event

import net.baizhi.client.utils.ClientUtils

class EventManager {

    private val registry = hashMapOf<Class<out Event>, MutableList<EventHook>>()

    fun registerListener(listener: Listenable) {
        for (method in listener.javaClass.declaredMethods) {
            if (method.isAnnotationPresent(EventTarget::class.java) && method.parameterTypes.size == 1) {
                if (!method.isAccessible)
                    method.isAccessible = true

                val eventClass = method.parameterTypes[0] as Class<out Event>
                val eventTarget = method.getAnnotation(EventTarget::class.java)

                val invokableEventTargets = registry.getOrElse(eventClass, { arrayListOf<EventHook>() })
                try {
                    invokableEventTargets.add(EventHook(listener, method, eventTarget.priority, eventTarget))
                } catch (e: Exception) {
                    e.printStackTrace()

                    invokableEventTargets.add(EventHook(listener, method, eventTarget))
                }
                invokableEventTargets.sortBy { it.priority }
                registry.put(eventClass, invokableEventTargets)
            }
        }
    }

    fun unregisterListener(listenable: Listenable) {
        for ((key, targets) in registry) {
            targets.removeIf { it.eventClass == listenable }

            registry.put(key, targets)
        }
    }

    fun callEvent(event: Event) {
        val targets = registry.get(event.javaClass) ?: return

        for (hook in targets) {
            if (!hook.eventClass.handleEvents() && !hook.isIgnoreCondition)
                continue

            try {
                hook.method.invoke(hook.eventClass, event)
            } catch (throwable: Throwable) {
                ClientUtils.getLogger().error(
                    "[EventManager] Failed to handle event ${event.javaClass.simpleName} in " +
                        "${hook.eventClass.javaClass.simpleName}.${hook.method.name}",
                    throwable
                )
            }
        }
    }

    fun hasActiveHandlers(eventClass: Class<out Event>): Boolean {
        val targets = registry[eventClass] ?: return false

        for (hook in targets) {
            if (hook.eventClass.handleEvents() || hook.isIgnoreCondition)
                return true
        }

        return false
    }
}
