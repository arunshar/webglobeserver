/* 
 * Copyright (c) 2016 Varun Chandola <chandola@buffalo.edu>.
 * Released under the MIT License
 * http://www.opensource.org/licenses/mit-license.php
 */

/**
 * Dataset View Model
 * 
 * @param {type}
 *            ko
 * @param {type} $
 * @returns {DatasetsViewModel}
 */
define(
    [ 'knockout', 'jquery', 'model/Constants' ],
    function(ko, $, constants) {

      /**
       * The datasets viewing model
       * 
       * @param {Globe}
       *            globe The globe that provides the supported
       *            projections
       * @param {LogViewModel}
       *            logger The logging module for console based outputs
       * @constructor
       */
      function DatasetsViewModel(globe, logger) {
	var self = this;
	layerManager = globe.layerManager;
	self.availableDatasets = ko.observableArray([]);
	self.selectedDataset = ko.observable();
	self.selectedDatasetAnalysis = ko.observable();
	self.fields = ko.observableArray([]);
	self.analysisFields = ko.observableArray([]);
	self.analysisMethods = ko.observableArray([ "Change Detection" ]);
	self.infoActive = false;
	self.openActive = false;
	self.openAnalysis = false;
	self.downloading = false;
	self.submitting = false;

	/*
	 * Populate available datasets from the database
	 */

	self.populateDatasets = function() {
	  self.availableDatasets.removeAll();
	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  $
	    .ajax(
		{
		  url : webGlobeServer
		    + 'GetDatasetDetails',
		  cache : false,
		  type : 'POST',
		  contentType : 'application/json; charset=utf-8',
		  success : function(dataJSON) {
		    for (var i = 0; i < dataJSON.count.value; i++) {
		      var datasetInfo = 'dataset' + i;
		      var id = dataJSON[datasetInfo].id;
		      var url = dataJSON[datasetInfo].url;
		      var name = dataJSON[datasetInfo].name;
		      if (dataJSON[datasetInfo].available == 'all')
			user_data = false;
		      else
			user_data = true;

		      var info = dataJSON[datasetInfo].info;
		      var info_url = dataJSON[datasetInfo].info_url;
		      var fieldcount = dataJSON[datasetInfo].fieldcount;
		      var fields = [];
		      for (var j = 0; j < fieldcount; j++) {
			var fieldInfo = 'field' + j;
			var fieldName = dataJSON[datasetInfo][fieldInfo];
			fields.push(fieldName);
		      }
		      var datasetLayer = layerManager.createDatasetLayer(name);
		      var n = 25
		      var shortName = (name.length<n) ? ' '.repeat(n-name.length)+name : name.substring(0,n-1);

		      self.availableDatasets.push({
			'id' : id,
			'url' : url,
			'name' : name,
			'shortName' : shortName,
			'user_data' : user_data,
			'info' : info,
			'info_url' : info_url,
			'fields' : fields,
			'enabled' : false,
			'layer' : datasetLayer,
			'images': ko.observableArray(),
			'variableAddress' : "",
			'loaded' : false
		      });
		    }

		  }
		})
	  .fail(
	      function(xhr, textStatus, err) {
		logger
		  .log(
		      "Error getting dataset information from the server",
		      "alert-danger");
		return 'NA';
	      });
	}


	self.showDatasetPanel = function(dataset) {
	  if (!self.openActive) {
	    self.selectedDataset = dataset;
	    for (var i = 0; i < dataset.fields.length; i++) {
	      self.fields.push(dataset.fields[i]);
	    }
	    $("#datasetOpenPanel").show();
	    $("#selectTimePanel").hide();
	    self.openActive = true;
	  } else {
	    if(self.selectedDataset.layer.enabled){
	      self.selectedDataset.layer.enabled = false;
	      globe.redraw();
	    }
	    self.selectedDataset = null;
	    self.fields.removeAll();

	    $("#datasetOpenPanel").hide();
	    self.openActive = false;
	  }
	}

	self.showAnalysisPanel = function(dataset) {
	  if (!self.openAnalysis) {
	    self.selectedDatasetAnalysis = dataset;
	    for (var i = 0; i < dataset.fields.length; i++) {
	      self.analysisFields.push(dataset.fields[i]);
	    }
	    $("#analysisPanel").show();
	    self.openAnalysis = true;
	  } else {
	    self.selectedDatasetAnalysis = null;
	    self.analysisFields.removeAll();
	    $("#analysisPanel").hide();
	    self.openAnalysis = false;
	  }
	}

	self.displayTimePanel = function() {
	  var id = self.selectedDataset.id;
	  var fieldname = $("#fieldSelect :selected").text();
	  self.selectedDataset.fieldname = fieldname;
	  var webGlobeServer = constants.WEBGLOBE_SERVER;

	  $.ajax({
	    url: webGlobeServer + 'LoadNetcdfDataset',
	    cache: false,
	    type: 'POST',
	    contentType: 'application/json; charset=utf-8',
	    data: JSON.stringify({
	      id: id,
	      fieldname: fieldname
	    }),
	    success: function (dataJSON) {
	      $('#load-images').show();
	      $('#load-start-date').attr({
		"max" : dataJSON.variable.imageMaxDate,
		"min" : dataJSON.variable.imageMinDate
	      });
	      $('#load-start-date').val(dataJSON.variable.imageMinDate);

	      $('#load-end-date').attr({
		"max" : dataJSON.variable.imageMaxDate,
		"min" : dataJSON.variable.imageMinDate
	      });
	      $('#load-end-date').val(dataJSON.variable.imageMaxDate);
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log(err,"alert-danger");
	  });

	  $("#selectTimePanel").show();
	}

	self.loadImages = function() {
	  self.selectedDataset.layer.empty();
	  var webGlobeServer = constants.WEBGLOBE_SERVER;

	  var id = self.selectedDataset.id;
	  var fieldname = $("#fieldSelect :selected").text();

	  $.ajax({
	    url: webGlobeServer + 'LoadImages',
	    cache: false,
	    type: 'POST',
	    data: {
	      datasetId: id,
	      fieldname: fieldname,
	      from: $('#load-start-date').val(),
	      to: $('#load-end-date').val()
	    },
	    success: function (data) {
	      var imageUrls = data.imageUrls;
	      var imageDates = data.imageDates;
	      self.selectedDataset.layer.populate(imageUrls,imageDates);
	      self.selectedDataset.layer.enabled = true;
	      self.selectedDataset.loaded = true;
	      $('#load-images').hide();
	      logger.log("Succesfully loaded images","alert-info");
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log(err,"alert-danger");
	  });                            

	}

	self.isNotLoaded = function(){
	  if(self.selectedDataset != null){
	    return !self.selectedDataset.loaded;
	  }
	  return true;
	}
	self.showNext = function() {
	  self.selectedDataset.layer.showNext();
	  globe.redraw();					
	}

	self.showPrevious = function() {
	  self.selectedDataset.layer.showPrevious();
	  globe.redraw();					
	}

	self.showFirst = function() {
	  self.selectedDataset.layer.showFirst();
	  globe.redraw();
	}

	self.showLast = function() {
	  self.selectedDataset.layer.showLast();
	  globe.redraw();
	}
	self.analyzeDataset = function() {
	  if (self.submitting) {
	    logger
	      .log(
		  "Please wait till the current job is submitted.",
		  "alert-warning")
	      return;
	  }
	  var fieldname = $("#fieldAnalysisSelect :selected").text();
	  var analysisname = $("#analysisSelect :selected").text();
	  var analysisoutputname = $("#analysisOutputName").val();
	  var url = self.selectedDatasetAnalysis.url;
	  logger.log("Submitting " + analysisname + " <a href=\""
	      + url + "\">" + self.selectedDatasetAnalysis.name
	      + ":" + fieldname + "</a>", "alert-info");

	  var webGlobeServer = constants.WEBGLOBE_SERVER;

	  $.ajax({
	    url: webGlobeServer + 'RunJob',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      datasetid: self.selectedDatasetAnalysis.id,
	      datasetname: self.selectedDatasetAnalysis.name,
	      url: url,
	      analysisname: analysisname,
	      fieldname: fieldname,
	      analysisoutputname: analysisoutputname
	    }),
	    success: function (data) {
		var message = data.message;
		logger.log(message,'info');
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log(err,"alert-danger");
	  });                            

	  //finish submitting 
	}

	self.showInfo = function(dataset) {
	  if (!self.infoActive) {
	    $("#datasetInfo").show().html('<h4><a href="'+dataset.info_url+'">'+dataset.name+'</a></h4>\n<hr/>'+dataset.info);
	    self.infoActive = true;
	  } else {
	    $("#datasetInfo").hide();
	    self.infoActive = false;
	  }
	}
	self.uploadData = function(){
	  var hdfsURL = $('#hdfsURL').val();
	  var dataName = $('#dataName').val();
	  var dataInfo = $('#dataInfo').val();
	  var dataInfoURL = $('#dataInfoURL').val();
  	  if (hdfsURL == '' || dataName == '' || dataInfo == ''|| dataInfoURL == ''){
  	    logger.log('Insufficient arguments.','alert-danger');
	    return;
  	  }	
	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  self.uploading = true;

	  $.ajax({
	    url: webGlobeServer + 'UploadDataset',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      hdfsURL: hdfsURL,
	      dataName: dataName,
	      dataInfo: dataInfo,
	      dataInfoURL: dataInfoURL
	    }),
	    success: function (data) {
		var message = data.message;
		logger.log(message,'info');

	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log(err,"alert-danger");
	  });                            
	}
	self.populateDatasets();

      }
      return DatasetsViewModel;
    });
