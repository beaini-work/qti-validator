package org.example.qtivalidator;

import com.sun.net.httpserver.HttpServer;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class QtiValidationServer {

    // Read port from environment variable or use 5000 as fallback
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "5000"));

    /* ---------- pre-compiled QTI schemas ---------- */
    private static final Map<String, Schema> QTI_SCHEMAS = loadQtiSchemas();

    private static Map<String, Schema> loadQtiSchemas() {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setResourceResolver(new ClasspathResolver());        // resolve includes
            return Map.of(
                "qti3",
                sf.newSchema(QtiValidationServer.class.getResource(
                        "/schemas/qti3/imsqti_asiv3p0p1_v1p0.xsd"))
            );
        } catch (SAXException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws IOException {
        // Print environment variables and port info for debugging
        System.out.println("Environment variables:");
        System.getenv().forEach((key, value) -> System.out.println(key + "=" + value));
        System.out.println("Starting server on port: " + PORT);
        
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Add health check endpoint
            server.createContext("/healthCheck", exchange -> {
                String method = exchange.getRequestMethod();
                if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                if ("HEAD".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(200, -1); // no body for HEAD
                    return;
                }
                respond(exchange, 200, "OK");
            });

            // Also add health check at root path for ALB
            server.createContext("/", exchange -> {
                String methodRoot = exchange.getRequestMethod();
                if (!"GET".equalsIgnoreCase(methodRoot) && !"HEAD".equalsIgnoreCase(methodRoot)) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                if ("HEAD".equalsIgnoreCase(methodRoot)) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                respond(exchange, 200, "OK");
            });

            server.createContext("/validate", exchange -> {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                String xml = new String(exchange.getRequestBody().readAllBytes(),
                                        StandardCharsets.UTF_8);

                String param = getQueryParam(exchange.getRequestURI(), "schema");
                if (param == null || param.isBlank()) {
                    respondJson(exchange, 400, false, "Missing ?schema=");
                    return;
                }

                Schema schema = QTI_SCHEMAS.get(param);        // qti3 / qti21 ?
                if (schema == null)                            // else treat param as path/URL
                    schema = schemaFromDisk(param);

                Validator v = schema.newValidator();
                try {
                    v.validate(new StreamSource(new StringReader(xml)));
                    respondJson(exchange, 200, true, null);
                } catch (Exception e) {
                    respondJsonInvalid(exchange, 422, e);
                }
            });

            server.start();
            System.out.println("QTI-validator successfully started and listening on port " + PORT);
            System.out.println("Health check endpoints available at / and /healthCheck");
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ---------- helpers ---------- */

    private static String getQueryParam(URI uri, String key) {
        if (uri.getQuery() == null) return null;
        for (String p : uri.getQuery().split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key))
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
        return null;
    }

    private static Schema schemaFromDisk(String path) throws IOException {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return sf.newSchema(new File(path));
        } catch (SAXException e) {
            throw new IOException("Bad schema: " + e.getMessage(), e);
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex,
                                int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    /* ---------- json helpers ---------- */

    private static void respondJson(com.sun.net.httpserver.HttpExchange ex,
                                    int code, boolean valid, String error)
                                    throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"valid\": ").append(valid);
        if (!valid && error != null) {
            sb.append(", \"error\": \"").append(jsonEscape(error)).append("\"");
        }
        sb.append("}");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static void respondJsonInvalid(com.sun.net.httpserver.HttpExchange ex,
                                           int code, Exception exc) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"valid\": false, \"errors\": [");

        sb.append("{\"message\": \"").append(jsonEscape(exc.getMessage())).append("\"");

        if (exc instanceof SAXParseException spe) {
            int line = spe.getLineNumber();
            int col  = spe.getColumnNumber();
            if (line >= 0) sb.append(", \"line\": ").append(line);
            if (col  >= 0) sb.append(", \"column\": ").append(col);
        }

        sb.append("}]}");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /* ---------- tiny class-path resolver ---------- */
    private static class ClasspathResolver implements LSResourceResolver {
        @Override
        public LSInput resolveResource(String type, String ns, String pubId,
                                       String sysId, String baseURI) {
            String name = sysId.substring(sysId.lastIndexOf('/') + 1);
            InputStream in = QtiValidationServer.class.getResourceAsStream("/schemas/qti3/" + name);
            return in == null ? null : new LSInput() {
                public Reader getCharacterStream() { return null; }
                public void setCharacterStream(Reader r) { }
                public InputStream getByteStream() { return in; }
                public void setByteStream(InputStream i) { }
                public String getStringData() { return null; }
                public void setStringData(String s) { }
                public String getSystemId() { return sysId; }
                public void setSystemId(String s) { }
                public String getPublicId() { return pubId; }
                public void setPublicId(String s) { }
                public String getBaseURI() { return baseURI; }
                public void setBaseURI(String s) { }
                public String getEncoding() { return "UTF-8"; }
                public void setEncoding(String s) { }
                public boolean getCertifiedText() { return false; }
                public void setCertifiedText(boolean b) { }
            };
        }
    }
} 