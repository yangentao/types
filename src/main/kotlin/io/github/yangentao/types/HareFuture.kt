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
    private var _onResult: ValueCallback<T>? = null
    private var _onError: ValueCallback<Throwable>? = null
    private var _onCancel: Runnable? = null
    private var _onDone: Runnable? = null
    private var _timeoutFuture: ScheduledFuture<*>? = null

    @Volatile
    private var _result: T? = null

    @Volatile
    private var _cause: Throwable? = null

    @Suppress("UNCHECKED_CAST")
    private val result: T get() = _result as T

    private fun invokeCallback(block: Runnable) {
        taskVirtual(block)
    }

    @Suppress("UNCHECKED_CAST")
    fun onResult(callback: ValueCallback<T>): HareFuture<T> {
        if (isDone) {
            if (isSuccess) {
                invokeCallback {
                    callback.onValue(result)
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
        _timeoutFuture?.cancel(false)
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
            return _cause
        }

    @Suppress("Since15")
    override fun exceptionNow(): Throwable? {
        return _cause
    }

    @Suppress("UNCHECKED_CAST")
    fun tryGet(): T? {
        if (isSuccess) {
            return result
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(): T {
        return get(-1, TimeUnit.MILLISECONDS)
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(timeout: Long, unit: TimeUnit): T {
        if (!isDone) {
            if (timeout < 0) {
                _sem.acquire()
                _sem.release()
            } else {
                if (_sem.tryAcquire(timeout, unit)) {
                    _sem.release()
                } else {
                    throw TimeoutException()
                }
            }
        }
        when (_state.get()) {
            FutureState.RUNNING -> error("Should NOT happen")
            FutureState.SUCCESS -> return result
            FutureState.FAILED -> throw _cause!!
            FutureState.CANCELLED -> error("Already Cancelled")
        }
    }

    // return isSuccess
    fun waitMill(millseconds: Long): Boolean {
        if (!isDone) {
            if (_sem.tryAcquire(millseconds, TimeUnit.MILLISECONDS)) {
                _sem.release()
            }
        }
        return isSuccess
    }

    val isSuccess: Boolean get() = _state.get() == FutureState.SUCCESS
    val isFailed: Boolean get() = _state.get() == FutureState.FAILED
    val isRunning: Boolean get() = _state.get() == FutureState.RUNNING

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

open class AbsCompleter<T> {
    val future: HareFuture<T> = HareFuture()
    open val isCompleted: Boolean get() = future.isDone

    protected fun callSuccess(result: T) {
        future.complete(result)
    }

    protected fun callFailed(e: Throwable) {
        future.completeError(e)
    }
}

open class Completer<T> : AbsCompleter<T>() {

    open fun success(result: T) {
        if (isCompleted) return
        taskVirtual { callSuccess(result) }
    }

    open fun failed(e: Throwable) {
        if (isCompleted) return
        taskVirtual { callFailed(e) }
    }
}

class HareCompleter<T>() : Completer<T>() {
    private val flag: AtomicBoolean = AtomicBoolean(false)
    override val isCompleted: Boolean get() = flag.get() || future.isDone

    override fun success(result: T) {
        if (flag.getAndSet(true) || future.isDone) return
        taskVirtual { callSuccess(result) }
    }

    override fun failed(e: Throwable) {
        if (flag.getAndSet(true) || future.isDone) return
        taskVirtual { callFailed(e) }
    }
}
