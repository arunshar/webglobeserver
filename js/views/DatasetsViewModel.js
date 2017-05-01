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
	self.analysisMethods = ko.observableArray([ "Change Detection", "Anomaly Detection","Correlation Analysis" ]);
	self.availableColormaps = ko.observableArray(["Linear","Log","Discrete"]);
	//fields that need to be reset
	self.availableDatasets = ko.observableArray([]);
	self.selectedDataset = ko.observable();
	self.fields = ko.observableArray([]);
	//fields that are used to check the status
	self.infoActive = false;
	self.openActive = false;
	self.openAnalysis = false;
	self.downloading = false;
	self.submitting = false;
	self.uploading = false;
	self.probing = false;
	self.interval = null;

	/* register listeners */
	var handleClick = function (recognizer) {
	  // Obtain the event location.
	  var x = recognizer.clientX;
	  var y = recognizer.clientY;

	  var pickList = globe.wwd.pick(globe.wwd.canvasCoordinates(x, y));
	  var position = pickList.objects[0].position;
	  if(self.selectedDataset != null && self.selectedDataset.loaded == true){
	    self.plotChart(position.latitude,position.longitude);
	  }else{
	    globe.wwd.goTo(new WorldWind.Location(position.latitude, position.longitude));
	  }
	};

	// Listen for mouse clicks.
	self.clickRecognizer = new WorldWind.ClickRecognizer(globe.wwd, handleClick);

	// Listen for taps on mobile devices.
	self.tapRecognizer = new WorldWind.TapRecognizer(globe.wwd, handleClick);

	self.resetDatasets = function(){
	  self.hideTabs();
	  if((self.selectedDataset != null) && (!self.isNotLoaded())){
	    self.selectedDataset.layer.removeAllRenderables();
	    globe.redraw();
	    //next two steps are needed to handle memory issues
	    self.selectedDataset.layer.empty();
	    self.selectedDataset.loaded = false;
	  }
	  $('#dataset-info-tab').parent().addClass('active').siblings().removeClass('active');
	  $('#dataset-info-tab').attr('data-toggle', '');
	  $('#dataset-animate-tab').attr('data-toggle', '');
	  $('#dataset-analyze-tab').attr('data-toggle', '');
	  $('#dataset-charts-tab').attr('data-toggle', '');
	  self.selectedDataset = ko.observable();
	  self.fields.removeAll();
	}

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
		      var is_analyzable = true;
		      if (dataJSON[datasetInfo].is_analyzable == 0)
			is_analyzable = false;
		      var fieldcount = dataJSON[datasetInfo].fieldcount;
		      var fields = [];
		      for (var j = 0; j < fieldcount; j++) {
			
			var fieldInfo = 'field' + j;
			var fieldName = dataJSON[datasetInfo][fieldInfo];
			var minDate = dataJSON[datasetInfo]['field'+j+'_mindate'];
			var maxDate = dataJSON[datasetInfo]['field'+j+'_maxdate'];
			var field = {'name': fieldName, 'mindate': minDate, 'maxdate': maxDate};
			fields.push(field);
		      }
		      var datasetLayer = layerManager.createDatasetLayer(name);

		      self.availableDatasets.push({
			'index': i,
			'id' : id,
			'url' : url,
			'name' : name,
			'user_data' : user_data,
			'info' : info,
			'info_url' : info_url,
			'is_analyzable': is_analyzable,
			'fields' : fields,
			'enabled' : false,
			'layer' : datasetLayer,
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

	/*
	 * Handler for selection of dataset in the dataset panel
	 */
	self.datasetSelected = function(){
	  var index  = $("#datasetSelect :selected").attr('value');
	  self.resetDatasets();
	  if(self.availableDatasets()[index] != undefined){
	    $('#dataset-info-tab').parent().addClass('active').siblings().removeClass('active');
	    $('#dataset-info-tab').attr('data-toggle', 'tab');
	    $('#dataset-animate-tab').attr('data-toggle', 'tab');
	    $('#dataset-analyze-tab').attr('data-toggle', 'tab');
	    $('#dataset-charts-tab').attr('data-toggle', 'tab');
	    self.selectedDataset = self.availableDatasets()[index];  
	    var dataset = self.selectedDataset; 
	    for (var i = 0; i < dataset.fields.length; i++) {
	      self.fields.push(dataset.fields[i].name);
	    }
	    //show time data
	    $('#load-start-date').attr({
	      "max" : dataset.fields[0].maxdate,
	      "min" : dataset.fields[0].mindate 
	    });
	    $('#load-start-date').val(dataset.fields[0].mindate);

	    $('#load-end-date').attr({
	      "max" : dataset.fields[0].maxdate, 
	      "min" : dataset.fields[0].mindate 
	    });
	    $('#load-end-date').val(dataset.fields[0].maxdate);
	    self.showTab('info');
	  }
	}
	self.hideTabs = function(){
	  $('#dataset-info').hide();
	  $('#dataset-animate').hide();
	  $('#dataset-analyze').hide();
	  $('#dataset-charts').hide();
	}

	self.showTab = function(tabname){
	  
	  var index  = $("#datasetSelect :selected").attr('value');
	  if(self.availableDatasets()[index] != undefined){
	    $('#dataset-'+tabname).siblings().hide();
	    $('#dataset-'+tabname).show();
	    if(tabname == 'info'){
	      $('#dataset-info-span').text(self.selectedDataset.info);
	    }
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

	self.loadImages = function() {
	  if(self.isNotLoaded()){
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
		logger.log("Succesfully loaded images","alert-info");
	      }
	    }).fail(function (xhr, textStatus, err) {
	      logger.log(err,"alert-danger");
	    });
	  }	  
	}

	self.isNotLoaded = function(){
	  if(self.selectedDataset != null && self.selectedDataset != undefined){
	    return !self.selectedDataset.loaded;
	  }
	  return true;
	}
	self.showNext = function() {
	  if(!self.isNotLoaded()){
	    self.selectedDataset.layer.showNext();
	    globe.redraw();		
	  }	  
	}

	self.showPrevious = function() {
	  if(!self.isNotLoaded()){
	    self.selectedDataset.layer.showPrevious();
	    globe.redraw();		
	  }	  
	}

	self.showFirst = function() {
	  if(!self.isNotLoaded()){
	    self.selectedDataset.layer.showFirst();
	    globe.redraw();
	  }
	}

	self.showLast = function() {
	  if(!self.isNotLoaded()){
	    self.selectedDataset.layer.showLast();
	    globe.redraw();
	  }
	}

	self.play = function() {
	  if(!self.isNotLoaded()){
	    if(self.interval != null){
	      window.clearInterval(self.interval);
	    }
	    self.interval = window.setInterval(function () {
	      self.selectedDataset.layer.showNext();
	      globe.redraw();
	    }, 200);
	  }
	}

	self.stop = function() {
	  if(self.interval != null){
	    window.clearInterval(self.interval);
	  }
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
	  if(analysisname != "Change Detection"){
	    logger.log("Only Gaussian Process change detection is currently supported.","alert-warning");
	    return;
	  }
	  var analysisoutputname = $("#analysisOutputName").val();
	  if(analysisoutputname == ''){
	    logger.log("Missing input arguments", "alert-warning");
	    return;
	  }
	  var url = self.selectedDataset.url;
	  logger.log("Submitting " + analysisname + " <a href=\""
	      + url + "\">" + self.selectedDataset.name
	      + ":" + fieldname + "</a>", "alert-info");

	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  
	  $.ajax({
	    url: webGlobeServer + 'RunJob',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      datasetid: self.selectedDataset.id,
	      datasetname: self.selectedDataset.name,
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
	self.probeData = function(){
	  if(self.probing){
	    logger.log('Please wait until the current probe is finished.','alert-danger');
	    return;
	  }
	  var url= $('#url').val();
	  if (url == ''){ 
	    logger.log('Insufficient arguments.','alert-danger');
	    return;
	  }	

	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  self.probing = true;
	  $("#probe-spinner").show();
	  //probe the data set
	  $.ajax({
	    url: webGlobeServer + 'ProbeDataset',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      url: url,
	    }),
	    success: function (data) {
	      if(parseInt(data.status) == -1){
		logger.log("Error loading data set","alert-danger");
	      }else{
		var numvars = data.numvars;
		var name = data.name;
		var info = data.info;
		var infoURL = data.infoURL;
		logger.log('<h4>'+name+'</h4><hr/>Number of Variables = '+numvars,'alert-info');
		//prepopulate fields
		$('#upload-dataName').val(name);
		$('#upload-dataInfo').val(info);
		$('#upload-dataInfoURL').val(infoURL);
	      }
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log("Error loading data set","alert-danger");
	  });                            
	  self.probing = false;
	  $("#probe-spinner").hide();
	}

	self.uploadData = function(){
	  var url = $('#url').val();
	  var dataName = $('#upload-dataName').val();
	  var dataInfo = $('#upload-dataInfo').val();
	  var dataInfoURL = $('#upload-dataInfoURL').val();
	  var visualizationOnly = 0;
	  if ($('#upload-visualization-only').is(":checked"))
	  {
	    visualizationOnly = 1;
	  }
	  var selectedColormap = $('#upload-select-colormap').val();

	  if (url == '' || dataName == '' || dataInfo == ''){
	    logger.log('URL, name, and information are required arguments.','alert-danger');
	    return;
	  }	
	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  self.uploading = true;

	  $.ajax({
	    url: webGlobeServer + 'UploadDataset',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      url: url,
	      dataName: dataName,
	      dataInfo: dataInfo,
	      dataInfoURL: dataInfoURL,
	      visualizationOnly: visualizationOnly,
	      selectedColormap: selectedColormap
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

	self.plotChart = function(lat,lon){
	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  var datasetid = self.selectedDataset.id;
	  var fieldname = self.selectedDataset.fieldname;
	  if(lon < 0){
	    lon = 360 + lon;
	  }
	  //get data
	  $.ajax({
	    url: webGlobeServer + 'GetTimeSeriesData',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      datasetid: datasetid,
	      fieldname: fieldname,
	      lat: lat,
	      lon: lon
	    }),
	    success: function (dataJSON) {
	      var xdata = [];
	      var ydata = [];
	      var retnum = Object.keys(dataJSON).length;
	      if (retnum == 0){
		logger.log("No data returned for the selected location","alert-danger");
		return;
	      }
	      
	      var ylabel = dataJSON[0].unitString;
	      for(var i = 0; i < retnum; i++){
		xdata.push(dataJSON[i].date);
		ydata.push(dataJSON[i].value+0.0);
	      }
	      var xlabel = "Time";
	      var layout = {
		title: self.selectedDataset.fieldname,
		xaxis: {title: xlabel},
		yaxis: {title: ylabel},
		margin: {t:0}
	      };
	      Plotly.plot( innerChart, [{
	      	x: xdata,y: ydata }], layout );
	    }
	  }).fail(function (xhr, textStatus, err) {
	    logger.log("No data returned for the selected location","alert-danger");
	  });
	}
	self.clearChart = function(){
	  Plotly.purge("innerChart");
	}
	self.hideTabs();
      }

      return DatasetsViewModel;
    });
