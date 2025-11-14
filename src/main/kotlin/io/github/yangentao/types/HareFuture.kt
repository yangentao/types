package io.github.yangentao.types

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

enum class FutureState {
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}

class HareFuture<T> : Future<T> {
    private val _state: AtomicReference<FutureState> = AtomicReference(FutureState.RUNNING)
    private val _sem = Semaphore(0)
    private var _onResult: ValueCallback<T>? = null
    private var _onError: ValueCallback<Throwable>? = null
    private var _onCancel: Runnable? = null
    private var _onDone: Runnable? = null
    private var _timeoutFuture: ScheduledFuture<*>? = null

    @Volatile
    private var _result: T? = null

    @Volatile
    private var _cause: Throwable? = null

    private fun invokeCallback(block: Runnable) {
        taskVirtual(block)
    }

    @Suppress("UNCHECKED_CAST")
    fun onResult(callback: ValueCallback<T>): HareFuture<T> {
        if (isDone) {
            if (isSuccess) {
                invokeCallback {
                    callback.onValue(_result as T)
                }
            }
        } else {
            _onResult = callback
        }
        return this
    }

    fun onError(callback: ValueCallback<Throwable>): HareFuture<T> {
        if (isDone) {
            if (isFailed) {
                invokeCallback {
                    callback.onValue(_cause!!)
                }
            }
        } else {
            _onError = callback
        }
        return this
    }

    fun onCancel(callback: Runnable): HareFuture<T> {
        if (isDone) {
            if (isCancelled) {
                invokeCallback(callback)
            }
        } else {
            _onCancel = callback
        }
        return this
    }

    fun onDone(callback: Runnable): HareFuture<T> {
        if (isDone) {
            invokeCallback(callback)
        } else {
            _onDone = callback
        }
        return this
    }

    fun onTimeout(millSeconds: Long, callback: Runnable): HareFuture<T> {
        if (isDone) return this
        _timeoutFuture = delayTask(millSeconds) {
            if (!isDone) callback.run()
        }
        return this
    }

    private fun cancelTimeout() {
        _timeoutFuture?.cancel(false)
        _timeoutFuture = null
    }

    internal fun complete(value: T) {
        if (isDone) return
        cancelTimeout()
        this._result = value
        _state.set(FutureState.SUCCESS)
        _sem.release()
        val r = _onResult
        if (r != null) {
            invokeCallback {
                r.onValue(value)
                _onDone?.run()
            }
        } else {
            _onDone?.also { invokeCallback(it) }
        }
    }

    internal fun completeError(e: Throwable) {
        if (isDone) return
        cancelTimeout()
        this._cause = e
        _state.set(FutureState.FAILED)
        _sem.release()
        val oe = _onError
        if (oe != null) {
            invokeCallback {
                oe.onValue(e)
                _onDone?.also { it.run() }
            }
        } else {
            _onDone?.also { invokeCallback(it) }
        }
    }

    fun cancel() {
        cancel(false)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (isDone) return false
        cancelTimeout()
        _state.set(FutureState.CANCELLED)
        _sem.release()
        val oc = _onCancel
        if (oc != null) {
            invokeCallback {
                oc.run()
                _onDone?.also { it.run() }
            }
        } else {
            _onDone?.also { invokeCallback(it) }
        }

        return true
    }

    val error: Throwable?
        get() {
            if (isFailed) return _cause
            return null
        }

    @Suppress("Since15")
    override fun exceptionNow(): Throwable? {
        if (isFailed) return _cause
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
            FutureState.RUNNING -> error("Should NOT happen")
            FutureState.SUCCESS -> return _result as T
            FutureState.FAILED -> throw _cause!!
            FutureState.CANCELLED -> error("Already Cancelled")
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
            FutureState.RUNNING -> error("Should NOT happen")
            FutureState.SUCCESS -> return _result as T
            FutureState.FAILED -> throw _cause!!
            FutureState.CANCELLED -> error("Already Cancelled")
        }
    }

    // return isSuccess
    fun waitMill(millseconds: Long): Boolean {
        if (!isDone) {
            if (_sem.tryAcquire(millseconds, TimeUnit.MILLISECONDS)) {
                _sem.release()
                return isSuccess
            } else {
                return false
            }
        }
        return when (_state.get()) {
            FutureState.RUNNING -> false
            FutureState.SUCCESS -> true
            FutureState.FAILED -> false
            FutureState.CANCELLED -> false
        }
    }

    val isSuccess: Boolean get() = _state.get() == FutureState.SUCCESS

    val isFailed: Boolean get() = _state.get() == FutureState.FAILED

    override fun isCancelled(): Boolean {
        return _state.get() == FutureState.CANCELLED
    }

    override fun isDone(): Boolean {
        return _state.get() != FutureState.RUNNING
    }

    @Suppress("Since15")
    override fun state(): Future.State {
        return when (_state.get()) {
            FutureState.RUNNING -> Future.State.RUNNING
            FutureState.SUCCESS -> Future.State.SUCCESS
            FutureState.FAILED -> Future.State.FAILED
            FutureState.CANCELLED -> Future.State.CANCELLED
        }
    }

}

class Completer<T>() {
    private val comp: AtomicBoolean = AtomicBoolean(false)
    val future: HareFuture<T> = HareFuture()
    val isCompleted: Boolean get() = comp.get()

    fun complete(result: T) {
        if (comp.getAndSet(true)) error("Already Completed")
        taskVirtual { future.complete(result) }
    }

    fun completeError(e: Throwable) {
        if (comp.getAndSet(true)) error("Already Completed")
        taskVirtual { future.completeError(e) }
    }
}

class AnyCompleter<T>(val size: Int) {
    private val countDown: AtomicInteger = AtomicInteger(size)
    val future: HareFuture<List<T>> = HareFuture()
    val isCompleted: Boolean get() = countDown.get() <= 0
    val successList = ArrayList<T>()
    val failedList = ArrayList<Throwable>()

    fun complete(result: T) {
        if (countDown.getAndDecrement() <= 0) error("Already Completed")
        successList.add(result)
        if (countDown.get() <= 0) {
            taskVirtual { future.complete(successList) }
        }
    }

    fun completeError(e: Throwable) {
        if (countDown.getAndDecrement() <= 0) error("Already Completed")
        failedList.add(e)
        if (countDown.get() <= 0) {
            taskVirtual { future.complete(successList) }
        }
    }
}
