<%@ page session="true"%>
<html>
  <head>
    <!-- All the files that are required -->

    <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
    <link href='http://fonts.googleapis.com/css?family=Varela+Round' rel='stylesheet' type='text/css'>
    <!--Local Stylesheet-->
    <link rel="stylesheet" href="http://www.cse.buffalo.edu/~chandola/css/login.css"/>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-validate/1.13.1/jquery.validate.min.js"></script>
    <title>Login failed</title>
  </head>
  <body>
    <div class="text-center" style="padding:50px 0">
      <i class="fa fa-unlock fa-5x"></i>
      <br/><br/>
      <div><span> 
	  User '<%=request.getRemoteUser()%>' has been logged out.
	  <% session.invalidate(); %>
	  Please click <a href="index.html">here</a> to sign-in again.
	</span></div>
	<br/>
	<div><img width="240px" class="img-responsive" alt="WebGlobe 2.0" src='http://www.cse.buffalo.edu/~chandola/research/webglobe/webglobe_files/icon.png'/></div>
	<br/>
	<div><span> Powered by:</span></div>
	<div><a href="https://federatedcloud.org"><img width="160px" class="img-responsive" alt="Aristotle Federated Cloud" src='https://federatedcloud.org/images/Aristotle.jpg'/></a></div>

    </div>
  </body>
</html>


