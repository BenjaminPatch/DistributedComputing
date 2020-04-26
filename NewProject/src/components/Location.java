package components;

import java.util.List;

public class Location {
	String name;
	String locCode;
	List<String> info;
	List<String> services;

	public Location(String name, String locCode, List<String> info, List<String> services) {
		this.name = name;
		this.locCode = locCode;
		this.info = info;
		this.services = services;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setLocCode(String code) {
		this.locCode = code;
	}

	public void setInfo(List<String> info) {
		this.info = info;
	}

	public void addToInfo(String newLine) {
		this.info.add(newLine);
	}

	public void setServices(List<String> services) {
		this.services = services;
	}

	public void addToServices(String newService) {
		this.services.add(newService);
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getLocCode() {
		return this.locCode;
	}
	
	public List<String> getInfo() {
		return this.info;
	}
	
	public List<String> getServices() {
		return this.services;
	}
}
