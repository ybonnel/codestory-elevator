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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptimizedAlzheimerFloorsByDirection extends AlzheimerFloorsByDirection {

    private final int nbMaxWaitWithNoOverPeople;
    private final int nbMaxWaitWithOverPeople;
    private final int peopleInElevatorForOverFlow;
    private OptimizedAlzheimerElevator elevator;

    public OptimizedAlzheimerFloorsByDirection(int nbMaxWaitWithNoOverPeople, int nbMaxWaitWithOverPeople, int peopleInElevatorForOverFlow) {
        this.nbMaxWaitWithNoOverPeople = nbMaxWaitWithNoOverPeople;
        this.nbMaxWaitWithOverPeople = nbMaxWaitWithOverPeople;
        this.peopleInElevatorForOverFlow = peopleInElevatorForOverFlow;
    }

    public void setElevator(OptimizedAlzheimerElevator elevator) {
        this.elevator = elevator;
    }

    public int getNbMaxWait() {
        if (elevator.getPeopleInsideElevator() > peopleInElevatorForOverFlow) {
            return nbMaxWaitWithOverPeople;
        } else {
            return nbMaxWaitWithNoOverPeople;
        }
    }
}
