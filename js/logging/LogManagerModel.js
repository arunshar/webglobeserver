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
 * @returns {LayerManagerModel}
 */
define(['knockout',
    'jquery',
    'model/Constants'],
    function (ko, $, constants) {

      /**
       * @constructor
       */
      function LogManagerModel(type) {
	var self = this;
	self.currentClass = "alert-info";
	self.log = function (msg,type) {
	  var header = "<a class=\"close\" data-hide-closest=\".alert\">×</a>\n";
	  $("#infoAlert").removeClass(self.currentClass);
	  $("#infoAlert").addClass(type);

	  self.currentClass = type;
	  $("#infoAlert").show().html(header+msg);
	}
      }
      return LogManagerModel;
    }
    );
