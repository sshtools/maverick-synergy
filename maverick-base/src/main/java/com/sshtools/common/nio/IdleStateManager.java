/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
package com.sshtools.common.nio;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that checks the idle state of another class.
 *
 * @author Lee David Painter
 */
public class IdleStateManager  {

    Map<IdleStateListener, Long> listeners = new ConcurrentHashMap<IdleStateListener, Long>(50, 0.9f, 1);
    int numSecondsBeforeIdle;
    int servicePeriodSeconds;
    int numInactiveServicesPeriodsPerIdle;
    long lastService = 0;
    boolean servicing = false;

    public IdleStateManager(int servicePeriodSeconds,
                            int numInactiveServicesPeriodsPerIdle) {
        this.servicePeriodSeconds = servicePeriodSeconds;
        this.numInactiveServicesPeriodsPerIdle = numInactiveServicesPeriodsPerIdle;
    }

    /**
     * Called by a listener when they want their idle state to be reset.
     *
     * @param obj IdleStateListener
     */
    public synchronized void reset(IdleStateListener obj) {
    	if(listeners.containsKey(obj))
    		listeners.put(obj, Long.valueOf(System.currentTimeMillis()));
    }
    
    public synchronized void register(IdleStateListener obj) {
    	listeners.put(obj, Long.valueOf(System.currentTimeMillis()));
    }

    /**
     * Called by a listener when they want to remove themselves
     * @param obj IdleStateListener
     */
    public synchronized void remove(IdleStateListener obj) {
    	if(!servicing)
    		listeners.remove(obj);
    }

    /**
     * Called by a selector to determine when the service run is ready to
     * be executed.
     *
     * @return boolean
     */
    public boolean isReady() {
        return ((System.currentTimeMillis() - lastService) / 1000) >= servicePeriodSeconds;
    }

    /**
     * Called by a thread which is managing idle states
     */
    public synchronized void service() {

        lastService = System.currentTimeMillis();

        Map.Entry<IdleStateListener, Long> entry;

        servicing = true;
        for(Iterator<Map.Entry<IdleStateListener, Long>> it = listeners.entrySet().iterator();
                          it.hasNext();) {
            entry = it.next();
            long start = entry.getValue().longValue();
            long current = System.currentTimeMillis();
            long elasped = (current - start) / 1000;
            if(elasped >= (servicePeriodSeconds * numInactiveServicesPeriodsPerIdle)) {
               if((entry.getKey()).idle())
                   it.remove();
           }
        }
        servicing = false;

    }


}
