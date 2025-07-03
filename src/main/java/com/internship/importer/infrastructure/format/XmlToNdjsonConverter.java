package com.internship.importer.infrastructure.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.internship.importer.exception.XmlParsingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class XmlToNdjsonConverter implements StreamConverter {

    @Override
    public void convertInputStream(InputStream inputStream, Writer writer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "windows-1251"))) {

            ObjectMapper jsonMapper = new ObjectMapper();
            XMLInputFactory factory = XMLInputFactory.newInstance();

            log.info("XML conversion started...");


            String outerTag = findOuterTag(reader);
            processLines(reader, writer, jsonMapper, factory, outerTag);
        } catch (IOException e) {
            throw new XmlParsingException("Failed to read input stream", e);
        }
    }

    private ObjectNode parseSubjectLineWithStAX(String line, XMLInputFactory factory, ObjectMapper mapper,
                                                String outerTag) throws XMLStreamException {
        XMLEventReader xmlReader = factory.createXMLEventReader(new StringReader(line));
        ObjectNode node = null;

        while (xmlReader.hasNext()) {
            XMLEvent event = xmlReader.nextEvent();

            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                if (outerTag.equalsIgnoreCase(start.getName().getLocalPart())) {
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

    private void parseElementsIntoNode(XMLEventReader reader, ObjectNode parentNode, ObjectMapper mapper)
            throws XMLStreamException {
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
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\u0000", "");
    }

    private String extractOuterTagName(String line) {
        line = line.trim();

        Pattern openTagPattern = Pattern.compile("<\\s*([\\w:-]+)");
        Matcher openMatcher = openTagPattern.matcher(line);

        if (openMatcher.find()) {
            String tagName = openMatcher.group(1);

            String closingTag = "</" + tagName + ">";

            if (line.contains(closingTag)) {
                return tagName;
            }
        }

        return null;
    }


    public static boolean isDataStartTag(String line) {
        line = line.trim();

        return line.startsWith("<") &&
                !line.startsWith("<?") &&
                !line.startsWith("<!--") &&
                !line.startsWith("<!");
    }

    private String findOuterTag(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (isDataStartTag(line)) {
                String tag = extractOuterTagName(line);
                if (tag != null) return tag;
            }
        }
        throw new XmlParsingException("Outer tag not found");
    }

    private void processLines(BufferedReader reader, Writer writer, ObjectMapper mapper,
                              XMLInputFactory factory, String outerTag) throws IOException {
        String line;
        long lineCount = 0;
        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (lineCount == 10000) break;

            if (!line.contains(outerTag)) continue;

            try {
                ObjectNode node = convertLineToJson(line, outerTag, factory, mapper);
                if (node != null) {
                    writer.write(mapper.writeValueAsString(node));
                    writer.write('\n');
                }
            } catch (Exception ignored) {}
        }
        writer.flush();
    }

    private ObjectNode convertLineToJson(String line, String outerTag,
                                         XMLInputFactory factory, ObjectMapper mapper) throws Exception {
        String unescapedLine = StringEscapeUtils.unescapeXml(line);
        return parseSubjectLineWithStAX(unescapedLine, factory, mapper, outerTag);
    }
}
