package com.internship.importer.infrastructure.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.internship.importer.exception.XmlParsingException;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.Iterator;

@Component
public class XmlToNdjsonConverter implements StreamConverter {

    @Override
    public void convertInputStream(InputStream inputStream, Writer writer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "windows-1251"))) {

            ObjectMapper jsonMapper = new ObjectMapper();
            XMLInputFactory factory = XMLInputFactory.newInstance();

            long lineCount = 0;
            long processedCount = 0;
            long errorCount = 0;
            long startTime = System.currentTimeMillis();

            System.out.println("Starting conversion...");

            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (!line.contains("<SUBJECT")) continue;

                try {
                    String unescapedLine = StringEscapeUtils.unescapeXml(line);
                    ObjectNode node = parseSubjectLineWithStAX(unescapedLine, factory, jsonMapper);

                    if (node != null) {
                        writer.write(jsonMapper.writeValueAsString(node));
                        writer.write('\n');
                        processedCount++;

                        if (processedCount % 1000 == 0) {
                            long elapsed = System.currentTimeMillis() - startTime;
                            double rate = processedCount / (elapsed / 1000.0);
                            System.out.printf("Processed: %,d | Errors: %,d | Lines: %,d | Rate: %.2f/sec%n",
                                    processedCount, errorCount, lineCount, rate);
                        }
                    }


                } catch (Exception e) {
                    errorCount++;
                    System.err.printf("Error parsing line #%d: %s%n", lineCount, e.getMessage());
                    System.err.println("Line: " + line.substring(0, Math.min(line.length(), 200)));
                    if (errorCount % 100 == 0) {
                        System.err.printf("WARNING: %,d total errors%n", errorCount);
                    }
                }
            }

            writer.flush();
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.printf("Conversion completed! Processed: %,d | Errors: %,d | Lines: %,d | Time: %s%n",
                    processedCount, errorCount, lineCount, formatDuration(totalTime));

        } catch (IOException e) {
            throw new XmlParsingException("Failed to read input stream", e);
        }
    }

    private ObjectNode parseSubjectLineWithStAX(String line, XMLInputFactory factory, ObjectMapper mapper) throws XMLStreamException {
        XMLEventReader xmlReader = factory.createXMLEventReader(new StringReader(line));
        ObjectNode node = null;

        while (xmlReader.hasNext()) {
            XMLEvent event = xmlReader.nextEvent();

            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                if ("SUBJECT".equalsIgnoreCase(start.getName().getLocalPart())) {
                    node = mapper.createObjectNode();

                    for (Iterator<?> it = start.getAttributes(); it.hasNext(); ) {
                        Attribute attr = (Attribute) it.next();
                        node.put("@" + attr.getName().getLocalPart(), attr.getValue());
                    }

                    parseElementsIntoNode(xmlReader, node, mapper);
                }
            }
        }

        return node;
    }

    private void parseElementsIntoNode(XMLEventReader reader, ObjectNode parentNode, ObjectMapper mapper) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.peek();

            if (event.isEndElement()) {
                reader.nextEvent();
                return;
            } else if (event.isStartElement()) {
                StartElement start = reader.nextEvent().asStartElement();
                String tagName = start.getName().getLocalPart();

                XMLEvent next = reader.peek();

                if (next.isCharacters()) {
                    StringBuilder textBuilder = new StringBuilder();
                    while (reader.peek().isCharacters()) {
                        textBuilder.append(reader.nextEvent().asCharacters().getData());
                    }
                    reader.nextEvent();
                    parentNode.put(tagName, cleanValue(textBuilder.toString()));
                } else if (next.isStartElement()) {
                    ObjectNode childNode = mapper.createObjectNode();
                    parseElementsIntoNode(reader, childNode, mapper);
                    parentNode.set(tagName, childNode);
                } else {
                    reader.nextEvent();
                }
            } else {
                reader.nextEvent();
            }
        }
    }

    private String cleanValue(String value) {
        if (value == null) return "";
        return value.replace("\r", "")
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replace("\\", "\\\\")  // Escape backslashes first
                .replace("\"", "\\\"")  // Escape quotes
                .replace("\u0000", "");
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        else if (minutes > 0) return String.format("%dm %ds", minutes, seconds % 60);
        else return String.format("%ds", seconds);
    }
}
