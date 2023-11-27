<?php

/**
 * BrokeButHungry API Usage
 * Author:  Carla Hernandez 
 * Date:    November 22, 2023
 * Website: appsbycarla.com 
 * 
 * Base URI: https://brokebuthungry.appsbycarla.com/api.php
 *
 * The API supports both GET and POST requests for various functionalities.
 *
 * ENDPOINTS:
 * 
 * 1. Login (POST)
 *    - URL: https://brokebuthungry.appsbycarla.com/api.php
 *    - Required Parameters (POST): 
 *      - 'request': 'login'
 *      - 'username': [User's Username]
 *      - 'password': [User's Password]
 *    - Description: Authenticates user based on username and password.
 *
 * 2. Create Login (POST)
 *    - URL: https://brokebuthungry.appsbycarla.com/api.php
 *    - Required Parameters (POST): 
 *      - 'request': 'createLogin'
 *      - 'fname': [First Name]
 *      - 'lname': [Last Name]
 *      - 'username': [Desired Username]
 *      - 'password': [Desired Password]
 *      - 'email': [Email Address]
 *    - Description: Creates a new user account with provided details.
 *
 * 3. Reset Password (POST)
 *    - URL: https://brokebuthungry.appsbycarla.com/api.php
 *    - Required Parameters (POST): 
 *      - 'request': 'resetPassword'
 *      - 'fname': [First Name]
 *      - 'lname': [Last Name]
 *      - 'username': [User's Username]
 *      - 'email': [Email Address]
 *      - 'password': [New Password]
 *    - Description: Resets the user's password, verifying identity with name, username, and email.
 *
 * 4. Get Favorites (GET)
 *    - URL: https://brokebuthungry.appsbycarla.com/api.php?request=getFavorites&username=[username]
 *    - Required Parameters (GET): 
 *      - 'request': 'getFavorites'
 *      - 'username': [User's Username]
 *    - Description: Fetches the list of favorite recipes for the given username.
 *
 * USAGE EXAMPLES:
 * 
 * - To login a user:
 *   POST https://brokebuthungry.appsbycarla.com/api.php
 *   Body: {
 *     "request": "login",
 *     "username": "john_doe",
 *     "password": "12345"
 *   }
 * 
 * - To create a new user:
 *   POST https://brokebuthungry.appsbycarla.com/api.php
 *   Body: {
 *     "request": "createLogin",
 *     "fname": "John",
 *     "lname": "Doe",
 *     "username": "john_doe",
 *     "password": "12345",
 *     "email": "john@example.com"
 *   }
 *
 * - To reset a user's password:
 *   POST https://brokebuthungry.appsbycarla.com/api.php
 *   Body: {
 *     "request": "resetPassword",
 *     "fname": "John",
 *     "lname": "Doe",
 *     "username": "john_doe",
 *     "email": "john@example.com",
 *     "password": "newPassword123"
 *   }
 * 
 * - To retrieve favorites for a user:
 *   GET https://brokebuthungry.appsbycarla.com/api.php?request=getFavorites&username=john_doe
 *
 * NOTES:
 * - For POST requests, send data as application/x-www-form-urlencoded or as JSON in the request body.
 * - Ensure all data is URL-encoded where necessary.
 * - Replace placeholder values with actual user data.
 */


// Error Reporting
include 'debug.php';

// Database Connection
include 'db.php'; 

// Check the request type, POST ot GET
$requestType = $_SERVER['REQUEST_METHOD'];

// API router
switch ( $requestType ) {
    case 'POST':
        // Handle POST requests
        $request = $_POST['request'] ?? '';

        switch ( $request ) {
            case 'login':
                // Handle login request
                $username = $_POST['username'] ?? '';
                $password = $_POST['password'] ?? '';
            
                if ( $username != '' && $password != '' ) {
                    // Use prepared statements to prevent SQL Injection
                    // Fetch only the hashed password for the given username
                    $stmt = $con->prepare ( "SELECT password FROM Logins WHERE username = ?" );
                    $stmt->bind_param ( "s", $username );
                    $stmt->execute();
                    $result = $stmt->get_result();
            
                    if ( $result->num_rows > 0 ) {
                        $row = $result->fetch_assoc();
                        $hashedPassword = $row['password'];
            
                        // Verify the password
                        if ( password_verify ( $password, $hashedPassword ) ) {
                            echo json_encode ( ['status' => 'success', 'message' => 'Login successful'] );
                        } else {
                            echo json_encode ( ['status' => 'error', 'message' => 'Invalid username or password'] );
                        }
                    } else {
                        echo json_encode ( ['status' => 'error', 'message' => 'Invalid username or password'] );
                    }
                } else {
                    echo json_encode ( ['status' => 'error', 'message' => 'Username and password required'] );
                }
                break;

            case 'createLogin':
                // Handle create login request
                $fname    = $_POST['fname'] ?? '';
                $lname    = $_POST['lname'] ?? '';
                $username = $_POST['username'] ?? '';
                $password = $_POST['password'] ?? '';
                $email    = $_POST['email'] ?? '';
        
                // Validate inputs (basic example)
                if ( !filter_var ( $email, FILTER_VALIDATE_EMAIL ) || empty ( $password ) || empty ( $username ) ) {
                    echo json_encode ( ['status' => 'error', 'message' => 'Invalid input'] );
                    break;
                }
        
                // Hash the password
                $hashedPassword = password_hash ( $password, PASSWORD_DEFAULT );
        
                // Insert into the database using prepared statements
                $stmt = $con->prepare ( "INSERT INTO Logins (fname, lname, username, password, email) VALUES (?, ?, ?, ?, ?)" );
                $stmt->bind_param ( "sssss", $fname, $lname, $username, $hashedPassword, $email );
                $result = $stmt->execute();
        
                if ( $result ) {
                    echo json_encode ( ['status' => 'success', 'message' => 'User created successfully'] );
                } else {
                    echo json_encode ( ['status' => 'error', 'message' => 'Error creating user'] );
                }
                break;

            case 'resetPassword':
                // Handle reset password request
                $fname       = $_POST['fname'] ?? '';
                $lname       = $_POST['lname'] ?? '';
                $username    = $_POST['username'] ?? '';
                $email       = $_POST['email'] ?? '';
                $newPassword = $_POST['password'] ?? '';
            
                // Validate inputs
                if ( empty ( $fname ) || empty ( $lname ) || empty ( $username ) || empty ( $email ) || empty ( $newPassword ) ) {
                    echo json_encode ( ['status' => 'error', 'message' => 'All fields are required'] );
                    break;
                }
            
                // Verify user details
                $stmt = $con->prepare ( "SELECT * FROM Logins WHERE fname = ? AND lname = ? AND username = ? AND email = ?" );
                $stmt->bind_param ( "ssss", $fname, $lname, $username, $email );
                $stmt->execute();
                $result = $stmt->get_result();
            
                if ( $result->num_rows == 0 ) {
                    echo json_encode(['status' => 'error', 'message' => 'User details do not match']);
                    break;
                }
            
                // Hash the new password
                $hashedPassword = password_hash ( $newPassword, PASSWORD_DEFAULT );
            
                // Update the password in the database
                $updateStmt = $con->prepare ( "UPDATE Logins SET password = ? WHERE username = ?" );
                $updateStmt->bind_param ( "ss", $hashedPassword, $username );
                $updateResult = $updateStmt->execute();
            
                if ( $updateResult ) {
                    echo json_encode ( ['status' => 'success', 'message' => 'Password reset successfully'] );
                } else {
                    echo json_encode ( ['status' => 'error', 'message' => 'Error updating password'] );
                }
                break;
            
            // ... (Other POST cases)

            default:
                echo json_encode(['status' => 'error', 'message' => 'Invalid POST request']);
                break;
        }
        break;

        case 'GET':
            // Handle GET requests
            $request = $_GET['request'] ?? '';
        
            switch ( $request ) {
                case 'getFavorites':
                    // Handle get favorites request
                    $username = $_GET['username'] ?? '';
                    
                    if ( $username != '' ) {
                        $stmt = $con->prepare ( "SELECT * FROM Favorites WHERE username = ?" );
                        $stmt->bind_param ( "s", $username );
                        $stmt->execute();
                        $result = $stmt->get_result();
        
                        if ( !$result ) {
                            echo json_encode ( ['status' => 'error', 'message' => 'Error executing query: ' . $con->error] );
                            break;
                        }
        
                        $favorites = [];
                        while ( $row = $result->fetch_assoc() ) {
                            $favorites[] = $row;
                        }
        
                        echo json_encode ( ['status' => 'success', 'data' => $favorites] );
                    } else {
                        echo json_encode ( ['status' => 'error', 'message' => 'Username required'] );
                    }
                    break;
        
                // ... (Other GET cases)
        
                default:
                    echo json_encode ( ['status' => 'error', 'message' => 'Invalid GET request'] );
                    break;
            }
            break;
    
        // ... (Other request types)
    
        default:
            echo json_encode ( ['status' => 'error', 'message' => 'Invalid request method'] );
            break;
    }

// Close database connection
mysqli_close ( $con );

?>
