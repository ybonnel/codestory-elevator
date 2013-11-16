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

public class OptimizedAlzheimerElevator extends UpAndDownWithDirectionElevator {

    private final int nbMaxWaitWithNoOverPeople;
    private final int nbMaxWaitWithOverPeople;
    private final int peopleInElevatorForOverFlow;

    public OptimizedAlzheimerElevator(final int nbMaxWaitWithNoOverPeople,
                                      final int nbMaxWaitWithOverPeople,
                                      final int peopleInElevatorForOverFlow) {
        super(new OptimizedAlzheimerFloorsByDirection(nbMaxWaitWithNoOverPeople, nbMaxWaitWithOverPeople, peopleInElevatorForOverFlow));
        ((OptimizedAlzheimerFloorsByDirection) getFloorsByDirection()).setElevator(this);
        this.nbMaxWaitWithNoOverPeople = nbMaxWaitWithNoOverPeople;
        this.nbMaxWaitWithOverPeople = nbMaxWaitWithOverPeople;
        this.peopleInElevatorForOverFlow = peopleInElevatorForOverFlow;
    }

    public OptimizedAlzheimerElevator() {
        this(11, 16, 30);
    }

    public int getNbMaxWaitWithNoOverPeople() {
        return nbMaxWaitWithNoOverPeople;
    }

    public int getNbMaxWaitWithOverPeople() {
        return nbMaxWaitWithOverPeople;
    }

    public int getPeopleInElevatorForOverFlow() {
        return peopleInElevatorForOverFlow;
    }
}
