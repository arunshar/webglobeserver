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
 * @returns {AccountManagerModel}
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
      function AccountManagerModel(logger) {
	var self = this;
	self.userName = ko.observable();
	self.submittedJobs = ko.observableArray([]);
	self.webGlobeServer = constants.WEBGLOBE_SERVER;
	$.ajax({
	  url: self.webGlobeServer + 'GetAccountInfo',
	  cache: false,
	  type: 'POST',
	  contentType: 'application/json; charset=utf-8',
	  success: function (dataJSON) { 
	    self.userName(dataJSON.userInfo.userName);

	  }
	}).fail(function (xhr, textStatus, err) {
	  logger.log("Error getting account information from the server","alert-danger");
	  return 'NA';
	});

	self.getSubmittedJobs = function(){
	  $.ajax({
	    url: self.webGlobeServer + 'GetSubmittedJobsInfo',
	    cache: false,
	    type: 'POST',
	    contentType: 'application/json; charset=utf-8',
	    success: function (dataJSON) { 
	      self.submittedJobs.removeAll();
	      var retnum = Object.keys(dataJSON).length;
	      for(var i = 0; i < retnum; i++){
		self.submittedJobs.push({'index':i+1, 'name': dataJSON[i].name, 'field': dataJSON[i].field, 'submission_time': dataJSON[i].submission_time, 'analysis': dataJSON[i].analysis, 'status': dataJSON[i].status}); 
	      }
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log("Error getting job information from the server","alert-danger");
	    return 'NA';
	  });
	}
	self.getSubmittedJobs();
      }
      return AccountManagerModel;
    }
);
