package transaction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.ImageIcon;

import forks.Fork;
import forks.ForkView;
import main.ForkFarmer;
import types.Balance;
import types.Percentage;
import types.TimeU;
import util.FFUtil;
import util.Ico;
import util.Util;

public class Transaction {
	ImageIcon L_ARROW = Ico.loadIcon("icons/arrows/left.png");
	ImageIcon R_ARROW = Ico.loadIcon("icons/arrows/right.png");
	
	private static final Set<String> TSET = new HashSet<>();
	public static final List<Transaction> LIST = new ArrayList<>();
	
	public final Fork   f;
	public String hash = "" ;
	public Balance amount;
	public Balance value;
	public String target ="";
	public String date = "";
	public boolean blockReward;
	public TimeU lastWinTime = TimeU.BLANK;
	Percentage effort = Percentage.EMPTY;
	
	
	public Transaction(Fork f, String hash, double amount, String target, String date, boolean blockReward) {
		this.f = f;
		this.hash = hash;
		this.amount = new Balance(amount);
		this.target = target;
		this.date = date;
		this.blockReward = blockReward;
		this.updateValue();
		//new TimeU(date);
		
		if (blockReward && getTimeSince().inMinutes() < 5)
			effort = f.getEffort();
	}
	
	public void updateValue() {
		this.value = new Balance(amount.amt * f.price,2);
	}
	
	public ImageIcon getIco() {
		return blockReward ? Ico.TROPHY : null;
	}
	
	
	// copy constructor for TxReport
	public Transaction(Transaction t) {
		this.f = t.f;
		this.amount = new Balance(t.amount.amt);
	}

	public double getAmount() {
		return amount.amt;
	}
	
	public ImageIcon getIcon() {
		if (blockReward)
			return R_ARROW;
		
		if (null != f.wallet.addr)
			if (f.wallet.addr.equals(target))
				return R_ARROW;
		return L_ARROW;
	}
	
	public TimeU getTimeSince() {
		return TimeU.getTimeSince(LocalDateTime.parse(date, FFUtil.DTF));
	}
	
	public static Transaction load(Fork f) {
		Transaction newTX = null;
		
		int txRead = 0;
		if (-1 == f.wallet.index)
			return null;
		
		ForkFarmer.LOG.add(f.name + " getting transactions");
		
		Process p = null;
		PrintWriter pw = null;
		BufferedReader br = null;
		try {
			p = Util.startProcess(f.exePath, "wallet", "get_transactions");
			pw = new PrintWriter(p.getOutputStream());
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
			pw.println(f.wallet.index); // hack to 
			for (int i = 0; i < 50; i++)
				pw.println("c");
			pw.close();
			
			String l = null;
			while ( null != (l = br.readLine())) {
				boolean blockReward = false;
				if (l.contains("c to continue")) {
					pw.println("c");
					pw.flush();
				}
				
				if (l.startsWith("Connection error")) {
					p.destroyForcibly();
					break;
				}
				
				if (l.startsWith("Transaction ")) {
					txRead++;
					if (f.symbol.toUpperCase().equals("SIX") &&  txRead > 40) {
						p.destroyForcibly();
						break;
					}
					String tHash   = l.replace("Transaction ", "");
					if (TSET.contains(tHash)) // stop parsing if we already have this transaction
						continue;
					TSET.add(tHash);
					
					@SuppressWarnings("unused")
					String status  = br.readLine();
					String amountStr = br.readLine();
							
					if (amountStr.startsWith("Amount: "))
						amountStr = amountStr.substring("Amount: ".length());
					else if (amountStr.startsWith("Amount sent: "))
						amountStr = amountStr.substring("Amount sent: ".length());
					else if (amountStr.startsWith("Amount received: "))
						amountStr = amountStr.substring("Amount received: ".length());
					
					String firstWord = amountStr.substring(0, amountStr.indexOf(' '));
					firstWord.replace(",", ".");
					
					double amount = Double.parseDouble(firstWord);
					String address = br.readLine().substring(12);
					String date = br.readLine().substring(12);
					
					
					synchronized (Transaction.LIST) { 
 						Optional<Transaction> oT = LIST.stream().filter(z -> z.f.symbol.equals(f.symbol) && z.date.equals(date)).findAny();
					
						if (Math.abs(amount - f.fd.nftReward) < .02)
							blockReward = true;
						else if (firstWord.equals("1E-10")) // probably faucet?
							blockReward = true;
						else if (firstWord.equals("1E-7")) // probably faucet?
							blockReward = true;
					
						if (oT.isPresent()) {
							Transaction t = oT.get();
							newTX = t;
							t.amount.add(amount);
							t.updateValue();
							t.blockReward |= blockReward;
							if (Math.abs(t.getAmount() - f.fullReward) < .02)
								t.blockReward = true;
							if (t.effort == Percentage.EMPTY)
								t.effort = f.getEffort();
						} else {
							Transaction t = new Transaction(f, tHash,amount,address,date, blockReward);
							newTX = t;
							if (0 != amount)
								LIST.add(t);
						}
						
					}

				}

			}
				
		} catch (Exception e) {
			e.printStackTrace();
			f.lastException = e;
		}
		
		Util.closeQuietly(br);
		Util.closeQuietly(pw);
		Util.waitForProcess(p);
		
		update(f, newTX);
		
		ForkFarmer.LOG.add(f.name + " done getting transactions");

		return newTX;
		
	}
	
	private static void update(Fork f, Transaction newTX) {
		if (null == newTX)
			return;
		synchronized(Transaction.LIST) {
			// update fork last win handle
			if (null != f.lastWin && newTX.blockReward)
				newTX.lastWinTime = f.lastWin.getTimeSince();
				
			f.lastWin = LIST.stream()
				.filter(t -> f == t.f && t.blockReward)
				.reduce((a,b) -> a.getTimeSince().inMinutes() < b.getTimeSince().inMinutes() ? a:b).orElse(null);
		}
			
		ForkView.update(f);
		TransactionView.refresh();
	}
		
		
	public void browse() {
		
		String atbPath = f.fd.atbPath;
		if (null != atbPath) {
			String addr = target;
			Util.openLink("https://alltheblocks.net/" + atbPath + "/address/" + addr);
		}
	}

	public static void fromLog(Fork f, String s) {
		String timeStamp = s.substring(0,19);
		timeStamp = timeStamp.replace("T", " ");
		
		String txHAsh = Util.getWordAfter(s, "Farmed unfinished_block ");

		Transaction t = new Transaction(f,txHAsh,f.fd.fullReward,"Log Farming Reward",timeStamp,true);
		synchronized(Transaction.LIST) {
			LIST.add(t);
		}
		update(f,t);
		
	}
	
}
