// js for sample app view
(function () {
    'use strict';

    angular.module('ovFwdtraffic', [])
        .controller('OvFwdtrafficCtrl',
        ['$log', '$scope', 'TableBuilderService',

            function ($log, $scope, tbs) {
               // var self = this;

               // self.msg = 'A message from our app...';

               // $log.log('OvSampleCtrl has been created');
               tbs.buildTable({ scope:$scope, tag:'fwdtraffic'})
            }]);
}());
