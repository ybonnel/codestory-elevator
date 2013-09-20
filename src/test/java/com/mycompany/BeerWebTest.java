package com.mycompany;


import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

public class BeerWebTest extends TestWithMongo {

    @Before
    public void setup() throws IOException {
        goTo("/");
    }

    @Test
    public void should_not_have_beer() {
        assertThat(find("#beers")).isNotEmpty();
        assertThat(find("tbody tr")).isEmpty();
    }

    private void insertBeer(String nameOfBeer) {
        click("#addBeer");
        await().atMost(3, TimeUnit.SECONDS).until("#beers").isNotPresent();
        fill("#name").with(nameOfBeer);
        click("#submit");
        await().atMost(3, TimeUnit.SECONDS).until("#beers").isPresent();
    }

    @Test
    public void can_insert_beer() {
        insertBeer("name");
        FluentList<FluentWebElement> trInTbody = find("tbody tr");
        assertThat(trInTbody).hasSize(1);
        FluentWebElement oneBeer = trInTbody.get(0);
        assertThat(oneBeer.findFirst("td").getText()).isEqualTo("name");
    }

    @Test
    public void can_update_beer() {
        insertBeer("name");
        click("a.icon-edit");
        await().atMost(3, TimeUnit.SECONDS).until("#beers").isNotPresent();
        clear("#name");
        fill("#name").with("newName");
        click("#submit");
        await().atMost(3, TimeUnit.SECONDS).until("#beers").isPresent();
        FluentList<FluentWebElement> trInTbody = find("tbody tr");
        assertThat(trInTbody).hasSize(1);
        FluentWebElement oneBeer = trInTbody.get(0);
        assertThat(oneBeer.findFirst("td").getText()).isEqualTo("newName");
    }

    @Test
    public void can_delete_beer() {
        insertBeer("name");
        click("a.icon-remove");
        click("#remove");
        assertThat(find("tbody tr")).isEmpty();
    }
}
