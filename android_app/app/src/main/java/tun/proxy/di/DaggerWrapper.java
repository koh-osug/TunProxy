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

/**
 * Provides access to all interested customers to the dependency injection.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class DaggerWrapper {

    private static MainComponent component;

    public static MainComponent getComponent(Context context) {
        if (component == null) {
            initComponent(context);
        }
        return component;
    }

    private static void initComponent (Context context) {
        component = DaggerMainComponent
                .builder()
                .mainModule(new MainModule(context))
                .build();
    }
}