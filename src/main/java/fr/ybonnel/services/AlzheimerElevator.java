/*
 * Copyright 2013- Yan Bonnel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.ybonnel.services;

import java.util.HashMap;
import java.util.Map;

public class AlzheimerElevator extends UpAndDownWithDirectionElevator {

    private final Map<Integer, Integer> nbMaxWait;
    private final Map<Integer, Integer> nbMaxWaitInElevator;


    public AlzheimerElevator(int ticksBetweenReset, Map<Integer, Integer> nbMaxWait, Map<Integer, Integer> nbMaxWaitInElevator) {
        super(new AlzheimerFloorsByDirection(nbMaxWait, nbMaxWaitInElevator));
        ((AlzheimerFloorsByDirection)getFloorsByDirection()).setElevator(this);
        this.nbMaxWait = nbMaxWait;
        this.nbMaxWaitInElevator = nbMaxWaitInElevator;
        setTickBetweenReset(ticksBetweenReset);
    }

    public AlzheimerElevator() {
        this(180, new HashMap<Integer, Integer>(){{
                 put(0, 20);
                 put(20, 17);
                 put(40, 17);
                 put(60, 12);
             }}, new HashMap<Integer, Integer>(){{
                 put(0, 23);
                 put(20, 15);
                 put(40, 23);
                 put(60, 23);
             }}
        );
    }

    public AlzheimerElevator(int ticksBetweenReset) {
        this(ticksBetweenReset,
                new HashMap<Integer, Integer>(){{
                 put(0, 20);
                 put(20, 17);
                 put(40, 17);
                 put(60, 12);
             }}, new HashMap<Integer, Integer>(){{
                 put(0, 23);
                 put(20, 15);
                 put(40, 23);
                 put(60, 23);
             }}
        );
    }

    public Map<Integer, Integer> getNbMaxWait() {
        return nbMaxWait;
    }

    public Map<Integer, Integer> getNbMaxWaitInElevator() {
        return nbMaxWaitInElevator;
    }
}
