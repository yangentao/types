package io.github.yangentao.types

import java.util.concurrent.*

@Suppress("Since15")
fun taskVirtual(task: Runnable) {
    Thread.ofVirtual().start(task)
}

fun asyncTask(task: Runnable): Future<*> {
    return Tasks.submit(task)
}

fun <T> asyncCall(millSeconds: Long, task: Callable<T>): Future<T> {
    return Tasks.service.submit(task)
}

fun delayTask(millSeconds: Long, task: Runnable): ScheduledFuture<*> {
    return Tasks.delayMill(millSeconds, task)
}

fun <T> delayCall(millSeconds: Long, task: Callable<T>): ScheduledFuture<T> {
    return Tasks.service.schedule(task, millSeconds, TimeUnit.MILLISECONDS)
}

object Tasks {
    val service: ScheduledExecutorService = Executors.newScheduledThreadPool(4) {
        DeamonThread(it, "TaskPool")
    }

    fun close() {
        service.close()
    }

    //result = exec. submit(aCallable).get()
    fun <T> call(time: TimeValue, task: Callable<T>): ScheduledFuture<T> {
        return service.schedule(task, time.value, time.unit)
    }

    //result = exec. submit(aCallable).get()
    fun <T> call(task: Callable<T>): Future<T> {
        return service.submit(task)
    }

    fun submit(task: Runnable): Future<*> {
        return service.submit(task)
    }

    //TimeUnit.MILLISECONDS
    fun delayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
        return service.schedule(block, delay, TimeUnit.MILLISECONDS)
    }

    //TimeUnit.MILLISECONDS
    fun fixedDelayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
        return service.scheduleWithFixedDelay(block, delay, delay, TimeUnit.MILLISECONDS)
    }

    //TimeUnit.MILLISECONDS
    fun fixedRateMill(period: Long, block: Runnable): ScheduledFuture<*> {
        return service.scheduleAtFixedRate(block, period, period, TimeUnit.MILLISECONDS)
    }

    fun delay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.schedule(block, delay.value, delay.unit)
    }

    fun fixedDelay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.scheduleWithFixedDelay(block, delay.value, delay.value, delay.unit)
    }

    fun fixedRate(period: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.scheduleAtFixedRate(block, period.value, period.value, period.unit)
    }

}

