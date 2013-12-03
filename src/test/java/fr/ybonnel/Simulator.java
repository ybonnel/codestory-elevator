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
import fr.ybonnel.services.ByUserElevator;
import fr.ybonnel.services.ByUserElevators;
import fr.ybonnel.services.Elevators;
import org.openqa.selenium.By;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Simulator {

    private static final int LOWER_FLOOR = -5;
    private static final int HIGHER_FLOOR = 35;
    private static final int CABIN_SIZE = 30;
    private static final int CABIN_COUNT = 2;
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    private int currentTick = 0;

    private final Random random;

    private List<ElevatorsWithState> elevatorsWithStates = new ArrayList<>();
    private final List<Integer> arrivals;

    public Simulator(List<Integer> arrivals, Elevators... elevators) {
        this.arrivals = arrivals;
        random = new Random();
        for (Elevators elevator : elevators) {
            elevatorsWithStates.add(new ElevatorsWithState(elevator, LOWER_FLOOR, HIGHER_FLOOR, CABIN_SIZE, CABIN_COUNT));
        }
    }

    private void runOneTick() throws InterruptedException {

        List<Callable<Void>> runs = new ArrayList<>();

        for (final ElevatorsWithState elevatorsWithState : elevatorsWithStates) {
            runs.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    elevatorsWithState.oneTick(currentTick);
                    return null;
                }
            });
        }

        List<Future<Void>> results = executorService.invokeAll(runs);

        boolean hasFinished = false;

        while (!hasFinished) {
            hasFinished = true;
            for (Future<Void> result : results) {
                if (!result.isDone()) {
                    hasFinished = false;
                }
            }
            Thread.sleep(1);
        }

        for (int i = 0; i < arrivals.get(currentTick % arrivals.size()); i++) {
            int startFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) + LOWER_FLOOR;
            int destinationFloor;
            do {
                destinationFloor = random.nextInt(HIGHER_FLOOR - LOWER_FLOOR + 1) + LOWER_FLOOR;
            } while (destinationFloor == startFloor);

            for (ElevatorsWithState elevatorsWithState : elevatorsWithStates) {
                elevatorsWithState.addUser(currentTick, startFloor, destinationFloor);
            }
        }

        currentTick++;
    }



    public static void main(String[] args) throws InterruptedException {
        List<Integer> arrivals = new GsonBuilder().create().fromJson(
                new InputStreamReader(Simulator.class.getResourceAsStream("/repartition.json")),
                new TypeToken<List<Integer>>(){}.getType());

        /*List<ByUserElevators> elevators = new ArrayList<>();

        for (int maxWaitings = 10; maxWaitings < 500; maxWaitings+=1) {
            elevators.add(new ByUserElevators(false, maxWaitings));
        }*/

        Simulator simulator = new Simulator(arrivals,
                new ByUserElevators());



        for (int i=0; i<25000; i++) {
            simulator.runOneTick();
            System.out.println(i);
        }

        simulator.executorService.shutdown();

        Collections.sort(simulator.elevatorsWithStates, new Comparator<ElevatorsWithState>() {
            @Override
            public int compare(ElevatorsWithState o1, ElevatorsWithState o2) {
                return Integer.compare(o1.getScore(), o2.getScore());
            }
        });

        for (ElevatorsWithState elevatorWithState : simulator.elevatorsWithStates) {
            System.out.println("Elevator "
                    + elevatorWithState.getName()
                    + " : "
                    + elevatorWithState.getScore());
        }





    }

}
