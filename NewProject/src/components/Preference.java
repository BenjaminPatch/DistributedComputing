package components;

import javafx.util.Pair;

public class Preference {
	private String name;
	private int medicalCondition;
	private Pair<Integer, String> pref1;
	private String pref2;
	private String pref3;
	public Preference(String name, int medicalCondition, 
			Pair<Integer, String> pref1, String pref2, String pref3) {
		this.name = name;
		this.medicalCondition = medicalCondition;
		this.pref1 = pref1;
		this.pref2 = pref2;
		this.pref3 = pref3;
	}
	public String getName() {
		return name;
	}
	public int getMedicalCondition() {
		return medicalCondition;
	}
	public Pair<Integer, String> getPref1() {
		return pref1;
	}
	public String getPref2() {
		return pref2;
	}
	public String getPref3() {
		return pref3;
	}
}
