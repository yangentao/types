@file:Suppress("UnusedReceiverParameter")

package io.github.yangentao.types

import io.github.yangentao.types.Tasks.service
import java.util.concurrent.*

@Suppress("Since15")
val vtasks: ScheduledExecutorService = if (javaVersionInt >= 19) Executors.newScheduledThreadPool(4, Thread.ofVirtual().factory()) else Executors.newScheduledThreadPool(4)

fun <T> ExecutorService.call(task: Callable<T>): Future<T> {
    return service.submit(task)
}

fun <T> ScheduledExecutorService.delayCall(time: TimeValue, task: Callable<T>): ScheduledFuture<T> {
    return service.schedule(task, time.value, time.unit)
}

fun ScheduledExecutorService.delayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
    return service.schedule(block, delay, TimeUnit.MILLISECONDS)
}

fun ScheduledExecutorService.delay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
    return service.schedule(block, delay.value, delay.unit)
}

fun ScheduledExecutorService.fixedDelayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
    return service.scheduleWithFixedDelay(block, delay, delay, TimeUnit.MILLISECONDS)
}

fun ScheduledExecutorService.fixedDelay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
    return service.scheduleWithFixedDelay(block, delay.value, delay.value, delay.unit)
}

fun ScheduledExecutorService.fixedRateMill(period: Long, block: Runnable): ScheduledFuture<*> {
    return service.scheduleAtFixedRate(block, period, period, TimeUnit.MILLISECONDS)
}

fun ScheduledExecutorService.fixedRate(period: TimeValue, block: Runnable): ScheduledFuture<*> {
    return service.scheduleAtFixedRate(block, period.value, period.value, period.unit)
}

