package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.WordValuation;

public class SDTQuery {
	
	private SuffixValuation suffValuation;
	private WordValuation wordValuation;
	
	public SDTQuery(WordValuation forWordVal, SuffixValuation andSuffVal) {
		this.suffValuation = andSuffVal;
		this.wordValuation = forWordVal;
	}
	
	public SuffixValuation getSuffValuation() {
		return suffValuation;
	}
	
	public WordValuation getWordValuation() {
		return wordValuation;
	}

	public void setSuffValuation(SuffixValuation suffValuation) {
		this.suffValuation = suffValuation;
	}

	private SDT sdt;

	public void setAnswer(SDT sdt) {
		this.sdt = sdt;
	}
	
	public SDT getAnswer() {
		return this.sdt;
	}

}
