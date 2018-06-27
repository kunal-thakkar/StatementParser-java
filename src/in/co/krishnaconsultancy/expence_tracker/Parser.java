package in.co.krishnaconsultancy.expence_tracker;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

public class Parser {
	
	static String newLineSeparator = System.getProperty("line.separator");
	static List<String> months = Arrays.asList("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec");
	static {
		
	}
		
	public enum StatementTypes {
		HDFC, CITI, SBI, ICICI, StandardChartered
	}
	
	public interface Logger {
		void log(Entries entries);
	}
	
	public static void parseStatements(InputStream is, String password, StatementTypes type, Logger logger){
        try {
			switch (type) {
				case StandardChartered:
				parseScStatement(
				PDDocument.load(new BufferedInputStream(is), password), logger
				);
				break;
				case ICICI:
				parseIciciStatement(
				PDDocument.load(new BufferedInputStream(is), password), logger
				);
				break;
				case CITI:	//CITI
				parseCitiStatement(
				PDDocument.load(new BufferedInputStream(is), password), logger
				);
				break;
				case HDFC:	//HDFC
				parseHdfcStatement(
				PDDocument.load(new BufferedInputStream(is), password), logger
				);
				break;
				case SBI: //SBI
				parseSbiStatement(
				PDDocument.load(new BufferedInputStream(is), password), logger
				);
				break;
				default:
				break;
			}
			} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
		}
	}
	
	public static void parseStatements(File[] list, String password, StatementTypes type, Logger logger) {
		for(int i = 0; i < list.length; i++){
	        try {
				parseStatements(new FileInputStream(list[i]), password, type, logger);
				} catch (Exception e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				System.out.println(sw.toString());
			}
		}
	}
	
	private static void parseScStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split(newLineSeparator), linePart;
        String desc, line, transactionType;
        int len;
        for(int i = 0; i < lines.length; i++){
        	line = lines[i];
        	if(line.length() > 30 && line.matches(".*\\d{1,15}\\.\\d{2}(CR)?\\d{6}.*")){
				line = line.substring(28).trim();
        		if(line.endsWith("CR")){
        			transactionType = "cr";
        			line = line.substring(0, line.length() - 2);
				}
        		else {
        			transactionType = "dr";
				}
        		linePart = line.split(" ");
        		desc = "";
        		len = linePart.length;
        		desc = linePart[1];
        		for(int c = 2; c < len; c++){
        			if(linePart[c].length() != 0)
        			desc += " " + linePart[c];
				}
        		linePart[0] = linePart[0].replaceAll(",", "");
        		len = linePart[0].length();
        		logger.log(new Entries(linePart[0].substring(len-2, len), 
				linePart[0].substring(len-4, len-2), 
				linePart[0].substring(len-6, len-4), desc.replaceAll(",", " "), 
				Double.valueOf(linePart[0].substring(0, len-6)), transactionType, "SC"));
			}
        	else if(line.matches("^\\d{6}.*.\\d{1,15}\\.\\d{2}(CR|$)")){
        		line = line.replaceAll("\\s+", " ");
        		if(line.endsWith("CR")){
        			transactionType = "cr";
        			line = line.substring(0, line.length() - 2);
				}
        		else {
        			transactionType = "dr";
				}
        		linePart = line.split(" ");
        		desc = "";
        		len = linePart.length;
        		desc = linePart[1];
        		for(int c = 2; c < len - 1; c++){
        			if(linePart[c].length() != 0)
        			desc += " " + linePart[c];
				}
        		linePart[0] = linePart[0].replaceAll(",", "");
        		logger.log(new Entries("20"+linePart[0].substring(4, 6), 
				linePart[0].substring(2, 4), 
				linePart[0].substring(0, 2), desc.replaceAll(",", " "), 
				Double.valueOf(linePart[len-1].replaceAll(",", "")), transactionType, "SC"));
			}
		}
        pdf.close();
	}
	
	private static void parseIciciStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split(newLineSeparator), linePart, datePart;
        String desc, line, tempLine = "", transactionType;
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
						transactionType = "cr";
                		len--;
					}
            		else {
						transactionType = "dr";
            			linePart[len-1] = linePart[len-1].substring(0, linePart[len-1].lastIndexOf(".") + 3);
					}
            		desc = linePart[1];
            		for(int c = 2; c < len-1; c++){
            			desc += " " + linePart[c];
					}
					logger.log(new Entries(datePart[2], datePart[1], datePart[0], desc.replaceAll(",", " "), 
					Double.valueOf(linePart[len-1].replaceAll(",", "")), transactionType, "ICICI"));
				}
			}
		}
        pdf.close();
	}
	
	private static void parseSbiStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split(newLineSeparator), linePart;
        String desc, line, tempLine = "", transactionType;
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
				transactionType = line.endsWith(" C")?"cr":"dr";
				line = line.substring(0, line.length() - 2);
        		linePart = line.split(" ");
        		desc = "";
        		len = linePart.length;
        		desc = linePart[3];
        		for(int c = 4; c < len -1; c++){
        			if(linePart[c].length() != 0)
        			desc += " " + linePart[c];
				}
				logger.log(new Entries("20"+linePart[2], String.valueOf(months.indexOf(linePart[1])+1), linePart[0], desc.replaceAll(",", " "), Double.valueOf(linePart[len-1].replaceAll(",", "")), transactionType, "SBI"));
			}
		}
        pdf.close();
	}
	
	private static void parseHdfcStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split(newLineSeparator);
        String[] linePart, datePart;
        String desc, line, transactionType;
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
					transactionType = "cr";
					line = line.substring(0, line.length() - 3);
				}
				else {
					transactionType = "dr";
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
				logger.log(new Entries(datePart[2], datePart[1], datePart[0], desc.replaceAll(",", " "), Double.valueOf(linePart[len-1].replaceAll(",", "")), transactionType, "HDFC"));
			}
		}
        pdf.close();
	}
	
	private static void parseCitiStatement(PDDocument pdf, Logger logger) throws InvalidPasswordException, IOException{
        PDFTextStripper reader = new PDFTextStripper();
        String pageText = reader.getText(pdf);
        String[] lines = pageText.split(newLineSeparator);
        String[] linePart, datePart;
        String desc, transactionType;
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
				if(linePart[len-1].endsWith("CR")){
					transactionType = "cr";
					linePart[len-1] = linePart[len-1].substring(0, linePart[len-1].length()-2); 
				}
				else {
					transactionType = "dr";
				}
				logger.log(new Entries("20" + year, datePart[1], datePart[0], desc, Double.valueOf(linePart[len-1]), transactionType, "CITI"));
			}
        	else if(lines[i].matches("^\\d{2}/\\d{2}/\\d{2}.*$")){
        		year = Integer.parseInt(lines[i].split("/")[2].trim());
			}
		}
        pdf.close();
	}
	
	
}
