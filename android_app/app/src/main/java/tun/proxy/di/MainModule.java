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

package tun.proxy.di;

import android.content.Context;
import android.database.Observable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import tun.proxy.event.HideAppEvent;
import tun.proxy.event.SingleLiveEvent;

/**
 * DI with Dagger.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
@Module
public class MainModule {

    private final Context context;

    public MainModule(Context context) {
        this.context = context;
    }

    @Provides
    public Context provideContext() {
        return context;
    }

    @Provides
    public ExecutorService provideExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Singleton
    public SingleLiveEvent<Exception> exceptionSingleLiveEvent() {
        return new SingleLiveEvent<>();
    }

    @Provides
    @Singleton
    public SingleLiveEvent<HideAppEvent> hideAppSingleLiveEvent() {
        return new SingleLiveEvent<>();
    }

    @Provides
    @Singleton
    @Named("running")
    public SingleLiveEvent<Boolean> proxyRunningSingleLiveEvent() {
        return new SingleLiveEvent<>();
    }

    @Provides
    @Singleton
    @Named("vpnpermission")
    public SingleLiveEvent<Boolean> vpnPermissionGrantedSingleLiveEvent() {
        return new SingleLiveEvent<>();
    }

}
