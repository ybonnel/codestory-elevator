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
package fr.ybonnel;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.ybonnel.services.AlzheimerElevator;
import fr.ybonnel.services.Elevator;
import fr.ybonnel.services.FastDeliverElevator;
import fr.ybonnel.services.OptimizedAlzheimerElevator;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Simulator {

    private static final int LOWER_FLOOR = -3;
    private static final int HIGHER_FLOOR = 25;
    private static final int CABIN_SIZE = 42;

    private int currentTick = 0;

    private final Random random;

    private List<ElevatorWithState> elevatorWithStates = new ArrayList<>();
    private final List<Integer> arrivals;

    public Simulator(List<Integer> arrivals, Elevator ... elevators) {
        this.arrivals = arrivals;
        random = new Random();
        for (Elevator elevator : elevators) {
            elevatorWithStates.add(new ElevatorWithState(elevator, LOWER_FLOOR, HIGHER_FLOOR, CABIN_SIZE));
        }
    }

    private void runOneTick() {
        for (ElevatorWithState elevatorWithState : elevatorWithStates) {
            elevatorWithState.oneTick(currentTick);
        }

        for (int i = 0; i < arrivals.get(currentTick % arrivals.size()); i++) {
            int startFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) - LOWER_FLOOR;
            int destinationFloor;
            do {
                destinationFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) - LOWER_FLOOR;
            } while (destinationFloor == startFloor);

            for (ElevatorWithState elevatorWithState : elevatorWithStates) {
                elevatorWithState.addUser(currentTick, startFloor, destinationFloor);
            }
        }

        currentTick++;
    }

    public static void main(String[] args) {
        List<Integer> arrivals = new GsonBuilder().create().fromJson(
                new InputStreamReader(Simulator.class.getResourceAsStream("/repartition.json")),
                new TypeToken<List<Integer>>(){}.getType());

        Simulator simulator = new Simulator(arrivals,
                new OptimizedAlzheimerElevator(),
                new AlzheimerElevator(),
                new FastDeliverElevator());

        for (int i=0; i<arrivals.size()*2; i++) {
            simulator.runOneTick();
        }

        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates) {
            System.out.println("Elevator "
                    + elevatorWithState.getName()
                    + " : "
                    + elevatorWithState.getScore());
        }

        /*List<OptimizedAlzheimerElevator> elevators = new ArrayList<>();

        for (int nbMaxWaitNoOver = 1; nbMaxWaitNoOver <= 20; nbMaxWaitNoOver++) {
            for (int nbMaxWaitOver = 1; nbMaxWaitOver <= 20; nbMaxWaitOver++) {
                for (int overFlowCabin = 1; overFlowCabin <= 42; overFlowCabin++) {
                   elevators.add(new OptimizedAlzheimerElevator(
                           nbMaxWaitNoOver,
                           nbMaxWaitOver,
                           overFlowCabin
                   ));
                }
            }
        }

        simulator = new Simulator(arrivals,
                elevators.toArray(new Elevator[elevators.size()]));

        for (int i=0; i<arrivals.size()*2; i++) {
            System.out.println(i);
            simulator.runOneTick();
        }

        int maxScore = 0;
        OptimizedAlzheimerElevator bestElevator = null;

        for (ElevatorWithState elevatorWithState : simulator.elevatorWithStates) {
            if (elevatorWithState.getScore() > maxScore) {
                maxScore = elevatorWithState.getScore();
                bestElevator = (OptimizedAlzheimerElevator) elevatorWithState.getElevator();
            }
        }

        System.out.println("Best score : " + maxScore);
        System.out.println("Parameters : ("
                + bestElevator.getNbMaxWaitWithNoOverPeople()
                + " - " + bestElevator.getNbMaxWaitWithOverPeople()
                + " - " + bestElevator.getPeopleInElevatorForOverFlow()
                + ")");*/

    }


}
