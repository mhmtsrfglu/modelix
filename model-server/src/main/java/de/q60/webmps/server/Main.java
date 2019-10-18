package de.q60.webmps.server;

/*Generated by MPS */


import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.EnumSet;

public class Main {
    public static void main(String[] args) {
        System.out.println("Server process started");

        System.out.println("Waiting ");

        try {
            String portStr = System.getenv("PORT");
            InetSocketAddress bindTo = new InetSocketAddress(InetAddress.getByName("0.0.0.0"),
                    portStr == null ? 28101 : Integer.parseInt(portStr));

            final Server server = new Server(bindTo);

            ServletContextHandler servletHandler = new ServletContextHandler();
            servletHandler.addServlet(new ServletHolder(new ModelServerServlet(new ModelServer(new IgniteStoreClient()))), "/ws");

            EventSourceServlet sseServlet = new SSETestServlet();
            servletHandler.addServlet(new ServletHolder(sseServlet), "/sse");

            servletHandler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    resp.setContentType("text/html");
                    resp.getWriter().println("Model Server");
                }
            }), "/");

            ResourceHandler staticResources = new ResourceHandler();
            staticResources.setResourceBase("/Users/slisson/mps/mps192/webmps");

            HandlerList handlerList = new HandlerList();
            handlerList.addHandler(staticResources);
            handlerList.addHandler(servletHandler);

            server.setHandler(handlerList);
            server.start();
            System.out.println("Server started");

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        server.stop();
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            System.out.println("Server failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static Handler withContext(String path, Handler handler) {
        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath(path);
        contextHandler.setHandler(handler);
        return contextHandler;
    }

    private static class SSETestServlet extends EventSourceServlet {
        @Override
        protected EventSource newEventSource(HttpServletRequest request) {
            return new EventSource() {
                private Emitter emitter;
                private Timer timer = new Timer(1000, (e) -> {
                    if (emitter == null) return;
                    try {
                        emitter.data("data-" + System.currentTimeMillis());
                        emitter.event("time","time-" + System.currentTimeMillis());
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                });

                @Override
                public void onOpen(Emitter emitter) throws IOException {
                    this.emitter = emitter;
                    timer.start();
                }

                @Override
                public void onClose() {
                    timer.stop();
                    emitter = null;
                }
            };
        }
    }
}
