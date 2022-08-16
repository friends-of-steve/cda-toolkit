package com.stlstevemoore.cda.utilities;

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;

import javax.xml.namespace.QName;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Util {
    OMElement documentRoot = null;

    public static void main(String[] args) {

        System.out.println("Hello");
        Util u = new Util();

        try {
            u.executeTextFile(args[0]);
            /*
            OMElement documentRoot = Util.parse_xml(new File("/tmp/ccda.xml"));
            Util.write_xml("/tmp/a.xml", documentRoot);

            System.out.println("Allergies/Intolerances");
            OMElement m1 = u.t1(documentRoot);
            Util.write_xml("/tmp/b.xml", m1);

            System.out.println("Encounters");
            OMElement m11 = u.t1Encounters(documentRoot);

            OMElement m2 = u.t2(m1);
            Util.write_xml("/tmp/c.xml", m2);

            OMElement m3 = u.t2Encounters(m2);
            Util.write_xml("/tmp/d.xml", m3);

             */

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        String z = "xx";
    }

    public void executeTextFile(String path) throws Exception {
        List<String> commands = readTextFile(path);
        Iterator<String> iterator = commands.iterator();
        while (iterator.hasNext()) {
            String command = iterator.next().trim();
            if (command.length() == 0 || command.startsWith("#")) {
                continue;
            }
            parseExecuteCommand(command);
        }
    }

    public void parseExecuteCommand(String command) throws Exception {
        String[] tokens = command.split("\t");
        if (tokens == null) {
            return;
        }
        switch (tokens[0]) {
            case "READ":
                executeRead(tokens[1]);
                break;
            case "WRITE":
                executeWrite(tokens[1]);
                break;
            case "PRINT":
                executePrint(tokens[1].replaceAll("_",""));
                break;
            case "STRIP":
                executeStrip(tokens[1].replaceAll("_", ""), tokens[2], tokens[3]);
                break;
            case "TEXT":
                executeText(tokens[1].replaceAll("_", ""), tokens[2]);
                break;

            default:
                throw new Exception("Do not recognize this command: " + tokens[0]);
        }
    }

    public void executeRead(String path)  throws Exception {
        documentRoot = Util.parse_xml(path);
    }
    public void executeWrite(String path) throws Exception {
        Util.write_xml(path, documentRoot);
    }

    public void executePrint(String templateId) throws Exception {
        OMElement sectionElement = scanToSection(documentRoot, templateId);
        System.out.println("\n" + templateId);
        printEntries(sectionElement);
    }

    public void executeText(String templateId, String newText) throws Exception {
        OMElement sectionElement = scanToSection(documentRoot, templateId);
        System.out.println("\n" + templateId);
        replaceSectionText(sectionElement, newText);
    }

    public void executeStrip(String templateId, String lowValue, String highValue) throws Exception {
        OMElement sectionElement = scanToSection(documentRoot, templateId);
        System.out.println("\n" + templateId);
        stripEntries(sectionElement, lowValue, highValue);
    }

    public List<String> readTextFile(String path) throws Exception {
        List<String> lines = Collections.emptyList();
        lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);

        return lines;
    }

    public static OMElement parse_xml(String path) throws FactoryConfigurationError, Exception {
        return parse_xml(new File(path));
    }

    public static OMElement parse_xml(File infile) throws FactoryConfigurationError, Exception {

        FileReader fileReader = new FileReader(infile.getCanonicalFile());
        XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(fileReader);
        StAXOMBuilder builder = new StAXOMBuilder(streamReader);
        OMElement omElement = builder.getDocumentElement();
        if (omElement == null) {
            throw new Exception("No document file when trying to parse file: " + infile.getAbsolutePath());
        }
        return omElement;
    }

    public static void write_xml(String outfile, OMElement ele) throws IOException, XMLStreamException {
        write_xml(new File(outfile), ele);
    }

    public static void write_xml(File outfile, OMElement ele) throws IOException, XMLStreamException {
        FileWriter fw = new FileWriter(outfile);
        BufferedWriter out = new BufferedWriter(fw);

        try {
            ele.serialize(out);
        } finally {
            out.flush();
            out.close();
            fw.close();
        }
    }

    public OMElement t1(OMElement e) throws Exception {
        Iterator<OMElement> it = e.getChildElements();

        return e;
    }

    public OMElement t2(OMElement e) throws Exception {
        Iterator<OMElement> it = e.getChildElements();

        return e;
    }

    public OMElement t1Encounters(OMElement e) throws Exception {
        Iterator<OMElement> it = e.getChildElements();

        OMElement encounters = scanToSection(e, "2.16.840.1.113883.10.20.22.2.22");
        printEntries(encounters);
        return e;
    }

    public OMElement t2Encounters(OMElement e) throws Exception {
        Iterator<OMElement> it = e.getChildElements();

        OMElement allergiesIntolerances = scanToSection(e, "2.16.840.1.113883.10.20.22.2.22");
        stripEntries(allergiesIntolerances, "2007", "2010");
        printEntries(allergiesIntolerances);
        return e;
    }

    public OMElement scanToSection(OMElement documentRoot, String templateId) throws Exception {
        OMElement rtnElememt = null;

        javax.xml.namespace.QName qname = documentRoot.getQName();
        QName componentQName = new QName(qname.getNamespaceURI(), "component");
        QName structuredBodyQName = new QName(qname.getNamespaceURI(), "structuredBody");
        QName templateIdQName = new QName(qname.getNamespaceURI(), "templateId");
        QName rootQName = new QName(qname.getNamespaceURI(), "root");
        QName root = new QName("root");

        OMElement component = documentRoot.getFirstChildWithName(componentQName);
        OMElement structuredBody = component.getFirstChildWithName(structuredBodyQName);

        Iterator<OMElement> componentIterator = structuredBody.getChildElements();
        while (rtnElememt == null && componentIterator.hasNext()) {
            OMElement section = componentIterator.next().getFirstElement();
            OMElement templateIdElement = section.getFirstChildWithName(templateIdQName);
            String z = templateIdElement.getAttributeValue(root);
            String y = z + "x";
            if (z.equals(templateId)) {
                rtnElememt = section;
            }
        }
        return rtnElememt;
    }


    private void printEntries(OMElement sectionElement) throws Exception {
        javax.xml.namespace.QName qname = sectionElement.getQName();
        QName titleQName = new QName(qname.getNamespaceURI(), "title");
        QName entryQName = new QName(qname.getNamespaceURI(), "entry");
        QName effectiveTimeQName = new QName(qname.getNamespaceURI(), "effectiveTime");
        QName lowQName = new QName(qname.getNamespaceURI(), "low");
        QName valueQName = new QName("value");

        OMElement titleElement = sectionElement.getFirstChildWithName(titleQName);
        if (titleElement != null) {
            System.out.println("Title: " + titleElement.getText());
        }

        Iterator<OMElement> entryIterator = sectionElement.getChildrenWithName(entryQName);
        while (entryIterator.hasNext()) {
            OMElement element = entryIterator.next();
            element = element.getFirstElement();

            String timeValue = "";
            String label = "";
            OMElement effectiveTime = element.getFirstChildWithName(effectiveTimeQName);
            if (effectiveTime.getAttributeValue(valueQName) != null) {
                timeValue = effectiveTime.getAttributeValue(valueQName);
                label = "Effective time: ";
            } else {
                OMElement lowTime = effectiveTime.getFirstChildWithName(lowQName);
                timeValue = lowTime.getAttributeValue(valueQName);
                label = "Effective time (low): ";
            }

            System.out.println(label + timeValue);
        }
    }

    private void stripEntries(OMElement sectionElement, String filterLow, String filterHigh) throws Exception {
        javax.xml.namespace.QName qname = sectionElement.getQName();
        QName entryQName = new QName(qname.getNamespaceURI(), "entry");

        QName effectiveTimeQName = new QName(qname.getNamespaceURI(), "effectiveTime");
        QName lowQName = new QName(qname.getNamespaceURI(), "low");
        QName valueQName = new QName("value");

        Iterator<OMElement> elementIterator = sectionElement.getChildrenWithName(entryQName);
        while (elementIterator.hasNext()) {
            OMElement entry = elementIterator.next();
            OMElement element = entry.getFirstElement();

            OMElement effectiveTime = element.getFirstChildWithName(effectiveTimeQName);
            String timeValue = "";

            if (effectiveTime.getAttributeValue(valueQName) != null) {
                timeValue = effectiveTime.getAttributeValue(valueQName);
            } else {
                OMElement lowTime = effectiveTime.getFirstChildWithName(lowQName);
                timeValue = lowTime.getAttributeValue(valueQName);
            }

//            System.out.println("Low Time Value: " + timeValue);

            if (timeValue.compareTo(filterLow) < 0 || timeValue.compareTo(filterHigh) > 0) {
//                System.out.println("Discard based on date/time: " + timeValue + " " + filterLow + " : " + filterHigh);
                entry.discard();
            }
        }
    }

    private void replaceSectionText(OMElement sectionElement, String newText) throws Exception {
        javax.xml.namespace.QName qname = sectionElement.getQName();
        QName textQName = new QName(qname.getNamespaceURI(), "text");

        OMElement textElement = sectionElement.getFirstChildWithName(textQName);
        if (textElement != null) {
            Iterator<OMElement> iterator = textElement.getChildElements();
            while (iterator.hasNext()) {
                OMElement e = iterator.next();
                e.detach();
            }
            textElement.setText(newText);
        }
    }

}

