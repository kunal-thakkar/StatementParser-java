package in.co.krishnaconsultancy.expence_tracker;

import java.util.Calendar;
import java.util.Date;

public class Entries {
	static Calendar calendar = Calendar.getInstance();
	Date date;
	double amt;
	String desc, transactionType, toString, source;
	
	public Entries(String year, String month, String day, String desc, double amt, String transactionType, String source) {
		calendar.set(Integer.parseInt(year), Integer.parseInt(month)-1, Integer.parseInt(day));
		this.date = calendar.getTime();
		this.desc = desc;
		this.amt = amt;
		this.transactionType = transactionType;
		this.source = source;
		this.toString = String.format("%s\t%s-%s-%s\t%s\t%s\t%s", source, year, month, day, desc, amt, transactionType);
	}

	public Date getDate(){
		return this.date;
	}
	
	public String getDescription(){
		return this.desc;
	}
	
	public String toString(){
		return toString;
	}
}
