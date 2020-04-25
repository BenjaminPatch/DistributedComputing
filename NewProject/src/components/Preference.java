package components;

import javafx.util.Pair;

public class Preference {
	private String name;
	private String medicalCondition;
	private Pair<Integer, String> pref1;
	private String pref2;
	private String pref3;
	public Preference(String name, String medicalCondition, 
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

	public void setName(String val) {
		this.name = val;
	}

	public String getMedicalCondition() {
		return medicalCondition;
	}

	public void setMedicalCondition(String val) {
		this.medicalCondition = val;
	}

	public Pair<Integer, String> getPref1() {
		return pref1;
	}

	public void setPref1(Pair<Integer, String> val) {
		this.pref1 = val;
	}

	public String getPref2() {
		return pref2;
	}

	public void setPref2(String val) {
		this.pref2 = val;
	}

	public String getPref3() {
		return pref3;
	}

	public void setPref3(String val) {
		this.pref3 = val;
	}
}
