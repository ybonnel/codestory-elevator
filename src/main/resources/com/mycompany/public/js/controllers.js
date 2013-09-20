

function MainCtrl($scope, BeerService) {
    $scope.beers = BeerService.query();
    $scope.delete = function(beer) {
        $('#delete' + beer.id).modal('hide');
        BeerService.delete({id:beer.id}, function(data){
            $scope.beers = BeerService.query();
        });
    }
}

function EditCtrl($scope, $routeParams, $location, BeerService) {
    $scope.beer = BeerService.get({id:$routeParams.id});
    $scope.submitMessage = "Update beer";
    $scope.saveBeer = function(beer) {
        if ($scope.form.$invalid) {
            $scope.form.name.$dirty = true;
        } else {
            BeerService.update(beer, function(data) {
                $location.path('/main');
            });
        }
    };
}

function NewCtrl($scope, $location, BeerService) {
    $scope.submitMessage = "Save beer";
    $scope.saveBeer = function(beer) {
        if ($scope.form.$invalid) {
            $scope.form.name.$dirty = true;
        } else {
            BeerService.save(beer, function(data) {
                $location.path('/main');
            });
        }
    };
}


