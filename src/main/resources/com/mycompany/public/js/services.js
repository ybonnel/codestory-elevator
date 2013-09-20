var services = angular.module('ExperimentServices', ['ngResource']);


services.factory('BeerService', function($resource) {
    return $resource('/beer/:id',
        {id: "@id"},
        {update: {method:'PUT'}}
    );
});