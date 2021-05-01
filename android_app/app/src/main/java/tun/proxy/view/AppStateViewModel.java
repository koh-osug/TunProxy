/*
 *     TunProxy is a proxy forwarding tool using Android's VPNService.
 *     Copyright (C) 2021 raise.isayan@gmail.com / Karsten Ohme
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package tun.proxy.view;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Observable;
import java.util.Observer;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Getter;
import tun.proxy.model.AppState;

/**
 * View model for the app state.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
@Singleton
@Getter
public class AppStateViewModel extends ViewModel {

    private LiveData<Boolean> busy;

    private LiveData<Boolean> proxyRunning;

    private LiveData<Boolean> startedRemotely;

    @Inject
    public AppStateViewModel(AppState appState) {
        final MutableLiveData<AppState> appStateMutableLiveData = new MutableLiveData<>();
        appState.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                appStateMutableLiveData.postValue((AppState) o);
            }
        });
        this.busy = Transformations.map(appStateMutableLiveData, new Function<AppState, Boolean>() {
            @Override
            public Boolean apply(AppState input) {
                return input.isBusy();
            }
        });
        this.proxyRunning = Transformations.map(appStateMutableLiveData, new Function<AppState, Boolean>() {
            @Override
            public Boolean apply(AppState input) {
                return input.isProxyRunning();
            }
        });
        this.startedRemotely = Transformations.map(appStateMutableLiveData, new Function<AppState, Boolean>() {
            @Override
            public Boolean apply(AppState input) {
                return input.isStartedRemotely();
            }
        });
        // trigger initially, needed on onCreate for orientation changes
        appState.update();
    }
}
