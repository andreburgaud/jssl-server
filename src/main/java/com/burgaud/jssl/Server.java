package com.burgaud.jssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;

import java.nio.file.Path;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;

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

    private final int port;
    private final String jksFile;
    private final String jksPasswd;
    private final String[] protocols;
    private static final String APP_VERSION = "0.7.0";
    private static final String APP_NAME = "JSSL Test Server";
    private static final String DEFAULT_JKS_FILE = "jssl.jks";
    private static final String DEFAULT_JKS_PASSWD = "password";
    private static final int DEFAULT_PORT = 9999;

    HttpsServer httpsServer;


    private String getJksFromExePath(String jksFile) throws URISyntaxException {
        // Search for the JKS file in the same directory as the executable
        Path path = Path.of(
            Server.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
        ).getParent();
        return path.resolve(jksFile).toString();
    }

    private String getJksPath(String jksFile) throws FileNotFoundException {
        // The file is found in directory of execution or via absolute path
        var f = new File(jksFile);
        if (f.exists()) {
            return jksFile;
        }
        try {
            return getJksFromExePath(jksFile);
        } catch (URISyntaxException e) {
            System.out.println(e.getMessage());
        }
        throw new FileNotFoundException(jksFile);
    }

    static class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String resp = String.format("%s version %s\n", APP_NAME, APP_VERSION);
            HttpsExchange x = (HttpsExchange) exchange;
            x.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            x.sendResponseHeaders(200, resp.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(resp.getBytes());
            os.close();
        }
    }

    static class GracefulExit extends Thread {
        Server _server;
        public GracefulExit(Server server) {
            this._server = server;
        }
        @Override
        public void run() {
            System.out.println("\nStopping the server...");
            _server.httpsServer.stop(1);
            System.out.println("Bye!");
        }
    }

    Server(int port, String jksFile, String jksPasswd, String[] protocols) {
        this.port = port;
        this.jksFile = jksFile;
        this.jksPasswd = jksPasswd;
        this.protocols = protocols;
    }



    public void start() {
        try {
            InetSocketAddress address = new InetSocketAddress(port);
            httpsServer = HttpsServer.create(address, 0);
            SSLContext context = SSLContext.getInstance("SSL");

            char[] passwd = jksPasswd.toCharArray();
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

            try {
                String path = getJksPath(jksFile);
                InputStream is = new FileInputStream(path);
                ks.load(is, passwd);
            } catch(FileNotFoundException e) {
                System.out.printf("Error: JKS file (%s) not found.\n", jksFile);
                System.exit(1);
            } catch(java.io.IOException e) {
                System.out.printf("Error: Problem opening JKS file %s: %s.\n", jksFile, e.getMessage());
                System.exit(1);
            }

            KeyManagerFactory km = KeyManagerFactory.getInstance("PKIX");
            km.init(ks, passwd);

            TrustManagerFactory tm = TrustManagerFactory.getInstance("PKIX");
            tm.init(ks);

            context.init(km.getKeyManagers(), tm.getTrustManagers(), null);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(context) {
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
                        params.setSSLParameters(sslParams);
                    } catch (Exception ex) {
                        System.out.println("Failed to create the HTTPS port");
                    }
                }
            });
            httpsServer.createContext("/", new RequestHandler());
            httpsServer.setExecutor(null);
            httpsServer.start();
        } catch (Exception exception) {
            System.out.println("Error starting HTTPS server");
            exception.printStackTrace();
        }
    }

    public static void usage() {
        var help = """
            Usage:
              jssl-server [--help | --sslv3 | --tlsv1 | --tlsv1.1 | --tlsv1.2 | --tlsv1.3]

            Description:
              Start a testing SSL/TLS Server with specific protocols enabled. By default, the following
              SSL/TLS protocol versions are enabled: SSL 3.0, TLS 1.0, TLS 1.1, TLS 1.2, and TLS 1.3.

              To select one or more specific protocols, use the corresponding options.

            Options:
              -h, --help     Show this help message and exit.
              -v, --version  Show the application version.
              -p, --port     Set a custom port for the server (default: 9999).
              --keystore     Set the path of the keystore file (default: jssl.jks).
              --password     Set the password for the keystore (default: password).
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
        System.out.printf("%s version %s - Java version %s\n", APP_NAME, APP_VERSION, System.getProperty("java.version"));
        System.out.println("(c) 2023 Andre Burgaud\n");
    }

    public static void main(String[] args) throws Exception {

        final String[] PROTOCOLS = {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

        printBanner();

        var protocols = new ArrayList<>();
        var jksFile = DEFAULT_JKS_FILE;
        var password = DEFAULT_JKS_PASSWD;
        var port = DEFAULT_PORT;

        var it = Arrays.asList(args).listIterator();
        while (it.hasNext()) {
            var arg = it.next();
            if (arg.startsWith("-")) {
                try {
                    switch (arg.toLowerCase()) {
                        case "-h", "-help", "--help" -> {
                            usage();
                            System.exit(0);
                        }
                        case "-v", "--version" -> {
                            System.out.printf("%s version %s\n", APP_NAME, APP_VERSION);
                            System.exit(0);
                        }
                        case "-p", "--port" -> port = Integer.parseInt(it.next());
                        case "-keystore", "--keystore" -> jksFile = it.next();
                        case "-password", "--password" -> password = it.next();
                        case "-sslv3", "--sslv3" -> protocols.add("SSLv3");
                        case "-tlsv1", "--tlsv1" -> protocols.add("TLSv1");
                        case "-tlsv1.1", "--tlsv1.1" -> protocols.add("TLSv1.1");
                        case "-tlsv1.2", "--tlsv1.2" -> protocols.add("TLSv1.2");
                        case "-tlsv1.3", "--tlsv1.3" -> protocols.add("TLSv1.3");
                        default -> System.out.printf("Ignoring unexpected option %s\n", arg);
                    }
                } catch(NoSuchElementException e) {
                    System.out.printf("Error: did you miss the option argument to %s?\n", arg);
                    System.exit(1);
                }
            } else {
                System.out.printf("Ignoring unexpected argument %s\n", arg);
            }
        }

        if (protocols.size() == 0) protocols.addAll(Arrays.asList(PROTOCOLS));
        String[] protocolsArray = protocols.toArray(new String[protocols.size()]);

        var sslServer = new Server(port, jksFile, password, protocolsArray);

        System.out.printf("Starting single threaded %s at localhost:%d\n", APP_NAME, DEFAULT_PORT);
        System.out.printf("Enabled protocols: %s\n", String.join(", ", protocolsArray));
        Runtime.getRuntime().addShutdownHook(new GracefulExit(sslServer));
        sslServer.start();
    }
}