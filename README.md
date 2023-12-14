# JiraZephyrConnector
HTTP Proxy Server for JIRA/Zephyr Scale Integration

Author: Roland Ross Hadi

## Description
JiraZephyrConnector is a Java-based HTTP server designed as a proxy for integrating with JIRA instances, specifically tailored to work with Zephyr Scale. This server runs on a customizable port and handles HTTP requests targeting JIRA's REST API endpoints.

## Key Features
1. **CORS Support**: Handles Cross-Origin Resource Sharing (CORS) preflight requests.
2. **Basic Authentication**: Supports basic authentication for secure access to the JIRA server.
3. **Request Handling**: Processes and relays both GET and POST requests to the JIRA server.
4. **Dynamic Configuration**: Configurable through system properties to adapt to different deployment environments.
5. **Endpoint Management**: Manages specific API endpoints (like test plans and test runs) using the ProxyHandler class.
6. **Robust Communication**: Ensures reliable communication between the client and JIRA server, including error handling.

## Usage
The server can be configured and run with custom settings for port, JIRA URL, origin, and user credentials. It is suitable for scenarios that require middleware solutions for secure, cross-domain communication with JIRA APIs.

## Installation and Running
Compile the source code using:
```
javac -source 1.8 -target 1.8 JiraZephyrConnector.java
```

Run the server with the following command (replace placeholders with actual values):
```
java -Dserver.port=8383 -Djira.url=http://xxx.xxx.xxx.xxx -Dallowed.origin=http://xxx.xxx.xxx.xxx -Djira.username=username -Djira.password=password JiraZephyrConnector
```

## Note
This implementation by Roland Ross Hadi showcases the practical application of Java's networking capabilities for building middleware solutions.

## License

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


