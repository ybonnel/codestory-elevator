package com.mycompany;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mycompany.model.Beer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BeerTest extends TestWithMongo {

    @Test
    public void should_return_no_beer() {
        List<Beer> beers = new Gson().fromJson(HttpRequest.get(defaultUrl() + "/beer").body(), new TypeToken<List<Beer>>(){}.getType());
        assertTrue(beers.isEmpty());
    }

    private String insertBeer() {
        Beer beer = new Beer();
        beer.setName("name");
        Gson gson = new Gson();
        HttpRequest send = HttpRequest.post(defaultUrl() + "/beer").send(gson.toJson(beer));
        assertEquals(send.body(), 201, send.code());

        List<Beer> beers = gson.fromJson(HttpRequest.get(defaultUrl() + "/beer").body(), new TypeToken<List<Beer>>(){}.getType());
        assertEquals(1, beers.size());
        assertEquals("name", beers.get(0).getName());
        return beers.get(0).getId();
    }

    @Test
    public void should_create_and_get_by_id() {
        String id = insertBeer();

        Beer newBeer = new Gson().fromJson(HttpRequest.get(defaultUrl() + "/beer/" + id).body(), Beer.class);
        assertNotNull(newBeer);
        assertEquals(id, newBeer.getId());
        assertEquals("name", newBeer.getName());
    }

    @Test
    public void should_update() {
        String id = insertBeer();
        Gson gson = new Gson();

        Beer newBeer = new Beer();
        newBeer.setName("newName");

        assertEquals(204, HttpRequest.put(defaultUrl() + "/beer/" + id).send(gson.toJson(newBeer)).code());

        List<Beer> beers = gson.fromJson(HttpRequest.get(defaultUrl() + "/beer").body(), new TypeToken<List<Beer>>(){}.getType());
        assertEquals(1, beers.size());
        assertEquals("newName", beers.get(0).getName());
    }

    @Test
    public void should_delete() {
        String id = insertBeer();
        Gson gson = new Gson();

        assertEquals(204, HttpRequest.delete(defaultUrl() + "/beer/" + id).code());
        List<Beer> beers = gson.fromJson(HttpRequest.get(defaultUrl() + "/beer").body(), new TypeToken<List<Beer>>(){}.getType());
        assertTrue(beers.isEmpty());
    }
}
