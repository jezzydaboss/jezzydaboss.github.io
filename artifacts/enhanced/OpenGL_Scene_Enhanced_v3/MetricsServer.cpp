///////////////////////////////////////////////////////////////////////////////
// MetricsServer.cpp
// ============
// see MetricsServer.h for full design notes.
///////////////////////////////////////////////////////////////////////////////

#include "MetricsServer.h"

#include <iostream>
#include <cstring>

#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

MetricsServer::MetricsServer(int port)
	: m_port(port), m_listenSocket(-1), m_running(false)
{
	m_currentPayload = "{}";
}

MetricsServer::~MetricsServer()
{
	Stop();
}

bool MetricsServer::Start()
{
	m_listenSocket = socket(AF_INET, SOCK_STREAM, 0);
	if (m_listenSocket < 0)
	{
		std::cout << "MetricsServer: ERROR - could not create socket." << std::endl;
		return false;
	}

	// allow immediate reuse of the port after the app restarts, instead
	// of the OS holding it in TIME_WAIT
	int opt = 1;
	setsockopt(m_listenSocket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

	sockaddr_in address{};
	address.sin_family = AF_INET;
	address.sin_addr.s_addr = INADDR_ANY;
	address.sin_port = htons(static_cast<uint16_t>(m_port));

	if (bind(m_listenSocket, (struct sockaddr*)&address, sizeof(address)) < 0)
	{
		std::cout << "MetricsServer: ERROR - could not bind to port " << m_port << "." << std::endl;
		close(m_listenSocket);
		m_listenSocket = -1;
		return false;
	}

	if (listen(m_listenSocket, 4) < 0)
	{
		std::cout << "MetricsServer: ERROR - listen() failed." << std::endl;
		close(m_listenSocket);
		m_listenSocket = -1;
		return false;
	}

	m_running = true;
	m_serverThread = std::thread(&MetricsServer::ServeLoop, this);

	std::cout << "MetricsServer: listening on http://localhost:" << m_port << "/metrics" << std::endl;
	return true;
}

void MetricsServer::Stop()
{
	if (!m_running)
	{
		return;
	}
	m_running = false;

	// closing the listening socket unblocks the accept() call in
	// ServeLoop() so the background thread can exit cleanly
	if (m_listenSocket >= 0)
	{
		close(m_listenSocket);
		m_listenSocket = -1;
	}

	if (m_serverThread.joinable())
	{
		m_serverThread.join();
	}
}

void MetricsServer::UpdatePayload(const std::string& jsonPayload)
{
	std::lock_guard<std::mutex> lock(m_payloadMutex);
	m_currentPayload = jsonPayload;
}

std::string MetricsServer::BuildHttpResponse(const std::string& body)
{
	std::string response;
	response += "HTTP/1.1 200 OK\r\n";
	response += "Content-Type: application/json\r\n";
	// ENHANCEMENT: allows a browser dashboard served from any origin
	// (a different port, or opened directly as a local file) to fetch()
	// this endpoint without being blocked by the browser's CORS policy
	response += "Access-Control-Allow-Origin: *\r\n";
	response += "Content-Length: " + std::to_string(body.size()) + "\r\n";
	response += "Connection: close\r\n";
	response += "\r\n";
	response += body;
	return response;
}

void MetricsServer::ServeLoop()
{
	while (m_running)
	{
		sockaddr_in clientAddress{};
		socklen_t clientLen = sizeof(clientAddress);

		int clientSocket = accept(m_listenSocket, (struct sockaddr*)&clientAddress, &clientLen);
		if (clientSocket < 0)
		{
			// accept() returns an error when the listening socket is
			// closed by Stop() - that's the normal shutdown path, not
			// a real error, so just exit the loop quietly
			break;
		}

		// we don't need to parse the request in any detail - this
		// server only exposes one resource, so any GET request gets
		// the current metrics payload
		char requestBuffer[1024] = { 0 };
		recv(clientSocket, requestBuffer, sizeof(requestBuffer) - 1, 0);

		std::string body;
		{
			std::lock_guard<std::mutex> lock(m_payloadMutex);
			body = m_currentPayload;
		}

		std::string response = BuildHttpResponse(body);
		send(clientSocket, response.c_str(), response.size(), 0);

		close(clientSocket);
	}
}
