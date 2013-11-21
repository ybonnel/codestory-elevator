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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FastDeliverElevator extends UpAndDownWithDirectionElevator {

    public FastDeliverElevator() {
        super(new AlzheimerFastDeliverFloorsByDirection());
        ((AlzheimerFastDeliverFloorsByDirection)getFloorsByDirection()).setElevator(this);
    }

    private int currentTick = -1;
    private List<Integer> peopleByTick = new ArrayList<>(Collections.nCopies(16000, 0));

    @Override
    protected void addCall(int floor, String to) {
        if (currentTick >= 0 && currentTick < 16000) {
            peopleByTick.set(currentTick, peopleByTick.get(currentTick)+1);
        }
        super.addCall(floor, to);
    }

    @Override
    protected Command getNextCommand() {
        currentTick++;
        return super.getNextCommand();
    }

    @Override
    public void reset(String cause, Integer lowerFloor, Integer higherFloor, Integer cabinSize) {
        super.reset(cause, lowerFloor, higherFloor, cabinSize);
        if (cause != null && cause.startsWith("the elevator is at floor ")) {
            currentTick = -1;
            peopleByTick = new ArrayList<>(Collections.nCopies(16000, 0));
        }
    }

    public List<Integer> getPeopleByTick() {
        return peopleByTick;
    }

    public int getCurrentTick() {
        return currentTick;
    }
}
