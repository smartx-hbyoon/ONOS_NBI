// js for sample app view
(function () {
    'use strict';

    angular.module('ovNetcsmon', [])
        .controller('OvNetcsmonCtrl',
        ['$log', '$scope', 'TableBuilderService',

            function ($log, $scope, tbs) {
            	tbs.buildTable({
            		scope:$scope,
            		tag:'netcsmon'
            	});

                $log.log('OvNetcsmonCtrl has been created');
            }]);
}());