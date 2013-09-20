package com.mycompany.services;


import com.mycompany.model.Beer;
import fr.ybonnel.simpleweb4j.exception.HttpErrorException;
import fr.ybonnel.simpleweb4j.handlers.resource.RestResource;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeerRessource extends RestResource<Beer> {

    public BeerRessource(String route) {
        super(route, Beer.class);
    }

    @Override
    public Beer getById(String id) throws HttpErrorException {
        return MongoService.getDatastore().get(Beer.class, new ObjectId(id)).prepareForJson();
    }

    @Override
    public Collection<Beer> getAll() throws HttpErrorException {
        List<Beer> beers = new ArrayList<>();
        for (Beer beer : MongoService.getDatastore().find(Beer.class).asList()) {
            beers.add(beer.prepareForJson());
        }
        return beers;
    }

    @Override
    public void update(String id, Beer resource) throws HttpErrorException {
        resource.setId(id);
        MongoService.getDatastore().save(resource.prepareForDb());
    }

    @Override
    public Beer create(Beer resource) throws HttpErrorException {
        MongoService.getDatastore().save(resource.prepareForDb());
        return resource;
    }

    @Override
    public void delete(String id) throws HttpErrorException {
        MongoService.getDatastore().delete(Beer.class, new ObjectId(id));
    }
}
