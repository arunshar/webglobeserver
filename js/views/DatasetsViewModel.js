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
	self.years = ko.observableArray([]);
	self.plotChartSwitch = false;
	//fields that are used to check the status
	self.submitting = false;
	self.uploading = false;
	self.probing = false;
	self.interval = null;
	self.blurvalue = ko.observable('0.5');
	self.radiusvalue = ko.observable('2');
	self.currentLatitude = ko.observable('Select location on the globe');
	self.currentLongitude = ko.observable('Select location on the globe');
	/* register listeners */
	var handleClick = function (recognizer) {
	  // Obtain the event location.
	  var x = recognizer.clientX;
	  var y = recognizer.clientY;

	  var pickList = globe.wwd.pick(globe.wwd.canvasCoordinates(x, y));
	  var position = pickList.objects[0].position;
	  self.currentLatitude(position.latitude.toFixed(2));
	  self.currentLongitude(position.longitude.toFixed(2));
	  if(self.selectedDataset != null && self.plotChartSwitch){
	    if(self.selectedDataset.loaded) {
	      reverseGeocode(position.latitude,position.longitude);
	    }
	    //alert(position.latitude+","+position.longitude);
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
	  self.years.removeAll();
	  self.clearChart();
	  self.plotChartSwitch = false;
	  $('#togglePlottingOn').hide();
	  $('#togglePlottingOff').show();
	}

	/*
	 * Populate available datasets from the database
	 */
	self.populateDatasets = function() {
	  self.availableDatasets.removeAll();
	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  $.ajax(
	      {
		url : webGlobeServer
		  + 'GetDatasetDetails',
		cache : false,
		type : 'POST',
		contentType : 'application/json; charset=utf-8',
		data: JSON.stringify({
		  username: constants.WEBGLOBE_USER
		}),
		success : function(dataJSON) {
		  for (var i = 0; i < dataJSON.count.value; i++) {
		    var datasetInfo = 'dataset' + i;
		    var id = dataJSON[datasetInfo].id;
		    var name = dataJSON[datasetInfo].name;

		    var info = dataJSON[datasetInfo].info;
		    var info_url = dataJSON[datasetInfo].info_url;
		    var minDate = dataJSON[datasetInfo]['mindate'];
		    var maxDate = dataJSON[datasetInfo]['maxdate'];
		    var minYear = minDate.substring(0,4);
		    var maxYear = maxDate.substring(0,4);
		    var years = [];
		    for (var j = minYear; j <= maxYear; j++){
		      years.push(j);
		    }
		    
		    var maxDate = dataJSON[datasetInfo]['maxdate'];
		    var fieldcount = dataJSON[datasetInfo].fieldcount;
		    var fields = [];
		    for (var j = 0; j < fieldcount; j++) {
		      var fieldInfo = 'field' + j;
		      var fieldName = dataJSON[datasetInfo][fieldInfo];
		      var field = {'name': fieldName, 'mindate': minDate, 'maxdate': maxDate};
		      fields.push(field);
		    }
		    var datasetLayer = layerManager.createDatasetLayer(name);

		    self.availableDatasets.push({
		      'index': i,
		      'id' : id,
		      'url' : url,
		      'name' : name,
		      'info' : info,
		      'info_url' : info_url,
		      'fields' : fields,
		      'years' : years,
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
	    for (var i = 0; i < dataset.years.length; i++) {
	      self.years.push(dataset.years[i]);
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
	    //do not activate analyze tab for non analyzable 
	    $('#dataset-'+tabname).siblings().hide();
	    $('#dataset-'+tabname).show();
	    if(tabname == 'info'){
	      $('#dataset-info-span').text(self.selectedDataset.info);
	    }
	  }
	}

	self.loadData = function() {
	  if(self.isNotLoaded()){
	    var webGlobeServer = constants.WEBGLOBE_SERVER;

	    var id = self.selectedDataset.id;
	    var fieldname = $("#fieldSelect :selected").text();
	    $("#load-spinner").show();
	    $("#load-data").attr("disabled",true);

	    $.ajax({
	      url: webGlobeServer + 'LoadData',
	      cache: false,
	      type: 'POST',
	      data: {
		username: constants.WEBGLOBE_USER,
		datasetId: id,
		fieldname: fieldname,
		from: $('#load-start-date').val(),
		to: $('#load-end-date').val()
	      },
	      success: function (data) {
		self.selectedDataset.loaded = true;
		self.selectedDataset.data = data.data;
		self.selectedDataset.dates = data.dates;
		self.selectedDataset.bounds = data.bounds;
		self.selectedDataset.shape = data.shape;
		self.selectedDataset.limits = data.limits;

		//var imageUrls = data.imageUrls;
		//var imageDates = data.imageDates;
		//self.selectedDataset.layer.populate(imageUrls,imageDates);
		self.updateHeatmap();
		logger.log("Succesfully loaded data","alert-info");
		$("#load-spinner").hide();
		$("#load-data").attr("disabled",false);
	      }
	    }).fail(function (xhr, textStatus, err) {
	      logger.log("Error loading data","alert-danger");
	      $("#load-spinner").hide();
	      $("#load-data").attr("disabled",false);
	    });
	  }	  
	}

	self.updateHeatmap = function(){
	  self.selectedDataset.layer.populateJSON(self.selectedDataset.bounds,self.selectedDataset.data,self.selectedDataset.shape,self.selectedDataset.dates,self.selectedDataset.limits,self.blurvalue(),self.radiusvalue());
	  self.selectedDataset.layer.enabled = true;
	  globe.redraw();
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
	  }else{
	    logger.log("Load data from the info tab first.","alert-danger");
	  }	  
	}

	self.showPrevious = function() {
	  if(!self.isNotLoaded()){
	    self.selectedDataset.layer.showPrevious();
	    globe.redraw();		
	  }else{
	    logger.log("Load data from the info tab first.","alert-danger");
	  }	  
	}

	self.showFirst = function() {
	  if(!self.isNotLoaded()){
	    self.selectedDataset.layer.showFirst();
	    globe.redraw();
	  }else{
	    logger.log("Load data from the info tab first.","alert-danger");
	  }
	}

	self.showLast = function() {
	  if(!self.isNotLoaded()){
	    self.selectedDataset.layer.showLast();
	    globe.redraw();
	  }else{
	    logger.log("Load data from the info tab first.","alert-danger");
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
	    }, 300);
	  }else{
	    logger.log("Load data from the info tab first.","alert-danger");
	  }
	}

	self.stop = function() {
	  if(self.interval != null){
	    window.clearInterval(self.interval);
	  }
	}

	self.analyzeDataset = function(){
	  var analysisname = $("#analysisSelect :selected").text();
	  var analysisname1 = analysisname.replace(/\s+/g, '').toLowerCase();
	  $('#'+analysisname1+'modal').modal();
	  $('.modal-backdrop').appendTo('#dataset-analyze');
	  $('body').removeClass();
	}

	self.analyzeDatasetSubmit = function() {
	  if (self.submitting) {
	    logger
	      .log(
		  "Please wait till the current job is submitted.",
		  "alert-warning")
	      return;
	  }

	  var fieldname = $("#fieldAnalysisSelect :selected").text();
	  var analysisname = $("#analysisSelect :selected").text();
	  var analysisname1 = analysisname.replace(/\s+/g, '').toLowerCase();
	  var outputname = analysisname1+'OutputName';
	  var analysisoutputname = $("#"+outputname).val();
	  if(analysisoutputname == ''){
	    logger.log("Missing input arguments", "alert-warning");
	    return;
	  }
	  var args = '';
	   
	  if(analysisname == "Correlation Analysis"){
	    var year = $('#'+analysisname1+'Year :selected').text();
	    var selectedLat = self.currentLatitude();
	    var selectedLon = self.currentLongitude();
	    args = year+';'+selectedLat+';'+selectedLon;
	  }
	  var webGlobeServer = constants.WEBGLOBE_SERVER;

	  var datasetid = self.selectedDataset.id;
	  logger.log("Submitting " + analysisname + " " + self.selectedDataset.name
	      + ":" + fieldname, "alert-info");

	  $.ajax({
	     url: webGlobeServer + 'RunJobSerial',
	     cache: false,
	     type: 'POST',
	     data: JSON.stringify({
	       username: constants.WEBGLOBE_USER,
	       datasetid: self.selectedDataset.id,
	       analysisname: analysisname,
	       fieldname: fieldname,
	       args: args,
	       analysisoutputname: analysisoutputname
	     }),
	     success: function (data) {
	       var message = data.message;
	       logger.log(message,'alert-info');
	     }
	   }).fail(function (xhr, textStatus, err) {
	     logger.log(err,"alert-danger");
	   });

	  //finish submitting 
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
	  $("#probe-data").attr("disabled",true);
	  $("#upload-data").attr("disabled",true);
	  //probe the data set
	  $.ajax({
	    url: webGlobeServer + 'ProbeDataset',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      username: constants.WEBGLOBE_USER,
	      url: url,
	    }),
	    success: function (data) {
	      $("#probe-spinner").hide();
	      $("#probe-data").attr("disabled",false);
	      $("#upload-data").attr("disabled",false);
	      if(parseInt(data.status) == -1){
		logger.log("Error loading data set","alert-danger");
	      }else{
		var numvars = data.numvars;
		var name = data.name;
		var info = data.info;
		var infoURL = data.infoURL;
		//prepopulate fields
		$('#upload-dataName').val(name);
		$('#upload-dataInfo').val(info);
		$('#upload-dataInfoURL').val(infoURL);
	      }
	    }
	  }).fail(function (xhr, textStatus, err) {
	    $("#probe-spinner").hide();
	    $("#probe-data").attr("disabled",false);
	    $("#upload-data").attr("disabled",false);

	    logger.log("Error loading data set","alert-danger");
	  });                            
	  self.probing = false;
	}

	self.uploadData = function(){
	  var url = $('#url').val();
	  var dataName = $('#upload-dataName').val();
	  var dataInfo = $('#upload-dataInfo').val();
	  var dataInfoURL = $('#upload-dataInfoURL').val();
	  var selectedColormap = $('#upload-select-colormap').val();
	  var stride = $('#upload-stride').val();

	  if (url == '' || dataName == '' || dataInfo == '' || isNaN(stride)){
	    logger.log('URL, name, stride, and information are required arguments.','alert-danger');
	    return;
	  }	
	  if (dataName.length > 64){
	    logger.log('Dataset name should be 64 characters or less.','alert-danger');
	    return;
	  }	
	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  self.uploading = true;

	  $.ajax({
	    url: webGlobeServer + 'UploadDataset',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      username: constants.WEBGLOBE_USER,
	      url: url,
	      dataName: dataName,
	      dataInfo: dataInfo,
	      dataInfoURL: dataInfoURL,
	      stride: stride
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
	  $("#plot-chart-spinner").show();
	  if(self.selectedDataset.loaded){
	    //find index for the lat 
	    var latWidth = (self.selectedDataset.bounds[1] - self.selectedDataset.bounds[0])/self.selectedDataset.shape[0];
	    var latIndex = Math.floor((Number(lat) - self.selectedDataset.bounds[0])/latWidth);
	    var lonWidth = (self.selectedDataset.bounds[3] - self.selectedDataset.bounds[2])/self.selectedDataset.shape[1];
	    var lonIndex = Math.floor((Number(lon) - self.selectedDataset.bounds[2])/lonWidth);
	    //get data
	    var xdata = self.selectedDataset.dates;
	    var ydata = [];
	    var start = latIndex*self.selectedDataset.shape[1]*self.selectedDataset.shape[2]+lonIndex*self.selectedDataset.shape[2]; 
	    for(var i = 0; i < self.selectedDataset.shape[2]; i++){
	      ydata.push(self.selectedDataset.data[start+i]);
	    }

	    var ylabel = "Observation";
	    var xlabel = "Time";
	    var layout = {
	      title: self.selectedDataset.fieldname,
	      xaxis: {title: xlabel},
	      yaxis: {title: ylabel},
	      margin: {t:0}
	    };
	    Plotly.plot( innerChart, [{
	      x: xdata,y: ydata }], layout );
	  }else{
	    logger.log("Load data from the info tab first.","alert-danger");
	  }
	  $("#plot-chart-spinner").hide();
	}
	self.plotChart1 = function(lat,lon){
	  $("#plot-chart-spinner").show();
	  var webGlobeServer = constants.WEBGLOBE_SERVER;
	  var datasetid = self.selectedDataset.id;
	  var fieldname = $("#fieldChartsSelect :selected").text();
	  if(lon < 0){
	    lon = 360 + lon;
	  }

	  //get data
	  $.ajax({
	    url: webGlobeServer + 'GetTimeSeriesData',
	    cache: false,
	    type: 'POST',
	    data: JSON.stringify({
	      username: constants.WEBGLOBE_USER,
	      datasetid: datasetid,
	      fieldname: fieldname,
	      lat: lat,
	      lon: lon
	    }),
	    success: function (dataJSON) {
	      $("#plot-chart-spinner").hide();
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
	    $("#plot-chart-spinner").hide();
	    logger.log("No data returned for the selected location","alert-danger");
	  });
	}
	self.clearChart = function(){
	  Plotly.purge("innerChart");
	  resetLocations();
	}
	self.togglePlotting = function(){
	  self.plotChartSwitch = !self.plotChartSwitch;
	  $("#togglePlottingOn").toggle();
	  $("#togglePlottingOff").toggle();
	}

	self.hideTabs();
      }

      return DatasetsViewModel;
    });


var numOfTraces = 0;

/* 
   reverse geo-coding function...
   LATITUDE, LONGITUDE ---> name of closest city/town
   */ 

function reverseGeocode(lat,long) {
  axios.get('https://maps.googleapis.com/maps/api/geocode/json',{
      params: {
	latlng: lat+","+long,
	key: keys.GOOGLE_API_KEY
      }
      })
  .then(function (response) {

    console.log(response);

    var formattedAddress = response.data.results[0].formatted_address;

    $( "#traces" ).append( '<li class="list-group-item"><code>trace '+ numOfTraces + '</code>: '+formattedAddress+'</li>');
    numOfTraces++;

  })
  .catch(function (error) {
    console.log(error);
  });
}

function resetLocations() {
  numOfTraces = 0;
  $( "#traces" ).html("");
}
