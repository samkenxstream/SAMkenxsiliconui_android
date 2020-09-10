/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.platform

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Success
import im.vector.app.core.utils.DataSource
import im.vector.app.core.utils.PublishDataSource
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

abstract class VectorViewModel<S : MvRxState, VA : VectorViewModelAction, VE : VectorViewEvents>(initialState: S)
    : BaseMvRxViewModel<S>(initialState, false) {

    interface Factory<S : MvRxState> {
        fun create(state: S): BaseMvRxViewModel<S>
    }

    // Used to post transient events to the View
    protected val _viewEvents = PublishDataSource<VE>()
    val viewEvents: DataSource<VE> = _viewEvents
    private val isStarted = AtomicBoolean(false)

    /**
     * Call this method when you are ready to grab data for your ViewModel.
     * Mostly to be used at the end of onCreateView from fragment.
     * It's safe to be called multiple time.
     */
    fun start() {
        if (!isStarted.getAndSet(true)) {
            Timber.v("Start viewModel ${this.javaClass.name}")
            viewModelScope.launch {
                onStarted()
            }
        }
    }

    /**
     * This is the method where you want to start observing rx data, subscribe to state...
     * Will be called only once. It's bound the viewModelScope and is launched on Main thread
     */
    protected open suspend fun onStarted() = Unit

    /**
     * This method does the same thing as the execute function, but it doesn't subscribe to the stream
     * so you can use this in a switchMap or a flatMap
     */
    fun <T> Single<T>.toAsync(stateReducer: S.(Async<T>) -> S): Single<Async<T>> {
        setState { stateReducer(Loading()) }
        return this.map { Success(it) as Async<T> }
                .onErrorReturn { Fail(it) }
                .doOnSuccess { setState { stateReducer(it) } }
    }

    /**
     * This method does the same thing as the execute function, but it doesn't subscribe to the stream
     * so you can use this in a switchMap or a flatMap
     */
    fun <T> Observable<T>.toAsync(stateReducer: S.(Async<T>) -> S): Observable<Async<T>> {
        setState { stateReducer(Loading()) }
        return this.map { Success(it) as Async<T> }
                .onErrorReturn { Fail(it) }
                .doOnNext { setState { stateReducer(it) } }
    }

    abstract fun handle(action: VA)
}
