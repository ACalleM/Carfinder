<?php
 
// get the HTTP method, path and body of the request
$method = $_SERVER['REQUEST_METHOD'];
$request = explode('/', trim($_SERVER['PATH_INFO'],'/'));
$input = json_decode(file_get_contents('php://input'));


// connect to the mysql database
$link = mysqli_connect('localhost', 'user', 'Xxxx01', 'carfinder');
mysqli_set_charset($link,'utf8');
 
// create SQL based on HTTP method
switch ($method) {
	case 'GET':
		$user = "0";
		$deviceAddress = htmlspecialchars($_GET["deviceAddress"]);
		$sql = "SELECT * FROM DEVICE_LOCATIONS WHERE USER='$user' AND DEVICE_ADDRESS='$deviceAddress' ORDER BY DATE DESC LIMIT 1";
		break;
	case 'POST':
		$user = "0";
		$location = $input->location;
		$sql = "INSERT INTO DEVICE_LOCATIONS (USER,BEARER,DEVICE_NAME,DEVICE_ADDRESS,DATE,LATITUDE,LONGITUDE,ACCURACY,DESCRIPTION) VALUES ('$user','$input->bearer','$input->deviceName','$input->deviceAddress','$input->date',$location->latitude,$location->longitude,$location->accuracy,'$location->description')";
		break;
	case 'DELETE':
		$sql = "delete DEVICE_LOCATIONS";
		break;
}
 
// excecute SQL statement
$result = mysqli_query($link,$sql);
 
// die if SQL statement failed
if (!$result) {
        error_log("CARFINDER: Execute problems, " . mysqli_error($link));
        mysqli_close($link);
        http_response_code(500);
	return;
}

switch ($method) {
	case 'GET':
		if (mysqli_num_rows($result) == 0) {
        		http_response_code(404);
		} elseif (mysqli_num_rows($result) > 1) {
       			error_log("CARFINDER: More than one result");
        		http_response_code(500);
		} else {
			$record = mysqli_fetch_object($result);
			header('Content-Type: application/json');
			echo json_encode(array("id"=>$record->ID,"user"=>$record->USER,"bearer"=>$record->BEARER,"deviceName"=>$record->BEARER,"deviceAddress"=>$record->DEVICE_ADDRESS,"date"=>$record->DATE,"location"=>array("latitude"=>$record->LATITUDE,"longitude"=>$record->LONGITUDE,"accuracy"=>$record->ACCURACY,"description"=>$record->DESCRIPTION)));
  		}
		break;
	case 'POST':
		$id = mysqli_insert_id($link);
		header('Content-Type: application/json');
		echo(json_encode(array("id"=>$id)));
		break;
	case 'DELETE':
		echo mysqli_affected_rows($link);
		break;
}
 

// close mysql connection
mysqli_close($link);
