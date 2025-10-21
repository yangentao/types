@file:Suppress("Since15")

package io.github.yangentao.types

import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

fun startVirtual(task: Runnable): Thread {
    return Thread.ofVirtual().start(task)
}

fun delayVirtual(millSeconds: Long, task: Runnable): Thread {
    val a = DelayRunnable(millSeconds, task)
    return Thread.startVirtualThread(a)
}

private class DelayRunnable(millSeconds: Long, val runnable: Runnable) : Runnable {
    private var _calcel = AtomicBoolean(false)
    private val endTime: Long = System.currentTimeMillis() + millSeconds

    val isCancel: Boolean get() = _calcel.get()

    fun cancel() {
        if (_calcel.get()) return
        _calcel.set(true)
    }

    override fun run() {
        if (isCancel) return
        if (!Thread.currentThread().isVirtual) {
            println("应该在Virtual Thread中调用！")
        }
        try {
            while (true) {
                val now = System.currentTimeMillis()
                val delta = endTime - now

                if (delta <= 0) {
                    break
                } else if (delta < 500) {
                    Thread.sleep(delta)
                    break
                } else {
                    Thread.sleep(500)
                }
            }

            if (isCancel) return
            runnable.run()
        } catch (ex: InterruptedException) {
            _calcel.set(true)
        }
    }
}

class HareFuture<T> : Future<T> {
    private val _state: AtomicReference<Future.State> = AtomicReference(Future.State.RUNNING)
    private val _sem = Semaphore(0)
    private var _onResult: OnValue<T>? = null
    private var _onError: OnValue<Throwable>? = null
    private var _onCancel: Runnable? = null
    private var _timeoutThread: Thread? = null

    @Volatile
    private var _result: T? = null

    @Volatile
    private var _error: Throwable? = null

    fun onResult(callback: OnValue<T>): HareFuture<T> {
        _onResult = callback
        return this
    }

    fun onError(callback: OnValue<Throwable>): HareFuture<T> {
        _onError = callback
        return this
    }

    fun onCancel(callback: Runnable): HareFuture<T> {
        _onCancel = callback
        return this
    }

    fun onTimeout(millSeconds: Long, callback: Runnable): HareFuture<T> {
        /// onTimeout的callback中执行的代码， 不能在cancel时被中断
        // onTimeout(1000){
        //    fu.cancel()
        //    ......// 这里要执行下去， 不能被 interrupt
        // }
        // 这里的代码， 是不能被中断的， 比如 在其中调用了fu.cancel()
        _timeoutThread = delayVirtual(millSeconds) {
            startVirtual(callback)
        }
        return this
    }

    private fun cancelTimeout() {
        val t = _timeoutThread ?: return
        if (!t.isAlive) return
        if (t.isInterrupted) return
        t.interrupt()
        _timeoutThread = null
    }

    internal fun complete(value: T) {
        if (isDone) return
        cancelTimeout()
        this._result = value
        _state.set(Future.State.SUCCESS)
        _sem.release()
        val r = _onResult
        if (r != null) {
            startVirtual { r.onValue(value) }
        }
    }

    internal fun completeError(e: Throwable) {
        if (isDone) return
        cancelTimeout()
        this._error = e
        _state.set(Future.State.FAILED)
        _sem.release()
        val oe = _onError
        if (oe != null) {
            startVirtual { oe.onValue(e) }
        }
    }

    fun cancel() {
        cancel(false)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (isDone) return false
        cancelTimeout()
        _state.set(Future.State.CANCELLED)
        _sem.release()
        val oc = _onCancel
        if (oc != null) {
            startVirtual { oc.run() }
        }
        return true
    }

    override fun exceptionNow(): Throwable? {
        if (isFailed) return _error
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun tryGet(): T? {
        if (isSuccess) {
            return _result as T
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(): T {
        if (!isDone) {
            _sem.acquire()
            _sem.release()
        }
        when (_state.get()) {
            Future.State.RUNNING -> error("Should NOT happen")
            Future.State.SUCCESS -> return _result as T
            Future.State.FAILED -> throw _error!!
            Future.State.CANCELLED -> error("Already Cancelled")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(timeout: Long, unit: TimeUnit): T {
        if (!isDone) {
            if (_sem.tryAcquire(timeout, unit)) {
                _sem.release()
            } else {
                throw TimeoutException()
            }
        }
        when (_state.get()) {
            Future.State.RUNNING -> error("Should NOT happen")
            Future.State.SUCCESS -> return _result as T
            Future.State.FAILED -> throw _error!!
            Future.State.CANCELLED -> error("Already Cancelled")
        }
    }

    val isSuccess: Boolean get() = _state.get() == Future.State.SUCCESS

    val isFailed: Boolean get() = _state.get() == Future.State.FAILED

    override fun isCancelled(): Boolean {
        return _state.get() == Future.State.CANCELLED
    }

    override fun isDone(): Boolean {
        return _state.get() != Future.State.RUNNING
    }

    override fun state(): Future.State {
        return _state.get()
    }

}

class Completer<T>(private val sync: Boolean = false) {
    private val comp: AtomicBoolean = AtomicBoolean(false)
    val future: HareFuture<T> = HareFuture()
    val isCompleted: Boolean get() = comp.get()

    fun complete(result: T) {
        if (comp.getAndSet(true)) error("Already Completed")
        if (sync) {
            future.complete(result)
        } else {
            startVirtual { future.complete(result) }
        }
    }

    fun completeError(e: Throwable) {
        if (comp.getAndSet(true)) error("Already Completed")
        if (sync) {
            future.completeError(e)
        } else {
            startVirtual { future.completeError(e) }
        }
    }
}
