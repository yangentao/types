package io.github.yangentao.types

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
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
    private var _onResult: OnValue<T>? = null
    private var _onError: OnValue<Throwable>? = null
    private var _onCancel: Runnable? = null
    private var _timeoutFuture: ScheduledFuture<*>? = null

    @Volatile
    private var _result: T? = null

    @Volatile
    private var _cause: Throwable? = null

    @Suppress("UNCHECKED_CAST")
    fun onResult(callback: OnValue<T>): HareFuture<T> {
        if (isDone) {
            if (isSuccess) {
                callback.onValue(_result as T)
            }
        } else {
            _onResult = callback
        }
        return this
    }

    fun onError(callback: OnValue<Throwable>): HareFuture<T> {
        if (isDone) {
            if (isFailed) {
                callback.onValue(_cause!!)
            }
        } else {
            _onError = callback
        }
        return this
    }

    fun onCancel(callback: Runnable): HareFuture<T> {
        if (isDone) {
            if (isCancelled) {
                callback.run()
            }
        } else {
            _onCancel = callback
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
            asyncTask { r.onValue(value) }
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
            asyncTask { oe.onValue(e) }
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
            asyncTask { oc.run() }
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

class Completer<T>(private val sync: Boolean = false) {
    private val comp: AtomicBoolean = AtomicBoolean(false)
    val future: HareFuture<T> = HareFuture()
    val isCompleted: Boolean get() = comp.get()

    fun complete(result: T) {
        if (comp.getAndSet(true)) error("Already Completed")
        if (sync) {
            future.complete(result)
        } else {
            asyncTask { future.complete(result) }
        }
    }

    fun completeError(e: Throwable) {
        if (comp.getAndSet(true)) error("Already Completed")
        if (sync) {
            future.completeError(e)
        } else {
            asyncTask { future.completeError(e) }
        }
    }
}
