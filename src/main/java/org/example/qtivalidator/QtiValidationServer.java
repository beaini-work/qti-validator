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
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class QtiValidationServer {

    private static final int PORT = 8080;

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
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/validate", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String xml = new String(exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8);

            String param = getQueryParam(exchange.getRequestURI(), "schema");
            if (param == null || param.isBlank()) {
                respond(exchange, 400, "Missing ?schema=");
                return;
            }

            Schema schema = QTI_SCHEMAS.get(param);        // qti3 / qti21 ?
            if (schema == null)                            // else treat param as path/URL
                schema = schemaFromDisk(param);

            Validator v = schema.newValidator();
            try {
                v.validate(new StreamSource(new StringReader(xml)));
                respond(exchange, 200, "VALID");
            } catch (Exception e) {
                respond(exchange, 422, "INVALID: " + e.getMessage());
            }
        });

        server.start();
        System.out.printf("QTI-validator listening on http://localhost:%d/validate%n", PORT);
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