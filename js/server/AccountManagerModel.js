/* 
 * Copyright (c) 2016 Varun Chandola <chandola@buffalo.edu>.
 * Released under the MIT License
 * http://www.opensource.org/licenses/mit-license.php
 */

/**
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
       * @constructor
       */
      function AccountManagerModel(logger) {
	var self = this;
	self.userName = ko.observable();
	self.submittedAnalysisJobs = ko.observableArray([]);
	self.submittedUploadJobs = ko.observableArray([]);
	self.webGlobeServer = constants.WEBGLOBE_SERVER;
	$.ajax({
	  url: self.webGlobeServer + 'GetAccountInfo',
	  cache: false,
	  type: 'POST',
	  contentType: 'application/json; charset=utf-8',
	  data: JSON.stringify({
	    username: constants.WEBGLOBE_USER
	  }),
	  success: function (data) { 
	    self.userName(data.userInfo.userName);
	  }
	}).fail(function (xhr, textStatus, err) {
	  logger.log("Error getting account information from the server","alert-danger");
	  return 'NA';
	});

	self.getSubmittedAnalysisJobs = function(){
	  $.ajax({
	    url: self.webGlobeServer + 'GetSubmittedAnalysisJobsInfo',
	    cache: false,
	    type: 'POST',
	    contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify({
	      username: constants.WEBGLOBE_USER
	    }),
	    success: function (data) { 
	      self.submittedAnalysisJobs.removeAll();
	      var retnum = Object.keys(data).length;
	      for(var i = 0; i < retnum; i++){
		self.submittedAnalysisJobs.push({'index':data[i].id, 'name': data[i].name, 'field': data[i].field, 'submission_time': data[i].submission_time, 'finish_time': data[i].finish_time, 'analysis': data[i].analysis, 'status': data[i].status, 'result_loc': data[i].result_loc}); 
	      }
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log("Error getting analysis job information from the server","alert-danger");
	    return 'NA';
	  });
	}
	self.getSubmittedAnalysisJobs();

	self.getSubmittedUploadJobs = function(){
	  $.ajax({
	    url: self.webGlobeServer + 'GetSubmittedUploadJobsInfo',
	    cache: false,
	    type: 'POST',
	    contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify({
	      username: constants.WEBGLOBE_USER
	    }),
	    success: function (data) { 
	      self.submittedUploadJobs.removeAll();
	      var retnum = Object.keys(data).length;
	      for(var i = 0; i < retnum; i++){
		self.submittedUploadJobs.push({'index':data[i].id, 'dataset_name': data[i].dataset_name, 'submission_time': data[i].submission_time, 'finish_time': data[i].finish_time,'status': data[i].status}); 
	      }
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log("Error getting upload job information from the server","alert-danger");
	    return 'NA';
	  });
	}
	self.getSubmittedUploadJobs();
	self.getSubmittedAnalysisJobInfo = function(result_loc){
	  logger.log("The results file was created at <br/><span style=\"font-size:10px;\">"+result_loc+"</span>","alert-info")
	}
      }
      return AccountManagerModel;
    }
);
