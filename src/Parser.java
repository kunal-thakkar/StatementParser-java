import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

public class Parser {
	
	public enum StatementTypes {
		HDFC, CITI, SBI, ICICI
	}
	
	interface Logger {
		void log(String str);
	}

	public static void parseStatements(File[] list, String password, StatementTypes type, Logger logger) throws InvalidPasswordException, FileNotFoundException, IOException{
		for(int i = 0; i < list.length; i++){
	        switch (type) {
	        case ICICI:
				parseIciciStatement(
					PDDocument.load(new BufferedInputStream(new FileInputStream(list[i])), password), logger
				);
	        	break;
			case CITI:	//CITI
				parseCitiStatement(
					PDDocument.load(new BufferedInputStream(new FileInputStream(list[i])), password), logger
				);
				break;
			case HDFC:	//HDFC
				parseHdfcStatement(
					PDDocument.load(new BufferedInputStream(new FileInputStream(list[i])), password), logger
				);
				break;
			case SBI: //SBI
				parseSbiStatement(
					PDDocument.load(new BufferedInputStream(new FileInputStream(list[i])), password), logger
				);
				break;
			default:
				break;
			}
		}
	}

	private static void parseIciciStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split("\r\n"), linePart, datePart;
        String desc, line, tempLine = "";
        int len;
        boolean inTable = false, isComplete = true;
        for(int i = 0; i < lines.length; i++){
        	line = lines[i].trim();
        	if(line.startsWith("TRANSACTION DETAILS") || 
    			line.startsWith("Card Number : ") ||
    			line.startsWith("Great offers on your card")
        	){
        		inTable = false;
        		continue;
        	}
        	else if(line.equals("Ref. Number")){
        		inTable = true;
        		continue;
        	}
        	if(inTable){
            	if(lines[i].matches("^?\\d{2,9}/\\d{2}/\\d{4}.*$")){
            		if(!line.matches(".*\\d{1,5}\\.\\d{1,10}.*")){
            			isComplete = false;
            			tempLine += line + " ";
            		}
            		else {
            			isComplete = true;
            		}
            	}
            	else {
            		if(!isComplete){
            			tempLine += " " + line;
                		if(line.matches(".*\\d{1,5}\\.\\d{1,10}.*")){
                			isComplete = true;
                			line = tempLine;
                			tempLine = "";
                		}
            		}
            	}
            	if(isComplete){
            		linePart = line.split(" ");
            		datePart = linePart[0].split("/");
            		len = datePart[0].length();
            		if(len > 2) datePart[0] = datePart[0].substring(len - 2);
            		len = linePart.length;
            		if(linePart[len-1].indexOf("CR") != -1){
                		linePart[len-2] += "Cr";
                		len--;
            		}
            		else {
            			linePart[len-1] = linePart[len-1].substring(0, linePart[len-1].lastIndexOf(".") + 3);
            		}
            		desc = linePart[1];
            		for(int c = 2; c < len-1; c++){
            			desc += " " + linePart[c];
            		}
            		logger.log(String.format("%s-%s-%s,%s,%s", datePart[2], datePart[1], datePart[0], desc.replaceAll(",", " ").trim(), linePart[len-1].replaceAll(",", "")));
            	}
        	}
        }
        pdf.close();
	}
	
	private static void parseSbiStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split("\r\n"), linePart;
        String desc, line, tempLine = "";
        int len;
        boolean is_split_line = false, lineFound = false;
        for(int i = 0; i < lines.length; i++){
        	line = lines[i].trim(); 
        	if(is_split_line){
        		tempLine += " " + line;
        		if(tempLine.endsWith(" D") || tempLine.endsWith(" C")){
        			is_split_line = false;
        			line = tempLine;
        			lineFound = true;
        		}
        	}
        	else if(line.matches("\\d{2}\\s(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s\\d{2}.*$")){
        		if(line.length() == 11){
        			continue;
        		}
        		else if(line.length() == 9){
        			is_split_line = true;
        			tempLine = line;
        			lineFound = false;
        			continue;
        		}
        		else{
        			lineFound = true;
        		}
        	}
        	else {
        		lineFound = false;
        	}
            if(lineFound) {
    			line = line.substring(0, line.length() - 2) + (line.endsWith(" C")?"Cr":"");
        		linePart = line.split(" ");
        		desc = "";
        		len = linePart.length;
        		desc = linePart[3];
        		for(int c = 4; c < len -1; c++){
        			if(linePart[c].length() != 0)
        			desc += " " + linePart[c];
        		}
        		logger.log(String.format("20%s-%s-%s,%s,%s", linePart[2], linePart[1], linePart[0], desc.replaceAll(",", " "), linePart[len-1].replaceAll(",", "")));
        	}
        }
	}

	private static void parseHdfcStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split("\r\n");
        String[] linePart, datePart;
        String desc, line;
        int len;
        boolean is_first_match = true;
        for(int i = 0; i < lines.length; i++){
        	line = lines[i].trim(); 
        	if(line.matches("\\d{2}/\\d{2}/\\d{4}.*$")){
        		if(is_first_match){
        			is_first_match = false;
        			continue;
        		}
        		if(line.endsWith(" Cr")) {
        			line = line.replace(" Cr", "CR");
        		}
        		linePart = line.split(" ");
        		datePart = linePart[0].split("/");
        		desc = "";
        		len = linePart.length;
        		desc = linePart[1];
        		for(int c = 2; c < len -1; c++){
        			if(linePart[c].length() != 0)
        			desc += " " + linePart[c];
        		}
        		logger.log(String.format("%s-%s-%s\t%s\t%s", datePart[2], datePart[1], datePart[0], desc.replaceAll(",", " "), linePart[len-1].replaceAll(",", "")));
        	}
        }
        pdf.close();
	}
	
	private static void parseCitiStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split("\r\n");
        String[] linePart, datePart;
        String desc;
        int year = 0, len;
        for(int i = 0; i < lines.length; i++){
        	if(lines[i].matches("^\\d{2}/\\d{2} .*$")){
        		linePart = lines[i].split(" ");
        		datePart = linePart[0].split("/");
        		desc = "";
        		len = linePart.length;
        		desc = linePart[1];
        		for(int c = 2; c < len -1; c++){
        			if(linePart[c].length() != 0)
        			desc += " " + linePart[c];
        		}
        		logger.log(String.format("20%d-%s-%s\t%s\t%s", year, datePart[1], datePart[0], desc, linePart[len-1]));
        	}
        	else if(lines[i].matches("^\\d{2}/\\d{2}/\\d{2}.*$")){
        		year = Integer.parseInt(lines[i].split("/")[2].trim());
        	}
        }
        pdf.close();
	}


}
