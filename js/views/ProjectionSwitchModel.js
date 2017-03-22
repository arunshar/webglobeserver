/* 
 * Copyright (c) 2016 Varun Chandola <chandola@buffalo.edu>.
 * Released under the MIT License
 * http://www.opensource.org/licenses/mit-license.php
 */

/**
 * Projection content module
 *
 * @param {type} ko
 * @param {type} $
 * @returns {ProjectionSwitchModel}
 */
define(['knockout',
        'jquery',
        'model/Constants'],
    function (ko, $, constants) {

        /**
         * The projection switching model
         * @param {Globe} globe The globe that provides the supported projections
         * @constructor
         */
        function ProjectionSwitchModel(globe) {
            var self = this;

            // Track the current projection
            self.currentProjection = ko.observable('3D');

            // Projection click handler
            self.changeProjection = function () {
		if (self.currentProjection() == constants.PROJECTION_NAME_3D) {
		  self.currentProjection(constants.PROJECTION_NAME_EQ_RECT);
		  globe.setProjection(constants.PROJECTION_NAME_EQ_RECT);
		} else {
		  self.currentProjection(constants.PROJECTION_NAME_3D);
		  globe.setProjection(constants.PROJECTION_NAME_3D);
		}
            };
        }

        return ProjectionSwitchModel;
    }
);
