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

package tun.proxy.model;

import java.util.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Getter;

/**
 * The app state,
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
@Getter
@Singleton
public class AppState extends Observable {

    boolean proxyRunning;

    boolean busy;

    boolean startedRemotely;

    @Inject
    public AppState() {
    }


    public void setProxyRunning(boolean proxyRunning) {
        this.proxyRunning = proxyRunning;
        update();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        update();
    }

    public void setStartedRemotely(boolean startedRemotely) {
        this.startedRemotely = startedRemotely;
        update();
    }

    public void update() {
        setChanged();
        notifyObservers();
    }

}
