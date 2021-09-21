package types;

import java.text.DecimalFormat;
import java.text.ParseException;

public class Balance implements Comparable<Balance> {
	public static final Balance NOT_SUPPORTED = new Balance("*Not Supported*",0);
	public double balance = -1;
	String balanceStr = "";
	
	public Balance() {
		
	}
	
	public Balance(String s, double amt) {
		this.balanceStr = s;
		this.balance = amt;
	}
	
	public Balance(double amt) {
		balance = amt;
		DecimalFormat df = new DecimalFormat("#,##0.######");
		balanceStr = df.format(balance);
	}
	
	public Balance(int amt) {
		balance = amt;
		DecimalFormat df = new DecimalFormat("#,##0.######");
		balanceStr = df.format(balance);
	}
	
	public Balance(String balanceStr) throws ParseException {
		/*
		NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
		Number n = nf.parse(balanceStr);
		balance = n.doubleValue();
		*/		
		balance = Double.parseDouble(balanceStr);
		DecimalFormat df = new DecimalFormat("#,##0.######");
		this.balanceStr = df.format(balance);
	}

	public String toString() {
		return balanceStr;
	}

	public int compareTo(Balance b) {
		return (int)(balance - b.balance);
	}

	public void add(double amount) {
		balance += amount;
		DecimalFormat df = new DecimalFormat("#,##0.######");
		balanceStr = df.format(balance);
	}

	public void add(Balance b) {
		balance += b.balance;
		DecimalFormat df = new DecimalFormat("#,##0.######");
		balanceStr = df.format(balance);
	}

}
