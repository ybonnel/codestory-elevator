angular.module('simpleweb4j-experiments',
    ['ExperimentServices'])
    .config(['$routeProvider',function($routeProvider) {
        $routeProvider
            .when('/main', {
                templateUrl: 'partial/main.html',
                controller: MainCtrl
            })
            .when('/edit/:id', {
                templateUrl: 'partial/editOrNew.html',
                controller: EditCtrl
            })
            .when('/new', {
                templateUrl: 'partial/editOrNew.html',
                controller: NewCtrl
            })
            .otherwise({
                redirectTo: '/main'
            })
    }]);