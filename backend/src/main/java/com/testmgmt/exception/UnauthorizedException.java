package com.testmgmt.exception;

/**
 * Thrown when an authenticated identity (e.g. from IDEM/SSO) is not authorized
 * to access the application — typically because the user does not exist in the
 * application's Users table. Results in an HTTP 401 response.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
