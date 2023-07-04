package com.burgaud.jssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.ArrayList;

import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class Server {

    private int port;
    private String ksFile;
    private String ksPasswd;
    private String[] protocols;
    private static final String VERSION = "0.3.0";
    private static final String APP = "JSSL Test Server";

    class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String resp = String.format("%s version %s\n", APP, VERSION);
            HttpsExchange x = (HttpsExchange) exchange;
            x.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            x.sendResponseHeaders(200, resp.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(resp.getBytes());
            os.close();
        }
    }

    Server(int port, String ksFile, String ksPasswd, String[] protocols) {
        this.port = port;
        this.ksFile = ksFile;
        this.ksPasswd = ksPasswd;
        this.protocols = protocols;
    }

    public void start() {
        try {
            InetSocketAddress address = new InetSocketAddress(port);
            HttpsServer server = HttpsServer.create(address, 0);
            SSLContext context = SSLContext.getInstance("SSL");

            char[] passwd = ksPasswd.toCharArray();
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream is = new FileInputStream(ksFile);
            ks.load(is, passwd);

            KeyManagerFactory km = KeyManagerFactory.getInstance("SunX509");
            km.init(ks, passwd);

            TrustManagerFactory tm = TrustManagerFactory.getInstance("SunX509");
            tm.init(ks);

            context.init(km.getKeyManagers(), tm.getTrustManagers(), null);
            server.setHttpsConfigurator(new HttpsConfigurator(context) {
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext context = getSSLContext();
                        SSLEngine engine = context.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        var remote = params.getClientAddress();
                        System.out.printf("Connection from %s:%d\n", remote.getHostString(), remote.getPort());
                        SSLParameters sslParams = context.getSupportedSSLParameters();
                        sslParams.setProtocols(protocols);
                        var sslProtocols = sslParams.getProtocols();
                        System.out.printf("SSL Protocols: %s\n", String.join(", ", sslProtocols));
                        params.setSSLParameters(sslParams);

                    } catch (Exception ex) {
                        System.out.println("Failed to create the HTTPS port");
                    }
                }
            });
            server.createContext("/", new RequestHandler());
            server.setExecutor(null);
            server.start();

        } catch (Exception exception) {
            System.out.println("Error starting HTTPS server");
            exception.printStackTrace();
        }
    }

    public static void usage() {
        var help = """
            Usage:
              jssl-server [--help | --sslv3 | --tlsv1 | --tlsv1.1 | --tlsv1.2 | --tlsv1.2]

            Description:
              Start a testing SSL/TLS Server with specific protocols enabled. By default, the following
              SSL/TLS protocol versions are enabled: SSL 3.0, TLS 1.0, TLS 1.1, TLS 1.2, and TLS 1.3.

              To select one or more specific protocols, use the corresponding options.

            Options:
              -h, --help     Show this help message and exit.
              --sslv3        Enable SSL 3.0.
              --tlsv1        Enable TLS 1.0.
              --tlsv1.1      Enable TLS 1.1.
              --tlsv1.2      Enable TLS 1.2.
              --tlsv1.3      Enable TLS 1.3.
            """;
        System.out.println(help);
    }

    public static void printBanner() {
        var banner = """
                _ ___ ___ _    ___
             _ | / __/ __| |  / __| ___ _ ___ _____ _ _
            | || \\__ \\__ \\ |__\\__ \\/ -_) '_\\ V / -_) '_|
             \\__/|___/___/____|___/\\___|_|  \\_/\\___|_|
                                                        """;
        System.out.println(banner);
        System.out.printf("%s version %s - Java version %s\n", Server.APP, Server.VERSION, System.getProperty("java.version"));
        System.out.println("(c) 2023 Andre Burgaud\n");
    }

    public static void main(String[] args) throws Exception {

        final String KS_NAME = "jssl.jks";
        final String KS_PASSWD = "password";
        final int PORT = 9999;
        final String[] PROTOCOLS = {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

        printBanner();

        var protocols = new ArrayList<>();

        if (args.length > 0) {
            for (var arg : args) {
                if (arg.startsWith("-")) {
                    switch (arg.toLowerCase()) {
                    case "-h":
                    case "-help":
                    case "--help":
                        usage();
                        System.exit(0);
                    case "-sslv3":
                    case "--sslv3":
                        protocols.add("SSLv3");
                        break;
                    case "-tlsv1":
                    case "--tlsv1":
                        protocols.add("TLSv1");
                        break;
                    case "-tlsv1.1":
                    case "--tlsv1.1":
                        protocols.add("TLSv1.1");
                        break;
                    case "-tlsv1.2":
                    case "--tlsv1.2":
                        protocols.add("TLSv1.2");
                        break;
                    case "-tlsv1.3":
                    case "--tlsv1.3":
                        protocols.add("TLSv1.3");
                        break;
                    default:
                        System.out.printf("Ignoring unexpected option %s\n", arg);
                    }
                }
                else {
                    System.out.printf("Ignoring unexpected argument %s\n", arg);
                }
            }
        }

        if (protocols.size() == 0) {
            protocols.addAll(Arrays.asList(PROTOCOLS));
        }

        String[] protocolsArray = protocols.toArray(new String[protocols.size()]);
        var sslServer = new Server(PORT, KS_NAME, KS_PASSWD, protocolsArray);

        System.out.printf("Starting single threaded %s at localhost:%d\n", Server.APP, PORT);
        System.out.printf("Enabled protocols: %s\n", String.join(", ", protocolsArray));
        sslServer.start();
    }
}