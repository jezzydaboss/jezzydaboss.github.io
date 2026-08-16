///////////////////////////////////////////////////////////////////////////////
// MetricsServer.h
// ============
// a minimal embedded HTTP server that exposes live frame metrics to any HTTP client
// e.g. a browser-based dashboard over the network.
//
//  ENHANCEMENT (Milestone Two, CS 499): this is what actually makes the
//  "Full-Stack App Integration" claim in the Module One enhancement plan
//  true rather than aspirational. The previous version of this
//  enhancement (ExportFrameMetrics()) only wrote metrics to a local JSON
//  file on disk useful, but not "an accessible web platform" as
//  claimed, since nothing outside the local filesystem could reach it.
//  This class opens a real TCP listening socket and answers HTTP GET
//  requests with a JSON payload, which is what "designing programmatic
//  connections (endpoints) that pipe data out of a core system into an
//  accessible web platform" actually requires.
//
//  Design notes:
//   - Runs its accept/serve loop on a background std::thread so it never
//     blocks the render loop.
//   - The JSON payload is updated from the main thread each frame via
//     UpdatePayload() and protected by a mutex, since it's read from the
//     server thread whenever a request comes in.
//   - Sends an Access-Control-Allow-Origin: * header so a dashboard
//     served from a different origin (e.g. opened as a local file, or
//     served from a different port) can fetch() it directly from
//     JavaScript without being blocked by CORS.
//   - Implemented with POSIX sockets, which matches this
//     project's actual current toolchain (tasks.json targets clang++ on
//     macOS via Homebrew) and Linux. It intentionally does NOT attempt a
//     Winsock implementation for MSVC/Windows, since the project no
//     longer ships a Visual Studio project file see the note in
//     MetricsServer.cpp if a Windows port is needed later.
///////////////////////////////////////////////////////////////////////////////

#pragma once

#include <string>
#include <thread>
#include <mutex>
#include <atomic>

class MetricsServer
{
public:
	// port is the TCP port to listen on, e.g. 8080. The server does not
	// start listening until Start() is called.
	explicit MetricsServer(int port);
	~MetricsServer();

	// starts the background thread that accepts and serves HTTP requests.
	// returns false if the listening socket could not be created/bound.
	bool Start();

	// stops the server and joins the background thread. Safe to call
	// even if Start() was never called or already stopped.
	void Stop();

	// called from the main/render thread once per frame (or whenever new
	// data is available) to update what the server will respond with on
	// the next request. jsonPayload should be a complete, valid JSON
	// document as a string.
	void UpdatePayload(const std::string& jsonPayload);

private:
	int m_port;
	int m_listenSocket;
	std::thread m_serverThread;
	std::atomic<bool> m_running;

	std::mutex m_payloadMutex;
	std::string m_currentPayload;

	// the function that runs on the background thread: accepts
	// connections in a loop and serves the current payload to each one
	void ServeLoop();

	// builds a minimal, valid HTTP/1.1 response wrapping the given body
	static std::string BuildHttpResponse(const std::string& body);
};
