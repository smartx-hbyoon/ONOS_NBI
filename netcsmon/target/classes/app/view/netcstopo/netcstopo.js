// js for sample app view
(function () {
    'use strict';

    angular.module('ovNetcstopo', [])
        .controller('OvNetcstopoCtrl',
        ['$log', '$scope', 'TableBuilderService',

            function ($log, $scope, tbs) {
            	tbs.buildTable({
            		scope:$scope,
            		tag:'netcstopo'
            	});

                $log.log('OvNetcstopoCtrl has been created');
            }]);
}());